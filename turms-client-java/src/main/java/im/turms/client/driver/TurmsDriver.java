/*
 * Copyright (C) 2019 The Turms Project
 * https://github.com/turms-im/turms
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package im.turms.client.driver;

import com.google.protobuf.Descriptors;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import im.turms.client.util.StringUtil;
import im.turms.client.driver.service.ConnectionService;
import im.turms.client.driver.service.HeartbeatService;
import im.turms.client.driver.service.MessageService;
import im.turms.client.driver.service.SessionService;
import im.turms.client.model.ConnectOptions;
import im.turms.client.model.SessionDisconnectInfo;
import im.turms.client.model.SessionStatus;
import im.turms.client.model.UserLocation;
import im.turms.client.util.ProtoUtil;
import im.turms.client.util.TurmsBusinessExceptionUtil;
import im.turms.common.constant.DeviceType;
import im.turms.common.constant.UserStatus;
import im.turms.common.constant.statuscode.TurmsStatusCode;
import im.turms.common.exception.TurmsBusinessException;
import im.turms.common.model.dto.notification.TurmsNotification;
import im.turms.common.model.dto.request.TurmsRequest;
import java8.util.concurrent.CompletableFuture;
import java8.util.function.Consumer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author James Chen
 */
public class TurmsDriver {

    private static final Logger LOGGER = Logger.getLogger(TurmsDriver.class.getName());
    private static final String SCHEDULED_THREAD_NAME = "turms-scheduler";
    public static final ScheduledExecutorService scheduledService = new ScheduledThreadPoolExecutor(1, runnable -> {
        Thread t = new Thread(runnable);
        t.setName(SCHEDULED_THREAD_NAME);
        return t;
    });

    private Consumer<Void> onSessionConnected;
    private Consumer<SessionDisconnectInfo> onSessionDisconnected;
    private Consumer<SessionDisconnectInfo> onSessionClosed;

    private final StateStore stateStore;

    private final ConnectionService connectionService;
    private final HeartbeatService heartbeatService;
    private final MessageService messageService;
    private final SessionService sessionService;

    public TurmsDriver(@Nullable String websocketUrl,
                       @Nullable Integer connectTimeout,
                       @Nullable Integer requestTimeout,
                       @Nullable Integer minRequestInterval,
                       @Nullable Integer heartbeatInterval,
                       @Nullable Boolean storePassword) {
        stateStore = new StateStore();

        connectionService = initConnectionService(websocketUrl, connectTimeout, storePassword);
        heartbeatService = new HeartbeatService(stateStore, minRequestInterval, heartbeatInterval);
        messageService = new MessageService(stateStore, requestTimeout, minRequestInterval);
        sessionService = initSessionService();
    }

    // Initializers

    private ConnectionService initConnectionService(String websocketUrl, Integer connectTimeout, Boolean storePassword) {
        ConnectionService service = new ConnectionService(stateStore, websocketUrl, connectTimeout, storePassword);
        service.addOnConnectedListener(unused -> onConnectionConnected());
        service.addOnDisconnectedListener(this::onConnectionDisconnected);
        service.addOnClosedListener(this::onConnectionClosed);
        service.addOnMessageListener(this::onMessage);
        return service;
    }

    private SessionService initSessionService() {
        SessionService service = new SessionService(stateStore);
        service.addOnSessionConnectedListeners(unused -> {
            if (onSessionConnected != null) {
                onSessionConnected.accept(null);
            }
        });
        service.addOnSessionDisconnectedListeners(sessionDisconnectInfo -> {
            if (onSessionDisconnected != null) {
                onSessionDisconnected.accept(sessionDisconnectInfo);
            }
        });
        service.addOnSessionClosedListeners(sessionDisconnectInfo -> {
            if (onSessionClosed != null) {
                onSessionClosed.accept(sessionDisconnectInfo);
            }
        });
        return service;
    }

    // Session Service

    public SessionStatus getStatus() {
        return sessionService.getStatus();
    }

    public boolean isConnected() {
        return sessionService.isConnected();
    }

    public boolean isClosed() {
        return sessionService.isClosed();
    }

    public void setOnSessionConnected(Consumer<Void> listener) {
        onSessionConnected = listener;
    }

    public void setOnSessionDisconnected(Consumer<SessionDisconnectInfo> listener) {
        onSessionDisconnected = listener;
    }

    public void setOnSessionClosed(Consumer<SessionDisconnectInfo> listener) {
        onSessionClosed = listener;
    }

    // Heartbeat Service

    public void startHeartbeat() {
        heartbeatService.start();
    }

    public void stopHeartbeat() {
        heartbeatService.stop();
    }

    public void resetHeartbeat() {
        heartbeatService.reset();
    }

