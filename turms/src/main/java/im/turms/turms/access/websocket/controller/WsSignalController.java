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

package im.turms.turms.access.websocket.controller;

import im.turms.turms.annotation.websocket.TurmsRequestMapping;
import im.turms.turms.common.TurmsStatusCode;
import im.turms.turms.constant.MessageDeliveryStatus;
import im.turms.turms.pojo.bo.RequestResult;
import im.turms.turms.pojo.bo.TurmsRequestWrapper;
import im.turms.turms.pojo.request.TurmsRequest;
import im.turms.turms.pojo.request.signal.AckRequest;
import im.turms.turms.service.message.MessageService;
import im.turms.turms.service.message.MessageStatusService;
import org.springframework.stereotype.Controller;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.function.Function;

@Controller
public class WsSignalController {
    private final MessageService messageService;
    private final MessageStatusService messageStatusService;

    public WsSignalController(MessageService messageService, MessageStatusService messageStatusService) {
        this.messageService = messageService;
        this.messageStatusService = messageStatusService;
    }

    @TurmsRequestMapping(TurmsRequest.KindCase.ACK_REQUEST)
    public Function<TurmsRequestWrapper, Mono<RequestResult>> handleAckRequest() {
        return turmsRequestWrapper -> {
            AckRequest ackRequest = turmsRequestWrapper.getTurmsRequest().getAckRequest();
            List<Long> messagesIds = ackRequest.getMessagesIdsList();
            if (messagesIds.isEmpty()) {
                return Mono.just(RequestResult.status(TurmsStatusCode.ILLEGAL_ARGUMENTS));
            }
            return messageStatusService
                    .authAndUpdateMessagesDeliveryStatus(
                            turmsRequestWrapper.getUserId(),
                            messagesIds,
                            MessageDeliveryStatus.RECEIVED)
                    .map(RequestResult::okIfTrue);
        };
    }
}
