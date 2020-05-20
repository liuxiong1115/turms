package im.turms.client.driver;

import com.google.protobuf.Descriptors;
import com.google.protobuf.Int64Value;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import im.turms.client.TurmsClient;
import im.turms.client.common.Function4;
import im.turms.client.common.StringUtil;
import im.turms.client.util.ProtoUtil;
import im.turms.common.TurmsCloseStatus;
import im.turms.common.TurmsStatusCode;
import im.turms.common.constant.DeviceType;
import im.turms.common.constant.UserStatus;
import im.turms.common.exception.TurmsBusinessException;
import im.turms.common.model.bo.user.UserLocation;
import im.turms.common.model.dto.notification.TurmsNotification;
import im.turms.common.model.dto.request.TurmsRequest;
import okhttp3.*;
import okio.ByteString;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.util.AbstractMap.SimpleEntry;

public class TurmsDriver {
    private static final Logger LOGGER = Logger.getLogger(TurmsDriver.class.getName());

    private static final Integer HEARTBEAT_INTERVAL = 20 * 1000;
    public static final String REQUEST_ID_FIELD = "rid";
    public static final String USER_ID_FIELD = "uid";
    public static final String PASSWORD_FIELD = "pwd";
    public static final String DEVICE_TYPE_FIELD = "dt";
    public static final String USER_ONLINE_STATUS_FIELD = "us";
    public static final String USER_LOCATION_FIELD = "loc";
    public static final String USER_DEVICE_DETAILS = "dd";
    private static final String LOCATION_SPLIT = ":";

    private final Integer heartbeatInterval;

    private final TurmsClient turmsClient;

    private final OkHttpClient httpClient;
    private WebSocket websocket;
    private volatile boolean isWebsocketOpen;
    private CompletableFuture<Void> disconnectFuture;
    private final ScheduledExecutorService heartbeatTimer = Executors.newSingleThreadScheduledExecutor();
    private ScheduledFuture<?> heartbeatFuture;
    private final HashMap<Long, SimpleEntry<TurmsRequest, CompletableFuture<TurmsNotification>>> requestMap = new HashMap<>();

    private final List<Function<TurmsNotification, Void>> onNotificationListeners = new LinkedList<>();
    private final ConcurrentLinkedQueue<CompletableFuture<Void>> heartbeatCallbacks = new ConcurrentLinkedQueue<>();
    // TurmsCloseStatus, WebSocket status code, WebSocket reason, error
    private Function4<TurmsCloseStatus, Integer, String, Throwable, Void> onClose;

    private String websocketUrl = "ws://localhost:9510";
    private int connectionTimeout = 10;
    private int minRequestsInterval = 0;
    private long lastRequestDate = 0;

    private String address;
    private String sessionId;

    public OkHttpClient getHttpClient() {
        return httpClient;
    }

    public List<Function<TurmsNotification, Void>> getOnNotificationListeners() {
        return onNotificationListeners;
    }

