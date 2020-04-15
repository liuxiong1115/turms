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

package im.turms.turms.access.websocket.dispatcher;

import com.google.protobuf.Int32Value;
import com.google.protobuf.Int64Value;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.StringValue;
import com.google.protobuf.util.JsonFormat;
import im.turms.common.TurmsCloseStatus;
import im.turms.common.TurmsStatusCode;
import im.turms.common.constant.DeviceType;
import im.turms.common.exception.TurmsBusinessException;
import im.turms.common.model.dto.notification.TurmsNotification;
import im.turms.common.model.dto.request.TurmsRequest;
import im.turms.turms.annotation.websocket.TurmsRequestMapping;
import im.turms.turms.constant.CloseStatusFactory;
import im.turms.turms.manager.TurmsClusterManager;
import im.turms.turms.manager.TurmsPluginManager;
import im.turms.turms.plugin.ClientRequestHandler;
import im.turms.turms.pojo.bo.RequestResult;
import im.turms.turms.pojo.bo.TurmsRequestWrapper;
import im.turms.turms.pojo.bo.UserSessionId;
import im.turms.turms.pojo.domain.UserActionLog;
import im.turms.turms.service.message.OutboundMessageService;
import im.turms.turms.service.user.UserActionLogService;
import im.turms.turms.service.user.onlineuser.IrresponsibleUserService;
import im.turms.turms.service.user.onlineuser.OnlineUserService;
import im.turms.turms.util.SessionUtil;
import lombok.extern.log4j.Log4j2;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Mono;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;
import java.util.*;
import java.util.function.Function;

@Log4j2
@Service
public class InboundMessageDispatcher {
    private final OutboundMessageService outboundMessageService;
    private final OnlineUserService onlineUserService;
    private final IrresponsibleUserService irresponsibleUserService;
    private final UserActionLogService userActionLogService;
    private final TurmsClusterManager turmsClusterManager;
    private final TurmsPluginManager turmsPluginManager;
    private final EnumMap<TurmsRequest.KindCase, Function<TurmsRequestWrapper, Mono<RequestResult>>> router;
    private final boolean pluginEnabled;
    private final boolean logUserAction;
    private final JsonFormat.Printer jsonPrinter;

    public InboundMessageDispatcher(ApplicationContext context, OutboundMessageService outboundMessageService, OnlineUserService onlineUserService, TurmsClusterManager turmsClusterManager, TurmsPluginManager turmsPluginManager, UserActionLogService userActionLogService, IrresponsibleUserService irresponsibleUserService) {
        router = new EnumMap<>(TurmsRequest.KindCase.class);
        this.outboundMessageService = outboundMessageService;
        Map<String, Object> beans = context.getBeansWithAnnotation(TurmsRequestMapping.class);
        for (Map.Entry<String, Object> entry : beans.entrySet()) {
            Function<TurmsRequestWrapper, Mono<RequestResult>> callback = (Function<TurmsRequestWrapper, Mono<RequestResult>>) entry.getValue();
            TurmsRequestMapping mapping = getMapping(callback, entry.getKey());
            if (mapping != null) {
                router.put(mapping.value(), callback);
            }
        }
        this.onlineUserService = onlineUserService;
        this.turmsClusterManager = turmsClusterManager;
        this.turmsPluginManager = turmsPluginManager;
        this.userActionLogService = userActionLogService;
        pluginEnabled = turmsClusterManager.getTurmsProperties().getPlugin().isEnabled();
        logUserAction = turmsClusterManager.getTurmsProperties().getLog().isLogUserAction();
        if (logUserAction) {
            jsonPrinter = JsonFormat.printer();
        } else {
            jsonPrinter = null;
        }
        this.irresponsibleUserService = irresponsibleUserService;
    }

    private TurmsRequestMapping getMapping(Function<TurmsRequestWrapper, Mono<RequestResult>> request, String methodName) {
        String className = request.getClass().getName().split("\\$")[0];
        try {
            Class<?> targetClass = getClass().getClassLoader().loadClass(className);
            return (TurmsRequestMapping) targetClass.getMethod(methodName).getAnnotations()[0];
        } catch (ClassNotFoundException | NoSuchMethodException e) {
            return null;
        }
    }

