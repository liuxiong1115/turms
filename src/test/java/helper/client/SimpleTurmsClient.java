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

package helper.client;

import com.google.protobuf.Int64Value;
import com.google.protobuf.InvalidProtocolBufferException;
import im.turms.turms.pojo.notification.TurmsNotification;
import im.turms.turms.pojo.request.TurmsRequest;
import lombok.Setter;
import org.apache.commons.lang3.RandomUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;
import org.springframework.web.reactive.socket.client.ReactorNettyWebSocketClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.function.Function;

import static helper.util.LoginUtil.getLoginParams;
import static helper.util.LoginUtil.getServerUrl;

/**
 * WARNING: The client is used only for testing
 */
public class SimpleTurmsClient {
    private Integer port;
    private Long userId;
    private String password;
    private FluxSink<WebSocketMessage> outputSink;
    // request id -> callback
    private Map<Long, Function<TurmsNotification, Void>> requestCallbackMap;
    @Setter
    private Function<TurmsNotification, Void> notificationsCallback;
    private WebSocketSession webSocketSession;
    private CountDownLatch latch;

    public SimpleTurmsClient(
            @NotNull Integer port,
            @NotNull Long userId,
            @NotNull String password,
            @Nullable Function<TurmsNotification, Void> notificationsCallback) throws InterruptedException {
        this.port = port;
        this.userId = userId;
        this.password = password;
        this.notificationsCallback = notificationsCallback;
        requestCallbackMap = new HashMap<>();
        latch = new CountDownLatch(1);
        initWebSocketClient();
    }

    private void initWebSocketClient() throws InterruptedException {
        ReactorNettyWebSocketClient client = new ReactorNettyWebSocketClient();
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.COOKIE, getLoginParams(this.userId, this.password));
        Executors.newSingleThreadExecutor()
                .execute(() -> {
                    try {
                        client.execute(getServerUrl(port),
                                headers,
                                session -> {
                                    webSocketSession = session;
                                    Flux<WebSocketMessage> input = session.receive()
                                            .doOnNext(message -> {
                                                ByteBuffer byteBuffer = message.getPayload().asByteBuffer();
                                                try {
                                                    TurmsNotification turmsNotification = TurmsNotification.parseFrom(byteBuffer);
                                                    if (notificationsCallback != null) {
                                                        notificationsCallback.apply(turmsNotification);
                                                    }
                                                    if (turmsNotification.hasRequestId()) {
                                                        Function<TurmsNotification, Void> requestCallback = requestCallbackMap
                                                                .get(turmsNotification.getRequestId().getValue());
                                                        if (requestCallback != null) {
                                                            requestCallback.apply(turmsNotification);
                                                        }
                                                    }
                                                } catch (InvalidProtocolBufferException e) {
                                                    e.printStackTrace();
                                                }
                                            });
                                    Flux<WebSocketMessage> output = Flux.create(fluxSink -> outputSink = fluxSink);
                                    latch.countDown();
                                    return session.send(output).mergeWith(input.then()).then();
                                }).subscribe();
                    } catch (URISyntaxException e) {
                        e.printStackTrace();
                    }
                });
        latch.await();
    }

    public boolean isClosed() {
        return webSocketSession == null;
    }

    public synchronized void close() {
        if (webSocketSession != null) {
            webSocketSession.close();
            webSocketSession = null;
        }
        if (outputSink != null && !outputSink.isCancelled()) {
            outputSink.complete();
            outputSink = null;
        }
    }

    public void send(
            @NotNull TurmsRequest.Builder builder,
            @Nullable Function<TurmsNotification, Void> callback) {
        long requestId = RandomUtils.nextLong();
        TurmsRequest request = builder
                .setRequestId(Int64Value
                        .newBuilder()
                        .setValue(requestId)
                        .build())
                .build();
        WebSocketMessage message = webSocketSession.binaryMessage(dataBufferFactory ->
                dataBufferFactory.wrap(request.toByteArray()));
        if (callback != null) {
            requestCallbackMap.put(requestId, callback);
        }
        outputSink.next(message);
    }

    public void returnAfterConnectedOrFailed() throws InterruptedException {
        latch.await();
    }
}
