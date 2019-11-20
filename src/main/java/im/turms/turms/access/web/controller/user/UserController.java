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
import im.turms.turms.pojo.bo.UserOnlineInfo;
import im.turms.turms.pojo.domain.Group;
import im.turms.turms.pojo.domain.User;
import im.turms.turms.pojo.domain.UserLocation;
import im.turms.turms.pojo.dto.AddUserDTO;
import im.turms.turms.pojo.dto.UpdateOnlineStatusDTO;
import im.turms.turms.pojo.dto.UpdateUserDTO;
import im.turms.turms.service.group.GroupService;
import im.turms.turms.service.message.MessageService;
import im.turms.turms.service.user.UserService;
import im.turms.turms.service.user.onlineuser.OnlineUserService;
import im.turms.turms.service.user.onlineuser.UsersNearbyService;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.*;

import static im.turms.turms.common.Constants.*;

@RestController
@RequestMapping("/users")
public class UserController {
    private final UserService userService;
    private final OnlineUserService onlineUserService;
    private final UsersNearbyService usersNearbyService;
    private final GroupService groupService;
    private final MessageService messageService;
    private final TurmsClusterManager turmsClusterManager;
    private final PageUtil pageUtil;
    private final DateTimeUtil dateTimeUtil;

    public UserController(UserService userService, OnlineUserService onlineUserService, GroupService groupService, PageUtil pageUtil, UsersNearbyService usersNearbyService, MessageService messageService, DateTimeUtil dateTimeUtil, TurmsClusterManager turmsClusterManager) {
        this.userService = userService;
        this.onlineUserService = onlineUserService;
        this.groupService = groupService;
        this.pageUtil = pageUtil;
        this.usersNearbyService = usersNearbyService;
        this.messageService = messageService;
        this.dateTimeUtil = dateTimeUtil;
        this.turmsClusterManager = turmsClusterManager;
    }

