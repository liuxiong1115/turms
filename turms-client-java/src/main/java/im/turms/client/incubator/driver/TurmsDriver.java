package im.turms.client.incubator.driver;

import com.google.protobuf.Descriptors;
import com.google.protobuf.Int64Value;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import im.turms.client.incubator.common.Function5;
import im.turms.client.incubator.common.StringUtil;
import im.turms.client.incubator.common.TurmsLogger;
import im.turms.client.incubator.util.ProtoUtil;
import im.turms.turms.common.TurmsStatusCode;
import im.turms.turms.constant.DeviceType;
import im.turms.turms.constant.UserStatus;
import im.turms.turms.exception.TurmsBusinessException;
import im.turms.turms.pojo.bo.user.UserLocation;
import im.turms.turms.pojo.notification.TurmsNotification;
import im.turms.turms.pojo.request.TurmsRequest;
import org.apache.commons.lang3.tuple.Pair;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;
import java.util.function.Function;
import java.util.logging.Level;

public class TurmsDriver {
    private static final Integer HEARTBEAT_INTERVAL = 20 * 1000;
    public static final String REQUEST_ID_FIELD = "rid";
    public static final String USER_ID_FIELD = "uid";
    public static final String PASSWORD_FIELD = "pwd";
    public static final String DEVICE_TYPE_FIELD = "dt";
    public static final String USER_ONLINE_STATUS_FIELD = "us";
    public static final String USER_LOCATION_FIELD = "loc";
    public static final String USER_DEVICE_DETAILS = "dd";
    private static final String LOCATION_SPLIT = ":";

    private Integer heartbeatInterval;

    private WebSocket websocket;
    private boolean isSessionEstablished;
    private final ScheduledExecutorService heartbeatTimer = Executors.newSingleThreadScheduledExecutor();
    private ScheduledFuture<?> heartbeatFuture;
    private final HashMap<Long, Pair<TurmsRequest, CompletableFuture<TurmsNotification>>> requestMap = new HashMap<>();

    private Function<TurmsNotification, Void> onMessage;
    private Function5<Boolean, TurmsStatusCode, Throwable, Integer, String, Void> onClose;

    private String websocketUrl = "ws://localhost:9510";
    private int connectionTimeout = 10 * 1000;
    private int minRequestsInterval = 0;
    private long lastRequestDate = 0;
    private Long userId;
    private String password;
    private UserLocation userLocation;
    private UserStatus userOnlineStatus;
    private DeviceType deviceType;

    private String address;
    private String sessionId;

