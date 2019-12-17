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

package im.turms.turms.access.web.controller.message;

import im.turms.turms.access.web.util.ResponseFactory;
import im.turms.turms.annotation.web.RequiredPermission;
import im.turms.turms.common.PageUtil;
import im.turms.turms.pojo.domain.MessageStatus;
import im.turms.turms.pojo.dto.AcknowledgedDTO;
import im.turms.turms.pojo.dto.PaginationDTO;
import im.turms.turms.pojo.dto.ResponseDTO;
import im.turms.turms.pojo.dto.UpdateMessageStatusDTO;
import im.turms.turms.service.message.MessageStatusService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collection;
import java.util.Set;

import static im.turms.turms.constant.AdminPermission.MESSAGE_STATUS_QUERY;
import static im.turms.turms.constant.AdminPermission.MESSAGE_STATUS_UPDATE;

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
    @RequiredPermission(MESSAGE_STATUS_QUERY)
    public Mono<ResponseEntity<ResponseDTO<Collection<MessageStatus>>>> queryMessageStatuses(
            @RequestParam(required = false) Set<Long> ids) {
        Flux<MessageStatus> messageStatuses = messageStatusService.queryMessageStatuses(ids);
        return ResponseFactory.okIfTruthy(messageStatuses);
    }

    @GetMapping("/page")
    @RequiredPermission(MESSAGE_STATUS_QUERY)
    public Mono<ResponseEntity<ResponseDTO<PaginationDTO<MessageStatus>>>> queryMessageStatuses(
            @RequestParam(required = false) Set<Long> ids,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(required = false) Integer size) {
        size = pageUtil.getSize(size);
        Flux<MessageStatus> messageStatuses = messageStatusService.queryMessageStatuses(ids, page, size);
        return ResponseFactory.page(messageStatuses.count(), messageStatuses);
    }

    @PutMapping
    @RequiredPermission(MESSAGE_STATUS_UPDATE)
    public Mono<ResponseEntity<ResponseDTO<AcknowledgedDTO>>> updateMessageStatuses(
            @RequestParam(required = false) Set<Long> ids,
            @RequestBody UpdateMessageStatusDTO dto) {
        Mono<Boolean> updateMono = messageStatusService.updateMessageStatuses(
                ids, null,
                dto.getRecallDate(), dto.getReadDate(), dto.getReceptionDate(), null);
        return ResponseFactory.acknowledged(updateMono);
    }
}