    @GetMapping
    @RequiredPermission(AdminPermission.USER_QUERY)
    public Mono<ResponseEntity> getUsers(
            @RequestParam(required = false) Set<Long> userIds,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) Date registrationDateStart,
            @RequestParam(required = false) Date registrationDateEnd,
            @RequestParam(required = false) Date deletionDateStart,
            @RequestParam(required = false) Date deletionDateEnd,
            @RequestParam(required = false) Boolean active,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "0") int size) {
        if (userId != null) {
            return ResponseFactory.okWhenTruthy(userService.queryUser(userId));
        } else {
            size = pageUtil.getSize(size);
            Flux<User> users = userService.queryUsers(
                    userIds,
                    registrationDateStart,
                    registrationDateEnd,
                    deletionDateStart,
                    deletionDateEnd,
                    active,
                    page,
                    size);
            return ResponseFactory.okWhenTruthy(users);
        }
    }

    @PostMapping
    @RequiredPermission(AdminPermission.USER_CREATE)
    public Mono<ResponseEntity> addUser(@RequestBody AddUserDTO addUserDTO) {
        Mono<User> addUser = userService.addUser(
                addUserDTO.getId(),
                addUserDTO.getPassword(),
                addUserDTO.getName(),
                addUserDTO.getIntro(),
                addUserDTO.getProfilePictureUrl(),
                addUserDTO.getProfileAccess(),
                addUserDTO.getRegistrationDate(),
                addUserDTO.getActive());
        return ResponseFactory.okWhenTruthy(addUser);
    }

    @DeleteMapping
    @RequiredPermission(AdminPermission.USER_DELETE)
    public Mono<ResponseEntity> deleteUsers(
            @RequestParam Set<Long> userIds,
            @RequestParam(defaultValue = "false") boolean deleteRelationships,
            @RequestParam(required = false) Boolean logicallyDelete) {
        Mono<Boolean> deleted = userService.deleteUsers(userIds, deleteRelationships, logicallyDelete);
        return ResponseFactory.acknowledged(deleted);
    }

    @PutMapping
    @RequiredPermission(AdminPermission.USER_UPDATE)
    public Mono<ResponseEntity> updateUser(
            @RequestParam Set<Long> userIds,
            @RequestBody UpdateUserDTO updateUserDTO) {
        boolean validated = updateUserDTO.getPassword() != null
                && updateUserDTO.getName() != null
                && updateUserDTO.getIntro() != null
                && updateUserDTO.getProfilePictureUrl() != null
                && updateUserDTO.getRegistrationDate() != null
                && updateUserDTO.getActive() != null;
        if (validated) {
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
        } else {
            return ResponseFactory.code(TurmsStatusCode.ILLEGAL_ARGUMENTS);
        }
    }

    @GetMapping("/count")
    @RequiredPermission(AdminPermission.USER_QUERY)
    public Mono<ResponseEntity> countUsers(
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
            @RequestParam(defaultValue = "false") Boolean countOnlineUsers,
            @RequestParam(defaultValue = "NOOP") DivideBy divideBy) {
        if (countOnlineUsers != null && countOnlineUsers) {
            return ResponseFactory.withKey(TOTAL, onlineUserService.countOnlineUsers());
        }
        if (divideBy == null || divideBy == DivideBy.NOOP) {
            List<Mono<Pair<String, Long>>> counts = new LinkedList<>();
            if (deletedStartDate != null || deletedEndDate != null) {
                counts.add(userService.countDeletedUsers(
                        deletedStartDate,
                        deletedEndDate)
                        .map(total -> Pair.of(DELETED_USERS, total)));
            }
            if (sentMessageStartDate != null || sentMessageEndDate != null) {
                counts.add(messageService.countUsersWhoSentMessage(
                        sentMessageStartDate,
                        sentMessageEndDate,
                        null,
                        false)
                        .map(total -> Pair.of(USERS_WHO_SENT_MESSAGES, total)));
            }
            if (loggedInStartDate != null || loggedInEndDate != null) {
                counts.add(userService.countLoggedInUsers(
                        loggedInStartDate,
                        loggedInEndDate)
                        .map(total -> Pair.of(LOGGED_IN_USERS, total)));
            }
            if (maxOnlineUsersStartDate != null || maxOnlineUsersEndDate != null) {
                counts.add(userService.countMaxOnlineUsers(
                        maxOnlineUsersStartDate,
                        maxOnlineUsersEndDate)
                        .map(total -> Pair.of(MAX_ONLINE_USERS, total)));
            }
            if (counts.isEmpty() || registeredStartDate != null || registeredEndDate != null) {
                counts.add(userService.countRegisteredUsers(
                        registeredStartDate,
                        registeredEndDate)
                        .map(total -> Pair.of(REGISTERED_USERS, total)));
            }
            return ResponseFactory.collectCountResults(counts);
        } else {
            List<Mono<Pair<String, List<Map<String, ?>>>>> counts = new LinkedList<>();
            if (deletedStartDate != null && deletedEndDate != null) {
                counts.add(dateTimeUtil.checkAndQueryBetweenDate(
                        DELETED_USERS,
                        deletedStartDate,
                        deletedEndDate,
                        divideBy,
                        userService::countDeletedUsers));
            }
            if (sentMessageStartDate != null && sentMessageEndDate != null) {
                counts.add(dateTimeUtil.checkAndQueryBetweenDate(
                        USERS_WHO_SENT_MESSAGES,
                        sentMessageStartDate,
                        sentMessageEndDate,
                        divideBy,
                        messageService::countUsersWhoSentMessage,
                        null,
                        false));
            }
            if (loggedInStartDate != null && loggedInEndDate != null) {
                counts.add(dateTimeUtil.checkAndQueryBetweenDate(
                        LOGGED_IN_USERS,
                        loggedInStartDate,
                        loggedInEndDate,
                        divideBy,
                        userService::countLoggedInUsers));
            }
            if (maxOnlineUsersStartDate != null && maxOnlineUsersEndDate != null) {
                counts.add(dateTimeUtil.checkAndQueryBetweenDate(
                        MAX_ONLINE_USERS,
                        maxOnlineUsersStartDate,
                        maxOnlineUsersEndDate,
                        divideBy,
                        userService::countMaxOnlineUsers));
            }
            if (registeredStartDate != null && registeredEndDate != null) {
                counts.add(dateTimeUtil.checkAndQueryBetweenDate(
                        REGISTERED_USERS,
                        registeredStartDate,
                        registeredEndDate,
                        divideBy,
                        userService::countRegisteredUsers));
            }
            if (counts.isEmpty()) {
                return ResponseFactory.code(TurmsStatusCode.ILLEGAL_ARGUMENTS);
            }
            return ResponseFactory.collectCountResults(counts);
        }
    }

    /**
     * Note: If userIds is null or empty, turms only queries the online status of local users
     *
     * @param number this only works when userIds is null or empty
     */
    @GetMapping("/online-statuses")
    @RequiredPermission(AdminPermission.USER_QUERY)
    public Mono<ResponseEntity> getOnlineUsersStatus(
            @RequestParam(required = false) Set<Long> userIds,
            @RequestParam(defaultValue = "20") Integer number) {
        if (userIds != null && !userIds.isEmpty()) {
            List<Mono<UserOnlineInfo>> queryUsers = new ArrayList<>(userIds.size());
            for (Long userId : userIds) {
                Mono<UserOnlineInfo> queryInfo = onlineUserService.queryUserOnlineInfo(userId);
                queryInfo = queryInfo.map(info -> {
                    if (info == OFFLINE_USER_ONLINE_INFO) {
                        return UserOnlineInfo.builder()
                                .userId(userId)
                                .userStatus(UserStatus.OFFLINE)
                                .build();
                    } else {
                        return info;
                    }
                });
                queryUsers.add(queryInfo);
            }
            return ResponseFactory.okWhenTruthy(Flux.merge(queryUsers));
        } else {
            if (number > turmsClusterManager.getTurmsProperties().getSecurity()
                    .getMaxQueryOnlineUsersStatusPerRequest()) {
                throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS);
            }
            Flux<UserOnlineInfo> userOnlineInfoFlux = onlineUserService.queryUserOnlineInfos(number);
            return ResponseFactory.okWhenTruthy(userOnlineInfoFlux);
        }
    }

    @PutMapping("/online-statuses")
    @RequiredPermission(AdminPermission.USER_UPDATE)
    public Mono<ResponseEntity> updateUserOnlineStatus(
            @RequestParam Long userId,
            @RequestBody UpdateOnlineStatusDTO updateOnlineStatusDTO) {
        Mono<Boolean> updated = onlineUserService.updateOnlineUserStatus(userId, updateOnlineStatusDTO.getOnlineStatus());
        return ResponseFactory.okWhenTruthy(updated);
    }

    @GetMapping("/users-nearby")
    @RequiredPermission(AdminPermission.USER_QUERY)
    public Mono<ResponseEntity> getUsersNearby(
            @RequestParam Long userId,
            @RequestParam(required = false) DeviceType deviceType,
            @RequestParam(required = false) Integer maxPeopleNumber,
            @RequestParam(required = false) Double maxDistance) {
        Flux<User> usersNearby = usersNearbyService.queryUsersProfilesNearby(userId, deviceType, maxPeopleNumber, maxDistance);
        return ResponseFactory.okWhenTruthy(usersNearby);
    }

    @GetMapping("/locations")
    @RequiredPermission(AdminPermission.USER_QUERY)
    public ResponseEntity getUserLocations(@RequestParam Long userId) {
        SortedSet<UserLocation> userLocations = onlineUserService.getUserLocations(userId);
        return ResponseFactory.okWhenTruthy(userLocations);
    }

    @GetMapping("/groups")
    @RequiredPermission(AdminPermission.USER_QUERY)
    public Mono<ResponseEntity> getUserJoinedGroup(@RequestParam Long userId) {
        Flux<Group> groups = groupService.queryUserJoinedGroup(userId);
        return ResponseFactory.okWhenTruthy(groups);
    }
}
