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

package im.turms.gateway.access.tcp;

import com.google.protobuf.Int32Value;
import com.google.protobuf.Int64Value;
import com.google.protobuf.StringValue;
import im.turms.common.constant.statuscode.SessionCloseStatus;
import im.turms.common.model.dto.notification.TurmsNotification;
import im.turms.common.model.dto.request.TurmsRequest;
import im.turms.common.util.RandomUtil;
import im.turms.gateway.access.tcp.controller.SessionController;
import im.turms.gateway.access.tcp.dto.RequestHandlerResult;
import im.turms.gateway.access.tcp.factory.TcpServerFactory;
import im.turms.gateway.access.tcp.model.UserSessionWrapper;
import im.turms.gateway.access.tcp.util.TurmsNotificationUtil;
import im.turms.gateway.constant.ErrorMessage;
import im.turms.gateway.pojo.bo.session.UserSession;
import im.turms.gateway.pojo.dto.SimpleTurmsRequest;
import im.turms.gateway.service.mediator.ServiceMediator;
import im.turms.gateway.util.TurmsRequestUtil;
import im.turms.server.common.dto.CloseReason;
import im.turms.server.common.dto.ServiceRequest;
import im.turms.server.common.pojo.ThrowableInfo;
import im.turms.server.common.property.TurmsPropertiesManager;
import im.turms.server.common.util.CloseReasonUtil;
import im.turms.server.common.util.ProtoUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.EmptyByteBuf;
import io.netty.buffer.UnpooledByteBufAllocator;
import io.netty.util.HashedWheelTimer;
import io.netty.util.Timeout;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.netty.Connection;
import reactor.netty.DisposableServer;

import javax.annotation.PreDestroy;
import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;

/**
 * @author James Chen
 */
@Log4j2
@Component
public class TcpDispatcher {

    private static final ByteBuf HEARTBEAT_RESPONSE = new EmptyByteBuf(UnpooledByteBufAllocator.DEFAULT);

    private final DisposableServer server;
    private final ServiceMediator serviceMediator;
    private final SessionController sessionController;
    private final HashedWheelTimer idleConnectionTimeoutTimer;
    private final int closeIdleConnectionAfter;

    public TcpDispatcher(TurmsPropertiesManager propertiesManager,
                         ServiceMediator serviceMediator,
                         SessionController sessionController) {
        this.serviceMediator = serviceMediator;
        this.sessionController = sessionController;
        closeIdleConnectionAfter = propertiesManager.getLocalProperties().getGateway().getTcp().getCloseIdleConnectionAfterSeconds();
        this.idleConnectionTimeoutTimer = closeIdleConnectionAfter > 0
                ? new HashedWheelTimer()
                : null;
        server = TcpServerFactory.create(propertiesManager, (inbound, outbound) -> {
            Connection connection = (Connection) inbound;
            InetSocketAddress address = (InetSocketAddress) connection.address();
            String ip = address.getHostString();
            UserSessionWrapper sessionWrapper = new UserSessionWrapper(connection);
            Timeout idleConnectionTimeout = idleConnectionTimeoutTimer == null
                    ? null
                    : addIdleConnectionTimeoutTask(connection);
            connection.inbound()
                    .receive()
                    .doOnNext(data -> {
                        if (!connection.isDisposed()) {
                            // Note that handleRequestData should never return MonoError
                            Mono<ByteBuf> response = handleRequest(sessionWrapper, data, ip, idleConnectionTimeout);
                            connection.outbound()
                                    .send(response, byteBuf -> true)
                                    .then()
                                    .subscribe();
                        }
                    })
                    .subscribe();
            return connection.onDispose();
        });
    }

    private Timeout addIdleConnectionTimeoutTask(Connection connection) {
        return idleConnectionTimeoutTimer.newTimeout(timeout -> {
            // TODO: Use a new close status
            CloseReason closeReason = CloseReason.get(SessionCloseStatus.HEARTBEAT_TIMEOUT);
            TurmsNotification notification = CloseReasonUtil.toNotification(closeReason);
            connection.outbound().sendObject(notification)
                    .then(unused -> connection.dispose())
                    .then()
                    .subscribe();
        }, closeIdleConnectionAfter, TimeUnit.SECONDS);
    }

    @PreDestroy
    public void preDestroy() {
        if (server != null) {
            server.dispose();
        }
    }

    /**
     * @implNote If a throwable instance is thrown for failing to handle the client request,
     * the method should recover it to TurmsNotification.
     * So the method should never return MonoError and it should be considered as a bug if it occurs.
     */
    private Mono<ByteBuf> handleRequest(UserSessionWrapper sessionWrapper, ByteBuf data, String ip, Timeout idleConnectionTimeout) {
        if (data.isReadable()) {
            SimpleTurmsRequest request = TurmsRequestUtil.parseSimpleRequest(data.nioBuffer());
            TurmsRequest.KindCase requestType = request.getType();
            Mono<TurmsNotification> notificationMono;
            switch (requestType) {
                case CREATE_SESSION_REQUEST:
                    notificationMono = sessionController.handleCreateSessionRequest(sessionWrapper, request.getCreateSessionRequest(), ip, idleConnectionTimeout)
                            .map(result -> getNotificationFromHandlerResult(result, request.getRequestId()));
                    break;
                case DELETE_SESSION_REQUEST:
                    notificationMono = sessionController.handleDeleteSessionRequest(sessionWrapper);
                    break;
                default:
                    notificationMono = handleServiceRequest(sessionWrapper, request, data);
                    break;
            }
            return notificationMono
                    .onErrorResume(throwable -> {
                        ThrowableInfo info = ThrowableInfo.get(throwable);
                        if (info.getCode().isServerError()) {
                            log.error(ErrorMessage.FAILED_TO_HANDLE_SERVICE_REQUEST_WITH_REQUEST, request, throwable);
                        }
                        return Mono.just(info.toNotification(request.getRequestId()));
                    })
                    .map(ProtoUtil::getDirectByteBuffer);
        } else {
            return handleHeartbeatRequest(sessionWrapper)
                    .flatMap(updated -> updated ? Mono.just(HEARTBEAT_RESPONSE) : Mono.empty());
        }
    }

    private Mono<Boolean> handleHeartbeatRequest(UserSessionWrapper sessionWrapper) {
        UserSession session = sessionWrapper.getUserSession();
        if (session != null) {
            return serviceMediator.processHeartbeatRequest(session.getUserId(), session.getDeviceType());
        } else {
            return Mono.just(false);
        }
    }

    private Mono<TurmsNotification> handleServiceRequest(UserSessionWrapper sessionWrapper, SimpleTurmsRequest request, ByteBuf data) {
        UserSession session = sessionWrapper.getUserSession();
        if (session == null) {
            return Mono.just(TurmsNotificationUtil.sessionClosed(request.getRequestId()));
        }
        ServiceRequest serviceRequest = new ServiceRequest(RandomUtil.nextPositiveLong(),
                session.getUserId(),
                session.getDeviceType(),
                request.getRequestId(),
                request.getType(),
                data);
        return serviceMediator.processServiceRequest(serviceRequest);
    }

    private TurmsNotification getNotificationFromHandlerResult(RequestHandlerResult result, long requestId) {
        TurmsNotification.Builder builder = TurmsNotification.newBuilder()
                .setRequestId(Int64Value.newBuilder().setValue(requestId).build())
                .setCode(Int32Value.newBuilder().setValue(result.getCode().getBusinessCode()).build());
        String reason = result.getReason();
        if (reason != null) {
            builder.setReason(StringValue.newBuilder().setValue(reason).build());
        }
        return builder.build();
    }

}