    public String getAddress() {
        return address;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setOnClose(Function4<TurmsCloseStatus, Integer, String, Throwable, Void> onClose) {
        this.onClose = onClose;
    }

    public TurmsDriver(@NotNull TurmsClient turmsClient,
                       @Nullable String websocketUrl,
                       @Nullable Integer connectionTimeout,
                       @Nullable Integer minRequestsInterval) {
        this.turmsClient = turmsClient;
        if (websocketUrl != null) {
            this.websocketUrl = websocketUrl;
        }
        if (connectionTimeout != null) {
            this.connectionTimeout = connectionTimeout;
        }
        if (minRequestsInterval != null) {
            this.minRequestsInterval = minRequestsInterval;
        }
        httpClient = new OkHttpClient.Builder()
                .connectTimeout(Duration.ofSeconds(this.connectionTimeout))
                .build();
        this.heartbeatInterval = HEARTBEAT_INTERVAL;
    }

    public TurmsDriver(@NotNull TurmsClient turmsClient) {
        this(turmsClient, null, null, null);
    }

    public CompletableFuture<Void> sendHeartbeat() {
        CompletableFuture<Void> future = new CompletableFuture<>();
        if (this.connected()) {
            lastRequestDate = System.currentTimeMillis();
            boolean wasEnqueued = websocket.send(ByteString.EMPTY);
            if (wasEnqueued) {
                heartbeatCallbacks.offer(future);
            } else {
                future.completeExceptionally(TurmsBusinessException.get(TurmsStatusCode.MESSAGE_IS_REJECTED));
            }
        } else {
            future.completeExceptionally(TurmsBusinessException.get(TurmsStatusCode.CLIENT_SESSION_HAS_BEEN_CLOSED));
        }
        return future;
    }

    public boolean connected() {
        return websocket != null && isWebsocketOpen;
    }

    public CompletableFuture<Void> disconnect() {
        CompletableFuture<Void> future = new CompletableFuture<>();
        if (connected()) {
            // close is a synchronous method
            boolean wasEnqueued = this.websocket.close(1000, null);
            if (wasEnqueued) {
                isWebsocketOpen = false;
                disconnectFuture = future;
            } else {
                future.completeExceptionally(TurmsBusinessException.get(TurmsStatusCode.MESSAGE_IS_REJECTED));
            }
        } else {
            future.completeExceptionally(TurmsBusinessException.get(TurmsStatusCode.CLIENT_SESSION_HAS_BEEN_CLOSED));
        }
        return future;
    }

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
        CompletableFuture<Void> loginFuture = new CompletableFuture<>();
        if (connected()) {
            loginFuture.completeExceptionally(TurmsBusinessException.get(TurmsStatusCode.CLIENT_SESSION_ALREADY_ESTABLISHED));
            return loginFuture;
        } else {
            long connectionRequestId = (long) Math.ceil(Math.random() * Long.MAX_VALUE);
            Request.Builder requestBuilder = new Request.Builder()
                    .url(websocketUrl)
                    .header(REQUEST_ID_FIELD, String.valueOf(connectionRequestId))
                    .header(USER_ID_FIELD, String.valueOf(userId))
                    .header(PASSWORD_FIELD, password);
            if (userLocation != null) {
                String location = String.format("%f%s%f", userLocation.getLongitude(), LOCATION_SPLIT, userLocation.getLatitude());
                requestBuilder.header(USER_LOCATION_FIELD, location);
            }
            if (userOnlineStatus != null) {
                requestBuilder.header(USER_ONLINE_STATUS_FIELD, userOnlineStatus.toString());
            }
            if (deviceType != null) {
                requestBuilder.header(DEVICE_TYPE_FIELD, deviceType.name());
            }
            websocket = httpClient.newWebSocket(requestBuilder.build(), new WebSocketListener() {
                @Override
                public void onOpen(@org.jetbrains.annotations.NotNull WebSocket webSocket, @org.jetbrains.annotations.NotNull Response response) {
                    heartbeatFuture = heartbeatTimer.scheduleAtFixedRate(
                            (() -> checkAndSendHeartbeatTask()),
                            heartbeatInterval,
                            heartbeatInterval,
                            TimeUnit.SECONDS);
                    isWebsocketOpen = true;
                    loginFuture.complete(null);
                }

                @Override
                public void onMessage(@org.jetbrains.annotations.NotNull WebSocket webSocket, @org.jetbrains.annotations.NotNull ByteString bytes) {
                    TurmsNotification notification;
                    try {
                        notification = TurmsNotification.parseFrom(bytes.asByteBuffer());
                    } catch (InvalidProtocolBufferException e) {
                        LOGGER.log(Level.SEVERE, "", e);
                        return;
                    }
                    if (notification != null) {
                        boolean isSessionInfo = notification.hasData() && notification.getData().hasSession();
                        if (isSessionInfo) {
                            address = notification.getData().getSession().getAddress();
                            sessionId = notification.getData().getSession().getSessionId();
                        } else if (notification.hasRequestId()) {
                            long requestId = notification.getRequestId().getValue();
                            SimpleEntry<TurmsRequest, CompletableFuture<TurmsNotification>> pair = requestMap.remove(requestId);
                            if (pair != null) {
                                CompletableFuture<TurmsNotification> future = pair.getValue();
                                if (notification.hasCode()) {
                                    int code = notification.getCode().getValue();
                                    if (TurmsStatusCode.isSuccess(code)) {
                                        future.complete(notification);
                                    } else {
                                        TurmsBusinessException exception = notification.hasReason()
                                                ? TurmsBusinessException.get(code, notification.getReason().getValue())
                                                : TurmsBusinessException.get(code);
                                        if (exception != null) {
                                            future.completeExceptionally(exception);
                                        } else {
                                            LOGGER.log(Level.WARNING, "Unknown status code");
                                        }
                                    }
                                } else {
                                    future.complete(notification);
                                }
                            }
                        }
                        for (Function<TurmsNotification, Void> listener : onNotificationListeners) {
                            try {
                                listener.apply(notification);
                            } catch (Exception e) {
                                LOGGER.log(Level.SEVERE, "", e);
                            }
                        }
                    }
                }

                @Override
                public void onClosed(@org.jetbrains.annotations.NotNull WebSocket webSocket, int code, @org.jetbrains.annotations.NotNull String reason) {
                    clearWebSocket();
                    TurmsCloseStatus status = TurmsCloseStatus.get(code);
                    if (status == TurmsCloseStatus.REDIRECT && !reason.isEmpty()) {
                        try {
                            reconnect(reason).get(10, TimeUnit.SECONDS);
                        } catch (Exception e) {
                            RuntimeException runtimeException = new RuntimeException("Failed to reconnect", e);
                            if (onClose != null) {
                                onClose.apply(status, code, reason, runtimeException);
                            }
                        }
                    } else if (onClose != null) {
                        onClose.apply(status, code, reason, null);
                    }
                }

                @Override
                public void onFailure(@org.jetbrains.annotations.NotNull WebSocket webSocket, @org.jetbrains.annotations.NotNull Throwable throwable, @org.jetbrains.annotations.Nullable Response response) {
                    clearWebSocket();
                    // response != null when it failed at handshake stage
                    boolean isReconnecting = false;
                    if (response != null && response.code() == 307) {
                        String reason = response.header("reason");
                        if (reason != null) {
                            isReconnecting = true;
                            reconnect(reason).whenComplete((aVoid, t) -> {
                                if (t != null) {
                                    loginFuture.completeExceptionally(throwable);
                                } else {
                                    loginFuture.complete(null);
                                }
                            });
                        }
                    }
                    if (onClose != null) {
                        onClose.apply(null, null, null, throwable);
                    }
                    if (!isReconnecting) {
                        loginFuture.completeExceptionally(throwable);
                    }
                }
            });
        }
        return loginFuture;
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
        CompletableFuture<TurmsNotification> future = new CompletableFuture<>();
        if (this.connected()) {
            Date now = new Date();
            if (minRequestsInterval == 0 || now.getTime() - lastRequestDate > minRequestsInterval) {
                lastRequestDate = now.getTime();
                long requestId = generateRandomId();
                requestBuilder.setRequestId(Int64Value.newBuilder().setValue(requestId).build());
                TurmsRequest request = requestBuilder.build();
                ByteBuffer data = ByteBuffer.wrap(request.toByteArray());
                requestMap.put(requestId, new SimpleEntry<>(request, future));
                boolean wasEnqueued = websocket.send(ByteString.of(data));
                if (!wasEnqueued) {
                    future.completeExceptionally(TurmsBusinessException.get(TurmsStatusCode.MESSAGE_IS_REJECTED));
                }
            } else {
                future.completeExceptionally(TurmsBusinessException.get(TurmsStatusCode.CLIENT_REQUESTS_TOO_FREQUENT));
            }
        } else {
            future.completeExceptionally(TurmsBusinessException.get(TurmsStatusCode.CLIENT_SESSION_HAS_BEEN_CLOSED));
        }
        return future;
    }

