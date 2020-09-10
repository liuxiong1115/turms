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
import im.turms.client.common.StringUtil;
import im.turms.client.driver.service.ConnectionService;
import im.turms.client.driver.service.HeartbeatService;
import im.turms.client.driver.service.MessageService;
import im.turms.client.driver.service.SessionService;
import im.turms.client.model.SessionDisconnectInfo;
import im.turms.client.model.SessionStatus;
import im.turms.client.model.UserLocation;
import im.turms.client.util.ProtoUtil;
import im.turms.common.constant.DeviceType;
import im.turms.common.constant.UserStatus;
import im.turms.common.constant.statuscode.TurmsStatusCode;
import im.turms.common.exception.TurmsBusinessException;
import im.turms.common.model.dto.notification.TurmsNotification;
import im.turms.common.model.dto.request.TurmsRequest;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author James Chen
 */
public class TurmsDriver {

    private static final Logger LOGGER = Logger.getLogger(TurmsDriver.class.getName());

    private Consumer<Void> onSessionConnected;
    private Consumer<SessionDisconnectInfo> onSessionDisconnected;
    private Consumer<SessionDisconnectInfo> onSessionClosed;

    private String websocketUrl = "ws://localhost:9510";
    private Duration connectionTimeout = Duration.ofSeconds(10);

    private final StateStore stateStore;

    private final ConnectionService connectionService;
    private final HeartbeatService heartbeatService;
    private final MessageService messageService;
    private final SessionService sessionService;

    public TurmsDriver(@Nullable String websocketUrl,
                       @Nullable Duration connectionTimeout,
                       @Nullable Duration minRequestsInterval) {
        if (websocketUrl != null) {
            this.websocketUrl = websocketUrl;
        }
        if (connectionTimeout != null) {
            this.connectionTimeout = connectionTimeout;
        }

        stateStore = new StateStore();

        connectionService = initConnectionService();
        heartbeatService = new HeartbeatService(stateStore, minRequestsInterval, null);
        messageService = new MessageService(stateStore, minRequestsInterval);
        sessionService = initSessionService();
    }

    // Initializers

    private ConnectionService initConnectionService() {
        ConnectionService service = new ConnectionService(stateStore);
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

    public CompletableFuture<Void> sendHeartbeat() {
        return heartbeatService.send();
    }

    public void resetHeartBeatTimer() {
        heartbeatService.reset();
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
            @Nullable UserLocation userLocation) {
        return connectionService.connect(websocketUrl,
                connectionTimeout,
                userId,
                password,
                deviceType,
                userOnlineStatus,
                userLocation);
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

    public CompletableFuture<TurmsNotification> send(Message.Builder builder, Map<String, ?> fields) {
        if (fields != null) {
            ProtoUtil.fillFields(builder, fields);
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
        heartbeatService.rejectHeartbeatCallbacks(TurmsBusinessException.get(TurmsStatusCode.CLIENT_SESSION_HAS_BEEN_CLOSED));
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
                LOGGER.log(Level.SEVERE, "", e);
                return;
            }
            boolean isSessionInfo = notification.hasData() && notification.getData().hasSession();
            if (isSessionInfo) {
                sessionService.setSessionId(notification.getData().getSession().getSessionId());
            }
            messageService.triggerOnNotificationReceived(notification);
        } else {
            heartbeatService.notifyHeartbeatCallbacks();
        }
    }

}
