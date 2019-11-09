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
import org.apache.commons.lang3.RandomUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;
import org.springframework.web.reactive.socket.client.ReactorNettyWebSocketClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.Mono;
import reactor.core.publisher.MonoSink;

import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

import static helper.util.LoginUtil.getLoginParams;
import static helper.util.LoginUtil.getServerUrl;

/**
 * WARNING: The client is used only for testing
 */
public class SimpleTurmsClient {
    private Integer port;
    private FluxSink<WebSocketMessage> outputSink;
    // request id -> callback
    private Map<Long, MonoSink<TurmsNotification>> requestCallbackMap;
    private FluxSink<TurmsNotification> notificationsSink;
    private WebSocketSession webSocketSession;

    public SimpleTurmsClient(Integer port) throws InterruptedException {
        this.port = port;
        requestCallbackMap = new HashMap<>();
        Flux.create((Consumer<FluxSink<TurmsNotification>>) TurmsNotificationFluxSink ->
                notificationsSink = TurmsNotificationFluxSink).subscribe();
        initWebSocketClient();
    }

    private void initWebSocketClient() throws InterruptedException {
        ReactorNettyWebSocketClient client = new ReactorNettyWebSocketClient();
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.COOKIE, getLoginParams(1L, "123"));
        CountDownLatch latch = new CountDownLatch(1);
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
                                                    if (turmsNotification.hasRequestId()) {
                                                        MonoSink<TurmsNotification> callback = requestCallbackMap
                                                                .get(turmsNotification.getRequestId().getValue());
                                                        callback.success(turmsNotification);
                                                    } else {
                                                        notificationsSink.next(turmsNotification);
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

    public Mono<TurmsNotification> send(TurmsRequest.Builder builder) {
        long requestId = RandomUtils.nextLong();
        TurmsRequest request = builder
                .setRequestId(Int64Value
                        .newBuilder()
                        .setValue(requestId)
                        .build())
                .build();
        WebSocketMessage message = webSocketSession.binaryMessage(dataBufferFactory ->
                dataBufferFactory.wrap(request.toByteArray()));
        return Mono.create(monoSink -> {
            requestCallbackMap.put(requestId, monoSink);
            outputSink.next(message);
        });
    }
}
