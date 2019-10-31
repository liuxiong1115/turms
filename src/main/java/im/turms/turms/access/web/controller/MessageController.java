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

package im.turms.turms.access.web.controller;

import im.turms.turms.access.web.util.ResponseFactory;
import im.turms.turms.annotation.web.RequiredPermission;
import im.turms.turms.common.DateTimeUtil;
import im.turms.turms.common.PageUtil;
import im.turms.turms.common.TurmsStatusCode;
import im.turms.turms.constant.AdminPermission;
import im.turms.turms.constant.ChatType;
import im.turms.turms.constant.DivideBy;
import im.turms.turms.constant.MessageDeliveryStatus;
import im.turms.turms.pojo.domain.Message;
import im.turms.turms.service.message.MessageService;
import org.apache.commons.lang3.EnumUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.*;

import static im.turms.turms.common.Constants.*;

@RestController
@RequestMapping("/messages")
public class MessageController {
    private final MessageService messageService;
    private final PageUtil pageUtil;
    private final DateTimeUtil dateTimeUtil;

    public MessageController(MessageService messageService, PageUtil pageUtil, DateTimeUtil dateTimeUtil) {
        this.messageService = messageService;
        this.pageUtil = pageUtil;
        this.dateTimeUtil = dateTimeUtil;
    }

    @GetMapping
    @RequiredPermission(AdminPermission.MESSAGE_QUERY)
    public Mono<ResponseEntity> getCompleteMessages(
            @RequestParam(required = false) String chatType,
            @RequestParam(required = false) Long fromId,
            @RequestParam(required = false) Long toId,
            @RequestParam(required = false) Date startDate,
            @RequestParam(required = false) Date endDate,
            @RequestParam(required = false) MessageDeliveryStatus deliveryStatus,
            @RequestParam(defaultValue = "0") Integer size) {
        Flux<Message> completeMessages = messageService.queryCompleteMessages(
                false,
                EnumUtils.getEnum(ChatType.class, chatType),
                fromId,
                toId,
                startDate,
                endDate,
                deliveryStatus,
                pageUtil.getSize(size));
        return ResponseFactory.okWhenTruthy(completeMessages);
    }

    @PostMapping
    @RequiredPermission(AdminPermission.MESSAGE_CREATE)
    public Mono<ResponseEntity> createMessages(
            @RequestParam(defaultValue = "true") Boolean deliver,
            @RequestBody Message message) {
        if (message.getTargetId() == null ||
                (message.getText() == null && message.getRecords() == null)) {
            throw new IllegalArgumentException();
        }
        Mono<Boolean> success = messageService.sendAdminMessage(deliver, message);
        return ResponseFactory.okWhenTruthy(success);
    }

    @DeleteMapping
    @RequiredPermission(AdminPermission.MESSAGE_DELETE)
    public Mono<ResponseEntity> deleteMessages(
            @RequestParam Set<Long> messagesIds,
            @RequestParam(defaultValue = "false") Boolean deleteMessagesStatuses,
            @RequestParam(required = false) Boolean logicalDelete) {
        Mono<Boolean> deleted = messageService
                .deleteMessages(messagesIds, deleteMessagesStatuses, logicalDelete);
        return ResponseFactory.acknowledged(deleted);
    }