    public Mono<WebSocketMessage> dispatch(@NotNull WebSocketSession session, @NotNull WebSocketMessage webSocketMessage) {
        Long userId = SessionUtil.getUserIdFromSession(session);
        DeviceType deviceType = SessionUtil.getDeviceTypeFromSession(session);
        if (userId != null) {
            if (!turmsClusterManager.isServing()) {
                onlineUserService.setLocalUserOffline(userId, CloseStatusFactory.get(TurmsCloseStatus.SERVER_UNAVAILABLE));
                return Mono.empty();
            }
            // The code is to handle the edge case that a user is logging in and ready to become logged status (online)
            // and the responsibility of cluster members is redistributing, and members are disconnecting online users.
            // If it's done before the user becomes online status, the user will be connecting to an irresponsible server that cannot realize it.
            String serverAddress = turmsClusterManager.getAddressIfCurrentNodeIrresponsibleByUserId(userId);
            if (serverAddress != null && !irresponsibleUserService.isIrresponsibleUserServedByCurrentNode(userId)) {
                onlineUserService.setLocalUserOffline(userId, CloseStatusFactory.get(TurmsCloseStatus.REDIRECT, serverAddress));
                return Mono.empty();
            }

            onlineUserService.updateHeartbeatTimestamp(userId, deviceType);
            switch (webSocketMessage.getType()) {
                case BINARY:
                    return handleBinaryMessage(userId, deviceType, webSocketMessage, session);
                case TEXT:
                case PING:
                    return Mono.just(session.pongMessage(DataBufferFactory::allocateBuffer));
                case PONG:
                default:
                    return Mono.empty();
            }
        } else {
            // This should never happen
            return session.close(CloseStatusFactory.get(TurmsCloseStatus.SERVER_ERROR, "The user ID is missing"))
                    .then(Mono.error(new NoSuchElementException("The user ID is missing in session")));
        }
    }

    private Mono<Boolean> notifyRelatedUsersOfAction(
            @NotNull RequestResult requestResult,
            @NotNull Long requesterId,
            @NotNull DeviceType requesterDevice) {
        TurmsRequest dataForRecipients = requestResult.getDataForRecipients();
        if (dataForRecipients != null && !requestResult.getRecipients().isEmpty()) {
            byte[] data = TurmsNotification
                    .newBuilder()
                    .setRelayedRequest(dataForRecipients)
                    .setRequesterId(Int64Value.newBuilder().setValue(requesterId).build())
                    .build()
                    .toByteArray();
            UserSessionId senderSessionId = null;
            if (requestResult.relayDataToOtherSenderOnlineDevices) {
                senderSessionId = new UserSessionId(requesterId, requesterDevice);
            }
            return outboundMessageService.relayClientMessageToClients(
                    data,
                    requestResult.getRecipients(),
                    senderSessionId,
                    true);
        } else {
            return Mono.just(true);
        }
    }

    /**
     * Convert RequestResult to WebSocketMessage.
     * Scenario 1: If Mono<RequestResult> returns a RequestResult object -> TurmsStatusCode.getCode()
     * Scenario 2: If Mono<RequestResult> is Mono.empty() -> TurmsStatusCode.NO_CONTENT
     * Scenario 3: If Mono<RequestResult> throws a TurmsBusinessException -> TurmsStatusCode.getCode()
     * Scenario 4: If Mono<RequestResult> throws an exception of other types -> TurmsStatusCode.FAILED and throwable.getMessage()
     */
    private Mono<WebSocketMessage> handleResult(
            @NotNull WebSocketSession session,
            @NotNull Mono<RequestResult> result,
            @Nullable Long requestId,
            @NotNull Long requesterId,
            @NotNull DeviceType requesterDevice) {
        return result
                .defaultIfEmpty(RequestResult.NO_CONTENT)
                .onErrorResume(throwable -> {
                    if (throwable instanceof TurmsBusinessException) {
                        return Mono.just(RequestResult.create(((TurmsBusinessException) throwable).getCode()));
                    } else {
                        String message;
                        if (turmsClusterManager.getTurmsProperties().getSecurity().isRespondStackTraceIfException()) {
                            message = throwable.getMessage().concat(Arrays.toString(throwable.getStackTrace()));
                        } else {
                            message = throwable.getMessage();
                        }
                        return Mono.just(RequestResult.create(TurmsStatusCode.FAILED, message));
                    }
                })
                .flatMap(requestResult -> {
                    if (requestResult.getCode() == TurmsStatusCode.OK) {
                        return handleSuccessResult(requestResult, session, requestId, requesterId, requesterDevice);
                    } else {
                        return handleFailResult(requestResult, session, requestId);
                    }
                });
    }

