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

package im.turms.client.driver.service;

import im.turms.client.driver.StateStore;
import im.turms.client.model.ConnectOptions;
import im.turms.client.model.SessionDisconnectInfo;
import im.turms.client.model.UserLocation;
import im.turms.common.constant.DeviceType;
import im.turms.common.constant.UserStatus;
import im.turms.common.constant.statuscode.SessionCloseStatus;
import im.turms.common.constant.statuscode.TurmsStatusCode;
import im.turms.common.exception.TurmsBusinessException;
import im.turms.common.util.RandomUtil;
import java8.util.concurrent.CompletableFuture;
import java8.util.function.Consumer;
import okhttp3.*;
import okio.ByteString;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;
import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;

/**
 * @author James Chen
 */
public class ConnectionService {

    private static final String HEADER_REASON = "X-API-Reason";

    public static final String REQUEST_ID_FIELD = "rid";
    public static final String USER_ID_FIELD = "uid";
    public static final String PASSWORD_FIELD = "pwd";
    public static final String DEVICE_TYPE_FIELD = "dt";
    public static final String USER_ONLINE_STATUS_FIELD = "us";
    public static final String USER_LOCATION_FIELD = "loc";
    private static final String LOCATION_DELIMITER = ":";

    private static final String DEFAULT_WEBSOCKET_URL = "ws://localhost:9510";
    private static final int DEFAULT_CONNECT_TIMEOUT = 30 * 1000;

    private final StateStore stateStore;
    private final String initialWsUrl;
    private final Integer initialConnectTimeout;
    private final boolean storePassword;

    private OkHttpClient httpClient;
    private ConnectOptions connectOptions;

    private boolean isClosedByClient;
    private final ConcurrentLinkedQueue<CompletableFuture<Void>> disconnectFutures = new ConcurrentLinkedQueue<>();

    private final List<Consumer<Void>> onConnectedListeners = new LinkedList<>();
    private final List<Consumer<SessionDisconnectInfo>> onDisconnectedListeners = new LinkedList<>();
    private final List<Consumer<SessionDisconnectInfo>> onClosedListeners = new LinkedList<>();
    private final List<Consumer<ByteBuffer>> onMessageListeners = new LinkedList<>();

    public ConnectionService(StateStore stateStore, @Nullable String wsUrl, @Nullable Integer connectTimeout, @Nullable Boolean storePassword) {
        this.stateStore = stateStore;
        initialWsUrl = wsUrl != null ? wsUrl : DEFAULT_WEBSOCKET_URL;
        initialConnectTimeout = connectTimeout != null ? connectTimeout : DEFAULT_CONNECT_TIMEOUT;
        this.storePassword = storePassword == null || storePassword;
        httpClient = new OkHttpClient.Builder()
                .build();
    }

    private void resetStates() {
        this.stateStore.setConnectionRequestId(null);
        isClosedByClient = false;
        completeDisconnectFutures();
    }

    // Listeners

    public void addOnConnectedListener(Consumer<Void> listener) {
        onConnectedListeners.add(listener);
    }

    public void addOnDisconnectedListener(Consumer<SessionDisconnectInfo> listener) {
        onDisconnectedListeners.add(listener);
    }

    public void addOnClosedListener(Consumer<SessionDisconnectInfo> listener) {
        onClosedListeners.add(listener);
    }

    public void addOnMessageListener(Consumer<ByteBuffer> listener) {
        onMessageListeners.add(listener);
    }

    private void notifyOnConnectedListeners() {
        for (Consumer<Void> listener : onConnectedListeners) {
            listener.accept(null);
        }
    }

    private void notifyOnDisconnectedListeners(SessionDisconnectInfo info) {
        for (Consumer<SessionDisconnectInfo> listener : onDisconnectedListeners) {
            listener.accept(info);
        }
    }

    private void notifyOnClosedListeners(SessionDisconnectInfo info) {
        for (Consumer<SessionDisconnectInfo> listener : onClosedListeners) {
            listener.accept(info);
        }
    }

