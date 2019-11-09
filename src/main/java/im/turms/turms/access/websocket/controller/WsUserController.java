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

package im.turms.turms.access.websocket.controller;

import im.turms.turms.annotation.websocket.TurmsRequestMapping;
import im.turms.turms.cluster.TurmsClusterManager;
import im.turms.turms.common.ProtoUtil;
import im.turms.turms.common.TurmsStatusCode;
import im.turms.turms.constant.ProfileAccessStrategy;
import im.turms.turms.constant.UserStatus;
import im.turms.turms.pojo.bo.RequestResult;
import im.turms.turms.pojo.bo.TurmsRequestWrapper;
import im.turms.turms.pojo.bo.UserOnlineInfo;
import im.turms.turms.pojo.bo.common.Int64Values;
import im.turms.turms.pojo.bo.user.UsersInfosWithVersion;
import im.turms.turms.pojo.bo.user.UsersOnlineStatuses;
import im.turms.turms.pojo.domain.User;
import im.turms.turms.pojo.notification.TurmsNotification;
import im.turms.turms.pojo.request.TurmsRequest;
import im.turms.turms.pojo.request.user.*;
import im.turms.turms.service.group.GroupMemberService;
import im.turms.turms.service.user.UserService;
import im.turms.turms.service.user.onlineuser.OnlineUserService;
import im.turms.turms.service.user.onlineuser.UsersNearbyService;
import im.turms.turms.service.user.relationship.UserRelationshipService;
import org.springframework.stereotype.Controller;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static im.turms.turms.common.Constants.OFFLINE_USER_ONLINE_INFO;

@Controller
public class WsUserController {
    private final UserService userService;
    private final UserRelationshipService userRelationshipService;
    private final UsersNearbyService usersNearbyService;
    private final OnlineUserService onlineUserService;
    private final GroupMemberService groupMemberService;
    private final TurmsClusterManager turmsClusterManager;

    public WsUserController(UserService userService, UsersNearbyService usersNearbyService, OnlineUserService onlineUserService, TurmsClusterManager turmsClusterManager, GroupMemberService groupMemberService, UserRelationshipService userRelationshipService) {
        this.userService = userService;
        this.usersNearbyService = usersNearbyService;
        this.onlineUserService = onlineUserService;
        this.turmsClusterManager = turmsClusterManager;
        this.groupMemberService = groupMemberService;
        this.userRelationshipService = userRelationshipService;
    }

    @TurmsRequestMapping(TurmsRequest.KindCase.QUERY_USER_GROUP_INVITATIONS_REQUEST)
    public Function<TurmsRequestWrapper, Mono<RequestResult>> handleQueryUserGroupInvitationsRequest() {
        return turmsRequestWrapper -> {
            QueryUserGroupInvitationsRequest request = turmsRequestWrapper.getTurmsRequest().getQueryUserGroupInvitationsRequest();
            Date lastUpdatedDate = request.hasLastUpdatedDate() ? new Date(request.getLastUpdatedDate().getValue()) : null;
            return userService.queryUserGroupInvitationsWithVersion(
                    turmsRequestWrapper.getUserId(),
                    lastUpdatedDate)
                    .map(groupInvitationsWithVersion -> RequestResult.responseData(TurmsNotification.Data
                            .newBuilder()
                            .setGroupInvitationsWithVersion(groupInvitationsWithVersion)
                            .build()));
        };
    }

    @TurmsRequestMapping(TurmsRequest.KindCase.QUERY_USER_PROFILE_REQUEST)
    public Function<TurmsRequestWrapper, Mono<RequestResult>> handleQueryUserProfileRequest() {
        return turmsRequestWrapper -> {
            QueryUserProfileRequest request = turmsRequestWrapper.getTurmsRequest().getQueryUserProfileRequest();
            return userService.authAndQueryUserProfile(
                    turmsRequestWrapper.getUserId(),
                    request.getUserId())
                    .map(user -> {
                        UsersInfosWithVersion.Builder userBuilder = UsersInfosWithVersion
                                .newBuilder()
                                .addUserInfos(ProtoUtil.userProfile2proto(user).build());
                        return RequestResult.responseData(TurmsNotification.Data
                                .newBuilder()
                                .setUsersInfosWithVersion(userBuilder)
                                .build());
                    });
        };
    }

