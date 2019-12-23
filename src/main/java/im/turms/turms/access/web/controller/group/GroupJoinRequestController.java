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

package im.turms.turms.access.web.controller.group;

import im.turms.turms.access.web.util.ResponseFactory;
import im.turms.turms.annotation.web.RequiredPermission;
import im.turms.turms.common.PageUtil;
import im.turms.turms.constant.RequestStatus;
import im.turms.turms.pojo.bo.common.DateRange;
import im.turms.turms.pojo.domain.GroupJoinRequest;
import im.turms.turms.pojo.dto.*;
import im.turms.turms.service.group.GroupJoinRequestService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collection;
import java.util.Date;
import java.util.Set;

import static im.turms.turms.constant.AdminPermission.*;

@RestController
@RequestMapping("/groups/join-requests")
public class GroupJoinRequestController {
    private final GroupJoinRequestService groupJoinRequestService;
    private final PageUtil pageUtil;

    public GroupJoinRequestController(GroupJoinRequestService groupJoinRequestService, PageUtil pageUtil) {
        this.groupJoinRequestService = groupJoinRequestService;
        this.pageUtil = pageUtil;
    }

    @GetMapping
    @RequiredPermission(GROUP_JOIN_REQUEST_QUERY)
    public Mono<ResponseEntity<ResponseDTO<Collection<GroupJoinRequest>>>> queryGroupJoinRequests(
            @RequestParam(required = false) Set<Long> ids,
            @RequestParam(required = false) Long groupId,
            @RequestParam(required = false) Long requesterId,
            @RequestParam(required = false) Long responderId,
            @RequestParam(required = false) RequestStatus status,
            @RequestParam(required = false) Date creationDateStart,
            @RequestParam(required = false) Date creationDateEnd,
            @RequestParam(required = false) Date responseDateStart,
            @RequestParam(required = false) Date responseDateEnd,
            @RequestParam(required = false) Date expirationDateStart,
            @RequestParam(required = false) Date expirationDateEnd,
            @RequestParam(required = false) Integer size) {
        size = pageUtil.getSize(size);
        Flux<GroupJoinRequest> joinRequestFlux = groupJoinRequestService.queryJoinRequests(
                ids,
                groupId,
                requesterId,
                responderId,
                status,
                DateRange.of(creationDateStart, creationDateEnd),
                DateRange.of(responseDateStart, responseDateEnd),
                DateRange.of(expirationDateStart, expirationDateEnd),
                0,
                size);
        return ResponseFactory.okIfTruthy(joinRequestFlux);
    }

    @GetMapping("/page")
    @RequiredPermission(GROUP_JOIN_REQUEST_QUERY)
    public Mono<ResponseEntity<ResponseDTO<PaginationDTO<GroupJoinRequest>>>> queryGroupJoinRequests(
            @RequestParam(required = false) Set<Long> ids,
            @RequestParam(required = false) Long groupId,
            @RequestParam(required = false) Long requesterId,
            @RequestParam(required = false) Long responderId,
            @RequestParam(required = false) RequestStatus status,
            @RequestParam(required = false) Date creationDateStart,
            @RequestParam(required = false) Date creationDateEnd,
            @RequestParam(required = false) Date responseDateStart,
            @RequestParam(required = false) Date responseDateEnd,
            @RequestParam(required = false) Date expirationDateStart,
            @RequestParam(required = false) Date expirationDateEnd,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(required = false) Integer size) {
        size = pageUtil.getSize(size);
        Mono<Long> count = groupJoinRequestService.countJoinRequests(
                ids,
                groupId,
                requesterId,
                responderId,
                status,
                DateRange.of(creationDateStart, creationDateEnd),
                DateRange.of(responseDateStart, responseDateEnd),
                DateRange.of(expirationDateStart, expirationDateEnd));
        Flux<GroupJoinRequest> joinRequestFlux = groupJoinRequestService.queryJoinRequests(
                ids,
                groupId,
                requesterId,
                responderId,
                status,
                DateRange.of(creationDateStart, creationDateEnd),
                DateRange.of(responseDateStart, responseDateEnd),
                DateRange.of(expirationDateStart, expirationDateEnd),
                page,
                size);
        return ResponseFactory.page(count, joinRequestFlux);
    }

    @PostMapping
    @RequiredPermission(GROUP_JOIN_REQUEST_CREATE)
    public Mono<ResponseEntity<ResponseDTO<GroupJoinRequest>>> addGroupJoinRequest(@RequestBody AddGroupJoinRequestDTO dto) {
        Mono<GroupJoinRequest> createMono = groupJoinRequestService.createGroupJoinRequest(
                dto.getGroupId(),
                dto.getRequesterId(),
                dto.getResponderId(),
                dto.getContent(),
                dto.getStatus(),
                dto.getCreationDate(),
                dto.getResponseDate(),
                dto.getExpirationDate());
        return ResponseFactory.okIfTruthy(createMono);
    }

    @PutMapping
    @RequiredPermission(GROUP_JOIN_REQUEST_UPDATE)
    public Mono<ResponseEntity<ResponseDTO<AcknowledgedDTO>>> updateGroupJoinRequests(
            @RequestParam Set<Long> ids,
            @RequestBody UpdateGroupJoinRequestDTO dto) {
        Mono<Boolean> updateMono = groupJoinRequestService.updateJoinRequests(
                ids,
                dto.getRequesterId(),
                dto.getResponderId(),
                dto.getContent(),
                dto.getStatus(),
                dto.getCreationDate(),
                dto.getResponseDate(),
                dto.getExpirationDate());
        return ResponseFactory.acknowledged(updateMono);
    }

    @DeleteMapping
    @RequiredPermission(GROUP_JOIN_REQUEST_DELETE)
    public Mono<ResponseEntity<ResponseDTO<AcknowledgedDTO>>> deleteGroupJoinRequests(
            @RequestParam(required = false) Set<Long> ids,
            @RequestParam(required = false) Long groupId,
            @RequestParam(required = false) Long inviterId,
            @RequestParam(required = false) Long inviteeId,
            @RequestParam(required = false) RequestStatus status,
            @RequestParam(required = false) Date creationDateStart,
            @RequestParam(required = false) Date creationDateEnd,
            @RequestParam(required = false) Date responseDateStart,
            @RequestParam(required = false) Date responseDateEnd,
            @RequestParam(required = false) Date expirationDateStart,
            @RequestParam(required = false) Date expirationDateEnd) {
        Mono<Boolean> deleteMono = groupJoinRequestService.deleteJoinRequests(
                ids,
                groupId,
                inviterId,
                inviteeId,
                status,
                DateRange.of(creationDateStart, creationDateEnd),
                DateRange.of(responseDateStart, responseDateEnd),
                DateRange.of(expirationDateStart, expirationDateEnd));
        return ResponseFactory.acknowledged(deleteMono);
    }
}