    public String getAddress() {
        return address;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setOnMessage(Function<TurmsNotification, Void> onMessage) {
        this.onMessage = onMessage;
    }

    public void setOnClose(Function5<Boolean, TurmsStatusCode, Throwable, Integer, String, Void> onClose) {
        this.onClose = onClose;
    }

    public TurmsDriver(@Nullable String websocketUrl,
                       @Nullable Integer connectionTimeout,
                       @Nullable Integer minRequestsInterval) {
        if (websocketUrl != null) {
            this.websocketUrl = websocketUrl;
        }
        if (connectionTimeout != null) {
            this.connectionTimeout = connectionTimeout;
        }
        if (minRequestsInterval != null) {
            this.minRequestsInterval = minRequestsInterval;
        }
        this.heartbeatInterval = HEARTBEAT_INTERVAL;
    }

    public CompletableFuture<Void> sendHeartbeat() {
        if (this.connected()) {
            lastRequestDate = System.currentTimeMillis();
            return websocket.sendBinary(ByteBuffer.allocate(0), true)
                    .thenApply(webSocket -> null);
        } else {
            return CompletableFuture.failedFuture(TurmsBusinessException.get(TurmsStatusCode.CLIENT_SESSION_HAS_BEEN_CLOSED));
        }
    }

    public boolean connected() {
        return websocket != null && !websocket.isInputClosed() && !websocket.isOutputClosed();
    }

    public CompletableFuture<Void> disconnect() {
        if (connected()) {
            return this.websocket
                    .sendClose(WebSocket.NORMAL_CLOSURE, "")
                    .thenApply(webSocket -> null);
        } else {
            return CompletableFuture.failedFuture(TurmsBusinessException.get(TurmsStatusCode.CLIENT_SESSION_HAS_BEEN_CLOSED));
        }
    }

    public CompletableFuture<Void> connect(
            @NotNull long userId,
            @NotNull String password,
            @Nullable Integer connectionTimeout,
            @Nullable UserLocation userLocation,
            @Nullable UserStatus userOnlineStatus,
            @Nullable DeviceType deviceType) {
        if (connectionTimeout == null) {
            connectionTimeout = this.connectionTimeout;
        }
        Integer finalConnectionTimeout = connectionTimeout;
        if (connected()) {
            return CompletableFuture.failedFuture(TurmsBusinessException.get(TurmsStatusCode.CLIENT_SESSION_ALREADY_ESTABLISHED));
        } else {
            this.userId = userId;
            this.password = password;
            this.userLocation = userLocation;
            this.userOnlineStatus = userOnlineStatus;
            this.deviceType = deviceType;
            long connectionRequestId = (long) Math.ceil(Math.random() * Long.MAX_VALUE);
            WebSocket.Builder builder = HttpClient.newHttpClient()
                    .newWebSocketBuilder();
            builder.header(REQUEST_ID_FIELD, String.valueOf(connectionRequestId));
            builder.header(USER_ID_FIELD, String.valueOf(userId));
            builder.header(PASSWORD_FIELD, password);
            if (userLocation != null) {
                String location = String.format("%f%s%f", userLocation.getLongitude(), LOCATION_SPLIT, userLocation.getLatitude());
                builder.header(USER_LOCATION_FIELD, location);
            }
            if (userOnlineStatus != null) {
                builder.header(USER_ONLINE_STATUS_FIELD, userOnlineStatus.toString());
            }
            if (deviceType != null) {
                builder.header(DEVICE_TYPE_FIELD, deviceType.name());
            }
            return builder
                    .connectTimeout(Duration.ofSeconds(finalConnectionTimeout))
                    .buildAsync(URI.create(websocketUrl), new WebSocket.Listener() {

                        @Override
                        public void onOpen(WebSocket webSocket) {
                            webSocket.request(1);
                            onWebsocketOpen();
                        }

                        @Override
                        public CompletionStage<?> onBinary(WebSocket webSocket, ByteBuffer data, boolean last) {
                            webSocket.request(1);
                            TurmsNotification notification;
                            try {
                                notification = TurmsNotification.parseFrom(data);
                            } catch (InvalidProtocolBufferException e) {
                                TurmsLogger.logger.log(Level.SEVERE, "", e);
                                return CompletableFuture.failedStage(e);
                            }
                            if (notification != null) {
                                boolean isSessionInfo = notification.hasData() && notification.getData().hasSession();
                                if (isSessionInfo) {
                                    address = notification.getData().getSession().getAddress();
                                    sessionId = notification.getData().getSession().getSessionId();
                                } else if (notification.hasRequestId()) {
                                    long requestId = notification.getRequestId().getValue();
                                    Pair<TurmsRequest, CompletableFuture<TurmsNotification>> pair = requestMap.get(requestId);
                                    if (pair != null) {
                                        CompletableFuture<TurmsNotification> future = pair.getValue();
                                        if (notification.hasCode()) {
                                            int code = notification.getCode().getValue();
                                            if (TurmsStatusCode.isSuccess(code)) {
                                                future.complete(notification);
                                            } else {
                                                TurmsBusinessException exception;
                                                if (code == TurmsStatusCode.FAILED.getBusinessCode()) {
                                                    exception = TurmsBusinessException.get(code, notification.getReason().getValue());
                                                } else {
                                                    exception = TurmsBusinessException.get(code);
                                                }
                                                if (exception != null) {
                                                    future.completeExceptionally(exception);
                                                } else {
                                                    TurmsLogger.logger.log(Level.WARNING, "Unknown status code");
                                                }
                                            }
                                        } else {
                                            future.complete(notification);
                                        }
                                    }
                                }
                                if (onMessage != null) {
                                    onMessage.apply(notification);
                                }
                            }
                            return CompletableFuture.completedStage(notification);
                        }

                        @Override
                        public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
                            webSocket.request(1);
                            onWebsocketClose(statusCode, reason);
                            return null;
                        }

                        @Override
                        public void onError(WebSocket webSocket, Throwable error) {
                            onWebsocketError(error);
                        }
                    })
                    .whenComplete((webSocket, throwable) -> {
                        if (webSocket != null) {
                            this.websocket = webSocket;
                        }
                    })
                    .thenApply(webSocket -> null);
        }
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
        return send(requestBuilder);
    }

