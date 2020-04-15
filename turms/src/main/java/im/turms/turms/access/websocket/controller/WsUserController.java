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

import com.google.common.collect.Sets;
import im.turms.common.TurmsCloseStatus;
import im.turms.common.TurmsStatusCode;
import im.turms.common.constant.DeviceType;
import im.turms.common.constant.ProfileAccessStrategy;
import im.turms.common.constant.UserStatus;
import im.turms.common.model.bo.common.Int64Values;
import im.turms.common.model.bo.user.UserSessionId;
import im.turms.common.model.bo.user.UserSessionIds;
import im.turms.common.model.bo.user.UsersInfosWithVersion;
import im.turms.common.model.bo.user.UsersOnlineStatuses;
import im.turms.common.model.dto.notification.TurmsNotification;
import im.turms.common.model.dto.request.user.*;
import im.turms.turms.annotation.websocket.TurmsRequestMapping;
import im.turms.turms.constant.CloseStatusFactory;
import im.turms.turms.manager.TurmsClusterManager;
import im.turms.turms.pojo.bo.RequestResult;
import im.turms.turms.pojo.bo.TurmsRequestWrapper;
import im.turms.turms.pojo.bo.UserOnlineInfo;
import im.turms.turms.pojo.domain.User;
import im.turms.turms.service.group.GroupInvitationService;
import im.turms.turms.service.group.GroupMemberService;
import im.turms.turms.service.user.UserService;
import im.turms.turms.service.user.onlineuser.OnlineUserService;
import im.turms.turms.service.user.onlineuser.UsersNearbyService;
import im.turms.turms.service.user.relationship.UserRelationshipService;
import im.turms.turms.util.ProtoUtil;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Controller;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static im.turms.common.model.dto.request.TurmsRequest.KindCase.*;

@Controller
public class WsUserController {
    private final UserService userService;
    private final UserRelationshipService userRelationshipService;
    private final UsersNearbyService usersNearbyService;
    private final OnlineUserService onlineUserService;
    private final GroupMemberService groupMemberService;
    private final GroupInvitationService groupInvitationService;
    private final TurmsClusterManager turmsClusterManager;

    public WsUserController(UserService userService, UsersNearbyService usersNearbyService, OnlineUserService onlineUserService, TurmsClusterManager turmsClusterManager, GroupMemberService groupMemberService, UserRelationshipService userRelationshipService, GroupInvitationService groupInvitationService) {
        this.userService = userService;
        this.usersNearbyService = usersNearbyService;
        this.onlineUserService = onlineUserService;
        this.turmsClusterManager = turmsClusterManager;
        this.groupMemberService = groupMemberService;
        this.userRelationshipService = userRelationshipService;
        this.groupInvitationService = groupInvitationService;
    }

    @TurmsRequestMapping(QUERY_USER_GROUP_INVITATIONS_REQUEST)
    public Function<TurmsRequestWrapper, Mono<RequestResult>> handleQueryUserGroupInvitationsRequest() {
        return turmsRequestWrapper -> {
            QueryUserGroupInvitationsRequest request = turmsRequestWrapper.getTurmsRequest().getQueryUserGroupInvitationsRequest();
            Date lastUpdatedDate = request.hasLastUpdatedDate() ? new Date(request.getLastUpdatedDate().getValue()) : null;
            return groupInvitationService.queryUserGroupInvitationsWithVersion(
                    turmsRequestWrapper.getUserId(),
                    lastUpdatedDate)
                    .map(groupInvitationsWithVersion -> RequestResult.create(TurmsNotification.Data
                            .newBuilder()
                            .setGroupInvitationsWithVersion(groupInvitationsWithVersion)
                            .build()));
        };
    }

    @TurmsRequestMapping(QUERY_USER_PROFILE_REQUEST)
    public Function<TurmsRequestWrapper, Mono<RequestResult>> handleQueryUserProfileRequest() {
        return turmsRequestWrapper -> {
            QueryUserProfileRequest request = turmsRequestWrapper.getTurmsRequest().getQueryUserProfileRequest();
            return userService.authAndQueryUserProfile(
                    turmsRequestWrapper.getUserId(),
                    request.getUserId(),
                    false)
                    .map(user -> {
                        UsersInfosWithVersion.Builder userBuilder = UsersInfosWithVersion
                                .newBuilder()
                                .addUserInfos(ProtoUtil.userProfile2proto(user).build());
                        return RequestResult.create(TurmsNotification.Data
                                .newBuilder()
                                .setUsersInfosWithVersion(userBuilder)
                                .build());
                    });
        };
    }

