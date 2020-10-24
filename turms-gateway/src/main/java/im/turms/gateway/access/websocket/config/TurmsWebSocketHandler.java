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

package im.turms.gateway.access.websocket.config;

import im.turms.common.constant.DeviceType;
import im.turms.common.constant.statuscode.SessionCloseStatus;
import im.turms.common.constant.statuscode.TurmsStatusCode;
import im.turms.common.exception.TurmsBusinessException;
import im.turms.gateway.access.websocket.dto.CloseStatusFactory;
import im.turms.gateway.access.websocket.util.HandshakeRequestUtil;
import im.turms.gateway.manager.UserSessionsManager;
import im.turms.gateway.pojo.bo.session.UserSession;
import im.turms.gateway.pojo.dto.SimpleTurmsRequest;
import im.turms.gateway.service.impl.SessionService;
import im.turms.gateway.service.mediator.WorkflowMediator;
import im.turms.gateway.util.TurmsRequestUtil;
import im.turms.server.common.cluster.node.Node;
import im.turms.server.common.dto.ServiceRequest;
import im.turms.server.common.pojo.CloseReason;
import im.turms.server.common.util.CloseReasonUtil;
import im.turms.server.common.util.ProtoUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.EmptyByteBuf;
import io.netty.buffer.UnpooledByteBufAllocator;
import lombok.extern.log4j.Log4j2;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.NettyDataBufferFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.CloseStatus;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;
import org.springframework.web.reactive.socket.server.upgrade.ReactorNettyRequestUpgradeStrategy;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * @author James Chen
 */
@Component
@Log4j2
public class TurmsWebSocketHandler implements WebSocketHandler {

    private static final EmptyByteBuf EMPTY_BYTE_BUF = new EmptyByteBuf(UnpooledByteBufAllocator.DEFAULT);
    private static final EmptyByteBuf HEARTBEAT_BYTE_BUF = EMPTY_BYTE_BUF;
    private static final EmptyByteBuf PONG_BYTE_BUF = EMPTY_BYTE_BUF;

    private final Node node;
    private final WorkflowMediator workflowMediator;
    private final SessionService sessionService;

    public TurmsWebSocketHandler(
            Node node,
            WorkflowMediator workflowMediator,
            SessionService sessionService) {
        this.node = node;
        this.workflowMediator = workflowMediator;
        this.sessionService = sessionService;
    }

