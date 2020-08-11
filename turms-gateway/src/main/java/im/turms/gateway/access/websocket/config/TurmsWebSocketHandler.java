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
import im.turms.gateway.service.impl.SessionService;
import im.turms.gateway.service.mediator.WorkflowMediator;
import im.turms.gateway.util.TurmsRequestUtil;
import im.turms.server.common.cluster.node.Node;
import im.turms.server.common.dto.ServiceRequest;
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
            return disconnect(webSocketSession, userId, deviceType, CloseStatus.SERVER_ERROR);
        }
        UserSession session = userSessionsManager.getSession(deviceType);
        if (session == null) {
            log.error("The user session for the device type {} of the user {} is null", deviceType.name(), userId);
            return disconnect(webSocketSession, userId, deviceType, CloseStatus.SERVER_ERROR);
        }
        session.setWebSocketSession(webSocketSession);

        // 2. Set up the flux of responses (TurmsNotification) to users' requests
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
//                                long traceId = ThreadLocalRandom.current().nextLong(); TODO: tracing
                                long requestId = TurmsRequestUtil.parseRequestId(payload.asByteBuffer());
                                ByteBuf requestBuffer = NettyDataBufferFactory.toByteBuf(payload);
                                // FIXME: We use retain() as a workaround for now to fix the bug mentioned in https://github.com/turms-im/turms/issues/430
                                requestBuffer.retain();
                                ServiceRequest request = new ServiceRequest(userId, deviceType, requestId, requestBuffer);
                                return workflowMediator.processServiceRequest(request)
                                        .map(notification ->
                                                webSocketSession.binaryMessage(factory -> ((NettyDataBufferFactory) factory).wrap(ProtoUtil.getByteBuffer(notification))))
                                        .doOnTerminate(() -> {
                                            while (requestBuffer.refCnt() != 0) {
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
                })
                // doOnError will be handled by webSocketMessageFlux
                .doOnComplete(() -> workflowMediator.setLocalUserDeviceOffline(userId, deviceType, CloseStatusFactory.get(SessionCloseStatus.DISCONNECTED_BY_CLIENT)).subscribe());

        // 3. Merge inbound/outbound messages
        Flux<WebSocketMessage> webSocketMessageFlux = session.getNotificationSink()
                .asFlux()
                // doOnError will be handled by webSocketMessageFlux
                .doOnComplete(() -> workflowMediator.setLocalUserDeviceOffline(userId, deviceType, CloseStatusFactory.get(SessionCloseStatus.DISCONNECTED_BY_ADMIN)).subscribe())
                .map(byteBuf -> webSocketSession.binaryMessage(factory -> ((NettyDataBufferFactory) factory).wrap(byteBuf)))
                .mergeWith(responseOutput);

        // 4. Trivial things after session is established
        workflowMediator.onSessionEstablished(userSessionsManager, deviceType);
        workflowMediator.triggerGoOnlinePlugins(userSessionsManager, session).subscribe();

        // 5. Make sure the sessions information is cleared when the WebSocket session is closed
        // Note that although the code seems redundant, it's really useful when the underlying frameworks have bugs.
        // In that case, setLocalUserDeviceOffline may not be triggered in other places but only can be triggered here.
        // FIXME: A known bug: https://github.com/reactor/reactor-netty/issues/1091#issuecomment-665334373
        webSocketSession.closeStatus()
                .subscribe(closeStatus -> workflowMediator.setLocalUserDeviceOffline(userId, deviceType, CloseStatusFactory.get(SessionCloseStatus.DISCONNECTED_BY_ADMIN)).subscribe(),
                        throwable -> workflowMediator.setLocalUserDeviceOffline(userId, deviceType, CloseStatusFactory.get(SessionCloseStatus.SERVER_ERROR)).subscribe());

        // 6. Send responses and notifications
        return webSocketSession.send(webSocketMessageFlux
                .doOnError(throwable -> {
                    SessionCloseStatus closeStatus;
                    String reason = null;
                    if (throwable instanceof TurmsBusinessException) {
                        TurmsBusinessException exception = (TurmsBusinessException) throwable;
                        TurmsStatusCode code = exception.getCode();
                        switch (code) {
                            case UNAVAILABLE:
                                closeStatus = SessionCloseStatus.SERVER_UNAVAILABLE;
                                break;
                            case ILLEGAL_ARGUMENTS:
                                closeStatus = SessionCloseStatus.ILLEGAL_REQUEST;
                                break;
                            default:
                                closeStatus = SessionCloseStatus.SERVER_ERROR;
                                reason = throwable.toString();
                                log.error(throwable);
                                break;
                        }
                    } else {
                        closeStatus = SessionCloseStatus.SERVER_ERROR;
                        reason = throwable.toString();
                        log.error(throwable);
                    }
                    workflowMediator.setLocalUserDeviceOffline(userId, deviceType, CloseStatusFactory.get(closeStatus, reason))
                            .subscribe();
                }));
    }

    private Mono<Void> disconnect(WebSocketSession session, Long userId, DeviceType deviceType, CloseStatus closeStatus) {
        return workflowMediator.setLocalUserDeviceOffline(userId, deviceType, closeStatus)
                .then(session.close(closeStatus));
    }

}
