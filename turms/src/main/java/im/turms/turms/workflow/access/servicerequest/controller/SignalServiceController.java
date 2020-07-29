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

package im.turms.turms.workflow.access.servicerequest.controller;

import im.turms.common.constant.MessageDeliveryStatus;
import im.turms.common.constant.statuscode.TurmsStatusCode;
import im.turms.common.model.dto.request.signal.AckRequest;
import im.turms.server.common.cluster.node.Node;
import im.turms.turms.workflow.access.servicerequest.dispatcher.ClientRequestHandler;
import im.turms.turms.workflow.access.servicerequest.dispatcher.ServiceRequestMapping;
import im.turms.turms.workflow.access.servicerequest.dto.RequestHandlerResult;
import im.turms.turms.workflow.access.servicerequest.dto.RequestHandlerResultFactory;
import im.turms.turms.workflow.service.impl.message.MessageService;
import im.turms.turms.workflow.service.impl.message.MessageStatusService;
import org.springframework.stereotype.Controller;
import reactor.core.publisher.Mono;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static im.turms.common.model.dto.request.TurmsRequest.KindCase.ACK_REQUEST;

/**
 * @author James Chen
 */
@Controller
public class SignalServiceController {

    private final Node node;
    private final MessageService messageService;
    private final MessageStatusService messageStatusService;

    public SignalServiceController(
            Node node,
            MessageService messageService,
            MessageStatusService messageStatusService) {
        this.node = node;
        this.messageService = messageService;
        this.messageStatusService = messageStatusService;
    }

    /**
     * NOTE: If recipients acknowledge a received message, it DOESN'T mean that the message was read
     */
    @ServiceRequestMapping(ACK_REQUEST)
    public ClientRequestHandler handleAckRequest() {
        return clientRequest -> {
            AckRequest ackRequest = clientRequest.getTurmsRequest().getAckRequest();
            List<Long> messagesIds = ackRequest.getMessagesIdsList();
            if (messagesIds.isEmpty()) {
                return Mono.just(RequestHandlerResultFactory.get(TurmsStatusCode.ILLEGAL_ARGUMENTS, "The list of message ID must not be empty"));
            }
            Set<Long> ids = new HashSet<>(messagesIds);
            if (node.getSharedProperties().getService().getMessage().isDeletePrivateMessageAfterAcknowledged()) {
                return messageService.filterPrivateMessages(ids)
                        .flatMap(privateMessageIds -> {
                            if (!privateMessageIds.isEmpty()) {
                                if (privateMessageIds.size() == ids.size()) {
                                    return messageService.deleteMessages(privateMessageIds, true, false)
                                            .map(RequestHandlerResultFactory::okIfTrue);
                                } else {
                                    ids.removeAll(privateMessageIds);
                                    return authAndUpdateMessagesDeliveryStatus(clientRequest.getUserId(), ids)
                                            .then(messageService.deleteMessages(privateMessageIds, true, false)
                                                    .map(RequestHandlerResultFactory::okIfTrue));
                                }
                            } else {
                                return authAndUpdateMessagesDeliveryStatus(clientRequest.getUserId(), ids);
                            }
                        });
            } else {
                return authAndUpdateMessagesDeliveryStatus(clientRequest.getUserId(), ids);
            }
        };
    }

    private Mono<RequestHandlerResult> authAndUpdateMessagesDeliveryStatus(Long userId, Set<Long> messageIds) {
        return messageStatusService
                .authAndUpdateMessagesDeliveryStatus(userId, messageIds, MessageDeliveryStatus.RECEIVED)
                .map(RequestHandlerResultFactory::okIfTrue);
    }

}
