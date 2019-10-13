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
import im.turms.turms.constant.MessageDeliveryStatus;
import im.turms.turms.pojo.domain.Message;
import im.turms.turms.service.message.MessageService;
import org.apache.commons.lang3.EnumUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.text.ParseException;
import java.util.Collections;
import java.util.Date;
import java.util.Set;

@RestController
@RequestMapping("/messages")
public class MessageController {
    private final MessageService messageService;
    private final PageUtil pageUtil;

    public MessageController(MessageService messageService, PageUtil pageUtil) {
        this.messageService = messageService;
        this.pageUtil = pageUtil;
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
            @RequestParam String chatType,
            @RequestParam(required = false) String sendStartDate,
            @RequestParam(required = false) String sendEndDate,
            @RequestParam(required = false) String averageSendStartDate,
            @RequestParam(required = false) String averageSendEndDate,
            @RequestParam(required = false) String receiveStartDate,
            @RequestParam(required = false) String receiveEndDate,
            @RequestParam(required = false) String averageReceiveStartDate,
            @RequestParam(required = false) String averageReceiveEndDate) {
        ChatType type = EnumUtils.getEnum(ChatType.class, chatType);
        Mono<Long> count = Mono.empty();
        if (type != ChatType.UNRECOGNIZED) {
            try {
                if (sendStartDate != null || sendEndDate != null) {
                    count = messageService.countSendMessages(
                            DateTimeUtil.parseDay(sendStartDate),
                            DateTimeUtil.endOfDay(sendEndDate),
                            type);
                } else if (averageSendStartDate != null || averageSendEndDate != null) {
//                    count = messageService.countAverageSentMessages(averageSendStartDate, averageSendEndDate, type);
                } else if (receiveStartDate != null || receiveEndDate != null) {
                    count = messageService.countReceivedMessages(receiveStartDate, receiveEndDate, type);
                } else if (averageReceiveStartDate != null || averageReceiveEndDate != null) {
//                    count = messageService.countAverageReceivedMessages(
//                            averageReceiveStartDate,
//                            averageReceiveEndDate, type);
                }
                return ResponseFactory.okWhenTruthy(
                        count.map(number -> Collections.singletonMap("count", number)),
                        true);
            } catch (ParseException e) {
                return ResponseFactory.code(TurmsStatusCode.ILLEGAL_DATE_FORMAT);
            }
        } else {
            return ResponseFactory.code(TurmsStatusCode.ILLEGAL_ARGUMENTS);
        }
    }
}