    public Mono<WebSocketMessage> handleBinaryMessage(@NotNull Long userId, @NotNull DeviceType deviceType,
                                                      @NotNull WebSocketMessage message, @NotNull WebSocketSession session) {
        DataBuffer payload = message.getPayload();
        if (payload.capacity() == 0) {
            // Send an binary message instead of a pong frame to make sure turms-client-js can get the response
            return Mono.just(session.binaryMessage(DataBufferFactory::allocateBuffer));
        }
        try {
            TurmsRequest request = TurmsRequest.parseFrom(payload.asByteBuffer());
            if (request.getKindCase() != TurmsRequest.KindCase.KIND_NOT_SET) {
                Function<TurmsRequestWrapper, Mono<RequestResult>> handler = router.get(request.getKindCase());
                if (handler != null) {
                    Mono<TurmsRequestWrapper> wrapperMono = Mono.just(new TurmsRequestWrapper(
                            request, userId, deviceType, message, session));
                    if (pluginEnabled) {
                        List<ClientRequestHandler> handlerList = turmsPluginManager.getClientRequestHandlerList();
                        for (ClientRequestHandler clientRequestHandler : handlerList) {
                            wrapperMono = wrapperMono.flatMap(clientRequestHandler::transform);
                        }
                    }
                    Mono<RequestResult> result = wrapperMono.flatMap(requestWrapper -> {
                        Mono<RequestResult> requestResultMono = Mono.empty();
                        if (pluginEnabled) {
                            List<ClientRequestHandler> handlerList = turmsPluginManager.getClientRequestHandlerList();
                            for (ClientRequestHandler clientRequestHandler : handlerList) {
                                requestResultMono = requestResultMono
                                        .switchIfEmpty(clientRequestHandler.handleTurmsRequest(requestWrapper));
                            }
                        }
                        boolean triggerHandlers = pluginEnabled && !turmsPluginManager.getLogHandlerList().isEmpty();
                        if (logUserAction || triggerHandlers) {
                            Integer ip = SessionUtil.getIp(session);
                            UserActionLog actionLog;
                            try {
                                actionLog = new UserActionLog(turmsClusterManager.generateRandomId(), requestWrapper.getUserId(),
                                        requestWrapper.getDeviceType(), new Date(), ip, request.getKindCase().name(), jsonPrinter.print(request));
                            } catch (InvalidProtocolBufferException e) {
                                log.error(e.getMessage(), e);
                                return requestResultMono;
                            }
                            Mono<?> mono;
                            if (logUserAction) {
                                if (triggerHandlers) {
                                    mono = userActionLogService.save(actionLog)
                                            .doOnTerminate(userActionLogService.triggerLogHandlers(actionLog)::subscribe);
                                } else {
                                    mono = userActionLogService.save(actionLog);
                                }
                            } else {
                                mono = userActionLogService.triggerLogHandlers(actionLog);
                            }
                            requestResultMono = mono.then(requestResultMono);
                        }
                        return requestResultMono.switchIfEmpty(handler.apply(requestWrapper));
                    });
                    Long requestId = request.hasRequestId() ? request.getRequestId().getValue() : null;
                    return handleResult(session, result, requestId, userId, deviceType);
                } else {
                    onlineUserService.setLocalUserDeviceOffline(userId, deviceType, CloseStatusFactory.get(TurmsCloseStatus.ILLEGAL_REQUEST, "No handler for the request"));
                }
            } else {
                onlineUserService.setLocalUserDeviceOffline(userId, deviceType, CloseStatusFactory.get(TurmsCloseStatus.ILLEGAL_REQUEST));
            }
        } catch (Exception e) {
            onlineUserService.setLocalUserDeviceOffline(userId, deviceType, CloseStatusFactory.get(TurmsCloseStatus.ILLEGAL_REQUEST, e.getMessage()));
        }
        return Mono.empty();
    }

    private TurmsNotification.Builder generateNotificationBuilder(RequestResult requestResult, TurmsStatusCode code, long requestId) {
        TurmsNotification.Builder builder = TurmsNotification.newBuilder();
        String reason = requestResult.getReason();
        TurmsNotification.Data dataForRequester = requestResult.getDataForRequester();
        if (reason != null) {
            builder.setReason(StringValue.newBuilder().setValue(reason).build());
        }
        if (dataForRequester != null) {
            builder.setData(dataForRequester);
        }
        Int32Value businessCode = Int32Value.newBuilder()
                .setValue(code.getBusinessCode()).build();
        return builder
                .setCode(businessCode)
                .setRequestId(Int64Value.newBuilder().setValue(requestId).build());
    }

    private Mono<WebSocketMessage> handleSuccessResult(RequestResult requestResult, WebSocketSession session, Long requestId, Long requesterId, DeviceType requesterDevice) {
        return notifyRelatedUsersOfAction(requestResult, requesterId, requesterDevice)
                .flatMap(success -> {
                    if (requestId != null) {
                        TurmsNotification notification = generateNotificationBuilder(requestResult, requestResult.getCode(), requestId)
                                .build();
                        return Mono.just(session.binaryMessage(dataBufferFactory -> dataBufferFactory.wrap(notification.toByteArray())));
                    } else {
                        return Mono.empty();
                    }
                });
    }

    private Mono<WebSocketMessage> handleFailResult(RequestResult requestResult, WebSocketSession session, Long requestId) {
        if (requestId != null) {
            TurmsNotification notification = generateNotificationBuilder(requestResult, requestResult.getCode(), requestId)
                    .build();
            return Mono.just(session.binaryMessage(dataBufferFactory -> dataBufferFactory
                    .wrap(notification.toByteArray())));
        } else {
            return Mono.empty();
        }
    }
}