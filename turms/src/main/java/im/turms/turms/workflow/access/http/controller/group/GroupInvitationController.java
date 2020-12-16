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

package im.turms.turms.workflow.access.http.controller.group;

import im.turms.common.constant.RequestStatus;
import im.turms.turms.bo.DateRange;
import im.turms.turms.workflow.access.http.dto.request.group.AddGroupInvitationDTO;
import im.turms.turms.workflow.access.http.dto.request.group.UpdateGroupInvitationDTO;
import im.turms.turms.workflow.access.http.dto.response.*;
import im.turms.turms.workflow.access.http.permission.RequiredPermission;
import im.turms.turms.workflow.access.http.util.PageUtil;
import im.turms.turms.workflow.dao.domain.GroupInvitation;
import im.turms.turms.workflow.service.impl.group.GroupInvitationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collection;
import java.util.Date;
import java.util.Set;

import static im.turms.turms.workflow.access.http.permission.AdminPermission.*;

/**
 * @author James Chen
 */
@RestController
@RequestMapping("/groups/invitations")
public class GroupInvitationController {

    private final GroupInvitationService groupInvitationService;
    private final PageUtil pageUtil;

    public GroupInvitationController(PageUtil pageUtil, GroupInvitationService groupInvitationService) {
        this.pageUtil = pageUtil;
        this.groupInvitationService = groupInvitationService;
    }

    @PostMapping
    @RequiredPermission(GROUP_INVITATION_CREATE)
    public Mono<ResponseEntity<ResponseDTO<GroupInvitation>>> addGroupInvitation(@RequestBody AddGroupInvitationDTO addGroupInvitationDTO) {
        Mono<GroupInvitation> createMono = groupInvitationService.createGroupInvitation(
                addGroupInvitationDTO.getId(),
                addGroupInvitationDTO.getGroupId(),
                addGroupInvitationDTO.getInviterId(),
                addGroupInvitationDTO.getInviteeId(),
                addGroupInvitationDTO.getContent(),
                addGroupInvitationDTO.getStatus(),
                addGroupInvitationDTO.getCreationDate(),
                addGroupInvitationDTO.getResponseDate(),
                addGroupInvitationDTO.getExpirationDate());
        return ResponseFactory.okIfTruthy(createMono);
    }

    @GetMapping
    @RequiredPermission(GROUP_INVITATION_QUERY)
    public Mono<ResponseEntity<ResponseDTO<Collection<GroupInvitation>>>> queryGroupInvitations(
            @RequestParam(required = false) Set<Long> ids,
            @RequestParam(required = false) Set<Long> groupIds,
            @RequestParam(required = false) Set<Long> inviterIds,
            @RequestParam(required = false) Set<Long> inviteeIds,
            @RequestParam(required = false) Set<RequestStatus> statuses,
            @RequestParam(required = false) Date creationDateStart,
            @RequestParam(required = false) Date creationDateEnd,
            @RequestParam(required = false) Date responseDateStart,
            @RequestParam(required = false) Date responseDateEnd,
            @RequestParam(required = false) Date expirationDateStart,
            @RequestParam(required = false) Date expirationDateEnd,
            @RequestParam(required = false) Integer size) {
        size = pageUtil.getSize(size);
        Flux<GroupInvitation> invitationFlux = groupInvitationService.queryInvitations(
                ids,
                groupIds,
                inviterIds,
                inviteeIds,
                statuses,
                DateRange.of(creationDateStart, creationDateEnd),
                DateRange.of(responseDateStart, responseDateEnd),
                DateRange.of(expirationDateStart, expirationDateEnd),
                0,
                size);
        return ResponseFactory.okIfTruthy(invitationFlux);
    }

    @GetMapping("/page")
    @RequiredPermission(GROUP_INVITATION_QUERY)
    public Mono<ResponseEntity<ResponseDTO<PaginationDTO<GroupInvitation>>>> queryGroupInvitations(
            @RequestParam(required = false) Set<Long> ids,
            @RequestParam(required = false) Set<Long> groupIds,
            @RequestParam(required = false) Set<Long> inviterIds,
            @RequestParam(required = false) Set<Long> inviteeIds,
            @RequestParam(required = false) Set<RequestStatus> statuses,
            @RequestParam(required = false) Date creationDateStart,
            @RequestParam(required = false) Date creationDateEnd,
            @RequestParam(required = false) Date responseDateStart,
            @RequestParam(required = false) Date responseDateEnd,
            @RequestParam(required = false) Date expirationDateStart,
            @RequestParam(required = false) Date expirationDateEnd,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(required = false) Integer size) {
        size = pageUtil.getSize(size);
        Mono<Long> count = groupInvitationService.countInvitations(
                ids,
                groupIds,
                inviterIds,
                inviteeIds,
                statuses,
                DateRange.of(creationDateStart, creationDateEnd),
                DateRange.of(responseDateStart, responseDateEnd),
                DateRange.of(expirationDateStart, expirationDateEnd));
        Flux<GroupInvitation> invitationFlux = groupInvitationService.queryInvitations(
                ids,
                groupIds,
                inviterIds,
                inviteeIds,
                statuses,
                DateRange.of(creationDateStart, creationDateEnd),
                DateRange.of(responseDateStart, responseDateEnd),
                DateRange.of(expirationDateStart, expirationDateEnd),
                page,
                size);
        return ResponseFactory.page(count, invitationFlux);
    }

    @PutMapping
    @RequiredPermission(GROUP_INVITATION_UPDATE)
    public Mono<ResponseEntity<ResponseDTO<UpdateResultDTO>>> updateGroupInvitations(
            @RequestParam Set<Long> ids,
            @RequestBody UpdateGroupInvitationDTO updateGroupInvitationDTO) {
        Mono<UpdateResultDTO> updateMono = groupInvitationService.updateInvitations(
                ids,
                updateGroupInvitationDTO.getInviterId(),
                updateGroupInvitationDTO.getInviteeId(),
                updateGroupInvitationDTO.getContent(),
                updateGroupInvitationDTO.getStatus(),
                updateGroupInvitationDTO.getCreationDate(),
                updateGroupInvitationDTO.getResponseDate(),
                updateGroupInvitationDTO.getExpirationDate())
                .map(UpdateResultDTO::get);
        return ResponseFactory.okIfTruthy(updateMono);
    }

    @DeleteMapping
    @RequiredPermission(GROUP_INVITATION_DELETE)
    public Mono<ResponseEntity<ResponseDTO<DeleteResultDTO>>> deleteGroupInvitations(
            @RequestParam(required = false) Set<Long> ids) {
        Mono<DeleteResultDTO> deleteMono = groupInvitationService.deleteInvitations(ids)
                .map(DeleteResultDTO::get);
        return ResponseFactory.okIfTruthy(deleteMono);
    }

}