    @TurmsRequestMapping(TurmsRequest.KindCase.QUERY_USERS_IDS_NEARBY_REQUEST)
    public Function<TurmsRequestWrapper, Mono<RequestResult>> handleQueryUsersIdsNearbyRequest() {
        return turmsRequestWrapper -> {
            QueryUsersIdsNearbyRequest request = turmsRequestWrapper.getTurmsRequest().getQueryUsersIdsNearbyRequest();
            Double distance = request.hasDistance() ? (double) request.getDistance().getValue() : null;
            Integer maxNumber = request.hasMaxNumber() ? request.getMaxNumber().getValue() : null;
            usersNearbyService.upsertUserLocation(
                    turmsRequestWrapper.getUserId(),
                    request.getLongitude(),
                    request.getLatitude(),
                    new Date());
            return usersNearbyService.queryUsersIdsNearby(
                    turmsRequestWrapper.getUserId(),
                    turmsRequestWrapper.getDeviceType(),
                    maxNumber,
                    distance)
                    .collectList()
                    .map(ids -> RequestResult.responseData(TurmsNotification.Data
                            .newBuilder()
                            .setIds(Int64Values.newBuilder().addAllValues(ids))
                            .build()));
        };
    }

    //TODO: query specific fields
    @TurmsRequestMapping(TurmsRequest.KindCase.QUERY_USERS_INFOS_NEARBY_REQUEST)
    public Function<TurmsRequestWrapper, Mono<RequestResult>> handleQueryUsersInfosNearbyRequest() {
        return turmsRequestWrapper -> {
            QueryUsersInfosNearbyRequest request = turmsRequestWrapper.getTurmsRequest().getQueryUsersInfosNearbyRequest();
            Double distance = request.hasDistance() ? (double) request.getDistance().getValue() : null;
            Integer maxNumber = request.hasMaxNumber() ? request.getMaxNumber().getValue() : null;
            usersNearbyService.upsertUserLocation(
                    turmsRequestWrapper.getUserId(),
                    request.getLongitude(),
                    request.getLatitude(),
                    new Date());
            return usersNearbyService.queryUsersProfilesNearby(
                    turmsRequestWrapper.getUserId(),
                    turmsRequestWrapper.getDeviceType(),
                    maxNumber,
                    distance)
                    .collectList()
                    .map(users -> {
                        if (users.isEmpty()) {
                            return RequestResult.status(TurmsStatusCode.NOT_FOUND);
                        }
                        UsersInfosWithVersion.Builder builder = UsersInfosWithVersion.newBuilder();
                        for (User user : users) {
                            builder.addUserInfos(ProtoUtil.userProfile2proto(user));
                        }
                        return RequestResult
                                .responseData(TurmsNotification.Data
                                        .newBuilder()
                                        .setUsersInfosWithVersion(builder)
                                        .build());
                    });
        };
    }

    @TurmsRequestMapping(TurmsRequest.KindCase.QUERY_USERS_ONLINE_STATUS_REQUEST)
    public Function<TurmsRequestWrapper, Mono<RequestResult>> handleQueryUsersOnlineStatusRequest() {
        return turmsRequestWrapper -> {
            QueryUsersOnlineStatusRequest request = turmsRequestWrapper.getTurmsRequest().getQueryUsersOnlineStatusRequest();
            if (request.getUsersIdsCount() == 0) {
                return Mono.just(RequestResult.status(TurmsStatusCode.ILLEGAL_ARGUMENTS));
            }
            //TODO : Access Control
            List<Long> usersIds = request.getUsersIdsList();
            UsersOnlineStatuses.Builder statusesBuilder = UsersOnlineStatuses.newBuilder();
            List<Mono<UserOnlineInfo>> monos = new ArrayList<>(usersIds.size());
            for (Long userId : usersIds) {
                monos.add(onlineUserService.queryUserOnlineInfo(userId)
                        .defaultIfEmpty(OFFLINE_USER_ONLINE_INFO));
            }
            return Mono.zip(monos, objects -> objects)
                    .map(infos -> {
                        for (int i = 0; i < usersIds.size(); i++) {
                            statusesBuilder.addUserStatuses(ProtoUtil
                                    .userOnlineInfo2userStatus(usersIds.get(i), (UserOnlineInfo) infos[i])
                                    .build());
                        }
                        return RequestResult.responseData(TurmsNotification.Data.newBuilder()
                                .setUsersOnlineStatuses(statusesBuilder)
                                .build());
                    });
        };
    }

    @TurmsRequestMapping(TurmsRequest.KindCase.UPDATE_USER_LOCATION_REQUEST)
    public Function<TurmsRequestWrapper, Mono<RequestResult>> handleUpdateUserLocationRequest() {
        return turmsRequestWrapper -> {
            UpdateUserLocationRequest request = turmsRequestWrapper.getTurmsRequest().getUpdateUserLocationRequest();
            String name = request.hasName() ? request.getName().getValue() : null;
            String address = request.hasAddress() ? request.getAddress().getValue() : null;
            boolean updated = onlineUserService.updateUserLocation(
                    turmsRequestWrapper.getUserId(),
                    turmsRequestWrapper.getDeviceType(),
                    request.getLatitude(),
                    request.getLongitude(),
                    name,
                    address);
            return Mono.just(RequestResult.okIfTrue(updated));
        };
    }

