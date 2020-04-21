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

import im.turms.common.constant.DivideBy;
import im.turms.turms.access.web.util.ResponseFactory;
import im.turms.turms.annotation.web.RequiredPermission;
import im.turms.turms.pojo.bo.DateRange;
import im.turms.turms.pojo.domain.User;
import im.turms.turms.pojo.dto.*;
import im.turms.turms.service.message.MessageService;
import im.turms.turms.service.user.UserService;
import im.turms.turms.util.DateTimeUtil;
import im.turms.turms.util.PageUtil;
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

    @PostMapping
    @RequiredPermission(USER_CREATE)
    public Mono<ResponseEntity<ResponseDTO<User>>> addUser(@RequestBody AddUserDTO addUserDTO) {
        Mono<User> addUser = userService.addUser(
                addUserDTO.getId(),
                addUserDTO.getPassword(),
                addUserDTO.getName(),
                addUserDTO.getIntro(),
                addUserDTO.getProfileAccess(),
                addUserDTO.getPermissionGroupId(),
                addUserDTO.getRegistrationDate(),
                addUserDTO.getIsActive());
        return ResponseFactory.okIfTruthy(addUser);
    }

    @GetMapping
    @RequiredPermission(USER_QUERY)
    public Mono<ResponseEntity<ResponseDTO<Collection<User>>>> queryUsers(
            @RequestParam(required = false) Set<Long> ids,
            @RequestParam(required = false) Date registrationDateStart,
            @RequestParam(required = false) Date registrationDateEnd,
            @RequestParam(required = false) Date deletionDateStart,
            @RequestParam(required = false) Date deletionDateEnd,
            @RequestParam(required = false) Boolean isActive,
            @RequestParam(required = false) Integer size) {
        size = pageUtil.getSize(size);
        Flux<User> usersFlux = userService.queryUsers(
                ids,
                DateRange.of(registrationDateStart, registrationDateEnd),
                DateRange.of(deletionDateStart, deletionDateEnd),
                isActive,
                0,
                size,
                true);
        return ResponseFactory.okIfTruthy(usersFlux);
    }

    @GetMapping("/page")
    @RequiredPermission(USER_QUERY)
    public Mono<ResponseEntity<ResponseDTO<PaginationDTO<User>>>> queryUsers(
            @RequestParam(required = false) Set<Long> ids,
            @RequestParam(required = false) Date registrationDateStart,
            @RequestParam(required = false) Date registrationDateEnd,
            @RequestParam(required = false) Date deletionDateStart,
            @RequestParam(required = false) Date deletionDateEnd,
            @RequestParam(required = false) Boolean isActive,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(required = false) Integer size) {
        size = pageUtil.getSize(size);
        Mono<Long> count = userService.countUsers(
                ids,
                DateRange.of(registrationDateStart, registrationDateEnd),
                DateRange.of(deletionDateStart, deletionDateEnd),
                isActive);
        Flux<User> usersFlux = userService.queryUsers(
                ids,
                DateRange.of(registrationDateStart, registrationDateEnd),
                DateRange.of(deletionDateStart, deletionDateEnd),
                isActive,
                page,
                size,
                true);
        return ResponseFactory.page(count, usersFlux);
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
                        DateRange.of(deletedStartDate, deletedEndDate))
                        .doOnNext(statistics::setDeletedUsers));
            }
            if (sentMessageStartDate != null || sentMessageEndDate != null) {
                counts.add(messageService.countUsersWhoSentMessage(
                        DateRange.of(sentMessageStartDate, sentMessageEndDate),
                        null,
                        false)
                        .doOnNext(statistics::setUsersWhoSentMessages));
            }
            if (loggedInStartDate != null || loggedInEndDate != null) {
                counts.add(userService.countLoggedInUsers(
                        DateRange.of(loggedInStartDate, loggedInEndDate), true)
                        .doOnNext(statistics::setLoggedInUsers));
            }
            if (maxOnlineUsersStartDate != null || maxOnlineUsersEndDate != null) {
                counts.add(userService.countMaxOnlineUsers(
                        DateRange.of(maxOnlineUsersStartDate, maxOnlineUsersEndDate))
                        .doOnNext(statistics::setMaxOnlineUsers));
            }
            if (counts.isEmpty() || registeredStartDate != null || registeredEndDate != null) {
                counts.add(userService.countRegisteredUsers(
                        DateRange.of(registeredStartDate, registeredEndDate), true)
                        .doOnNext(statistics::setRegisteredUsers));
            }
        } else {
            if (deletedStartDate != null && deletedEndDate != null) {
                counts.add(dateTimeUtil.checkAndQueryBetweenDate(
                        DateRange.of(deletedStartDate, deletedEndDate),
                        divideBy,
                        userService::countDeletedUsers)
                        .doOnNext(statistics::setDeletedUsersRecords));
            }
            if (sentMessageStartDate != null && sentMessageEndDate != null) {
                counts.add(dateTimeUtil.checkAndQueryBetweenDate(
                        DateRange.of(sentMessageStartDate, sentMessageEndDate),
                        divideBy,
                        messageService::countUsersWhoSentMessage,
                        null,
                        false)
                        .doOnNext(statistics::setUsersWhoSentMessagesRecords));
            }
            if (loggedInStartDate != null && loggedInEndDate != null) {
                counts.add(dateTimeUtil.checkAndQueryBetweenDate(
                        DateRange.of(loggedInStartDate, loggedInEndDate),
                        divideBy,
                        dateRange -> userService.countLoggedInUsers(dateRange, true))
                        .doOnNext(statistics::setLoggedInUsersRecords));
            }
            if (maxOnlineUsersStartDate != null && maxOnlineUsersEndDate != null) {
                counts.add(dateTimeUtil.checkAndQueryBetweenDate(
                        DateRange.of(maxOnlineUsersStartDate, maxOnlineUsersEndDate),
                        divideBy,
                        userService::countMaxOnlineUsers)
                        .doOnNext(statistics::setMaxOnlineUsersRecords));
            }
            if (registeredStartDate != null && registeredEndDate != null) {
                counts.add(dateTimeUtil.checkAndQueryBetweenDate(
                        DateRange.of(registeredStartDate, registeredEndDate),
                        divideBy,
                        dateRange -> userService.countRegisteredUsers(dateRange, true))
                        .doOnNext(statistics::setRegisteredUsersRecords));
            }
            if (counts.isEmpty()) {
                return Mono.empty();
            }
        }
        return ResponseFactory.okIfTruthy(Flux.merge(counts).then(Mono.just(statistics)));
    }

    @PutMapping
    @RequiredPermission(USER_UPDATE)
    public Mono<ResponseEntity<ResponseDTO<AcknowledgedDTO>>> updateUser(
            @RequestParam Set<Long> ids,
            @RequestBody UpdateUserDTO updateUserDTO) {
        Mono<Boolean> updateMono = userService.updateUsers(
                ids,
                updateUserDTO.getPassword(),
                updateUserDTO.getName(),
                updateUserDTO.getIntro(),
                updateUserDTO.getProfileAccess(),
                updateUserDTO.getPermissionGroupId(),
                updateUserDTO.getRegistrationDate(),
                updateUserDTO.getIsActive());
        return ResponseFactory.acknowledged(updateMono);
    }

    @DeleteMapping
    @RequiredPermission(USER_DELETE)
    public Mono<ResponseEntity<ResponseDTO<AcknowledgedDTO>>> deleteUsers(
            @RequestParam Set<Long> ids,
            @RequestParam(required = false) Boolean shouldDeleteLogically) {
        Mono<Boolean> deleteMono = userService.deleteUsers(ids, shouldDeleteLogically);
        return ResponseFactory.acknowledged(deleteMono);
    }
}
