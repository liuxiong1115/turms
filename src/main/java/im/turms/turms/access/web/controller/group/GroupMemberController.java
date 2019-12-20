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
import im.turms.turms.constant.GroupMemberRole;
import im.turms.turms.pojo.domain.GroupMember;
import im.turms.turms.pojo.dto.*;
import im.turms.turms.service.group.GroupMemberService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collection;
import java.util.Date;
import java.util.HashSet;

import static im.turms.turms.constant.AdminPermission.*;

@RestController
@RequestMapping("/groups/members")
public class GroupMemberController {
    private final GroupMemberService groupMemberService;
    private final PageUtil pageUtil;

    public GroupMemberController(PageUtil pageUtil, GroupMemberService groupMemberService) {
        this.pageUtil = pageUtil;
        this.groupMemberService = groupMemberService;
    }

    @GetMapping
    @RequiredPermission(GROUP_MEMBER_QUERY)
    public Mono<ResponseEntity<ResponseDTO<Collection<GroupMember>>>> queryGroupMembers(
            @RequestParam(required = false) Long groupId,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) GroupMemberRole role,
            @RequestParam(required = false) Date joinDateStart,
            @RequestParam(required = false) Date joinDateEnd,
            @RequestParam(required = false) Date muteEndDateStart,
            @RequestParam(required = false) Date muteEndDateEnd,
            @RequestParam(required = false) Integer size) {
        size = pageUtil.getSize(size);
        Flux<GroupMember> groupMemberFlux = groupMemberService.queryGroupMembers(
                groupId,
                userId,
                role,
                joinDateStart,
                joinDateEnd,
                muteEndDateStart,
                muteEndDateEnd,
                0,
                size);
        return ResponseFactory.okIfTruthy(groupMemberFlux);
    }

    @GetMapping("/page")
    @RequiredPermission(GROUP_MEMBER_QUERY)
    public Mono<ResponseEntity<ResponseDTO<PaginationDTO<GroupMember>>>> queryGroupMembers(
            @RequestParam(required = false) Long groupId,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) GroupMemberRole role,
            @RequestParam(required = false) Date joinDateStart,
            @RequestParam(required = false) Date joinDateEnd,
            @RequestParam(required = false) Date muteEndDateStart,
            @RequestParam(required = false) Date muteEndDateEnd,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(required = false) Integer size) {
        size = pageUtil.getSize(size);
        Mono<Long> count = groupMemberService.countMembers(
                groupId,
                userId,
                role,
                joinDateStart,
                joinDateEnd,
                muteEndDateStart,
                muteEndDateEnd);
        Flux<GroupMember> userFlux = groupMemberService.queryGroupMembers(
                groupId,
                userId,
                role,
                joinDateStart,
                joinDateEnd,
                muteEndDateStart,
                muteEndDateEnd,
                page,
                size);
        return ResponseFactory.page(count, userFlux);
    }

    @PostMapping
    @RequiredPermission(GROUP_MEMBER_CREATE)
    public Mono<ResponseEntity<ResponseDTO<GroupMember>>> addGroupMember(@RequestBody AddGroupMemberDTO dto) {
        Mono<GroupMember> createMono = groupMemberService.addGroupMember(
                dto.getGroupId(),
                dto.getUserId(),
                dto.getRole(),
                dto.getName(),
                dto.getJoinDate(),
                dto.getMuteEndDate(),
                null);
        return ResponseFactory.okIfTruthy(createMono);
    }

    @PutMapping
    @RequiredPermission(GROUP_MEMBER_UPDATE)
    public Mono<ResponseEntity<ResponseDTO<AcknowledgedDTO>>> updateGroupMembers(
            @RequestParam GroupMember.KeyList keys,
            @RequestBody UpdateGroupMemberDTO dto) {
        Mono<Boolean> updateMono = groupMemberService.updateGroupMembers(
                new HashSet<>(keys.getKeys()),
                dto.getName(),
                dto.getRole(),
                dto.getJoinDate(),
                dto.getMuteEndDate(),
                null);
        return ResponseFactory.acknowledged(updateMono);
    }

    @DeleteMapping
    @RequiredPermission(GROUP_MEMBER_DELETE)
    public Mono<ResponseEntity<ResponseDTO<AcknowledgedDTO>>> deleteGroupMembers(
            @RequestParam(required = false) Long groupId,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) GroupMemberRole role,
            @RequestParam(required = false) Date joinDateStart,
            @RequestParam(required = false) Date joinDateEnd,
            @RequestParam(required = false) Date muteEndDateStart,
            @RequestParam(required = false) Date muteEndDateEnd) {
        Mono<Boolean> deleteMono = groupMemberService.deleteMembers(
                groupId,
                userId,
                role,
                joinDateStart,
                joinDateEnd,
                muteEndDateStart,
                muteEndDateEnd);
        return ResponseFactory.acknowledged(deleteMono);
    }
}
