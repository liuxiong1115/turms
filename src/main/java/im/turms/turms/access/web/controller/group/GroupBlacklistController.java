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
import java.util.Set;

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

    @PostMapping
    @RequiredPermission(GROUP_BLACKLIST_CREATE)
    public Mono<ResponseEntity<ResponseDTO<GroupBlacklistedUser>>> addGroupBlacklistedUser(@RequestBody AddGroupBlacklistedUserDTO addGroupBlacklistedUserDTO) {
        Mono<GroupBlacklistedUser> createMono = groupBlacklistService.addBlacklistedUser(
                addGroupBlacklistedUserDTO.getGroupId(),
                addGroupBlacklistedUserDTO.getUserId(),
                addGroupBlacklistedUserDTO.getRequesterId(),
                addGroupBlacklistedUserDTO.getBlockDate());
        return ResponseFactory.okIfTruthy(createMono);
    }

    @PutMapping
    @RequiredPermission(GROUP_BLACKLIST_UPDATE)
    public Mono<ResponseEntity<ResponseDTO<AcknowledgedDTO>>> updateGroupBlacklistedUsers(
            @RequestParam GroupBlacklistedUser.KeyList keys,
            @RequestBody UpdateGroupBlacklistedUserDTO updateGroupBlacklistedUserDTO) {
        Mono<Boolean> updateMono = groupBlacklistService.updateBlacklistedUsers(
                new HashSet<>(keys.getKeys()),
                updateGroupBlacklistedUserDTO.getGroupId(),
                updateGroupBlacklistedUserDTO.getBlockDate(),
                updateGroupBlacklistedUserDTO.getRequesterId());
        return ResponseFactory.acknowledged(updateMono);
    }

    @GetMapping
    @RequiredPermission(GROUP_BLACKLIST_QUERY)
    public Mono<ResponseEntity<ResponseDTO<Collection<GroupBlacklistedUser>>>> queryGroupBlacklistedUsers(
            @RequestParam(required = false) Set<Long> groupIds,
            @RequestParam(required = false) Set<Long> userIds,
            @RequestParam(required = false) Date blockDateStart,
            @RequestParam(required = false) Date blockDateEnd,
            @RequestParam(required = false) Set<Long> requesterIds,
            @RequestParam(required = false) Integer size) {
        size = pageUtil.getSize(size);
        Flux<GroupBlacklistedUser> userFlux = groupBlacklistService.queryBlacklistedUsers(
                groupIds,
                userIds,
                DateRange.of(blockDateStart, blockDateEnd),
                requesterIds,
                0,
                size);
        return ResponseFactory.okIfTruthy(userFlux);
    }

    @GetMapping("/page")
    @RequiredPermission(GROUP_BLACKLIST_QUERY)
    public Mono<ResponseEntity<ResponseDTO<PaginationDTO<GroupBlacklistedUser>>>> queryGroupBlacklistedUsers(
            @RequestParam(required = false) Set<Long> groupIds,
            @RequestParam(required = false) Set<Long> userIds,
            @RequestParam(required = false) Date blockDateStart,
            @RequestParam(required = false) Date blockDateEnd,
            @RequestParam(required = false) Set<Long> requesterIds,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(required = false) Integer size) {
        size = pageUtil.getSize(size);
        Mono<Long> count = groupBlacklistService.countBlacklistedUsers(groupIds,
                userIds,
                DateRange.of(blockDateStart, blockDateEnd),
                requesterIds);
        Flux<GroupBlacklistedUser> userFlux = groupBlacklistService.queryBlacklistedUsers(
                groupIds,
                userIds,
                DateRange.of(blockDateStart, blockDateEnd),
                requesterIds,
                page,
                size);
        return ResponseFactory.page(count, userFlux);
    }

    @DeleteMapping
    @RequiredPermission(GROUP_BLACKLIST_DELETE)
    public Mono<ResponseEntity<ResponseDTO<AcknowledgedDTO>>> deleteGroupBlacklistedUsers(
            @RequestParam GroupBlacklistedUser.KeyList keys) {
        Mono<Boolean> deleteMono = groupBlacklistService.deleteBlacklistedUsers(new HashSet<>(keys.getKeys()));
        return ResponseFactory.acknowledged(deleteMono);
    }
}