    private void notifyOnMessageListeners(ByteBuffer message) {
        for (Consumer<ByteBuffer> listener : onMessageListeners) {
            listener.accept(message);
        }
    }

    private void completeDisconnectFutures() {
        while (!disconnectFutures.isEmpty()) {
            disconnectFutures.poll().complete(null);
        }
    }

    // Connection

    public CompletableFuture<Void> connect(
            @Nullable String wsUrl,
            @Nullable Integer connectTimeout,
            long userId,
            @NotNull String password,
            @Nullable DeviceType deviceType,
            @Nullable UserStatus userOnlineStatus,
            @Nullable UserLocation userLocation) {
        wsUrl = wsUrl != null
                ? wsUrl
                : initialWsUrl;
        connectTimeout = connectTimeout != null
                ? connectTimeout
                : initialConnectTimeout;
        connectOptions = new ConnectOptions()
                .wsUrl(wsUrl)
                .connectTimeout(connectTimeout)
                .userId(userId)
                .password(password)
                .deviceType(deviceType)
                .userOnlineStatus(userOnlineStatus)
                .location(userLocation);
        return connect(connectOptions);
    }

    public CompletableFuture<Void> connect(ConnectOptions options) {
        CompletableFuture<Void> loginFuture = new CompletableFuture<>();
        if (stateStore.isConnected()) {
            loginFuture.completeExceptionally(TurmsBusinessException.get(TurmsStatusCode.CLIENT_SESSION_ALREADY_ESTABLISHED));
            return loginFuture;
        } else {
            resetStates();
            boolean isConnectTimeoutChanged;
            // TODO: redirect connect timeout
            Integer connectTimeout = options.connectTimeout();
            if (connectTimeout != null) {
                isConnectTimeoutChanged = connectTimeout != httpClient.connectTimeoutMillis();
            } else {
                isConnectTimeoutChanged = httpClient.connectTimeoutMillis() > 0;
            }
            if (isConnectTimeoutChanged) {
                int timeout = connectTimeout != null ? connectTimeout : 0;
                httpClient = httpClient.newBuilder()
                        .connectTimeout(timeout, TimeUnit.MILLISECONDS)
                        .build();
            }
            long connectionRequestId = RandomUtil.nextPositiveLong();
            Request.Builder requestBuilder = new Request.Builder()
                    .url(options.wsUrl())
                    .header(REQUEST_ID_FIELD, String.valueOf(connectionRequestId))
                    .header(USER_ID_FIELD, String.valueOf(options.userId()))
                    .header(PASSWORD_FIELD, options.password());
            if (options.deviceType() != null) {
                requestBuilder.header(DEVICE_TYPE_FIELD, options.deviceType().name());
            }
            if (options.userOnlineStatus() != null) {
                requestBuilder.header(USER_ONLINE_STATUS_FIELD, options.userOnlineStatus().toString());
            }
            UserLocation userLocation = options.location();
            if (userLocation != null) {
                String location = String.format("%f%s%f", userLocation.getLongitude(), LOCATION_DELIMITER, userLocation.getLatitude());
                requestBuilder.header(USER_LOCATION_FIELD, location);
            }
            if (!storePassword) {
                options.password(null);
            }
            WebSocket websocket = httpClient.newWebSocket(requestBuilder.build(), new WebSocketListener() {
                @Override
                public void onOpen(@org.jetbrains.annotations.NotNull WebSocket webSocket, @org.jetbrains.annotations.NotNull Response response) {
                    onWebSocketOpen();
                    loginFuture.complete(null);
                }

                @Override
                public void onMessage(@org.jetbrains.annotations.NotNull WebSocket webSocket, @org.jetbrains.annotations.NotNull ByteString bytes) {
                    notifyOnMessageListeners(bytes.asByteBuffer());
                }

                @Override
                public void onClosed(@org.jetbrains.annotations.NotNull WebSocket webSocket, int code, @org.jetbrains.annotations.NotNull String reason) {
                    onWebSocketClose(code, reason, null, null)
                            .handleAsync((unused, throwable) -> {
                                if (throwable != null) {
                                    loginFuture.completeExceptionally(throwable);
                                } else {
                                    // for the case when redirecting successfully
                                    loginFuture.complete(null);
                                }
                                return null;
                            });
                }

                /**
                 * @param response is not null when it failed at handshake stage
                 */
                @Override
                public void onFailure(@org.jetbrains.annotations.NotNull WebSocket webSocket, @org.jetbrains.annotations.NotNull Throwable throwable, @org.jetbrains.annotations.Nullable Response response) {
                    onWebSocketClose(1006, null, response, throwable)
                            .handleAsync((unused, e) -> {
                                if (e != null) {
                                    loginFuture.completeExceptionally(throwable);
                                } else {
                                    // for the case when redirecting successfully
                                    loginFuture.complete(null);
                                }
                                return null;
                            });
                }
            });
            stateStore.setWebSocket(websocket);
        }
        return loginFuture;
    }