    @TurmsRequestMapping(QUERY_USERS_IDS_NEARBY_REQUEST)
    public Function<TurmsRequestWrapper, Mono<RequestResult>> handleQueryUsersIdsNearbyRequest() {
        return turmsRequestWrapper -> {
            QueryUsersIdsNearbyRequest request = turmsRequestWrapper.getTurmsRequest().getQueryUsersIdsNearbyRequest();
            Double distance = request.hasDistance() ? (double) request.getDistance().getValue() : null;
            Integer maxNumber = request.hasMaxNumber() ? request.getMaxNumber().getValue() : null;
            usersNearbyService.upsertUserLocation(
                    turmsRequestWrapper.getUserId(),
                    turmsRequestWrapper.getDeviceType(),
                    request.getLongitude(),
                    request.getLatitude(),
                    new Date());
            if (usersNearbyService.isTreatUserIdAndDeviceTypeAsUniqueUser()) {
                return usersNearbyService.queryNearestUserSessionIds(
                        turmsRequestWrapper.getUserId(),
                        turmsRequestWrapper.getDeviceType(),
                        maxNumber,
                        distance)
                        .collectList()
                        .map(ids -> {
                            UserSessionIds.Builder builder = UserSessionIds.newBuilder();
                            for (Pair<Long, DeviceType> id : ids) {
                                UserSessionId sessionId = UserSessionId.newBuilder()
                                        .setUserId(id.getLeft())
                                        .setDeviceType(id.getRight())
                                        .build();
                                builder.addUserSessionIds(sessionId);
                            }
                            return RequestResult.create(TurmsNotification.Data
                                    .newBuilder()
                                    .setUserSessionIds(builder.build())
                                    .build());
                        });
            } else {
                return usersNearbyService.queryNearestUserIds(
                        turmsRequestWrapper.getUserId(),
                        turmsRequestWrapper.getDeviceType(),
                        maxNumber,
                        distance)
                        .collectList()
                        .map(ids -> RequestResult.create(TurmsNotification.Data
                                .newBuilder()
                                .setIds(Int64Values.newBuilder().addAllValues(ids))
                                .build()));
            }
        };
    }