    private long generateRandomId() {
        long id;
        do {
            id = (long) (Math.random() * 16384);
        } while (requestMap.containsKey(id));
        return id;
    }

    private void checkAndSendHeartbeatTask() {
        long difference = System.currentTimeMillis() - lastRequestDate;
        if (difference > minRequestsInterval) {
            this.sendHeartbeat();
        }
    }

    private CompletableFuture<Void> reconnect(String address) {
        boolean isSecure = websocketUrl.startsWith("wss://");
        websocketUrl = (isSecure ? "wss://" : "ws://") + address;
        return this.connect(
                turmsClient.getUserService().getUserId(),
                turmsClient.getUserService().getPassword(),
                turmsClient.getUserService().getDeviceType(),
                turmsClient.getUserService().getUserOnlineStatus(),
                turmsClient.getUserService().getLocation());
    }

    private void clearWebSocket() {
        isWebsocketOpen = false;
        if (heartbeatFuture != null && !heartbeatFuture.isCancelled() && !heartbeatFuture.isDone()) {
            heartbeatFuture.cancel(true);
        }
        if (disconnectFuture != null) {
            disconnectFuture.complete(null);
        }
        for (CompletableFuture<Void> future : heartbeatCallbacks) {
            future.completeExceptionally(TurmsBusinessException.get(TurmsStatusCode.CLIENT_SESSION_HAS_BEEN_CLOSED));
        }
    }
}
