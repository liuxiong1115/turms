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
import im.turms.turms.manager.TurmsClusterManager;
import im.turms.turms.service.user.onlineuser.IrresponsibleUserService;
import im.turms.turms.service.user.onlineuser.OnlineUserService;
import im.turms.turms.service.user.onlineuser.manager.OnlineUserManager;
import im.turms.turms.task.DeliveryUserMessageTask;
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

    public OutboundMessageService(OnlineUserService onlineUserService, TurmsClusterManager turmsClusterManager, IrresponsibleUserService irresponsibleUserService) {
        this.onlineUserService = onlineUserService;
        this.turmsClusterManager = turmsClusterManager;
        this.irresponsibleUserService = irresponsibleUserService;
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
            for (Member member : userIdsByMember.keySet()) {
                UUID localUuid = turmsClusterManager.getLocalMember().getUuid();
                if (member.getUuid() == localUuid) {
                    monoList.add(Mono.just(relayClientMessageToLocalClients(messageData, recipientIds)));
                } else {
                    monoList.add(relayClientMessageByMember(messageData, recipientIds, member));
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
                    return relayClientMessageByMemberId(messageData, recipientId, memberId);
                }
            } else if (isRelayable) {
                return relayClientMessageByRecipientId(messageData, recipientId);
            }
        }
        return Mono.just(false);
    }

    public boolean relayClientMessageToLocalClients(
            @NotNull byte[] messageData,
            @NotEmpty Set<Long> recipientIds) {
        boolean isSuccess = true;
        WebSocketMessage message = null;
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
                }
            } else {
                isSuccess = false;
            }
        }
        if (message != null) {
            message.release();
        }
        return isSuccess;
    }

    private Mono<Boolean> relayClientMessageByMemberId(
            @NotNull byte[] messageData,
            @NotNull Long recipientId,
            @NotNull UUID memberId) {
        Member member = turmsClusterManager.getMemberById(memberId);
        if (member != null) {
            return relayClientMessageByMember(messageData, Collections.singleton(recipientId), member);
        } else {
            return Mono.just(false);
        }
    }

    private Mono<Boolean> relayClientMessageByMember(
            @NotNull byte[] messageData,
            @NotNull Set<Long> recipientIds,
            @NotNull Member member) {
        DeliveryUserMessageTask task = new DeliveryUserMessageTask(messageData, recipientIds);
        Future<Boolean> future = turmsClusterManager.getExecutor()
                .submitToMember(task, member);
        return ReactorUtil.future2Mono(future);
    }

    private Mono<Boolean> relayClientMessageByRecipientId(
            @NotNull byte[] messageData,
            @NotNull Long recipientId) {
        Member member = turmsClusterManager.getMemberByUserId(recipientId);
        if (member != null) {
            return relayClientMessageByMember(messageData, Collections.singleton(recipientId), member);
        } else {
            return Mono.just(false);
        }
    }
}
