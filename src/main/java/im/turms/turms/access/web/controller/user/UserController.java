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

package im.turms.turms.access.web.controller.user;

import im.turms.turms.access.web.util.ResponseFactory;
import im.turms.turms.annotation.web.RequiredPermission;
import im.turms.turms.cluster.TurmsClusterManager;
import im.turms.turms.common.DateTimeUtil;
import im.turms.turms.common.PageUtil;
import im.turms.turms.common.TurmsStatusCode;
import im.turms.turms.constant.AdminPermission;
import im.turms.turms.constant.DeviceType;
import im.turms.turms.constant.DivideBy;
import im.turms.turms.constant.UserStatus;
import im.turms.turms.exception.TurmsBusinessException;
import im.turms.turms.pojo.bo.UserOnlineInfo;
import im.turms.turms.pojo.domain.Group;
import im.turms.turms.pojo.domain.User;
import im.turms.turms.pojo.domain.UserLocation;
import im.turms.turms.pojo.dto.*;
import im.turms.turms.service.group.GroupMemberService;
import im.turms.turms.service.group.GroupService;
import im.turms.turms.service.message.MessageService;
import im.turms.turms.service.user.UserService;
import im.turms.turms.service.user.onlineuser.OnlineUserService;
import im.turms.turms.service.user.onlineuser.UsersNearbyService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.socket.CloseStatus;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.*;

import static im.turms.turms.common.Constants.OFFLINE_USER_ONLINE_INFO;

@RestController
@RequestMapping("/users")
public class UserController {
    private final UserService userService;
    private final OnlineUserService onlineUserService;
    private final UsersNearbyService usersNearbyService;
    private final GroupService groupService;
    private final GroupMemberService groupMemberService;
    private final MessageService messageService;
    private final TurmsClusterManager turmsClusterManager;
    private final PageUtil pageUtil;
    private final DateTimeUtil dateTimeUtil;

    public UserController(UserService userService, OnlineUserService onlineUserService, GroupService groupService, PageUtil pageUtil, UsersNearbyService usersNearbyService, MessageService messageService, DateTimeUtil dateTimeUtil, TurmsClusterManager turmsClusterManager, GroupMemberService groupMemberService) {
        this.userService = userService;
        this.onlineUserService = onlineUserService;
        this.groupService = groupService;
        this.pageUtil = pageUtil;
        this.usersNearbyService = usersNearbyService;
        this.messageService = messageService;
        this.dateTimeUtil = dateTimeUtil;
        this.turmsClusterManager = turmsClusterManager;
        this.groupMemberService = groupMemberService;
    }

