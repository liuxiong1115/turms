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

package im.turms.client.service;

import im.turms.client.TurmsClient;
import im.turms.client.constant.TurmsStatusCode;
import im.turms.client.model.UserInfoWithVersion;
import im.turms.client.model.UserLocation;
import im.turms.client.util.MapUtil;
import im.turms.client.util.NotificationUtil;
import im.turms.client.util.SystemUtil;
import im.turms.client.util.TurmsBusinessExceptionUtil;
import im.turms.client.annotation.NotEmpty;
import im.turms.common.constant.DeviceType;
import im.turms.common.constant.ProfileAccessStrategy;
import im.turms.common.constant.ResponseAction;
import im.turms.common.constant.UserStatus;
import im.turms.common.model.bo.common.Int64ValuesWithVersion;
import im.turms.common.model.bo.user.*;
import im.turms.common.model.dto.notification.TurmsNotification;
import im.turms.common.model.dto.request.user.*;
import im.turms.common.model.dto.request.user.relationship.*;
import im.turms.common.util.Validator;
import java8.util.concurrent.CompletableFuture;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Date;
import java.util.List;
import java.util.Set;

/**
 * @author James Chen
 */
public class UserService {

    private final TurmsClient turmsClient;
    private Long userId;
    private String password;
    private UserLocation location;
    private UserStatus userOnlineStatus;
    private DeviceType deviceType;

    public UserService(TurmsClient turmsClient) {
        this.turmsClient = turmsClient;
    }

    public CompletableFuture<Void> login(
            long userId,
            @NotNull String password,
            @Nullable DeviceType deviceType,
            @Nullable UserStatus userOnlineStatus,
            @Nullable UserLocation location) {
        if (password == null) {
            return TurmsBusinessExceptionUtil.getFuture(TurmsStatusCode.ILLEGAL_ARGUMENTS, "password must not be null");
        }
        if (deviceType == null) {
            deviceType = SystemUtil.getDeviceType();
        }
        this.userId = userId;
        this.password = password;
        this.deviceType = deviceType;
        this.userOnlineStatus = userOnlineStatus != null ? userOnlineStatus : UserStatus.AVAILABLE;
        this.location = location;
        return turmsClient.getDriver().connect(userId, password, deviceType, userOnlineStatus, location)
                .thenApply(webSocket -> null);
    }

    public CompletableFuture<Void> login(long userId, @NotNull String password) {
        return login(userId, password, null, null, null);
    }

    public CompletableFuture<Void> relogin() {
        if (userId != null && password != null) {
            return this.login(userId, password, deviceType, userOnlineStatus, location);
        } else {
            return TurmsBusinessExceptionUtil.getFuture(TurmsStatusCode.ILLEGAL_ARGUMENTS, "The userId and password must not be null");
        }
    }

    public CompletableFuture<Void> logout() {
        return turmsClient.getDriver().disconnect();
    }

    public CompletableFuture<Void> updateUserOnlineStatus(@NotNull UserStatus onlineStatus) {
        if (onlineStatus == null) {
            return TurmsBusinessExceptionUtil.getFuture(TurmsStatusCode.ILLEGAL_ARGUMENTS, "onlineStatus must not be null");
        }
        if (onlineStatus == UserStatus.OFFLINE) {
            return TurmsBusinessExceptionUtil.getFuture(TurmsStatusCode.ILLEGAL_ARGUMENTS, "The online status must not be OFFLINE");
        }
        return turmsClient.getDriver()
                .send(UpdateUserOnlineStatusRequest.newBuilder(), MapUtil.of(
                        "user_status", onlineStatus))
                .thenApply(notification -> null);
    }

    public CompletableFuture<Void> disconnectOnlineDevices(@NotEmpty Set<DeviceType> deviceTypes) {
        if (deviceTypes == null || deviceTypes.isEmpty()) {
            return TurmsBusinessExceptionUtil.getFuture(TurmsStatusCode.ILLEGAL_ARGUMENTS, "deviceTypes must not be null or empty");
        }
        return turmsClient.getDriver()
                .send(UpdateUserOnlineStatusRequest.newBuilder(), MapUtil.of(
                        "user_status", UserStatus.OFFLINE,
                        "device_types", deviceTypes))
                .thenApply(notification -> null);
    }