    public CompletableFuture<Void> reconnect() {
        return connect(connectOptions);
    }

    public CompletableFuture<Void> reconnect(String host) {
        if (host != null) {
            String wsUrl = connectOptions.wsUrl();
            boolean isSecure = wsUrl != null && wsUrl.startsWith("wss://");
            String protocol = isSecure ? "wss://" : "ws://";
            connectOptions.wsUrl(protocol + host);
        }
        return connect(connectOptions);
    }

    public CompletableFuture<Void> disconnect() {
        CompletableFuture<Void> future = new CompletableFuture<>();
        if (stateStore.isConnected()) {
            // close is a synchronous method
            boolean wasEnqueued = stateStore.getWebSocket().close(1000, null);
            if (wasEnqueued) {
                isClosedByClient = true;
                stateStore.setConnected(false);
                disconnectFutures.offer(future);
            } else {
                future.completeExceptionally(TurmsBusinessException.get(TurmsStatusCode.MESSAGE_IS_REJECTED));
            }
        } else {
            future.completeExceptionally(TurmsBusinessException.get(TurmsStatusCode.CLIENT_SESSION_HAS_BEEN_CLOSED));
        }
        return future;
    }

    // Lifecycle hooks

    private void onWebSocketOpen() {
        stateStore.setConnected(true);
        notifyOnConnectedListeners();
    }

    private CompletableFuture<Void> onWebSocketClose(int code, String reason, Response response, Throwable throwable) {
        completeDisconnectFutures();
        boolean wasConnected = stateStore.isConnected();
        boolean closedByClient = isClosedByClient;
        stateStore.setConnected(false);
        stateStore.setWebSocket(null);

        SessionCloseStatus status = SessionCloseStatus.get(code);
        boolean isRedirectSignal = status == SessionCloseStatus.REDIRECT || (response != null && response.code() == 307);
        String redirectHost = null;
        if (isRedirectSignal) {
            if (response != null) {
                redirectHost = response.header(HEADER_REASON);
            }
            if (redirectHost == null && reason != null && !reason.isEmpty()) {
                redirectHost = reason;
            }
        }

        boolean shouldReconnect = isRedirectSignal && redirectHost != null;
        SessionDisconnectInfo disconnectInfo = new SessionDisconnectInfo(wasConnected, closedByClient, shouldReconnect, status, code, reason, throwable);
        notifyOnDisconnectedListeners(disconnectInfo);
        if (shouldReconnect) {
            return reconnect(redirectHost)
                    .exceptionally(t -> {
                        SessionDisconnectInfo closeInfo = new SessionDisconnectInfo(wasConnected, closedByClient, false, status, code, reason, throwable);
                        notifyOnClosedListeners(closeInfo);
                        return null;
                    });
        } else {
            SessionDisconnectInfo closeInfo = new SessionDisconnectInfo(wasConnected, closedByClient, false, status, code, reason, throwable);
            notifyOnClosedListeners(closeInfo);
            CompletableFuture<Void> future = new CompletableFuture<>();
            future.completeExceptionally(new RuntimeException());
            return future;
        }
    }

}
