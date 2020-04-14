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

package im.turms.turms.service.message;

import com.google.common.collect.Multimap;
import com.hazelcast.cluster.Member;
import im.turms.common.model.dto.notification.TurmsNotification;
import im.turms.turms.manager.TurmsClusterManager;
import im.turms.turms.manager.TurmsPluginManager;
import im.turms.turms.plugin.RelayedTurmsNotificationHandler;
import im.turms.turms.property.TurmsProperties;
import im.turms.turms.service.user.onlineuser.IrresponsibleUserService;
import im.turms.turms.service.user.onlineuser.OnlineUserService;
import im.turms.turms.service.user.onlineuser.manager.OnlineUserManager;
import im.turms.turms.task.DeliveryTurmsNotificationTask;
import im.turms.turms.util.ReactorUtil;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.Mono;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.*;
import java.util.concurrent.Future;

@Component
@Validated
public class OutboundMessageService {
    private final OnlineUserService onlineUserService;
    private final IrresponsibleUserService irresponsibleUserService;
    private final TurmsClusterManager turmsClusterManager;
    private final TurmsPluginManager turmsPluginManager;
    private final int maxMessageSizeToRelayDirectly;

    public OutboundMessageService(OnlineUserService onlineUserService, TurmsProperties turmsProperties, TurmsClusterManager turmsClusterManager, IrresponsibleUserService irresponsibleUserService, TurmsPluginManager turmsPluginManager) {
        this.onlineUserService = onlineUserService;
        this.turmsClusterManager = turmsClusterManager;
        this.irresponsibleUserService = irresponsibleUserService;
        this.turmsPluginManager = turmsPluginManager;
        this.maxMessageSizeToRelayDirectly = turmsProperties.getMessage().getMaxMessageSizeToRelayDirectly();
    }

    public Mono<Boolean> relayClientMessageToClients(
            @NotNull byte[] messageData,
            @NotNull Set<Long> recipientIds,
            boolean isRelayable) {
        if (recipientIds.isEmpty()) {
            return Mono.just(true);
        } else if (recipientIds.size() == 1) {
            return relayClientMessageToClient(messageData, recipientIds.iterator().next(), isRelayable);
        } else {
            Multimap<Member, Long> userIdsByMember = irresponsibleUserService.getMembersIfExists(recipientIds);
            for (Long recipientId : recipientIds) {
                if (!userIdsByMember.containsValue(recipientId)) {
                    Member member = turmsClusterManager.getMemberByUserId(recipientId);
                    userIdsByMember.put(member, recipientId);
                }
            }
            List<Mono<Boolean>> monoList = new ArrayList<>(userIdsByMember.keySet().size());
            UUID localUuid = turmsClusterManager.getLocalMember().getUuid();
            for (Member member : userIdsByMember.keySet()) {
                if (member.getUuid() == localUuid) {
                    monoList.add(Mono.just(relayClientMessageToLocalClients(messageData, recipientIds)));
                } else {
                    monoList.add(relayClientMessageByRemoteMember(messageData, recipientIds, member));
                }
            }
            return ReactorUtil.areAllTrue(monoList);
        }
    }

    public Mono<Boolean> relayClientMessageToClient(
            @NotNull byte[] messageData,
            @NotNull Long recipientId,
            boolean isRelayable) {
        boolean responsible = turmsClusterManager.isCurrentNodeResponsibleByUserId(recipientId);
        if (responsible) {
            return Mono.just(relayClientMessageToLocalClients(messageData, Collections.singleton(recipientId)));
        } else {
            UUID memberId = irresponsibleUserService.getMemberIdIfExists(recipientId);
            if (memberId != null) {
                if (memberId == turmsClusterManager.getLocalMember().getUuid()) {
                    return Mono.just(relayClientMessageToLocalClients(messageData, Collections.singleton(recipientId)));
                } else if (isRelayable) {
                    return relayClientMessageByRemoteMemberId(messageData, recipientId, memberId);
                }
            } else if (isRelayable) {
                return relayClientMessageByRecipientId(messageData, recipientId);
            }
        }
        return Mono.just(false);
    }