    @TurmsRequestMapping(QUERY_USERS_INFOS_NEARBY_REQUEST)
    public Function<TurmsRequestWrapper, Mono<RequestResult>> handleQueryUsersInfosNearbyRequest() {
        return turmsRequestWrapper -> {
            QueryUsersInfosNearbyRequest request = turmsRequestWrapper.getTurmsRequest().getQueryUsersInfosNearbyRequest();
            Double distance = request.hasDistance() ? (double) request.getDistance().getValue() : null;
            Integer maxNumber = request.hasMaxNumber() ? request.getMaxNumber().getValue() : null;
            usersNearbyService.upsertUserLocation(
                    turmsRequestWrapper.getUserId(),
                    turmsRequestWrapper.getDeviceType(),
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
                            return RequestResult.create(TurmsStatusCode.NO_CONTENT);
                        }
                        UsersInfosWithVersion.Builder builder = UsersInfosWithVersion.newBuilder();
                        for (User user : users) {
                            builder.addUserInfos(ProtoUtil.userProfile2proto(user));
                        }
                        return RequestResult
                                .create(TurmsNotification.Data
                                        .newBuilder()
                                        .setUsersInfosWithVersion(builder)
                                        .build());
                    });
        };
    }

    @TurmsRequestMapping(QUERY_USERS_ONLINE_STATUS_REQUEST)
    public Function<TurmsRequestWrapper, Mono<RequestResult>> handleQueryUsersOnlineStatusRequest() {
        return turmsRequestWrapper -> {
            QueryUsersOnlineStatusRequest request = turmsRequestWrapper.getTurmsRequest().getQueryUsersOnlineStatusRequest();
            if (request.getUsersIdsCount() == 0) {
                return Mono.empty();
            }
            //TODO : Access Control
            List<Long> usersIds = request.getUsersIdsList();
            UsersOnlineStatuses.Builder statusesBuilder = UsersOnlineStatuses.newBuilder();
            List<Mono<UserOnlineInfo>> monos = new ArrayList<>(usersIds.size());
            for (Long userId : usersIds) {
                monos.add(onlineUserService.queryUserOnlineInfo(userId));
            }
            return Mono.zip(monos, objects -> objects)
                    .map(infos -> {
                        for (int i = 0; i < usersIds.size(); i++) {
                            statusesBuilder.addUserStatuses(ProtoUtil
                                    .userOnlineInfo2userStatus(
                                            usersIds.get(i),
                                            (UserOnlineInfo) infos[i],
                                            turmsClusterManager.getTurmsProperties().getUser().isRespondOfflineIfInvisible())
                                    .build());
                        }
                        return RequestResult.create(TurmsNotification.Data.newBuilder()
                                .setUsersOnlineStatuses(statusesBuilder)
                                .build());
                    });
        };
    }

    @TurmsRequestMapping(UPDATE_USER_LOCATION_REQUEST)
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
    @TurmsRequestMapping(UPDATE_USER_ONLINE_STATUS_REQUEST)
    public Function<TurmsRequestWrapper, Mono<RequestResult>> handleUpdateUserOnlineStatusRequest() {
        return turmsRequestWrapper -> {
            UpdateUserOnlineStatusRequest request = turmsRequestWrapper.getTurmsRequest().getUpdateUserOnlineStatusRequest();
            UserStatus userStatus = request.getUserStatus();
            if (userStatus == UserStatus.UNRECOGNIZED) {
                return Mono.just(RequestResult.create(TurmsStatusCode.ILLEGAL_ARGUMENTS, "The user status must not be UNRECOGNIZED"));
            }
            Set<DeviceType> deviceTypes = request.getDeviceTypesCount() > 0 ? Sets.newHashSet(request.getDeviceTypesList()) : null;
            boolean updated;
            if (userStatus == UserStatus.OFFLINE) {
                if (deviceTypes != null) {
                    updated = onlineUserService.setLocalUserDevicesOffline(turmsRequestWrapper.getUserId(), deviceTypes, CloseStatusFactory.get(TurmsCloseStatus.DISCONNECTED_BY_OTHER_DEVICE));
                } else {
                    updated = onlineUserService.setLocalUserOffline(turmsRequestWrapper.getUserId(), CloseStatusFactory.get(TurmsCloseStatus.DISCONNECTED_BY_OTHER_DEVICE));
                }
            } else {
                updated = onlineUserService.getLocalOnlineUserManager(turmsRequestWrapper.getUserId()).setUserOnlineStatus(userStatus);
            }
            boolean notifyMembers = turmsClusterManager.getTurmsProperties().getNotification().isNotifyMembersAfterOtherMemberOnlineStatusUpdated();
            boolean notifyRelatedUser = turmsClusterManager.getTurmsProperties().getNotification().isNotifyRelatedUsersAfterOtherRelatedUserOnlineStatusUpdated();
            if (!notifyMembers && !notifyRelatedUser) {
                return Mono.just(RequestResult.okIfTrue(updated));
            } else {
                Mono<Set<Long>> queryMembersIds;
                Mono<Set<Long>> queryRelatedUsersIds;
                if (notifyMembers) {
                    queryMembersIds = groupMemberService.queryUsersJoinedGroupsMembersIds(
                            Set.of(turmsRequestWrapper.getUserId()));
                } else {
                    queryMembersIds = Mono.just(Collections.emptySet());
                }
                if (notifyRelatedUser) {
                    queryRelatedUsersIds = userRelationshipService.queryRelatedUsersIds(
                            Set.of(turmsRequestWrapper.getUserId()),
                            false)
                            .collect(Collectors.toSet());
                } else {
                    queryRelatedUsersIds = Mono.just(Collections.emptySet());
                }
                return queryMembersIds.zipWith(queryRelatedUsersIds)
                        .map(results -> {
                            results.getT1().addAll(results.getT2());
                            if (results.getT1().isEmpty()) {
                                return RequestResult.ok();
                            } else {
                                return RequestResult.create(
                                        results.getT1(),
                                        turmsRequestWrapper.getTurmsRequest());
                            }
                        });
            }
        };
    }

    @TurmsRequestMapping(UPDATE_USER_REQUEST)
    public Function<TurmsRequestWrapper, Mono<RequestResult>> handleUpdateUserRequest() {
        return turmsRequestWrapper -> {
            UpdateUserRequest request = turmsRequestWrapper.getTurmsRequest().getUpdateUserRequest();
            String password = request.hasPassword() ? request.getPassword().getValue() : null;
            String name = request.hasName() ? request.getName().getValue() : null;
            String intro = request.hasIntro() ? request.getIntro().getValue() : null;
            ProfileAccessStrategy profileAccessStrategy = request.getProfileAccessStrategy();
            return userService.updateUser(
                    turmsRequestWrapper.getUserId(),
                    password,
                    name,
                    intro,
                    profileAccessStrategy,
                    null,
                    null,
                    null)
                    .flatMap(updated -> {
                        if (updated != null && updated
                                && turmsClusterManager.getTurmsProperties().getNotification().isNotifyRelatedUsersAfterOtherRelatedUserInfoUpdated()) {
                            return userRelationshipService.queryRelatedUsersIds(Set.of(turmsRequestWrapper.getUserId()), false)
                                    .collect(Collectors.toSet())
                                    .map(relatedUsersIds -> {
                                        if (relatedUsersIds.isEmpty()) {
                                            return RequestResult.ok();
                                        } else {
                                            return RequestResult.create(
                                                    relatedUsersIds,
                                                    turmsRequestWrapper.getTurmsRequest());
                                        }
                                    });
                        }
                        return Mono.just(RequestResult.okIfTrue(updated));
                    });
        };
    }
}