    public CompletableFuture<Void> updatePassword(@NotNull String password) {
        if (password == null) {
            return TurmsBusinessExceptionUtil.getFuture(TurmsStatusCode.ILLEGAL_ARGUMENTS, "password must not be null");
        }
        return turmsClient.getDriver()
                .send(UpdateUserRequest.newBuilder(), MapUtil.of(
                        password, password))
                .thenApply(notification -> null);
    }

    public CompletableFuture<Void> updateProfile(
            @Nullable String name,
            @Nullable String intro,
            @Nullable ProfileAccessStrategy profileAccessStrategy) {
        if (Validator.areAllNull(name, intro, profileAccessStrategy)) {
            return CompletableFuture.completedFuture(null);
        }
        return turmsClient.getDriver()
                .send(UpdateUserRequest.newBuilder(), MapUtil.of(
                        "name", name,
                        "intro", intro,
                        "profile_access_strategy", profileAccessStrategy))
                .thenApply(notification -> null);
    }

    public CompletableFuture<UserInfoWithVersion> queryUserProfile(
            long userId,
            @Nullable Date lastUpdatedDate) {
        return turmsClient.getDriver()
                .send(QueryUserProfileRequest.newBuilder(), MapUtil.of(
                        "user_id", userId,
                        "last_updated_date", lastUpdatedDate))
                .thenApply(UserInfoWithVersion::from);
    }

    public CompletableFuture<List<Long>> queryUserIdsNearby(
            float latitude,
            float longitude,
            @Nullable Integer distance,
            @Nullable Integer maxNumber) {
        return turmsClient.getDriver()
                .send(QueryUserIdsNearbyRequest.newBuilder(), MapUtil.of(
                        "latitude", latitude,
                        "longitude", longitude,
                        "distance", distance,
                        "max_number", maxNumber))
                .thenApply(notification -> notification.getData().getIdsWithVersion().getValuesList());
    }

    public CompletableFuture<List<UserSessionId>> queryUserSessionIdsNearby(
            float latitude,
            float longitude,
            @Nullable Integer distance,
            @Nullable Integer maxNumber) {
        return turmsClient.getDriver()
                .send(QueryUserIdsNearbyRequest.newBuilder(), MapUtil.of(
                        "latitude", latitude,
                        "longitude", longitude,
                        "distance", distance,
                        "max_number", maxNumber))
                .thenApply(notification -> notification.getData().getUserSessionIds().getUserSessionIdsList());
    }

    public CompletableFuture<List<UserInfo>> queryUserInfosNearby(
            float latitude,
            float longitude,
            @Nullable Integer distance,
            @Nullable Integer maxNumber) {
        return turmsClient.getDriver()
                .send(QueryUserInfosNearbyRequest.newBuilder(), MapUtil.of(
                        "latitude", latitude,
                        "longitude", longitude,
                        "distance", distance,
                        "max_number", maxNumber))
                .thenApply(notification -> notification.getData().getUsersInfosWithVersion().getUserInfosList());
    }

