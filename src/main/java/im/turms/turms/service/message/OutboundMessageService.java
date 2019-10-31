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

import com.hazelcast.core.Member;
import im.turms.turms.cluster.TurmsClusterManager;
import im.turms.turms.common.ReactorUtil;
import im.turms.turms.pojo.request.TurmsRequest;
import im.turms.turms.pojo.response.TurmsResponse;
import im.turms.turms.service.user.onlineuser.OnlineUserManager;
import im.turms.turms.service.user.onlineuser.OnlineUserService;
import im.turms.turms.task.DeliveryUserMessageTask;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.Mono;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.concurrent.Future;

@Component
public class OutboundMessageService {
    private final OnlineUserService onlineUserService;
    private final TurmsClusterManager turmsClusterManager;

    public OutboundMessageService(OnlineUserService onlineUserService, TurmsClusterManager turmsClusterManager) {
        this.onlineUserService = onlineUserService;
        this.turmsClusterManager = turmsClusterManager;
    }

    //TODO: separate the logic
    public Mono<Boolean> relayClientMessageToClient(
            @Nullable WebSocketMessage clientMessage,
            @Nullable byte[] clientMessageBytes,
            @NotNull Long recipientId,
            boolean relayable) {
        if (clientMessage == null && clientMessageBytes == null) {
            throw new IllegalArgumentException();
        }
        boolean responsible = turmsClusterManager.isCurrentNodeResponsibleByUserId(recipientId);
        if (responsible) {
            OnlineUserManager onlineUserManager = onlineUserService.getLocalOnlineUserManager(recipientId);
            if (onlineUserManager != null) {
                if (clientMessage == null) {
                    List<WebSocketSession> sessions = onlineUserManager.getWebSocketSessions();
                    if (!sessions.isEmpty()) {
                        WebSocketSession session = sessions.get(0);
                        clientMessage = session.binaryMessage(dataBufferFactory ->
                                dataBufferFactory.wrap(clientMessageBytes));
                    } else {
                        return Mono.just(true);
                    }
                }
                List<FluxSink<WebSocketMessage>> outputSinks = onlineUserManager.getOutputSinks();
                for (FluxSink<WebSocketMessage> recipientSink : outputSinks) {
                    recipientSink.next(clientMessage);
                }
                return Mono.just(true);
            }
        } else if (relayable) {
            Member member = turmsClusterManager.getMemberByUserId(recipientId);
            if (member != null) {
                DeliveryUserMessageTask task = new DeliveryUserMessageTask(clientMessageBytes, recipientId);
                Future<Boolean> future = turmsClusterManager.getExecutor()
                        .submitToMember(task, member);
                return ReactorUtil.future2Mono(future);
            }
        }
        return Mono.just(false);
    }

    public void relayClientMessageToClient(
            @NotNull WebSocketSession session,
            @NotNull FluxSink<WebSocketMessage> outputSink,
            @NotNull TurmsRequest clientMessage) {
        WebSocketMessage message = session.binaryMessage(dataBufferFactory ->
                dataBufferFactory.wrap(clientMessage.toByteArray()));
        outputSink.next(message);
    }

    // Server message -> client
    public boolean sendServerMessageToClient(TurmsResponse serverMessage, Long receiverId) {
        //Check whether the recipient connects to the local note.
        OnlineUserManager manager = onlineUserService.getLocalOnlineUserManager(receiverId);
        if (manager != null) {
            List<WebSocketSession> recipientSessions = manager.getWebSocketSessions();
            if (recipientSessions != null && !recipientSessions.isEmpty()) {
                for (WebSocketSession recipientSession : recipientSessions) {
                    sendServerMessageToClient(recipientSession, serverMessage);
                }
                return true;
            }
        }
        return false;
    }

    public void sendServerMessageToClient(WebSocketSession session, TurmsResponse serverMessage) {
        if (session != null && serverMessage != null) {
            WebSocketMessage binaryMessage = session.binaryMessage(dataBufferFactory ->
                    dataBufferFactory.wrap(serverMessage.toByteArray())
            );
            session.send(Mono.just(binaryMessage)).subscribe();
        }
    }
}
