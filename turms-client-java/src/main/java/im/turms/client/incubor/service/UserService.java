package im.turms.client.incubor.service;

import im.turms.client.incubor.TurmsClient;
import im.turms.client.incubor.driver.TurmsDriver;
import im.turms.client.incubor.model.UserInfoWithVersion;
import im.turms.client.incubor.util.MapUtil;
import im.turms.client.incubor.util.NotificationUtil;
import im.turms.turms.common.TurmsStatusCode;
import im.turms.turms.common.Validator;
import im.turms.turms.constant.DeviceType;
import im.turms.turms.constant.ProfileAccessStrategy;
import im.turms.turms.constant.ResponseAction;
import im.turms.turms.constant.UserStatus;
import im.turms.turms.exception.TurmsBusinessException;
import im.turms.turms.pojo.bo.common.Int64ValuesWithVersion;
import im.turms.turms.pojo.bo.group.GroupInvitationsWithVersion;
import im.turms.turms.pojo.bo.user.*;
import im.turms.turms.pojo.request.user.*;
import im.turms.turms.pojo.request.user.relationship.*;
import lombok.Getter;

import javax.annotation.Nullable;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.net.http.WebSocket;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public class UserService {
    private TurmsClient turmsClient;
    @Getter
    private Long userId;
    @Getter
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
            @Nullable UserLocation location,
            @Nullable UserStatus userOnlineStatus,
            @Nullable DeviceType deviceType) {
        if (userOnlineStatus == null) {
            userOnlineStatus = UserStatus.AVAILABLE;
        }
        if (deviceType == null) {
            deviceType = DeviceType.UNKNOWN;
        }
        Validator.throwIfAnyFalsy(password);
        this.userId = userId;
        this.password = password;
        this.userOnlineStatus = userOnlineStatus;
        this.deviceType = deviceType;
        long requestId = (long) Math.ceil(Math.random() * Long.MAX_VALUE);
        TurmsDriver driver = turmsClient.getDriver();
        return driver.connect(userId, password, requestId, driver.getWebsocketUrl(), null, location, userOnlineStatus, deviceType)
                .thenApply(webSocket -> null);
    }

    public CompletableFuture<Void> relogin() {
        if (userId != null && password != null) {
            return this.login(userId, password, location, userOnlineStatus, deviceType);
        } else {
            return CompletableFuture.failedFuture(TurmsBusinessException.get(TurmsStatusCode.CLIENT_USER_ID_AND_PASSWORD_MUST_NOT_NULL));
        }
    }

    public CompletableFuture<WebSocket> logout() {
        return turmsClient.getDriver().disconnect();
    }

    public CompletableFuture<Void> updateUserOnlineStatus(UserStatus onlineStatus) {
        Validator.throwIfAnyFalsy(onlineStatus);
        if (onlineStatus == UserStatus.OFFLINE) {
            return CompletableFuture.failedFuture(TurmsBusinessException.get(TurmsStatusCode.ILLEGAL_ARGUMENTS));
        }
        return turmsClient.getDriver()
                .send(UpdateUserOnlineStatusRequest.newBuilder(), MapUtil.of(
                        "userStatus", onlineStatus))
                .thenApply(notification -> null);
    }

    public CompletableFuture<Void> updatePassword(@NotNull String password) {
        Validator.throwIfAnyFalsy(password);
        return turmsClient.getDriver()
                .send(UpdateUserRequest.newBuilder(), MapUtil.of(
                        password, password))
                .thenApply(notification -> null);
    }

    public CompletableFuture<Void> updateProfile(
            @Nullable String name,
            @Nullable String intro,
            @Nullable String profilePictureUrl,
            @Nullable ProfileAccessStrategy profileAccessStrategy) {
        Validator.throwIfAllFalsy(name, intro, profilePictureUrl, profileAccessStrategy);
        return turmsClient.getDriver()
                .send(UpdateUserRequest.newBuilder(), MapUtil.of(
                        "name", name,
                        "intro", intro,
                        "profilePictureUrl", profilePictureUrl,
                        "profileAccessStrategy", profileAccessStrategy))
                .thenApply(notification -> null);
    }

    public CompletableFuture<GroupInvitationsWithVersion> queryUserGroupInvitations(@Nullable Date lastUpdatedDate) {
        return turmsClient.getDriver()
                .send(QueryUserGroupInvitationsRequest.newBuilder(), MapUtil.of(
                        "lastUpdatedDate", lastUpdatedDate))
                .thenApply(notification -> notification.getData().getGroupInvitationsWithVersion());
    }

    public CompletableFuture<UserInfoWithVersion> queryUserProfile(
            long userId,
            @Nullable Date lastUpdatedDate) {
        Validator.throwIfAnyFalsy(userId);
        return turmsClient.getDriver()
                .send(QueryUserProfileRequest.newBuilder(), MapUtil.of(
                        "userId", userId,
                        "lastUpdatedDate", lastUpdatedDate))
                .thenApply(UserInfoWithVersion::from);
    }

    public CompletableFuture<List<Long>> queryUsersIdsNearby(
            float latitude,
            float longitude,
            @Nullable Integer distance,
            @Nullable Integer maxNumber) {
        return turmsClient.getDriver()
                .send(QueryUsersIdsNearbyRequest.newBuilder(), MapUtil.of(
                        "latitude", latitude,
                        "longitude", longitude,
                        "distance", distance,
                        "maxNumber", maxNumber))
                .thenApply(NotificationUtil::getIds);
    }

    public CompletableFuture<List<UserInfo>> queryUsersInfosNearby(
            float latitude,
            float longitude,
            @Nullable Integer distance,
            @Nullable Integer maxNumber) {
        Validator.throwIfAnyFalsy(latitude, longitude);
        return turmsClient.getDriver()
                .send(QueryUsersInfosNearbyRequest.newBuilder(), MapUtil.of(
                        "latitude", latitude,
                        "longitude", longitude,
                        "distance", distance,
                        "maxNumber", maxNumber))
                .thenApply(notification -> notification.getData().getUsersInfosWithVersion().getUserInfosList());
    }

    public CompletableFuture<List<UserStatusDetail>> queryUsersOnlineStatusRequest(@NotEmpty Set<Long> usersIds) {
        Validator.throwIfAnyFalsy(usersIds);
        return turmsClient.getDriver()
                .send(QueryUsersOnlineStatusRequest.newBuilder(), MapUtil.of(
                        "usersIds", usersIds))
                .thenApply(notification -> notification.getData().getUsersOnlineStatuses().getUserStatusesList());
    }

    // Relationship

    public CompletableFuture<UserRelationshipsWithVersion> queryRelationships(
            @Nullable List<Long> relatedUsersIds,
            @Nullable Boolean isRelatedUsers,
            @Nullable Boolean isBlocked,
            @Nullable Integer groupIndex,
            @Nullable Date lastUpdatedDate) {
        return turmsClient.getDriver()
                .send(QueryRelationshipsRequest.newBuilder(), MapUtil.of(
                        "relatedUsersIds", relatedUsersIds,
                        "isBlocked", isBlocked,
                        "groupIndex", groupIndex,
                        "lastUpdatedDate", lastUpdatedDate))
                .thenApply(notification -> notification.getData().getUserRelationshipsWithVersion());
    }

    public CompletableFuture<Int64ValuesWithVersion> queryRelatedUsersIds(
            @Nullable Boolean isBlocked,
            @Nullable Integer groupIndex,
            @Nullable Date lastUpdatedDate) {
        return turmsClient.getDriver()
                .send(QueryRelatedUsersIdsRequest.newBuilder(), MapUtil.of(
                        "isBlocked", isBlocked,
                        "groupIndex", groupIndex,
                        "lastUpdatedDate", lastUpdatedDate))
                .thenApply(notification -> notification.getData().getIdsWithVersion());
    }

    public CompletableFuture<UserRelationshipsWithVersion> queryFriends(
            @Nullable Integer groupIndex,
            @Nullable Date lastUpdatedDate) {
        return this.queryRelationships(null, true, false, groupIndex, lastUpdatedDate);
    }

    public CompletableFuture<UserRelationshipsWithVersion> queryBlacklistedUsers(
            @Nullable Integer groupIndex,
            @Nullable Date lastUpdatedDate) {
        return this.queryRelationships(null, true, true, groupIndex, lastUpdatedDate);
    }

    public CompletableFuture<Void> createRelationship(
            long userId,
            boolean isBlocked,
            @Nullable Integer groupIndex) {
        return turmsClient.getDriver()
                .send(CreateRelationshipRequest.newBuilder(), MapUtil.of(
                        "userId", userId,
                        "isBlocked", isBlocked,
                        "groupIndex", groupIndex))
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
                        "relatedUserId", relatedUserId,
                        "groupIndex", deleteGroupIndex,
                        "targetGroupIndex", targetGroupIndex))
                .thenApply(notification -> null);
    }

    public CompletableFuture<Void> updateRelationship(
            long relatedUserId,
            @Nullable Boolean isBlocked,
            @Nullable Integer groupIndex) {
        Validator.throwIfAllFalsy(isBlocked, groupIndex);
        return turmsClient.getDriver()
                .send(UpdateRelationshipRequest.newBuilder(), MapUtil.of(
                        "relatedUserId", relatedUserId,
                        "blocked", isBlocked,
                        "newGroupIndex", groupIndex))
                .thenApply(notification -> null);
    }

    public CompletableFuture<Void> sendFriendRequest(
            long recipientId,
            @NotNull String content) {
        Validator.throwIfAnyFalsy(content);
        return turmsClient.getDriver()
                .send(CreateFriendRequestRequest.newBuilder(), MapUtil.of(
                        "recipientId", recipientId,
                        "content", content))
                .thenApply(notification -> null);
    }

    public CompletableFuture<Void> replyFriendRequest(
            long requestId,
            @NotNull ResponseAction responseAction,
            @Nullable String reason) {
        Validator.throwIfAnyFalsy(responseAction);
        return turmsClient.getDriver()
                .send(UpdateFriendRequestRequest.newBuilder(), MapUtil.of(
                        "requestId", requestId,
                        "responseAction", responseAction,
                        "reason", reason))
                .thenApply(notification -> null);
    }

    public CompletableFuture<UserFriendRequestsWithVersion> queryFriendRequests(@Nullable Date lastUpdatedDate) {
        return turmsClient.getDriver()
                .send(QueryFriendRequestsRequest.newBuilder(), MapUtil.of(
                        "lastUpdatedDate", lastUpdatedDate))
                .thenApply(notification -> notification.getData().getUserFriendRequestsWithVersion());
    }

    public CompletableFuture<Void> createRelationshipGroup(@NotNull String name) {
        Validator.throwIfAnyFalsy(name);
        return turmsClient.getDriver()
                .send(CreateRelationshipGroupRequest.newBuilder(), MapUtil.of(
                        "name", name))
                .thenApply(notification -> null);
    }

    public CompletableFuture<Void> deleteRelationshipGroups(
            int groupIndex,
            @Nullable Integer targetGroupIndex) {
        return turmsClient.getDriver()
                .send(DeleteRelationshipGroupRequest.newBuilder(), MapUtil.of(
                        "groupIndex", groupIndex,
                        "targetGroupIndex", targetGroupIndex))
                .thenApply(notification -> null);
    }

    public CompletableFuture<Void> updateRelationshipGroup(
            int groupIndex,
            @NotNull String newName) {
        Validator.throwIfAnyFalsy(newName);
        return turmsClient.getDriver()
                .send(UpdateRelationshipGroupRequest.newBuilder(), MapUtil.of(
                        "groupIndex", groupIndex,
                        "newName", newName))
                .thenApply(notification -> null);
    }

    public CompletableFuture<UserRelationshipGroupsWithVersion> queryRelationshipGroups(@Nullable Date lastUpdatedDate) {
        return turmsClient.getDriver()
                .send(QueryRelationshipGroupsRequest.newBuilder(), MapUtil.of(
                        "lastUpdatedDate", lastUpdatedDate))
                .thenApply(notification -> notification.getData().getUserRelationshipGroupsWithVersion());
    }

    public CompletableFuture<Void> moveRelatedUserToGroup(
            long relatedUserId,
            int groupIndex) {
        return turmsClient.getDriver()
                .send(UpdateRelationshipRequest.newBuilder(), MapUtil.of(
                        "relatedUserId", relatedUserId,
                        "newGroupIndex", groupIndex))
                .thenApply(notification -> null);
    }

    /**
     * updateLocation() in UserService is different from sendMessage() with records of location in MessageService
     * updateLocation() in UserService sends the location of user to the server only.
     * sendMessage() with records of location sends user's location to both server and its recipients.
     */
    public CompletableFuture<Void> updateLocation(
            long latitude,
            long longitude,
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
}
