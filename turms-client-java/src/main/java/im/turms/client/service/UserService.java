package im.turms.client.service;

import im.turms.client.TurmsClient;
import im.turms.client.model.UserInfoWithVersion;
import im.turms.client.util.MapUtil;
import im.turms.client.util.NotificationUtil;
import im.turms.client.util.SystemUtil;
import im.turms.common.TurmsStatusCode;
import im.turms.common.constant.DeviceType;
import im.turms.common.constant.ProfileAccessStrategy;
import im.turms.common.constant.ResponseAction;
import im.turms.common.constant.UserStatus;
import im.turms.common.exception.TurmsBusinessException;
import im.turms.common.model.bo.common.Int64ValuesWithVersion;
import im.turms.common.model.bo.user.*;
import im.turms.common.model.dto.notification.TurmsNotification;
import im.turms.common.model.dto.request.user.*;
import im.turms.common.model.dto.request.user.relationship.*;
import im.turms.common.util.Validator;

import javax.annotation.Nullable;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public class UserService {
    private final TurmsClient turmsClient;
    private Long userId;
    private String password;
    private UserLocation location;
    private UserStatus userOnlineStatus;
    private DeviceType deviceType = SystemUtil.getDeviceType();

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
            return TurmsBusinessException.getFuture(TurmsStatusCode.ILLEGAL_ARGUMENTS, "password must not be null");
        }
        this.userId = userId;
        this.password = password;
        if (deviceType != null) {
            this.deviceType = deviceType;
        }
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
            return TurmsBusinessException.getFuture(TurmsStatusCode.CLIENT_USER_ID_AND_PASSWORD_MUST_NOT_NULL);
        }
    }

    public CompletableFuture<Void> logout() {
        return turmsClient.getDriver().disconnect();
    }

    public CompletableFuture<Void> updateUserOnlineStatus(@NotNull UserStatus onlineStatus) {
        if (onlineStatus == null) {
            return TurmsBusinessException.getFuture(TurmsStatusCode.ILLEGAL_ARGUMENTS, "onlineStatus must not be null");
        }
        if (onlineStatus == UserStatus.OFFLINE) {
            return TurmsBusinessException.getFuture(TurmsStatusCode.ILLEGAL_ARGUMENTS, "The online status must not be OFFLINE");
        }
        return turmsClient.getDriver()
                .send(UpdateUserOnlineStatusRequest.newBuilder(), MapUtil.of(
                        "user_status", onlineStatus))
                .thenApply(notification -> null);
    }

    public CompletableFuture<Void> disconnectOnlineDevices(@NotEmpty Set<DeviceType> deviceTypes) {
        if (deviceTypes == null || deviceTypes.isEmpty()) {
            return TurmsBusinessException.getFuture(TurmsStatusCode.ILLEGAL_ARGUMENTS, "deviceTypes must not be null or empty");
        }
        return turmsClient.getDriver()
                .send(UpdateUserOnlineStatusRequest.newBuilder(), MapUtil.of(
                        "user_status", UserStatus.OFFLINE,
                        "device_types", deviceTypes))
                .thenApply(notification -> null);
    }

    public CompletableFuture<Void> updatePassword(@NotNull String password) {
        if (password == null) {
            return TurmsBusinessException.getFuture(TurmsStatusCode.ILLEGAL_ARGUMENTS, "password must not be null");
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
                .send(QueryUsersIdsNearbyRequest.newBuilder(), MapUtil.of(
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
                .send(QueryUsersIdsNearbyRequest.newBuilder(), MapUtil.of(
                        "latitude", latitude,
                        "longitude", longitude,
                        "distance", distance,
                        "max_number", maxNumber))
                .thenApply(notification -> notification.getData().getUserSessionIds().getUserSessionIdsList());
    }

    public CompletableFuture<List<UserInfo>> queryUsersInfosNearby(
            float latitude,
            float longitude,
            @Nullable Integer distance,
            @Nullable Integer maxNumber) {
        return turmsClient.getDriver()
                .send(QueryUsersInfosNearbyRequest.newBuilder(), MapUtil.of(
                        "latitude", latitude,
                        "longitude", longitude,
                        "distance", distance,
                        "max_number", maxNumber))
                .thenApply(notification -> notification.getData().getUsersInfosWithVersion().getUserInfosList());
    }

    public CompletableFuture<List<UserStatusDetail>> queryUsersOnlineStatusRequest(@NotEmpty Set<Long> usersIds) {
        if (usersIds == null || usersIds.isEmpty()) {
            return TurmsBusinessException.getFuture(TurmsStatusCode.ILLEGAL_ARGUMENTS, "usersIds must not be null or empty");
        }
        return turmsClient.getDriver()
                .send(QueryUsersOnlineStatusRequest.newBuilder(), MapUtil.of(
                        "users_ids", usersIds))
                .thenApply(notification -> notification.getData().getUsersOnlineStatuses().getUserStatusesList());
    }

    // Relationship

    public CompletableFuture<UserRelationshipsWithVersion> queryRelationships(
            @Nullable Set<Long> relatedUsersIds,
            @Nullable Boolean isBlocked,
            @Nullable Integer groupIndex,
            @Nullable Date lastUpdatedDate) {
        return turmsClient.getDriver()
                .send(QueryRelationshipsRequest.newBuilder(), MapUtil.of(
                        "related_users_ids", relatedUsersIds,
                        "is_blocked", isBlocked,
                        "group_index", groupIndex,
                        "last_updated_date", lastUpdatedDate))
                .thenApply(notification -> {
                    TurmsNotification.Data data = notification.getData();
                    return data.hasUserRelationshipsWithVersion() ? data.getUserRelationshipsWithVersion() : null;
                });
    }

    public CompletableFuture<Int64ValuesWithVersion> queryRelatedUsersIds(
            @Nullable Boolean isBlocked,
            @Nullable Integer groupIndex,
            @Nullable Date lastUpdatedDate) {
        return turmsClient.getDriver()
                .send(QueryRelatedUsersIdsRequest.newBuilder(), MapUtil.of(
                        "is_blocked", isBlocked,
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
                        "is_blocked", isBlocked,
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
                        "related_user_id", relatedUserId,
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
                        "related_user_id", relatedUserId,
                        "blocked", isBlocked,
                        "new_group_index", groupIndex))
                .thenApply(notification -> null);
    }

    public CompletableFuture<Long> sendFriendRequest(
            long recipientId,
            @NotNull String content) {
        if (content == null) {
            return TurmsBusinessException.getFuture(TurmsStatusCode.ILLEGAL_ARGUMENTS, "content must not be null");
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
            return TurmsBusinessException.getFuture(TurmsStatusCode.ILLEGAL_ARGUMENTS, "responseAction must not be null");
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
            return TurmsBusinessException.getFuture(TurmsStatusCode.ILLEGAL_ARGUMENTS, "name must not be null");
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
            return TurmsBusinessException.getFuture(TurmsStatusCode.ILLEGAL_ARGUMENTS, "newName must not be null");
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
                        "related_user_id", relatedUserId,
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
