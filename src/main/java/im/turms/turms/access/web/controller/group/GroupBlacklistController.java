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
import im.turms.turms.pojo.bo.common.DateRange;
import im.turms.turms.pojo.domain.GroupBlacklistedUser;
import im.turms.turms.pojo.dto.*;
import im.turms.turms.service.group.GroupBlacklistService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collection;
import java.util.Date;
import java.util.HashSet;

import static im.turms.turms.constant.AdminPermission.*;

@RestController
@RequestMapping("/groups/blacklisted-users")
public class GroupBlacklistController {
    private final GroupBlacklistService groupBlacklistService;
    private final PageUtil pageUtil;

    public GroupBlacklistController(PageUtil pageUtil, GroupBlacklistService groupBlacklistService) {
        this.pageUtil = pageUtil;
        this.groupBlacklistService = groupBlacklistService;
    }

    @GetMapping
    @RequiredPermission(GROUP_BLACKLIST_QUERY)
    public Mono<ResponseEntity<ResponseDTO<Collection<GroupBlacklistedUser>>>> queryGroupBlacklistedUsers(
            @RequestParam(required = false) Long groupId,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) Date blockDateStart,
            @RequestParam(required = false) Date blockDateEnd,
            @RequestParam(required = false) Long requesterId,
            @RequestParam(required = false) Integer size) {
        size = pageUtil.getSize(size);
        Flux<GroupBlacklistedUser> userFlux = groupBlacklistService.queryBlacklistedUsers(
                groupId,
                userId,
                DateRange.of(blockDateStart, blockDateEnd),
                requesterId,
                0,
                size);
        return ResponseFactory.okIfTruthy(userFlux);
    }

    @GetMapping("/page")
    @RequiredPermission(GROUP_BLACKLIST_QUERY)
    public Mono<ResponseEntity<ResponseDTO<PaginationDTO<GroupBlacklistedUser>>>> queryGroupBlacklistedUsers(
            @RequestParam(required = false) Long groupId,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) Date blockDateStart,
            @RequestParam(required = false) Date blockDateEnd,
            @RequestParam(required = false) Long requesterId,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(required = false) Integer size) {
        size = pageUtil.getSize(size);
        Mono<Long> count = groupBlacklistService.countBlacklistedUsers(groupId,
                userId,
                DateRange.of(blockDateStart, blockDateEnd),
                requesterId);
        Flux<GroupBlacklistedUser> userFlux = groupBlacklistService.queryBlacklistedUsers(
                groupId,
                userId,
                DateRange.of(blockDateStart, blockDateEnd),
                requesterId,
                page,
                size);
        return ResponseFactory.page(count, userFlux);
    }

    @PostMapping
    @RequiredPermission(GROUP_BLACKLIST_CREATE)
    public Mono<ResponseEntity<ResponseDTO<GroupBlacklistedUser>>> addGroupBlacklistedUser(@RequestBody AddGroupBlacklistedUserDTO dto) {
        Mono<GroupBlacklistedUser> createMono = groupBlacklistService.addBlacklistedUser(
                dto.getGroupId(),
                dto.getUserId(),
                dto.getRequesterId(),
                dto.getBlockDate());
        return ResponseFactory.okIfTruthy(createMono);
    }

    @PutMapping
    @RequiredPermission(GROUP_BLACKLIST_UPDATE)
    public Mono<ResponseEntity<ResponseDTO<AcknowledgedDTO>>> updateGroupBlacklistedUsers(
            @RequestParam GroupBlacklistedUser.KeyList keys,
            @RequestBody UpdateGroupBlacklistedUserDTO dto) {
        Mono<Boolean> updateMono = groupBlacklistService.updateBlacklistedUsers(
                new HashSet<>(keys.getKeys()),
                dto.getBlockDate(),
                dto.getRequesterId());
        return ResponseFactory.acknowledged(updateMono);
    }

    @DeleteMapping
    @RequiredPermission(GROUP_BLACKLIST_DELETE)
    public Mono<ResponseEntity<ResponseDTO<AcknowledgedDTO>>> deleteGroupBlacklistedUsers(
            @RequestParam(required = false) Long groupId,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) Date blockDateStart,
            @RequestParam(required = false) Date blockDateEnd,
            @RequestParam(required = false) Long requesterId) {
        Mono<Boolean> deleteMono = groupBlacklistService.deleteBlacklistedUsers(
                groupId,
                userId,
                DateRange.of(blockDateStart, blockDateEnd),
                requesterId);
        return ResponseFactory.acknowledged(deleteMono);
    }
}