    @GetMapping
    @RequiredPermission(AdminPermission.USER_QUERY)
    public Mono<ResponseEntity<ResponseDTO<Collection<User>>>> queryUsers(
            @RequestParam(required = false) Set<Long> userIds,
            @RequestParam(required = false) Date registrationDateStart,
            @RequestParam(required = false) Date registrationDateEnd,
            @RequestParam(required = false) Date deletionDateStart,
            @RequestParam(required = false) Date deletionDateEnd,
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) Integer size) {
        size = pageUtil.getSize(size);
        Flux<User> usersFlux = userService.queryUsers(
                userIds,
                registrationDateStart,
                registrationDateEnd,
                deletionDateStart,
                deletionDateEnd,
                active,
                0,
                size);
        return ResponseFactory.okIfTruthy(usersFlux);
    }

    @GetMapping("/page")
    @RequiredPermission(AdminPermission.USER_QUERY)
    public Mono<ResponseEntity<ResponseDTO<PaginationDTO<User>>>> queryUsers(
            @RequestParam(required = false) Set<Long> userIds,
            @RequestParam(required = false) Date registrationDateStart,
            @RequestParam(required = false) Date registrationDateEnd,
            @RequestParam(required = false) Date deletionDateStart,
            @RequestParam(required = false) Date deletionDateEnd,
            @RequestParam(required = false) Boolean active,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(required = false) Integer size) {
        size = pageUtil.getSize(size);
        Mono<Long> count = userService.countUsers(
                userIds,
                registrationDateStart,
                registrationDateEnd,
                deletionDateStart,
                deletionDateEnd,
                active);
        Flux<User> usersFlux = userService.queryUsers(
                userIds,
                registrationDateStart,
                registrationDateEnd,
                deletionDateStart,
                deletionDateEnd,
                active,
                page,
                size);
        return ResponseFactory.page(count, usersFlux);
    }

    @PostMapping
    @RequiredPermission(AdminPermission.USER_CREATE)
    public Mono<ResponseEntity<ResponseDTO<User>>> addUser(@RequestBody AddUserDTO addUserDTO) {
        Mono<User> addUser = userService.addUser(
                addUserDTO.getId(),
                addUserDTO.getPassword(),
                addUserDTO.getName(),
                addUserDTO.getIntro(),
                addUserDTO.getProfilePictureUrl(),
                addUserDTO.getProfileAccess(),
                addUserDTO.getRegistrationDate(),
                addUserDTO.getActive());
        return ResponseFactory.okIfTruthy(addUser);
    }

    @DeleteMapping
    @RequiredPermission(AdminPermission.USER_DELETE)
    public Mono<ResponseEntity<ResponseDTO<AcknowledgedDTO>>> deleteUsers(
            @RequestParam Set<Long> userIds,
            @RequestParam(defaultValue = "false") boolean deleteRelationships,
            @RequestParam(required = false) Boolean logicallyDelete) {
        Mono<Boolean> deleted = userService.deleteUsers(userIds, deleteRelationships, logicallyDelete);
        return ResponseFactory.acknowledged(deleted);
    }

    @PutMapping
    @RequiredPermission(AdminPermission.USER_UPDATE)
    public Mono<ResponseEntity<ResponseDTO<AcknowledgedDTO>>> updateUser(
            @RequestParam Set<Long> userIds,
            @RequestBody UpdateUserDTO updateUserDTO) {
        Mono<Boolean> updated = userService.updateUsers(
                userIds,
                updateUserDTO.getPassword(),
                updateUserDTO.getName(),
                updateUserDTO.getIntro(),
                updateUserDTO.getProfilePictureUrl(),
                updateUserDTO.getProfileAccess(),
                updateUserDTO.getRegistrationDate(),
                updateUserDTO.getActive());
        return ResponseFactory.acknowledged(updated);
    }

    @GetMapping("/count")
    @RequiredPermission(AdminPermission.USER_QUERY)
    public Mono<ResponseEntity<ResponseDTO<UserStatisticsDTO>>> countUsers(
            @RequestParam(required = false) Date registeredStartDate,
            @RequestParam(required = false) Date registeredEndDate,
            @RequestParam(required = false) Date deletedStartDate,
            @RequestParam(required = false) Date deletedEndDate,
            @RequestParam(required = false) Date sentMessageStartDate,
            @RequestParam(required = false) Date sentMessageEndDate,
            @RequestParam(required = false) Date loggedInStartDate,
            @RequestParam(required = false) Date loggedInEndDate,
            @RequestParam(required = false) Date maxOnlineUsersStartDate,
            @RequestParam(required = false) Date maxOnlineUsersEndDate,
            @RequestParam(defaultValue = "NOOP") DivideBy divideBy) {
        List<Mono<?>> counts = new LinkedList<>();
        UserStatisticsDTO statistics = new UserStatisticsDTO();
        if (divideBy == null || divideBy == DivideBy.NOOP) {
            if (deletedStartDate != null || deletedEndDate != null) {
                counts.add(userService.countDeletedUsers(
                        deletedStartDate,
                        deletedEndDate)
                        .doOnSuccess(statistics::setDeletedUsers));
            }
            if (sentMessageStartDate != null || sentMessageEndDate != null) {
                counts.add(messageService.countUsersWhoSentMessage(
                        sentMessageStartDate,
                        sentMessageEndDate,
                        null,
                        false)
                        .doOnSuccess(statistics::setUsersWhoSentMessages));
            }
            if (loggedInStartDate != null || loggedInEndDate != null) {
                counts.add(userService.countLoggedInUsers(
                        loggedInStartDate,
                        loggedInEndDate)
                        .doOnSuccess(statistics::setLoggedInUsers));
            }
            if (maxOnlineUsersStartDate != null || maxOnlineUsersEndDate != null) {
                counts.add(userService.countMaxOnlineUsers(
                        maxOnlineUsersStartDate,
                        maxOnlineUsersEndDate)
                        .doOnSuccess(statistics::setMaxOnlineUsers));
            }
            if (counts.isEmpty() || registeredStartDate != null || registeredEndDate != null) {
                counts.add(userService.countRegisteredUsers(
                        registeredStartDate,
                        registeredEndDate)
                        .doOnSuccess(statistics::setRegisteredUsers));
            }
        } else {
            if (deletedStartDate != null && deletedEndDate != null) {
                counts.add(dateTimeUtil.checkAndQueryBetweenDate(
                        deletedStartDate,
                        deletedEndDate,
                        divideBy,
                        userService::countDeletedUsers)
                        .doOnSuccess(statistics::setDeletedUsersRecords));
            }
            if (sentMessageStartDate != null && sentMessageEndDate != null) {
                counts.add(dateTimeUtil.checkAndQueryBetweenDate(
                        sentMessageStartDate,
                        sentMessageEndDate,
                        divideBy,
                        messageService::countUsersWhoSentMessage,
                        null,
                        false)
                        .doOnSuccess(statistics::setUsersWhoSentMessagesRecords));
            }
            if (loggedInStartDate != null && loggedInEndDate != null) {
                counts.add(dateTimeUtil.checkAndQueryBetweenDate(
                        loggedInStartDate,
                        loggedInEndDate,
                        divideBy,
                        userService::countLoggedInUsers)
                        .doOnSuccess(statistics::setLoggedInUsersRecords));
            }
            if (maxOnlineUsersStartDate != null && maxOnlineUsersEndDate != null) {
                counts.add(dateTimeUtil.checkAndQueryBetweenDate(
                        maxOnlineUsersStartDate,
                        maxOnlineUsersEndDate,
                        divideBy,
                        userService::countMaxOnlineUsers)
                        .doOnSuccess(statistics::setMaxOnlineUsersRecords));
            }
            if (registeredStartDate != null && registeredEndDate != null) {
                counts.add(dateTimeUtil.checkAndQueryBetweenDate(
                        registeredStartDate,
                        registeredEndDate,
                        divideBy,
                        userService::countRegisteredUsers)
                        .doOnSuccess(statistics::setRegisteredUsersRecords));
            }
            if (counts.isEmpty()) {
                throw TurmsBusinessException.get(TurmsStatusCode.ILLEGAL_ARGUMENTS);
            }
        }
        return ResponseFactory.okIfTruthy(Flux.merge(counts).then(Mono.just(statistics)));
    }

    /**
     * @param number this only works when userIds is null or empty
     */
    @GetMapping("/online-statuses")
    @RequiredPermission(AdminPermission.USER_QUERY)
    public Mono<ResponseEntity<ResponseDTO<Collection<UserOnlineInfo>>>> queryOnlineUsersStatus(
            @RequestParam(required = false) Set<Long> userIds,
            @RequestParam(defaultValue = "20") Integer number) {
        if (userIds != null && !userIds.isEmpty()) {
            List<Mono<UserOnlineInfo>> userOnlineInfoMonos = new ArrayList<>(userIds.size());
            for (Long userId : userIds) {
                Mono<UserOnlineInfo> userOnlineInfoMno = onlineUserService.queryUserOnlineInfo(userId);
                userOnlineInfoMno = userOnlineInfoMno.map(info -> {
                    if (info == OFFLINE_USER_ONLINE_INFO) {
                        return UserOnlineInfo.builder()
                                .userId(userId)
                                .userStatus(UserStatus.OFFLINE)
                                .build();
                    } else {
                        return info;
                    }
                });
                userOnlineInfoMonos.add(userOnlineInfoMno);
            }
            return ResponseFactory.okIfTruthy(Flux.merge(userOnlineInfoMonos));
        } else {
            if (number > turmsClusterManager.getTurmsProperties().getSecurity()
                    .getMaxQueryOnlineUsersStatusPerRequest()) {
                throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS);
            }
            Flux<UserOnlineInfo> userOnlineInfoFlux = onlineUserService.queryUserOnlineInfos(number);
            return ResponseFactory.okIfTruthy(userOnlineInfoFlux);
        }
    }

    @GetMapping("/online-statuses/count")
    @RequiredPermission(AdminPermission.USER_QUERY)
    public Mono<ResponseEntity<ResponseDTO<TotalDTO>>> countOnlineUsers() {
        return ResponseFactory.total(onlineUserService.countOnlineUsers());
    }

    @PutMapping("/online-statuses")
    @RequiredPermission(AdminPermission.USER_UPDATE)
    public Mono<ResponseEntity<ResponseDTO<AcknowledgedDTO>>> updateUserOnlineStatus(
            @RequestParam Long userId,
            @RequestParam(required = false) Set<DeviceType> deviceTypes,
            @RequestBody UpdateOnlineStatusDTO updateOnlineStatusDTO) {
        Mono<Boolean> updated;
        UserStatus onlineStatus = updateOnlineStatusDTO.getOnlineStatus();
        if (onlineStatus == UserStatus.OFFLINE) {
            if (deviceTypes != null) {
                updated = onlineUserService.setUserDevicesOffline(userId, deviceTypes, CloseStatus.NORMAL);
            } else {
                updated = onlineUserService.setUserOffline(userId, CloseStatus.NORMAL);
            }
        } else {
            updated = onlineUserService.updateOnlineUserStatus(userId, onlineStatus);
        }
        return ResponseFactory.acknowledged(updated);
    }

    @GetMapping("/users-nearby")
    @RequiredPermission(AdminPermission.USER_QUERY)
    public Mono<ResponseEntity<ResponseDTO<Collection<User>>>> queryUsersNearby(
            @RequestParam Long userId,
            @RequestParam(required = false) DeviceType deviceType,
            @RequestParam(required = false) Integer maxPeopleNumber,
            @RequestParam(required = false) Double maxDistance) {
        Flux<User> usersNearby = usersNearbyService.queryUsersProfilesNearby(userId, deviceType, maxPeopleNumber, maxDistance);
        return ResponseFactory.okIfTruthy(usersNearby);
    }

    @GetMapping("/locations")
    @RequiredPermission(AdminPermission.USER_QUERY)
    public ResponseEntity<ResponseDTO<Collection<UserLocation>>> queryUserLocations(@RequestParam Long userId) {
        SortedSet<UserLocation> userLocations = onlineUserService.getUserLocations(userId);
        return ResponseFactory.okIfTruthy(userLocations);
    }

    @GetMapping("/groups")
    @RequiredPermission(AdminPermission.USER_QUERY)
    public Mono<ResponseEntity<ResponseDTO<Collection<Group>>>> queryUserJoinedGroups(
            @RequestParam Long userId,
            @RequestParam(required = false) Integer size) {
        size = pageUtil.getSize(size);
        Flux<Group> groupsFlux = groupService.queryUserJoinedGroups(userId, 0, size);
        return ResponseFactory.okIfTruthy(groupsFlux);
    }

    @GetMapping("/groups/page")
    @RequiredPermission(AdminPermission.USER_QUERY)
    public Mono<ResponseEntity<ResponseDTO<PaginationDTO<Group>>>> queryUserJoinedGroups(
            @RequestParam Long userId,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(required = false) Integer size) {
        size = pageUtil.getSize(size);
        Mono<Long> count = groupMemberService.countUserJoinedGroupsIds(userId);
        Flux<Group> groupsFlux = groupService.queryUserJoinedGroups(userId, page, size);
        return ResponseFactory.page(count, groupsFlux);
    }
}