    @GetMapping("/count")
    @RequiredPermission(AdminPermission.MESSAGE_QUERY)
    public Mono<ResponseEntity> countMessages(
            @RequestParam(required = false) ChatType chatType,
            @RequestParam(required = false) Date deliveredStartDate,
            @RequestParam(required = false) Date deliveredEndDate,
            @RequestParam(required = false) Date deliveredOnAverageStartDate,
            @RequestParam(required = false) Date deliveredOnAverageEndDate,
            @RequestParam(required = false) Date acknowledgedStartDate,
            @RequestParam(required = false) Date acknowledgedEndDate,
            @RequestParam(required = false) Date acknowledgedOnAverageStartDate,
            @RequestParam(required = false) Date acknowledgedOnAverageEndDate,
            @RequestParam(defaultValue = "NOOP") DivideBy divideBy) {
        if (chatType == ChatType.UNRECOGNIZED) {
            return ResponseFactory.code(TurmsStatusCode.ILLEGAL_ARGUMENTS);
        }
        if (divideBy == null || divideBy == DivideBy.NOOP) {
            List<Mono<Pair<String, Long>>> counts = new LinkedList<>();
            if (deliveredOnAverageStartDate != null || deliveredOnAverageEndDate != null) {
                counts.add(messageService.countDeliveredMessagesOnAverage(
                        deliveredOnAverageStartDate,
                        deliveredOnAverageEndDate,
                        chatType)
                        .map(total -> Pair.of(DELIVERED_MESSAGES_ON_AVERAGE, total)));
            }
            if (acknowledgedStartDate != null || acknowledgedEndDate != null) {
                counts.add(messageService.countAcknowledgedMessages(
                        acknowledgedStartDate,
                        acknowledgedEndDate,
                        chatType)
                        .map(total -> Pair.of(ACKNOWLEDGED_MESSAGES, total)));
            }
            if (acknowledgedOnAverageStartDate != null || acknowledgedOnAverageEndDate != null) {
                counts.add(messageService.countAcknowledgedMessagesOnAverage(
                        acknowledgedOnAverageStartDate,
                        acknowledgedOnAverageEndDate,
                        chatType)
                        .map(total -> Pair.of(ACKNOWLEDGED_MESSAGES_ON_AVERAGE, total)));
            }
            if (counts.isEmpty() || deliveredStartDate != null || deliveredEndDate != null) {
                counts.add(messageService.countDeliveredMessages(
                        deliveredStartDate,
                        deliveredEndDate,
                        chatType)
                        .map(total -> Pair.of(DELIVERED_MESSAGES, total)));
            }
            return ResponseFactory.collectCountResults(counts);
        } else {
            List<Mono<Pair<String, List<Map<String, ?>>>>> counts = new LinkedList<>();
            if (deliveredOnAverageStartDate != null && deliveredOnAverageEndDate != null) {
                counts.add(dateTimeUtil.checkAndQueryBetweenDate(
                        DELIVERED_MESSAGES_ON_AVERAGE,
                        deliveredOnAverageStartDate,
                        deliveredOnAverageEndDate,
                        divideBy,
                        messageService::countDeliveredMessagesOnAverage,
                        chatType));
            }
            if (acknowledgedStartDate != null && acknowledgedEndDate != null) {
                counts.add(dateTimeUtil.checkAndQueryBetweenDate(
                        ACKNOWLEDGED_MESSAGES,
                        acknowledgedStartDate,
                        acknowledgedEndDate,
                        divideBy,
                        messageService::countAcknowledgedMessages,
                        chatType));
            }
            if (acknowledgedOnAverageStartDate != null && acknowledgedOnAverageEndDate != null) {
                counts.add(dateTimeUtil.checkAndQueryBetweenDate(
                        ACKNOWLEDGED_MESSAGES_ON_AVERAGE,
                        acknowledgedOnAverageStartDate,
                        acknowledgedOnAverageEndDate,
                        divideBy,
                        messageService::countAcknowledgedMessagesOnAverage,
                        chatType));
            }
            if (deliveredStartDate != null && deliveredEndDate != null) {
                counts.add(dateTimeUtil.checkAndQueryBetweenDate(
                        DELIVERED_MESSAGES,
                        deliveredStartDate,
                        deliveredEndDate,
                        divideBy,
                        messageService::countDeliveredMessages,
                        chatType));
            }
            if (counts.isEmpty()) {
                return ResponseFactory.code(TurmsStatusCode.ILLEGAL_ARGUMENTS);
            }
            return ResponseFactory.collectCountResults(counts);
        }
    }
}
