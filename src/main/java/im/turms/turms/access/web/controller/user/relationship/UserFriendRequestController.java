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

package im.turms.turms.access.web.controller.user.relationship;

import im.turms.turms.access.web.util.ResponseFactory;
import im.turms.turms.annotation.web.RequiredPermission;
import im.turms.turms.common.PageUtil;
import im.turms.turms.constant.RequestStatus;
import im.turms.turms.pojo.domain.UserFriendRequest;
import im.turms.turms.pojo.dto.*;
import im.turms.turms.service.user.relationship.UserFriendRequestService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collection;
import java.util.Date;
import java.util.Set;

import static im.turms.turms.constant.AdminPermission.*;

@RestController
@RequestMapping("/users/relationships/friend-requests")
public class UserFriendRequestController {
    private final UserFriendRequestService userFriendRequestService;
    private final PageUtil pageUtil;

    public UserFriendRequestController(PageUtil pageUtil, UserFriendRequestService userFriendRequestService) {
        this.pageUtil = pageUtil;
        this.userFriendRequestService = userFriendRequestService;
    }

    @PostMapping
    @RequiredPermission(USER_FRIEND_REQUEST_CREATE)
    public Mono<ResponseEntity<ResponseDTO<UserFriendRequest>>> createFriendRequest(@RequestBody AddFriendRequestDTO dto) {
        Mono<UserFriendRequest> createMono = userFriendRequestService.createFriendRequest(
                dto.getRequesterId(),
                dto.getRecipientId(),
                dto.getContent(),
                dto.getStatus(),
                dto.getCreationDate(),
                dto.getResponseDate(),
                dto.getExpirationDate(),
                dto.getReason());
        return ResponseFactory.okIfTruthy(createMono);
    }

    @DeleteMapping
    @RequiredPermission(USER_FRIEND_REQUEST_DELETE)
    public Mono<ResponseEntity<ResponseDTO<AcknowledgedDTO>>> deleteFriendRequests(
            @RequestParam(required = false) Set<Long> ids,
            @RequestParam(required = false) Long requesterId,
            @RequestParam(required = false) Long recipientId,
            @RequestParam(required = false) RequestStatus status,
            @RequestParam(required = false) Date creationDateStart,
            @RequestParam(required = false) Date creationDateEnd,
            @RequestParam(required = false) Date responseDateStart,
            @RequestParam(required = false) Date responseDateEnd,
            @RequestParam(required = false) Date expirationDateStart,
            @RequestParam(required = false) Date expirationDateEnd) {
        Mono<Boolean> deleteMono = userFriendRequestService.deleteFriendRequests(
                ids,
                requesterId,
                recipientId,
                status,
                creationDateStart,
                creationDateEnd,
                responseDateStart,
                responseDateEnd,
                expirationDateStart,
                expirationDateEnd);
        return ResponseFactory.acknowledged(deleteMono);
    }

    @PutMapping
    @RequiredPermission(USER_FRIEND_REQUEST_UPDATE)
    public Mono<ResponseEntity<ResponseDTO<AcknowledgedDTO>>> updateFriendRequests(
            @RequestParam Set<Long> ids,
            @RequestBody UpdateFriendRequestDTO dto) {
        Mono<Boolean> updateMono = userFriendRequestService.updateFriendRequests(
                ids,
                dto.getRequesterId(),
                dto.getRecipientId(),
                dto.getContent(),
                dto.getStatus(),
                dto.getReason(),
                dto.getCreationDate(),
                dto.getResponseDate(),
                dto.getExpirationDate());
        return ResponseFactory.acknowledged(updateMono);
    }

    @GetMapping
    @RequiredPermission(USER_FRIEND_REQUEST_QUERY)
    public Mono<ResponseEntity<ResponseDTO<Collection<UserFriendRequest>>>> queryFriendRequests(
            @RequestParam(required = false) Set<Long> ids,
            @RequestParam(required = false) Long requesterId,
            @RequestParam(required = false) Long recipientId,
            @RequestParam(required = false) RequestStatus status,
            @RequestParam(required = false) Date creationDateStart,
            @RequestParam(required = false) Date creationDateEnd,
            @RequestParam(required = false) Date responseDateStart,
            @RequestParam(required = false) Date responseDateEnd,
            @RequestParam(required = false) Date expirationDateStart,
            @RequestParam(required = false) Date expirationDateEnd,
            @RequestParam(required = false) Integer size) {
        size = pageUtil.getSize(size);
        Flux<UserFriendRequest> userFriendRequestFlux = userFriendRequestService.queryFriendRequests(
                ids,
                requesterId,
                recipientId,
                status,
                creationDateStart,
                creationDateEnd,
                responseDateStart,
                responseDateEnd,
                expirationDateStart,
                expirationDateEnd,
                0,
                size);
        return ResponseFactory.okIfTruthy(userFriendRequestFlux);
    }

    @GetMapping("/page")
    @RequiredPermission(USER_FRIEND_REQUEST_QUERY)
    public Mono<ResponseEntity<ResponseDTO<PaginationDTO<UserFriendRequest>>>> queryFriendRequests(
            @RequestParam(required = false) Set<Long> ids,
            @RequestParam(required = false) Long requesterId,
            @RequestParam(required = false) Long recipientId,
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
        Mono<Long> count = userFriendRequestService.countFriendRequests(
                ids,
                requesterId,
                recipientId,
                status,
                creationDateStart,
                creationDateEnd,
                responseDateStart,
                responseDateEnd,
                expirationDateStart,
                expirationDateEnd);
        Flux<UserFriendRequest> userFriendRequestFlux = userFriendRequestService.queryFriendRequests(
                ids,
                requesterId,
                recipientId,
                status,
                creationDateStart,
                creationDateEnd,
                responseDateStart,
                responseDateEnd,
                expirationDateStart,
                expirationDateEnd,
                page,
                size);
        return ResponseFactory.page(count, userFriendRequestFlux);
    }
}