    public CompletableFuture<TurmsNotification> send(TurmsRequest.Builder requestBuilder) {
        if (this.connected()) {
            Date now = new Date();
            if (minRequestsInterval == 0 || now.getTime() - lastRequestDate > minRequestsInterval) {
                lastRequestDate = now.getTime();
                long requestId = generateRandomId();
                requestBuilder.setRequestId(Int64Value.newBuilder().setValue(requestId).build());
                TurmsRequest request = requestBuilder.build();
                ByteBuffer data = ByteBuffer.wrap(request.toByteArray());
                CompletableFuture<TurmsNotification> future = new CompletableFuture<>();
                requestMap.put(requestId, Pair.of(request, future));
                websocket.sendBinary(data, true).whenComplete((webSocket, throwable) -> {
                    if (throwable != null) {
                        future.completeExceptionally(throwable);
                    }
                });
                return future;
            } else {
                return CompletableFuture.failedFuture(TurmsBusinessException.get(TurmsStatusCode.CLIENT_REQUESTS_TOO_FREQUENT));
            }
        } else {
            return CompletableFuture.failedFuture(TurmsBusinessException.get(TurmsStatusCode.CLIENT_SESSION_HAS_BEEN_CLOSED));
        }
    }

    private long generateRandomId() {
        long id;
        do {
            id = (long) Math.floor(Math.random() * 9007199254740991L);
        } while (requestMap.containsKey(id));
        return id;
    }

    private void onWebsocketOpen() {
        isSessionEstablished = true;
        heartbeatFuture = this.heartbeatTimer.scheduleAtFixedRate(
                this::checkAndSendHeartbeatTask,
                heartbeatInterval,
                heartbeatInterval,
                TimeUnit.SECONDS);
    }

    private void checkAndSendHeartbeatTask() {
        long difference = System.currentTimeMillis() - lastRequestDate;
        if (difference > minRequestsInterval) {
            this.sendHeartbeat();
        }
    }

    private void onWebsocketClose(int statusCode, String reason) {
        boolean wasLogged = isSessionEstablished;
        isSessionEstablished = false;
        cancelHeartbeatFuture();
        if (statusCode == 307) {
            this.connect(userId, password, connectionTimeout, userLocation, userOnlineStatus, deviceType);
        } else if (onClose != null) {
            onClose.apply(wasLogged, TurmsStatusCode.CLIENT_SESSION_ALREADY_ESTABLISHED, null, statusCode, reason);
        }
    }

    private void onWebsocketError(Throwable error) {
        boolean wasLogged = isSessionEstablished;
        isSessionEstablished = false;
        cancelHeartbeatFuture();
        onClose.apply(wasLogged, null, error, null, null);
    }

    private void cancelHeartbeatFuture() {
        if (!heartbeatFuture.isCancelled() && !heartbeatFuture.isDone()) {
            heartbeatFuture.cancel(true);
        }
    }
}
