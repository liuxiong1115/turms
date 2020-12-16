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

package im.turms.turms.workflow.access.http.controller.message;

import im.turms.common.constant.MessageDeliveryStatus;
import im.turms.turms.bo.DateRange;
import im.turms.turms.workflow.access.http.dto.request.message.UpdateMessageStatusDTO;
import im.turms.turms.workflow.access.http.dto.response.PaginationDTO;
import im.turms.turms.workflow.access.http.dto.response.ResponseDTO;
import im.turms.turms.workflow.access.http.dto.response.ResponseFactory;
import im.turms.turms.workflow.access.http.permission.AdminPermission;
import im.turms.turms.workflow.access.http.permission.RequiredPermission;
import im.turms.turms.workflow.access.http.util.PageUtil;
import im.turms.turms.workflow.dao.domain.MessageStatus;
import im.turms.turms.workflow.service.impl.message.MessageStatusService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

/**
 * TODO: remove
 * @author James Chen
 */
@RestController
@RequestMapping("/messages/statuses")
public class MessageStatusController {
    private final MessageStatusService messageStatusService;
    private final PageUtil pageUtil;

    public MessageStatusController(MessageStatusService messageStatusService, PageUtil pageUtil) {
        this.messageStatusService = messageStatusService;
        this.pageUtil = pageUtil;
    }

    @GetMapping
    @RequiredPermission(AdminPermission.MESSAGE_STATUS_QUERY)
    public Mono<ResponseEntity<ResponseDTO<Collection<MessageStatus>>>> queryMessageStatuses(
            @RequestParam(required = false) Set<Long> messageIds,
            @RequestParam(required = false) Set<Long> recipientIds,
            @RequestParam(required = false) Boolean areSystemMessages,
            @RequestParam(required = false) Set<Long> senderIds,
            @RequestParam(required = false) Set<MessageDeliveryStatus> deliveryStatuses,
            @RequestParam(required = false) Date receptionDateStart,
            @RequestParam(required = false) Date receptionDateEnd,
            @RequestParam(required = false) Date readDateStart,
            @RequestParam(required = false) Date readDateEnd,
            @RequestParam(required = false) Date recallDateStart,
            @RequestParam(required = false) Date recallDateEnd,
            @RequestParam(required = false) Integer size) {
        size = pageUtil.getSize(size);
        Flux<MessageStatus> messageStatusesFlux = messageStatusService.queryMessageStatuses(
                messageIds,
                recipientIds,
                areSystemMessages,
                senderIds,
                deliveryStatuses,
                DateRange.of(receptionDateStart, receptionDateEnd),
                DateRange.of(readDateStart, readDateEnd),
                DateRange.of(recallDateStart, recallDateEnd),
                0,
                size);
        return ResponseFactory.okIfTruthy(messageStatusesFlux);
    }

    @GetMapping("/page")
    @RequiredPermission(AdminPermission.MESSAGE_STATUS_QUERY)
    public Mono<ResponseEntity<ResponseDTO<PaginationDTO<MessageStatus>>>> queryMessageStatuses(
            @RequestParam(required = false) Set<Long> messageIds,
            @RequestParam(required = false) Set<Long> recipientIds,
            @RequestParam(required = false) Boolean areSystemMessages,
            @RequestParam(required = false) Set<Long> senderIds,
            @RequestParam(required = false) Set<MessageDeliveryStatus> deliveryStatuses,
            @RequestParam(required = false) Date receptionDateStart,
            @RequestParam(required = false) Date receptionDateEnd,
            @RequestParam(required = false) Date readDateStart,
            @RequestParam(required = false) Date readDateEnd,
            @RequestParam(required = false) Date recallDateStart,
            @RequestParam(required = false) Date recallDateEnd,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(required = false) Integer size) {
        size = pageUtil.getSize(size);
        Mono<Long> count = messageStatusService.countMessageStatuses(
                messageIds,
                recipientIds,
                areSystemMessages,
                senderIds,
                deliveryStatuses,
                DateRange.of(receptionDateStart, receptionDateEnd),
                DateRange.of(readDateStart, readDateEnd),
                DateRange.of(recallDateStart, recallDateEnd));
        Flux<MessageStatus> messageStatusesFlux = messageStatusService.queryMessageStatuses(
                messageIds,
                recipientIds,
                areSystemMessages,
                senderIds,
                deliveryStatuses,
                DateRange.of(receptionDateStart, receptionDateEnd),
                DateRange.of(readDateStart, readDateEnd),
                DateRange.of(recallDateStart, recallDateEnd),
                page,
                size);
        return ResponseFactory.page(count, messageStatusesFlux);
    }

    @PutMapping
    @RequiredPermission(AdminPermission.MESSAGE_STATUS_UPDATE)
    public Mono<ResponseEntity<ResponseDTO<Void>>> updateMessageStatuses(
            MessageStatus.KeyList keys,
            @RequestBody UpdateMessageStatusDTO updateMessageStatusDTO) {
        Mono<Boolean> updateMono = messageStatusService.updateMessageStatuses(
                new HashSet<>(keys.getKeys()),
                updateMessageStatusDTO.getRecallDate(),
                updateMessageStatusDTO.getReadDate(),
                updateMessageStatusDTO.getReceptionDate(),
                null);
        return updateMono.then(ResponseFactory.ok());
    }

}