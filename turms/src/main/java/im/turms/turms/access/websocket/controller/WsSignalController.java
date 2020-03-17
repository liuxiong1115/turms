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

import im.turms.common.TurmsStatusCode;
import im.turms.common.constant.MessageDeliveryStatus;
import im.turms.common.model.dto.request.signal.AckRequest;
import im.turms.turms.annotation.websocket.TurmsRequestMapping;
import im.turms.turms.pojo.bo.RequestResult;
import im.turms.turms.pojo.bo.TurmsRequestWrapper;
import im.turms.turms.service.message.MessageStatusService;
import org.springframework.stereotype.Controller;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.function.Function;

import static im.turms.common.model.dto.request.TurmsRequest.KindCase.ACK_REQUEST;

@Controller
public class WsSignalController {
    private final MessageStatusService messageStatusService;

    public WsSignalController(MessageStatusService messageStatusService) {
        this.messageStatusService = messageStatusService;
    }

    /**
     * NOTE: If recipients acknowledged the message sent to them, it DOESN'T mean that the message is read
     * and only means the message is received
     */
    @TurmsRequestMapping(ACK_REQUEST)
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
