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
import im.turms.turms.common.DateTimeUtil;
import im.turms.turms.common.PageUtil;
import im.turms.turms.common.TurmsStatusCode;
import im.turms.turms.constant.DivideBy;
import im.turms.turms.exception.TurmsBusinessException;
import im.turms.turms.pojo.domain.User;
import im.turms.turms.pojo.dto.*;
import im.turms.turms.service.message.MessageService;
import im.turms.turms.service.user.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.*;

import static im.turms.turms.constant.AdminPermission.*;

@RestController
@RequestMapping("/users")
public class UserController {
    private final UserService userService;
    private final MessageService messageService;
    private final PageUtil pageUtil;
    private final DateTimeUtil dateTimeUtil;

    public UserController(UserService userService, PageUtil pageUtil, MessageService messageService, DateTimeUtil dateTimeUtil) {
        this.userService = userService;
        this.pageUtil = pageUtil;
        this.messageService = messageService;
        this.dateTimeUtil = dateTimeUtil;
    }

    @GetMapping
    @RequiredPermission(USER_QUERY)
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
    @RequiredPermission(USER_QUERY)
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
    @RequiredPermission(USER_CREATE)
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
    @RequiredPermission(USER_DELETE)
    public Mono<ResponseEntity<ResponseDTO<AcknowledgedDTO>>> deleteUsers(
            @RequestParam Set<Long> userIds,
            @RequestParam(defaultValue = "true") boolean deleteRelationshipsAndGroups,
            @RequestParam(required = false) Boolean logicallyDelete) {
        Mono<Boolean> deleted = userService.deleteUsers(userIds, deleteRelationshipsAndGroups, logicallyDelete);
        return ResponseFactory.acknowledged(deleted);
    }

    @PutMapping
    @RequiredPermission(USER_UPDATE)
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
    @RequiredPermission(USER_QUERY)
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
                        .doOnNext(statistics::setDeletedUsers));
            }
            if (sentMessageStartDate != null || sentMessageEndDate != null) {
                counts.add(messageService.countUsersWhoSentMessage(
                        sentMessageStartDate,
                        sentMessageEndDate,
                        null,
                        false)
                        .doOnNext(statistics::setUsersWhoSentMessages));
            }
            if (loggedInStartDate != null || loggedInEndDate != null) {
                counts.add(userService.countLoggedInUsers(
                        loggedInStartDate,
                        loggedInEndDate)
                        .doOnNext(statistics::setLoggedInUsers));
            }
            if (maxOnlineUsersStartDate != null || maxOnlineUsersEndDate != null) {
                counts.add(userService.countMaxOnlineUsers(
                        maxOnlineUsersStartDate,
                        maxOnlineUsersEndDate)
                        .doOnNext(statistics::setMaxOnlineUsers));
            }
            if (counts.isEmpty() || registeredStartDate != null || registeredEndDate != null) {
                counts.add(userService.countRegisteredUsers(
                        registeredStartDate,
                        registeredEndDate)
                        .doOnNext(statistics::setRegisteredUsers));
            }
        } else {
            if (deletedStartDate != null && deletedEndDate != null) {
                counts.add(dateTimeUtil.checkAndQueryBetweenDate(
                        deletedStartDate,
                        deletedEndDate,
                        divideBy,
                        userService::countDeletedUsers)
                        .doOnNext(statistics::setDeletedUsersRecords));
            }
            if (sentMessageStartDate != null && sentMessageEndDate != null) {
                counts.add(dateTimeUtil.checkAndQueryBetweenDate(
                        sentMessageStartDate,
                        sentMessageEndDate,
                        divideBy,
                        messageService::countUsersWhoSentMessage,
                        null,
                        false)
                        .doOnNext(statistics::setUsersWhoSentMessagesRecords));
            }
            if (loggedInStartDate != null && loggedInEndDate != null) {
                counts.add(dateTimeUtil.checkAndQueryBetweenDate(
                        loggedInStartDate,
                        loggedInEndDate,
                        divideBy,
                        userService::countLoggedInUsers)
                        .doOnNext(statistics::setLoggedInUsersRecords));
            }
            if (maxOnlineUsersStartDate != null && maxOnlineUsersEndDate != null) {
                counts.add(dateTimeUtil.checkAndQueryBetweenDate(
                        maxOnlineUsersStartDate,
                        maxOnlineUsersEndDate,
                        divideBy,
                        userService::countMaxOnlineUsers)
                        .doOnNext(statistics::setMaxOnlineUsersRecords));
            }
            if (registeredStartDate != null && registeredEndDate != null) {
                counts.add(dateTimeUtil.checkAndQueryBetweenDate(
                        registeredStartDate,
                        registeredEndDate,
                        divideBy,
                        userService::countRegisteredUsers)
                        .doOnNext(statistics::setRegisteredUsersRecords));
            }
            if (counts.isEmpty()) {
                throw TurmsBusinessException.get(TurmsStatusCode.ILLEGAL_ARGUMENTS);
            }
        }
        return ResponseFactory.okIfTruthy(Flux.merge(counts).then(Mono.just(statistics)));
    }
}