    public CompletableFuture<List<UserStatusDetail>> queryUserOnlineStatusesRequest(@NotEmpty Set<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return TurmsBusinessExceptionUtil.getFuture(TurmsStatusCode.ILLEGAL_ARGUMENTS, "userIds must not be null or empty");
        }
        return turmsClient.getDriver()
                .send(QueryUserOnlineStatusesRequest.newBuilder(), MapUtil.of(
                        "user_ids", userIds))
                .thenApply(notification -> notification.getData().getUsersOnlineStatuses().getUserStatusesList());
    }

    // Relationship

    public CompletableFuture<UserRelationshipsWithVersion> queryRelationships(
            @Nullable Set<Long> relatedUserIds,
            @Nullable Boolean isBlocked,
            @Nullable Integer groupIndex,
            @Nullable Date lastUpdatedDate) {
        return turmsClient.getDriver()
                .send(QueryRelationshipsRequest.newBuilder(), MapUtil.of(
                        "user_ids", relatedUserIds,
                        "blocked", isBlocked,
                        "group_index", groupIndex,
                        "last_updated_date", lastUpdatedDate))
                .thenApply(notification -> {
                    TurmsNotification.Data data = notification.getData();
                    return data.hasUserRelationshipsWithVersion() ? data.getUserRelationshipsWithVersion() : null;
                });
    }

    public CompletableFuture<Int64ValuesWithVersion> queryRelatedUserIds(
            @Nullable Boolean isBlocked,
            @Nullable Integer groupIndex,
            @Nullable Date lastUpdatedDate) {
        return turmsClient.getDriver()
                .send(QueryRelatedUserIdsRequest.newBuilder(), MapUtil.of(
                        "blocked", isBlocked,
                        "group_index", groupIndex,
                        "last_updated_date", lastUpdatedDate))
                .thenApply(notification -> {
                    TurmsNotification.Data data = notification.getData();
                    return data.hasIdsWithVersion() ? data.getIdsWithVersion() : null;
                });
    }

    public CompletableFuture<UserRelationshipsWithVersion> queryFriends(
            @Nullable Integer groupIndex,
            @Nullable Date lastUpdatedDate) {
        return this.queryRelationships(null, false, groupIndex, lastUpdatedDate);
    }

    public CompletableFuture<UserRelationshipsWithVersion> queryBlacklistedUsers(
            @Nullable Integer groupIndex,
            @Nullable Date lastUpdatedDate) {
        return this.queryRelationships(null, true, groupIndex, lastUpdatedDate);
    }

    public CompletableFuture<Void> createRelationship(
            long userId,
            boolean isBlocked,
            @Nullable Integer groupIndex) {
        return turmsClient.getDriver()
                .send(CreateRelationshipRequest.newBuilder(), MapUtil.of(
                        "user_id", userId,
                        "blocked", isBlocked,
                        "group_index", groupIndex))
                .thenApply(notification -> null);
    }

    public CompletableFuture<Void> createFriendRelationship(
            long userId,
            @Nullable Integer groupIndex) {
        return this.createRelationship(userId, false, groupIndex);
    }

    public CompletableFuture<Void> createBlacklistedUserRelationship(
            long userId,
            @Nullable Integer groupIndex) {
        return this.createRelationship(userId, true, groupIndex);
    }

    public CompletableFuture<Void> deleteRelationship(
            long relatedUserId,
            @Nullable Integer deleteGroupIndex,
            @Nullable Integer targetGroupIndex) {
        return turmsClient.getDriver()
                .send(DeleteRelationshipRequest.newBuilder(), MapUtil.of(
                        "user_id", relatedUserId,
                        "group_index", deleteGroupIndex,
                        "target_group_index", targetGroupIndex))
                .thenApply(notification -> null);
    }

    public CompletableFuture<Void> updateRelationship(
            long relatedUserId,
            @Nullable Boolean isBlocked,
            @Nullable Integer groupIndex) {
        if (Validator.areAllFalsy(isBlocked, groupIndex)) {
            return CompletableFuture.completedFuture(null);
        }
        return turmsClient.getDriver()
                .send(UpdateRelationshipRequest.newBuilder(), MapUtil.of(
                        "user_id", relatedUserId,
                        "blocked", isBlocked,
                        "new_group_index", groupIndex))
                .thenApply(notification -> null);
    }

    public CompletableFuture<Long> sendFriendRequest(
            long recipientId,
            @NotNull String content) {
        if (content == null) {
            return TurmsBusinessExceptionUtil.getFuture(TurmsStatusCode.ILLEGAL_ARGUMENTS, "content must not be null");
        }
        return turmsClient.getDriver()
                .send(CreateFriendRequestRequest.newBuilder(), MapUtil.of(
                        "recipient_id", recipientId,
                        "content", content))
                .thenApply(NotificationUtil::getFirstId);
    }

    public CompletableFuture<Void> replyFriendRequest(
            long requestId,
            @NotNull ResponseAction responseAction,
            @Nullable String reason) {
        if (responseAction == null) {
            return TurmsBusinessExceptionUtil.getFuture(TurmsStatusCode.ILLEGAL_ARGUMENTS, "responseAction must not be null");
        }
        return turmsClient.getDriver()
                .send(UpdateFriendRequestRequest.newBuilder(), MapUtil.of(
                        "request_id", requestId,
                        "response_action", responseAction,
                        "reason", reason))
                .thenApply(notification -> null);
    }

    public CompletableFuture<UserFriendRequestsWithVersion> queryFriendRequests(boolean areSentByMe, @Nullable Date lastUpdatedDate) {
        return turmsClient.getDriver()
                .send(QueryFriendRequestsRequest.newBuilder(), MapUtil.of(
                        "are_sent_by_me", areSentByMe,
                        "last_updated_date", lastUpdatedDate))
                .thenApply(notification -> {
                    TurmsNotification.Data data = notification.getData();
                    return data.hasUserFriendRequestsWithVersion() ? data.getUserFriendRequestsWithVersion() : null;
                });
    }

    public CompletableFuture<Integer> createRelationshipGroup(@NotNull String name) {
        if (name == null) {
            return TurmsBusinessExceptionUtil.getFuture(TurmsStatusCode.ILLEGAL_ARGUMENTS, "name must not be null");
        }
        return turmsClient.getDriver()
                .send(CreateRelationshipGroupRequest.newBuilder(), MapUtil.of(
                        "name", name))
                .thenApply(notification -> NotificationUtil.getFirstId(notification).intValue());
    }

    public CompletableFuture<Void> deleteRelationshipGroups(
            int groupIndex,
            @Nullable Integer targetGroupIndex) {
        return turmsClient.getDriver()
                .send(DeleteRelationshipGroupRequest.newBuilder(), MapUtil.of(
                        "group_index", groupIndex,
                        "target_group_index", targetGroupIndex))
                .thenApply(notification -> null);
    }

    public CompletableFuture<Void> updateRelationshipGroup(
            int groupIndex,
            @NotNull String newName) {
        if (newName == null) {
            return TurmsBusinessExceptionUtil.getFuture(TurmsStatusCode.ILLEGAL_ARGUMENTS, "newName must not be null");
        }
        return turmsClient.getDriver()
                .send(UpdateRelationshipGroupRequest.newBuilder(), MapUtil.of(
                        "group_index", groupIndex,
                        "new_name", newName))
                .thenApply(notification -> null);
    }

    public CompletableFuture<UserRelationshipGroupsWithVersion> queryRelationshipGroups(@Nullable Date lastUpdatedDate) {
        return turmsClient.getDriver()
                .send(QueryRelationshipGroupsRequest.newBuilder(), MapUtil.of(
                        "last_updated_date", lastUpdatedDate))
                .thenApply(notification -> {
                    TurmsNotification.Data data = notification.getData();
                    return data.hasUserRelationshipGroupsWithVersion() ? data.getUserRelationshipGroupsWithVersion() : null;
                });
    }

    public CompletableFuture<Void> moveRelatedUserToGroup(
            long relatedUserId,
            int groupIndex) {
        return turmsClient.getDriver()
                .send(UpdateRelationshipRequest.newBuilder(), MapUtil.of(
                        "user_id", relatedUserId,
                        "new_group_index", groupIndex))
                .thenApply(notification -> null);
    }

    /**
     * updateLocation() in UserService is different from sendMessage() with records of location in MessageService
     * updateLocation() in UserService sends the location of user to the server only.
     * sendMessage() with records of location sends user's location to both server and its recipients.
     */
    public CompletableFuture<Void> updateLocation(
            float latitude,
            float longitude,
            @Nullable String name,
            @Nullable String address) {
        return turmsClient.getDriver()
                .send(UpdateUserLocationRequest.newBuilder(), MapUtil.of(
                        "latitude", latitude,
                        "longitude", longitude,
                        "name", name,
                        "address", address))
                .thenApply(notification -> null);
    }

    public Long getUserId() {
        return userId;
    }

    public String getPassword() {
        return password;
    }

    public UserLocation getLocation() {
        return location;
    }

    public UserStatus getUserOnlineStatus() {
        return userOnlineStatus;
    }

    public DeviceType getDeviceType() {
        return deviceType;
    }
}