    public CompletableFuture<Void> sendHeartbeat() {
        return heartbeatService.send();
    }

    // Connection Service

    public CompletableFuture<Void> connect(long userId, @NotNull String password) {
        return connect(userId, password, null, null, null);
    }

    public CompletableFuture<Void> connect(
            long userId,
            @NotNull String password,
            @Nullable DeviceType deviceType) {
        return connect(userId, password, deviceType, null, null);
    }

    public CompletableFuture<Void> connect(
            long userId,
            @NotNull String password,
            @Nullable DeviceType deviceType,
            @Nullable UserStatus userOnlineStatus) {
        return connect(userId, password, deviceType, userOnlineStatus, null);
    }

    public CompletableFuture<Void> connect(
            long userId,
            @NotNull String password,
            @Nullable DeviceType deviceType,
            @Nullable UserStatus userOnlineStatus,
            @Nullable UserLocation location) {
        return connect(null, userId, password, deviceType, userOnlineStatus, location);
    }

    public CompletableFuture<Void> connect(
            @Nullable Integer connectTimeout,
            long userId,
            @NotNull String password,
            @Nullable DeviceType deviceType,
            @Nullable UserStatus userOnlineStatus,
            @Nullable UserLocation userLocation) {
        return connect(null,
                connectTimeout,
                userId,
                password,
                deviceType,
                userOnlineStatus,
                userLocation);
    }

    public CompletableFuture<Void> connect(
            @Nullable String wsUrl,
            @Nullable Integer connectTimeout,
            long userId,
            @NotNull String password,
            @Nullable DeviceType deviceType,
            @Nullable UserStatus userOnlineStatus,
            @Nullable UserLocation userLocation) {
        return connectionService.connect(wsUrl,
                connectTimeout,
                userId,
                password,
                deviceType,
                userOnlineStatus,
                userLocation);
    }

    public CompletableFuture<Void> connect(ConnectOptions options) {
        return connectionService.connect(options);
    }

    public CompletableFuture<Void> disconnect() {
        return connectionService.disconnect();
    }

    public CompletableFuture<Void> reconnect(String host) {
        return connectionService.reconnect(host);
    }

    // Message Service

    public CompletableFuture<TurmsNotification> send(TurmsRequest.Builder requestBuilder) {
        return messageService.sendRequest(requestBuilder);
    }

    public CompletableFuture<TurmsNotification> send(Message.Builder builder, @Nullable Map<String, ?> fields) {
        try {
            ProtoUtil.fillFields(builder, fields);
        } catch (Exception e) {
            return TurmsBusinessExceptionUtil.getFuture(TurmsStatusCode.INVALID_DATA, e);
        }
        Descriptors.Descriptor descriptor = builder.getDescriptorForType();
        String fieldName = StringUtil.camelToSnakeCase(descriptor.getName());
        TurmsRequest.Builder requestBuilder = TurmsRequest.newBuilder();
        Descriptors.Descriptor requestDescriptor = requestBuilder.getDescriptorForType();
        Descriptors.FieldDescriptor fieldDescriptor = requestDescriptor.findFieldByName(fieldName);
        requestBuilder.setField(fieldDescriptor, builder.build());
        return messageService.sendRequest(requestBuilder);
    }

    public void addOnNotificationListener(Consumer<TurmsNotification> listener) {
        messageService.addOnNotificationListener(listener);
    }

    // Intermediary functions as a mediator between services

    private void onConnectionConnected() {
        heartbeatService.start();
        sessionService.notifyOnSessionConnectedListeners();
    }

    private void onConnectionDisconnected(SessionDisconnectInfo info) {
        heartbeatService.stop();
        heartbeatService.rejectHeartbeatFutures(TurmsBusinessException.get(TurmsStatusCode.CLIENT_SESSION_HAS_BEEN_CLOSED));
    }

    private void onConnectionClosed(SessionDisconnectInfo info) {
        sessionService.notifyOnSessionClosedListeners(info);
    }

    private void onMessage(ByteBuffer byteBuffer) {
        if (byteBuffer.hasRemaining()) {
            TurmsNotification notification;
            try {
                notification = TurmsNotification.parseFrom(byteBuffer);
            } catch (InvalidProtocolBufferException e) {
                LOGGER.log(Level.SEVERE, "Failed to parse TurmsNotification", e);
                return;
            }
            boolean isSessionInfo = notification.hasData() && notification.getData().hasSession();
            if (isSessionInfo) {
                sessionService.setSessionId(notification.getData().getSession().getSessionId());
            }
            messageService.didReceiveNotification(notification);
        } else {
            heartbeatService.completeHeartbeatFutures();
        }
    }

}
