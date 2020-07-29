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

import helper.ExceptionUtil;
import im.turms.client.TurmsClient;
import im.turms.client.model.UserInfoWithVersion;
import im.turms.common.constant.ResponseAction;
import im.turms.common.constant.UserStatus;
import im.turms.common.constant.statuscode.TurmsStatusCode;
import im.turms.common.model.bo.common.Int64ValuesWithVersion;
import im.turms.common.model.bo.user.*;
import org.junit.jupiter.api.*;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static helper.Constants.*;
import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class UserServiceST {
    private static final UserStatus userStatus = UserStatus.AWAY;
    private static TurmsClient turmsClient;
    private static Integer relationshipGroupIndex;

    @BeforeAll
    static void setup() {
        turmsClient = new TurmsClient(WS_URL, null, null, STORAGE_SERVER_URL);
    }

    @AfterAll
    static void tearDown() {
        if (turmsClient.getDriver().connected()) {
            turmsClient.getDriver().disconnect();
        }
    }

    // Constructor

    @Test
    @Order(ORDER_FIRST)
    void constructor_shouldReturnNotNullGroupServiceInstance() {
        assertNotNull(turmsClient.getUserService());
    }

    // Login

    @Test
    @Order(ORDER_HIGHEST_PRIORITY)
    void login_shouldSucceed() throws ExecutionException, InterruptedException, TimeoutException {
        Void result = turmsClient.getUserService().login(1, "123")
                .get(5, TimeUnit.SECONDS);
        assertNull(result);
    }

    @Test
    @Order(ORDER_HIGHEST_PRIORITY + 1)
    void relogin_shouldSucceed() throws ExecutionException, InterruptedException, TimeoutException {
        turmsClient.getUserService().logout()
                .get(5, TimeUnit.SECONDS);
        Void result = turmsClient.getUserService().relogin()
                .get(5, TimeUnit.SECONDS);
        assertNull(result);
    }

    // Logout

    @Test
    @Order(ORDER_LAST)
    void logout_shouldSucceed() throws InterruptedException, ExecutionException, TimeoutException {
        Void result = turmsClient.getUserService().logout()
                .get(5, TimeUnit.SECONDS);
        assertNull(result);
    }

    // Create

    @Test
    @Order(ORDER_HIGH_PRIORITY)
    void createRelationship_shouldSucceed() throws InterruptedException, TimeoutException {
        try {
            Void result = turmsClient.getUserService().createRelationship(10L, true, null)
                    .get(5, TimeUnit.SECONDS);
            assertNull(result);
        } catch (ExecutionException e) {
            assertTrue(ExceptionUtil.isTurmsStatusCode(e, TurmsStatusCode.RELATIONSHIP_HAS_ESTABLISHED));
        }
    }

    @Test
    @Order(ORDER_HIGH_PRIORITY)
    void createFriendRelationship_shouldSucceed() throws InterruptedException, TimeoutException {
        try {
            Void result = turmsClient.getUserService().createFriendRelationship(10L, null)
                    .get(5, TimeUnit.SECONDS);
            assertNull(result);
        } catch (ExecutionException e) {
            assertTrue(ExceptionUtil.isTurmsStatusCode(e, TurmsStatusCode.RELATIONSHIP_HAS_ESTABLISHED));
        }
    }

    @Test
    @Order(ORDER_HIGH_PRIORITY)
    void createBlacklistedUserRelationship_shouldSucceed() throws InterruptedException, TimeoutException {
        try {
            Void result = turmsClient.getUserService().createBlacklistedUserRelationship(10L, null)
                    .get(5, TimeUnit.SECONDS);
            assertNull(result);
        } catch (ExecutionException e) {
            assertTrue(ExceptionUtil.isTurmsStatusCode(e, TurmsStatusCode.RELATIONSHIP_HAS_ESTABLISHED));
        }
    }

    @Test
    @Order(ORDER_HIGH_PRIORITY)
    void sendFriendRequest_shouldReturnFriendRequestId() throws InterruptedException, TimeoutException {
        try {
            Long friendRequestId = turmsClient.getUserService().sendFriendRequest(11L, "content")
                    .get(5, TimeUnit.SECONDS);
            assertNotNull(friendRequestId);
        } catch (ExecutionException e) {
            assertTrue(ExceptionUtil.isTurmsStatusCode(e, TurmsStatusCode.FRIEND_REQUEST_HAS_EXISTED));
        }
    }

    @Test
    @Order(ORDER_HIGH_PRIORITY)
    void createRelationshipGroup_shouldReturnRelationshipGroupIndex() throws ExecutionException, InterruptedException, TimeoutException {
        relationshipGroupIndex = turmsClient.getUserService().createRelationshipGroup("newGroup")
                .get(5, TimeUnit.SECONDS);
        assertNotNull(relationshipGroupIndex);
    }

    // Update

    @Test
    @Order(ORDER_MIDDLE_PRIORITY)
    void updateUserOnlineStatus_shouldSucceed() throws ExecutionException, InterruptedException, TimeoutException {
        Void result = turmsClient.getUserService().updateUserOnlineStatus(userStatus)
                .get(5, TimeUnit.SECONDS);
        assertNull(result);
    }

    @Test
    @Order(ORDER_MIDDLE_PRIORITY)
    void updatePassword_shouldSucceed() throws ExecutionException, InterruptedException, TimeoutException {
        Void result = turmsClient.getUserService().updatePassword("123")
                .get(5, TimeUnit.SECONDS);
        assertNull(result);
    }

    @Test
    @Order(ORDER_MIDDLE_PRIORITY)
    void updateProfile_shouldSucceed() throws ExecutionException, InterruptedException, TimeoutException {
        Void result = turmsClient.getUserService().updateProfile("123", "123", null)
                .get(5, TimeUnit.SECONDS);
        assertNull(result);
    }

    @Test
    @Order(ORDER_MIDDLE_PRIORITY)
    void updateRelationship_shouldSucceed() throws ExecutionException, InterruptedException, TimeoutException {
        Void result = turmsClient.getUserService().updateRelationship(10L, null, 1)
                .get(5, TimeUnit.SECONDS);
        assertNull(result);
    }

    @Test
    @Order(ORDER_MIDDLE_PRIORITY)
    void replyFriendRequest_shouldSucceed() throws ExecutionException, InterruptedException, TimeoutException {
        Void result = turmsClient.getUserService().replyFriendRequest(10L, ResponseAction.ACCEPT, "reason")
                .get(5, TimeUnit.SECONDS);
        assertNull(result);
    }

    @Test
    @Order(ORDER_MIDDLE_PRIORITY)
    void updateRelationshipGroup_shouldSucceed() throws ExecutionException, InterruptedException, TimeoutException {
        Void result = turmsClient.getUserService().updateRelationshipGroup(relationshipGroupIndex, "newGroupName")
                .get(5, TimeUnit.SECONDS);
        assertNull(result);
    }

    @Test
    @Order(ORDER_MIDDLE_PRIORITY)
    void moveRelatedUserToGroup_shouldSucceed() throws ExecutionException, InterruptedException, TimeoutException {
        Void result = turmsClient.getUserService().moveRelatedUserToGroup(2L, 1)
                .get(5, TimeUnit.SECONDS);
        assertNull(result);
        result = turmsClient.getUserService().moveRelatedUserToGroup(2L, 0)
                .get(5, TimeUnit.SECONDS);
        assertNull(result);
    }

    @Test
    @Order(ORDER_MIDDLE_PRIORITY)
    void updateLocation_shouldSucceed() throws ExecutionException, InterruptedException, TimeoutException {
        Void result = turmsClient.getUserService().updateLocation(2f, 2f, null, null)
                .get(5, TimeUnit.SECONDS);
        assertNull(result);
    }

    // Query

    @Test
    @Order(ORDER_LOW_PRIORITY)
    void queryUserProfile_shouldReturnUserInfoWithVersion() throws ExecutionException, InterruptedException, TimeoutException {
        UserInfoWithVersion result = turmsClient.getUserService().queryUserProfile(1, null)
                .get(5, TimeUnit.SECONDS);
        assertNotNull(result);
    }

    @Test
    @Order(ORDER_LOW_PRIORITY)
    void queryUsersNearby_shouldReturnUserIdsOrSessionIds() throws ExecutionException, InterruptedException, TimeoutException {
        List<Long> userIds = turmsClient.getUserService().queryUserIdsNearby(1f, 1f, null, null)
                .get(5, TimeUnit.SECONDS);
        List<UserSessionId> sessionIds = turmsClient.getUserService().queryUserSessionIdsNearby(1f, 1f, null, null)
                .get(5, TimeUnit.SECONDS);
        assertTrue(userIds != null || sessionIds != null);
    }

    @Test
    @Order(ORDER_LOW_PRIORITY)
    void queryUsersInfosNearby_shouldReturnUsersInfos() throws ExecutionException, InterruptedException, TimeoutException {
        List<UserInfo> result = turmsClient.getUserService().queryUsersInfosNearby(1f, 1f, null, null)
                .get(5, TimeUnit.SECONDS);
        assertNotNull(result);
    }

    @Test
    @Order(ORDER_LOW_PRIORITY)
    void queryUsersOnlineStatusRequest_shouldUsersOnlineStatus() throws ExecutionException, InterruptedException, TimeoutException {
        Set<Long> set = new HashSet<>();
        set.add(1L);
        List<UserStatusDetail> result = turmsClient.getUserService().queryUsersOnlineStatusRequest(set)
                .get(5, TimeUnit.SECONDS);
        assertEquals(userStatus, result.get(0).getUserStatus());
    }

    @Test
    @Order(ORDER_LOW_PRIORITY)
    void queryRelationships_shouldReturnUserRelationshipsWithVersion() throws ExecutionException, InterruptedException, TimeoutException {
        Set<Long> set = new HashSet<>();
        set.add(2L);
        UserRelationshipsWithVersion result = turmsClient.getUserService().queryRelationships(set, null, null, null)
                .get(5, TimeUnit.SECONDS);
        assertNotNull(result);
    }

    @Test
    @Order(ORDER_LOW_PRIORITY)
    void queryRelatedUsersIds_shouldReturnRelatedUsersIds() throws ExecutionException, InterruptedException, TimeoutException {
        Int64ValuesWithVersion result = turmsClient.getUserService().queryRelatedUsersIds(null, null, null)
                .get(5, TimeUnit.SECONDS);
        assertNotNull(result);
    }

    @Test
    @Order(ORDER_LOW_PRIORITY)
    void queryFriends_shouldReturnFriendRelationships() throws ExecutionException, InterruptedException, TimeoutException {
        UserRelationshipsWithVersion result = turmsClient.getUserService().queryFriends(null, null)
                .get(5, TimeUnit.SECONDS);
        assertNotNull(result);
    }

    @Test
    @Order(ORDER_LOW_PRIORITY)
    void queryBlacklistedUsers_shouldReturnBlacklist() throws ExecutionException, InterruptedException, TimeoutException {
        UserRelationshipsWithVersion result = turmsClient.getUserService().queryBlacklistedUsers(null, null)
                .get(5, TimeUnit.SECONDS);
        assertNotNull(result);
    }

    @Test
    @Order(ORDER_LOW_PRIORITY)
    void queryFriendRequests_shouldReturnFriendRequests() throws ExecutionException, InterruptedException, TimeoutException {
        UserFriendRequestsWithVersion result = turmsClient.getUserService().queryFriendRequests(true, null)
                .get(5, TimeUnit.SECONDS);
        assertNotNull(result);
    }

    @Test
    @Order(ORDER_LOW_PRIORITY)
    void queryRelationshipGroups_shouldReturnRelationshipGroups() throws ExecutionException, InterruptedException, TimeoutException {
        UserRelationshipGroupsWithVersion result = turmsClient.getUserService().queryRelationshipGroups(null)
                .get(5, TimeUnit.SECONDS);
        assertNotNull(result);
    }

    // Delete

    @Test
    @Order(ORDER_LOWEST_PRIORITY)
    void deleteRelationship_shouldSucceed() throws ExecutionException, InterruptedException, TimeoutException {
        Void result = turmsClient.getUserService().deleteRelationship(10L, null, null)
                .get(5, TimeUnit.SECONDS);
        assertNull(result);
    }

    @Test
    @Order(ORDER_LOWEST_PRIORITY)
    void deleteRelationshipGroups_shouldSucceed() throws ExecutionException, InterruptedException, TimeoutException {
        Void result = turmsClient.getUserService().deleteRelationshipGroups(relationshipGroupIndex, null)
                .get(5, TimeUnit.SECONDS);
        assertNull(result);
    }
}