    /**
     * Note that all operations will go here finally
     */
    public boolean relayClientMessageToLocalClients(
            @NotNull byte[] messageData,
            @NotEmpty Set<Long> recipientIds) {
        boolean isSuccess = true;
        WebSocketMessage message = null;
        boolean shouldTriggerHandlers = !turmsPluginManager.getNotificationHandlerList().isEmpty() && turmsClusterManager.getTurmsProperties().getPlugin().isEnabled();
        Set<Long> offlineRecipientIds = shouldTriggerHandlers ? new HashSet<>() : null;
        for (Long recipientId : recipientIds) {
            OnlineUserManager onlineUserManager = onlineUserService.getLocalOnlineUserManager(recipientId);
            if (onlineUserManager != null) {
                List<WebSocketSession> sessions = onlineUserManager.getWebSocketSessions();
                if (!sessions.isEmpty()) {
                    if (message == null) {
                        WebSocketSession session = sessions.get(0);
                        message = session.binaryMessage(factory -> factory.wrap(messageData));
                    }
                    List<FluxSink<WebSocketMessage>> outputSinks = onlineUserManager.getOutputSinks();
                    for (FluxSink<WebSocketMessage> recipientSink : outputSinks) {
                        message.retain();
                        recipientSink.next(message); // This will decrease the reference count of the message
                    }
                } else {
                    isSuccess = false;
                    if (shouldTriggerHandlers) {
                        offlineRecipientIds.add(recipientId);
                    }
                }
            } else {
                isSuccess = false;
                if (shouldTriggerHandlers) {
                    offlineRecipientIds.add(recipientId);
                }
            }
        }
        if (shouldTriggerHandlers) {
            try {
                TurmsNotification notification = TurmsNotification.parseFrom(messageData);
                for (RelayedTurmsNotificationHandler handler : turmsPluginManager.getNotificationHandlerList()) {
                    handler.handle(notification, recipientIds, offlineRecipientIds);
                }
            } catch (Exception ignored) {
            }
        }
        if (message != null) {
            message.release();
        }
        return isSuccess;
    }

    private Mono<Boolean> relayClientMessageByRemoteMemberId(
            @NotNull byte[] messageData,
            @NotNull Long recipientId,
            @NotNull UUID memberId) {
        Member member = turmsClusterManager.getMemberById(memberId);
        if (member != null) {
            return relayClientMessageByRemoteMember(messageData, Collections.singleton(recipientId), member);
        } else {
            return Mono.just(false);
        }
    }

    private Mono<Boolean> relayClientMessageByRemoteMember(
            @NotNull byte[] messageData,
            @NotNull Set<Long> recipientIds,
            @NotNull Member member) {
        if (messageData.length > maxMessageSizeToRelayDirectly && recipientIds.size() == 1) {
            return onlineUserService.checkIfRemoteUserOffline(member, recipientIds.iterator().next())
                    .flatMap(isOnline -> {
                        if (isOnline) {
                            DeliveryTurmsNotificationTask task = new DeliveryTurmsNotificationTask(messageData, recipientIds);
                            Future<Boolean> future = turmsClusterManager.getExecutor()
                                    .submitToMember(task, member);
                            return ReactorUtil.future2Mono(future);
                        } else {
                            return Mono.just(false);
                        }
                    });
        } else {
            DeliveryTurmsNotificationTask task = new DeliveryTurmsNotificationTask(messageData, recipientIds);
            Future<Boolean> future = turmsClusterManager.getExecutor()
                    .submitToMember(task, member);
            return ReactorUtil.future2Mono(future);
        }
    }

    private Mono<Boolean> relayClientMessageByRecipientId(
            @NotNull byte[] messageData,
            @NotNull Long recipientId) {
        Member member = turmsClusterManager.getMemberByUserId(recipientId);
        if (member != null) {
            return relayClientMessageByRemoteMember(messageData, Collections.singleton(recipientId), member);
        } else {
            return Mono.just(false);
        }
    }
}
