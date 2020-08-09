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

package unit.im.turms.gateway.access.websocket;

import im.turms.common.constant.statuscode.TurmsStatusCode;
import im.turms.gateway.access.websocket.config.TurmsHandshakeWebSocketService;
import im.turms.gateway.access.websocket.util.HandshakeRequestUtil;
import im.turms.gateway.service.mediator.WorkflowMediator;
import im.turms.server.common.cluster.node.Node;
import im.turms.server.common.property.TurmsProperties;
import im.turms.server.common.property.TurmsPropertiesManager;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.reactive.socket.server.RequestUpgradeStrategy;
import org.springframework.web.server.MethodNotAllowedException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.ServerWebInputException;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * @author James Chen
 */
class TurmsHandshakeWebSocketServiceTests {

    @Test
    void handleRequest_shouldReject_forRequestWrongMethod() {
        MockServerHttpRequest requestWithWrongMethod = MockServerHttpRequest.delete("").build();
        ServerWebExchange exchange = MockServerWebExchange.from(requestWithWrongMethod);
        Mono<Void> result = newMockWebSocketService(false).handleRequest(exchange, session -> null);

        StepVerifier.create(result)
                .expectError(MethodNotAllowedException.class)
                .verify();
    }

    @Test
    void handleRequest_shouldReject_forRequestWrongWebSocketHeader() {
        MockServerHttpRequest requestWithWrongHeader = MockServerHttpRequest.get("").build();
        ServerWebExchange exchange = MockServerWebExchange.from(requestWithWrongHeader);
        Mono<Void> result = newMockWebSocketService(false).handleRequest(exchange, session -> null);

        StepVerifier.create(result)
                .expectError(ServerWebInputException.class)
                .verify();
    }

    @Test
    void handleRequest_shouldReject_ifRequestHeaderUserIdNotExists() {
        MockServerHttpRequest.BaseBuilder<?> requestWithoutUserId = getQualifiedUpgradeRequest();
        ServerWebExchange exchange = MockServerWebExchange.from(requestWithoutUserId);
        Mono<Void> result = newMockWebSocketService(false).handleRequest(exchange, session -> null);

        StepVerifier.create(result)
                .expectErrorMatches(throwable ->
                        throwable instanceof ResponseStatusException
                                && ((ResponseStatusException) throwable).getStatus().equals(HttpStatus.UNAUTHORIZED))
                .verify();
    }

    @Test
    void handleRequest_shouldReject_ifNodeIsInactive() {
        MockServerHttpRequest.BaseBuilder<?> requestWithUserId = getQualifiedUpgradeRequest();
        requestWithUserId.header(HandshakeRequestUtil.USER_ID_FIELD, "1");
        ServerWebExchange exchange = MockServerWebExchange.from(requestWithUserId);
        Mono<Void> result = newMockWebSocketService(false).handleRequest(exchange, session -> null);

        StepVerifier.create(result)
                .expectErrorMatches(throwable ->
                        throwable instanceof ResponseStatusException
                                && ((ResponseStatusException) throwable).getStatus().equals(HttpStatus.SERVICE_UNAVAILABLE))
                .verify();
    }

    @Test
    void handleRequest_shouldSucceed_ifWithUserIdAndNodeIsActive() {
        MockServerHttpRequest.BaseBuilder<?> requestWithUserId = getQualifiedUpgradeRequest();
        requestWithUserId.header(HandshakeRequestUtil.USER_ID_FIELD, "1");
        ServerWebExchange exchange = MockServerWebExchange.from(requestWithUserId);
        Mono<Void> result = newMockWebSocketService(true).handleRequest(exchange, session -> null);

        StepVerifier.create(result)
                .verifyComplete();
    }

    private TurmsHandshakeWebSocketService newMockWebSocketService(boolean isNodeActive) {
        Node node = mock(Node.class);
        when(node.isActive()).thenReturn(isNodeActive);

        TurmsProperties properties = new TurmsProperties();
        TurmsPropertiesManager propertiesManager = mock(TurmsPropertiesManager.class);
        when(propertiesManager.getLocalProperties())
                .thenReturn(properties);

        WorkflowMediator workflowMediator = mock(WorkflowMediator.class);
        when(workflowMediator.processLoginRequest(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(Mono.just(TurmsStatusCode.OK));
        when(workflowMediator.rejectLoginRequest(any(), any(), any(), any()))
                .thenReturn(Mono.just(true));

        RequestUpgradeStrategy upgradeStrategy = mock(RequestUpgradeStrategy.class);
        when(upgradeStrategy.upgrade(any(), any(), any(), any()))
                .thenReturn(Mono.empty());

        TurmsHandshakeWebSocketService webSocketService = spy(new TurmsHandshakeWebSocketService(node, propertiesManager, workflowMediator));
        when(webSocketService.getUpgradeStrategy()).thenReturn(upgradeStrategy);
        return webSocketService;
    }

    private MockServerHttpRequest.BaseBuilder<?> getQualifiedUpgradeRequest() {
        return MockServerHttpRequest.get("")
                .header(HttpHeaders.UPGRADE, "WebSocket")
                .header(HttpHeaders.CONNECTION, "Upgrade")
                .header(com.google.common.net.HttpHeaders.SEC_WEBSOCKET_KEY, "key")
                .header(HandshakeRequestUtil.USER_ID_FIELD);
    }

}
