package im.turms.client.incubor.driver;

import com.google.protobuf.Descriptors;
import com.google.protobuf.Message;
import im.turms.client.incubor.TurmsClient;
import im.turms.client.incubor.common.TriFunction;
import im.turms.turms.common.ProtoUtil;
import im.turms.turms.common.TurmsStatusCode;
import im.turms.turms.common.Validator;
import im.turms.turms.constant.DeviceType;
import im.turms.turms.constant.UserStatus;
import im.turms.turms.exception.TurmsBusinessException;
import im.turms.turms.pojo.bo.user.UserLocation;
import im.turms.turms.pojo.notification.TurmsNotification;
import im.turms.turms.pojo.request.TurmsRequest;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import org.apache.commons.lang3.tuple.Pair;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;
import java.net.CookieManager;
import java.net.CookieStore;
import java.net.HttpCookie;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;
import java.util.function.Function;

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
    private HttpClient httpClient;
    private final ScheduledExecutorService heartbeatTimer = Executors.newSingleThreadScheduledExecutor();
    private ScheduledFuture<?> heartbeatFuture;
    private final HashMap<Long, Pair<TurmsRequest, CompletableFuture<TurmsNotification>>> requestMap = new HashMap<>();
    //TODO: https://github.com/turms-im/turms/issues/182
    @Setter
    private Function<TurmsNotification, Void> onMessage;
    @Setter
    private TriFunction<Boolean, TurmsStatusCode, Throwable, Void> onClose;

    private TurmsClient turmsClient;
    @Getter
    private String websocketUrl = "ws://localhost:9510";
    private String httpUrl = "http://localhost:9510";
    private int connectionTimeout = 10 * 1000;
    private int minRequestsInterval = 0;
    private Date lastRequestDate = new Date(0);
    private boolean queryReasonWhenLoginFailed = true;
    private boolean queryReasonWhenDisconnected = true;
    private Long userId;
    private String password;
    private Long connectionRequestId;
    private String sessionId;
    private String address;

    public TurmsDriver(@NotNull TurmsClient client,
                       String websocketUrl,
                       Integer connectionTimeout,
                       Integer minRequestsInterval,
                       String httpUrl,
                       Boolean queryReasonWhenLoginFailed,
                       Boolean queryReasonWhenDisconnected) {
        this.turmsClient = client;
        if (queryReasonWhenLoginFailed == null) {
            queryReasonWhenLoginFailed = true;
        }
        if (queryReasonWhenDisconnected == null) {
            queryReasonWhenDisconnected = true;
        }
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
        if (httpUrl != null) {
            this.httpUrl = httpUrl;
        }
        this.queryReasonWhenLoginFailed = queryReasonWhenLoginFailed;
        this.queryReasonWhenDisconnected = queryReasonWhenDisconnected;
    }

    public CompletableFuture<WebSocket> sendHeartbeat() {
        if (this.connected()) {
            lastRequestDate = new Date();
            return websocket.sendBinary(ByteBuffer.allocate(0), true);
        } else {
            return CompletableFuture.failedFuture(TurmsBusinessException.get(TurmsStatusCode.CLIENT_SESSION_HAS_BEEN_CLOSED));
        }
    }

    public boolean connected() {
        return websocket != null && !websocket.isInputClosed() && !websocket.isOutputClosed();
    }

    public CompletableFuture<WebSocket> disconnect() {
        if (connected()) {
            return this.websocket.sendClose(WebSocket.NORMAL_CLOSURE, null);
        } else {
            return CompletableFuture.failedFuture(TurmsBusinessException.get(TurmsStatusCode.CLIENT_SESSION_HAS_BEEN_CLOSED));
        }
    }

    public CompletableFuture<WebSocket> connect(
            @NotNull long userId,
            @NotNull String password,
            long requestId,
            String url,
            Integer connectionTimeout,
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
            this.connectionRequestId = requestId;
            WebSocket.Builder builder = HttpClient.newHttpClient()
                    .newWebSocketBuilder();
            builder.header(REQUEST_ID_FIELD, String.valueOf(requestId));
            builder.header(USER_ID_FIELD, String.valueOf(userId));
            builder.header(PASSWORD_FIELD, password);
            if (userLocation != null) {
                String location = String.format("%f:%f", userLocation.getLongitude(), userLocation.getLatitude());
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
                    .buildAsync(URI.create(url), new WebSocket.Listener() {
                        @SneakyThrows
                        @Override
                        public void onOpen(WebSocket webSocket) {
                            webSocket.request(1);
                            onWebsocketOpen();
                        }

                        @SneakyThrows
                        @Override
                        public CompletionStage<?> onBinary(WebSocket webSocket, ByteBuffer data, boolean last) {
                            webSocket.request(1);
                            TurmsNotification notification = TurmsNotification.parseFrom(data);
                            //TODO: notify
                            if (notification != null) {
                                if (notification.getData() != null && notification.getData().getSession() != null) {
                                    sessionId = notification.getData().getSession().getSessionId();
                                    address = notification.getData().getSession().getAddress();
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
                            if (statusCode == WebSocket.NORMAL_CLOSURE) {
                                onWebsocketClose();
                            }
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
                    });
        }
    }

    public CompletableFuture<TurmsNotification> send(Message.Builder builder) {
        return send(builder, null);
    }

    public CompletableFuture<TurmsNotification> send(Message.Builder builder, Map<String, ?> fields) {
        //TODO: test
        if (fields != null) {
            ProtoUtil.fillFields(builder, fields);
        }
        Descriptors.Descriptor descriptor = builder.getDescriptorForType();
        String fieldName = descriptor.getName();
        TurmsRequest.Builder requestBuilder = TurmsRequest.newBuilder();
        Descriptors.Descriptor requestDescriptor = requestBuilder.getDescriptorForType();
        Descriptors.FieldDescriptor fieldDescriptor = requestDescriptor.findFieldByName(fieldName);
        requestBuilder.setField(fieldDescriptor, builder.build());
        return send(requestBuilder.build());
    }

    public CompletableFuture<TurmsNotification> send(TurmsRequest request) {
        if (this.connected()) {
            Date now = new Date();
            if (minRequestsInterval == 0 || now.getTime() - this.lastRequestDate.getTime() > minRequestsInterval) {
                lastRequestDate = now;
                long requestId = generateRandomId();
                ByteBuffer data = ByteBuffer.wrap(request.toByteArray());
                CompletableFuture<TurmsNotification> future = new CompletableFuture<>();
                websocket.sendBinary(data, true).whenComplete((webSocket, throwable) -> {
                    if (throwable != null) {
                        future.completeExceptionally(throwable);
                    } else {
                        heartbeatFuture = this.heartbeatTimer.scheduleAtFixedRate(
                                this::checkAndSendHeartbeatTask,
                                heartbeatInterval,
                                heartbeatInterval,
                                TimeUnit.SECONDS);
                        requestMap.put(requestId, Pair.of(request, future));
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
    }

    private void checkAndSendHeartbeatTask() {
        long difference = System.currentTimeMillis() - lastRequestDate.getTime();
        if (difference > minRequestsInterval) {
            this.sendHeartbeat();
        }
    }

    private void onWebsocketClose() {
        boolean wasLogged = isSessionEstablished;
        isSessionEstablished = false;
        cancelHeartbeatFuture();
        if (onClose != null) {
            if (queryReasonWhenDisconnected && userId != null && sessionId != null) {
                queryReasonWhenDisconnected(userId, sessionId)
                        .whenComplete((response, throwable) -> {
                            TurmsStatusCode code = TurmsStatusCode.from(Integer.parseInt(response.body()));
                            onClose.apply(wasLogged, code, throwable);
                        });
            } else {
                onClose.apply(wasLogged, TurmsStatusCode.CLIENT_SESSION_ALREADY_ESTABLISHED, null);
            }
        }
    }

    private void onWebsocketError(Throwable error) {
        boolean wasLogged = isSessionEstablished;
        isSessionEstablished = false;
        cancelHeartbeatFuture();
        if (!wasLogged && queryReasonWhenLoginFailed && userId != null && connectionRequestId != null) {
            queryLoginFailedReason(this.userId, this.connectionRequestId)
                    .whenComplete((response, throwable) -> {
                        if (throwable != null) {
                            onClose.apply(wasLogged, null, throwable);
                        } else {
                            if (response.statusCode() == 307) {
                                turmsClient.getUserService().relogin();
                            } else {
                                if (onClose != null) {
                                    onClose.apply(wasLogged, TurmsStatusCode.NOT_RESPONSIBLE, null);
                                }
                            }
                        }
                    });
        } else {
            onClose.apply(wasLogged, null, error);
        }
    }

    private CompletableFuture<HttpResponse<String>> queryLoginFailedReason(long userId, long requestId) {
        if (httpClient == null) {
            httpClient = HttpClient.newHttpClient();
        }
        String requestUrl = String.format("%s/reasons/login-failed?userId=%d&requestId=%d", httpUrl, userId, requestId);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(requestUrl))
                .build();
        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString());
    }

    private CompletableFuture<HttpResponse<String>> queryReasonWhenDisconnected(long userId, @NotNull String sessionId) {
        Validator.throwIfAnyFalsy(sessionId);
        if (httpClient == null) {
            httpClient = HttpClient.newHttpClient();
        }
        String requestUrl = String.format("%s/reasons/disconnection?userId=%d&sessionId=%s", httpUrl, userId, sessionId);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(requestUrl))
                .build();
        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString());
    }

    private void cancelHeartbeatFuture() {
        if (!heartbeatFuture.isCancelled() && !heartbeatFuture.isDone()) {
            heartbeatFuture.cancel(true);
        }
    }
}