    /**
     * The user has been in "logged in" status when handle() is invoked
     *
     * @see ReactorNettyRequestUpgradeStrategy#upgrade(org.springframework.web.server.ServerWebExchange, org.springframework.web.reactive.socket.WebSocketHandler, java.lang.String, java.util.function.Supplier)
     */
    @Override
    public Mono<Void> handle(WebSocketSession webSocketSession) {
        // 1. Prepare and validate data
        long userId = (long) webSocketSession.getAttributes().get(HandshakeRequestUtil.USER_ID_FIELD);
        DeviceType deviceType = (DeviceType) webSocketSession.getAttributes().get(HandshakeRequestUtil.DEVICE_TYPE_FIELD);

        UserSessionsManager userSessionsManager = sessionService.getUserSessionsManager(userId);
        if (userSessionsManager == null) {
            log.error("The user sessions manager for the user {} is null", userId);
            return workflowMediator.setLocalUserDeviceOffline(userId, deviceType, CloseStatusFactory.get(SessionCloseStatus.SERVER_ERROR)).then();
        }
        UserSession session = userSessionsManager.getSession(deviceType);
        if (session == null) {
            log.error("The user session for the device type {} of the user {} is null", deviceType.name(), userId);
            return workflowMediator.setLocalUserDeviceOffline(userId, deviceType, CloseStatusFactory.get(SessionCloseStatus.SERVER_ERROR)).then();
        }
        session.setWebSocketSession(webSocketSession);

        // 2. Listen to the close event of the WebSocket session
        // to make sure the session information is cleared if it is closed with the close status besides SWITCH
        webSocketSession.closeStatus()
                .subscribe(closeStatus -> trySetOfflineAfterSessionClosed(session, userId, deviceType, closeStatus).subscribe(),
                        log::error);

        // 3. Set up the flux of responses (TurmsNotification) to users' requests
        Flux<WebSocketMessage> responseOutput = webSocketSession.receive()
                .flatMap(inboundMessage -> {
                    if (!node.isActive()) {
                        return Mono.error(TurmsBusinessException.get(TurmsStatusCode.UNAVAILABLE));
                    }
                    switch (inboundMessage.getType()) {
                        case BINARY:
                            DataBuffer payload = inboundMessage.getPayload();
                            if (payload.capacity() == 0) {
                                // Send an binary message instead of a pong frame to make sure that turms-client-js can get the response
                                // or it will be intercepted by some browsers
                                return workflowMediator.processHeartbeatRequest(userId, deviceType)
                                        .thenReturn(webSocketSession.binaryMessage(factory -> ((NettyDataBufferFactory) factory).wrap(HEARTBEAT_BYTE_BUF)));
                            } else {
                                // long traceId = RandomUtil.nextPositiveLong(); TODO: tracing
                                SimpleTurmsRequest turmsRequest = TurmsRequestUtil.parseSimpleRequest(payload.asByteBuffer());
                                ByteBuf requestBuffer = NettyDataBufferFactory.toByteBuf(payload);
                                // FIXME: We use retain() as a workaround for now to fix the bug mentioned in https://github.com/turms-im/turms/issues/430
                                requestBuffer.retain();
                                ServiceRequest request = new ServiceRequest(userId,
                                        deviceType,
                                        turmsRequest.getRequestId(),
                                        turmsRequest.getType(),
                                        requestBuffer);
                                return workflowMediator.processServiceRequest(request)
                                        .map(notification ->
                                                webSocketSession.binaryMessage(factory -> ((NettyDataBufferFactory) factory).wrap(ProtoUtil.getDirectByteBuffer(notification))))
                                        .doOnTerminate(() -> {
                                            while (requestBuffer.refCnt() > 0) {
                                                requestBuffer.release();
                                            }
                                        });
                            }
                        case TEXT:
                        case PING:
                            return Mono.just(webSocketSession.pongMessage(factory -> ((NettyDataBufferFactory) factory).wrap(PONG_BYTE_BUF)));
                        case PONG:
                        default:
                            // ignore the message
                            return Mono.empty();
                    }
                });

        // 4. Merge inbound/outbound messages
        Flux<WebSocketMessage> outputFlux = session.getNotificationSink()
                .asFlux()
                // Note: doOnError will be handled after merged with responseOutput
                .map(byteBuf -> webSocketSession.binaryMessage(factory -> ((NettyDataBufferFactory) factory).wrap(byteBuf)))
                .mergeWith(responseOutput)
                // Note: don't recover even if it's just a client error
                .doOnError(throwable -> {
                    CloseReason closeReason = CloseReasonUtil.parse(throwable);
                    SessionCloseStatus closeStatus = closeReason.getCloseStatus();
                    if (closeStatus.isServerError()) {
                        log.error("Failed to send outbound notification", throwable);
                    }
                    String reason = closeReason.getReason();
                    webSocketSession.close(CloseStatusFactory.get(closeStatus, reason)).subscribe();
                });

        // 5. Trivial things after the session is established
        workflowMediator.onSessionEstablished(userSessionsManager, deviceType);
        workflowMediator.triggerGoOnlinePlugins(userSessionsManager, session).subscribe();

        // 6. Send responses and notifications
        return webSocketSession.send(outputFlux);
    }

    private Mono<Void> trySetOfflineAfterSessionClosed(UserSession session, Long userId, DeviceType deviceType, CloseStatus closeStatus) {
        if (closeStatus.getCode() != SessionCloseStatus.SWITCH.getCode()) {
            session.setWebSocketSession(null);
            session.close(closeStatus);
            return workflowMediator.setLocalUserDeviceOffline(userId, deviceType, closeStatus).then();
        }
        return Mono.empty();
    }

}