    /**
     * Do not notify the user status change to somebodies like her/his related users.
     * The client itself should query whether there is any user status changes according to your own
     * business scenarios.
     */
    @TurmsRequestMapping(TurmsRequest.KindCase.UPDATE_USER_ONLINE_STATUS_REQUEST)
    public Function<TurmsRequestWrapper, Mono<RequestResult>> handleUpdateUserOnlineStatusRequest() {
        return turmsRequestWrapper -> {
            UpdateUserOnlineStatusRequest request = turmsRequestWrapper.getTurmsRequest().getUpdateUserOnlineStatusRequest();
            UserStatus userStatus = request.getUserStatus();
            if (userStatus == UserStatus.UNRECOGNIZED || userStatus == UserStatus.OFFLINE) {
                return Mono.just(RequestResult.status(TurmsStatusCode.ILLEGAL_ARGUMENTS));
            }
            boolean updated = onlineUserService.getLocalOnlineUserManager(turmsRequestWrapper.getUserId())
                    .setUserOnlineStatus(userStatus);
            boolean notifyMembers = turmsClusterManager.getTurmsProperties().getNotification().isNotifyMembersAfterOtherMemberOnlineStatusUpdated();
            boolean notifyRelatedUser = turmsClusterManager.getTurmsProperties().getNotification().isNotifyRelatedUsersAfterOtherRelatedUsersOnlineStatusUpdated();
            if (!notifyMembers && !notifyRelatedUser) {
                return Mono.just(RequestResult.okIfTrue(updated));
            } else {
                Mono<Set<Long>> queryMembersIds = Mono.just(Collections.emptySet());
                Mono<Set<Long>> queryRelatedUsersIds = Mono.just(Collections.emptySet());
                if (notifyMembers) {
                    queryMembersIds = groupMemberService.queryUserJoinedGroupsMembersIds(
                            turmsRequestWrapper.getUserId());
                }
                if (notifyRelatedUser) {
                    queryRelatedUsersIds = userRelationshipService.queryRelatedUsersIds(
                            turmsRequestWrapper.getUserId(),
                            false)
                            .collect(Collectors.toSet());
                }
                return queryMembersIds.zipWith(queryRelatedUsersIds)
                        .map(results -> {
                            results.getT1().addAll(results.getT2());
                            if (results.getT1().isEmpty()) {
                                return RequestResult.ok();
                            } else {
                                return RequestResult.recipientData(
                                        results.getT1(),
                                        turmsRequestWrapper.getTurmsRequest());
                            }
                        });
            }
        };
    }

    @TurmsRequestMapping(TurmsRequest.KindCase.UPDATE_USER_REQUEST)
    public Function<TurmsRequestWrapper, Mono<RequestResult>> handleUpdateUserRequest() {
        return turmsRequestWrapper -> {
            UpdateUserRequest request = turmsRequestWrapper.getTurmsRequest().getUpdateUserRequest();
            String password = request.hasPassword() ? request.getPassword().getValue() : null;
            String name = request.hasName() ? request.getName().getValue() : null;
            String intro = request.hasIntro() ? request.getIntro().getValue() : null;
            String profilePictureUrl = request.hasProfilePictureUrl() ? request.getProfilePictureUrl().getValue() : null;
            ProfileAccessStrategy profileAccessStrategy = request.getProfileAccessStrategy();
            return userService.updateUser(
                    turmsRequestWrapper.getUserId(),
                    password,
                    name,
                    intro,
                    profilePictureUrl,
                    profileAccessStrategy,
                    null,
                    null)
                    .flatMap(updated -> {
                        if (updated != null && updated) {
                            if (turmsClusterManager.getTurmsProperties().getNotification().isNotifyRelatedUsersAfterUserInfoUpdated()) {
                                return userRelationshipService.queryRelatedUsersIds(turmsRequestWrapper.getUserId(), false)
                                        .collect(Collectors.toSet())
                                        .map(relatedUsersIds -> {
                                            if (relatedUsersIds.isEmpty()) {
                                                return RequestResult.ok();
                                            } else {
                                                return RequestResult.recipientData(
                                                        relatedUsersIds,
                                                        turmsRequestWrapper.getTurmsRequest());
                                            }
                                        });
                            }
                        }
                        return Mono.just(RequestResult.okIfTrue(updated));
                    });
        };
    }
}
