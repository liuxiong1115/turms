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
import im.turms.client.constant.TurmsStatusCode;
import im.turms.client.model.GroupWithVersion;
import im.turms.common.constant.GroupMemberRole;
import im.turms.common.model.bo.common.Int64ValuesWithVersion;
import im.turms.common.model.bo.group.*;
import im.turms.common.model.bo.user.UsersInfosWithVersion;
import java8.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.*;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static helper.Constants.*;
import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class GroupServiceST {
    private static final long GROUP_MEMBER_ID = 3;
    private static final long GROUP_INVITATION_INVITEE = 4;
    private static final long GROUP_SUCCESSOR = 1;
    private static final long GROUP_BLACKLISTED_USER_ID = 5;
    private static TurmsClient turmsClient;
    private static Long groupId;
    private static Long groupJoinQuestionId;
    private static Long groupJoinRequestId;
    private static Long groupInvitationId;

    @BeforeAll
    static void setup() throws ExecutionException, InterruptedException, TimeoutException {
        turmsClient = new TurmsClient(WS_URL, STORAGE_SERVER_URL);
        CompletableFuture<Void> future = turmsClient.getDriver().connect(1, "123");
        future.get(5, TimeUnit.SECONDS);
    }

    @AfterAll
    static void tearDown() {
        if (turmsClient.getDriver().isConnected()) {
            turmsClient.getDriver().disconnect();
        }
    }

    // Constructor

    @Test
    @Order(ORDER_FIRST)
    void constructor_shouldReturnNotNullGroupServiceInstance() {
        assertNotNull(turmsClient.getGroupService());
    }

    // Create

    @Test
    @Order(ORDER_HIGHEST_PRIORITY)
    void createGroup_shouldReturnGroupId() throws ExecutionException, InterruptedException, TimeoutException {
        groupId = turmsClient.getGroupService().createGroup("name", "intro", "announcement", 10, null, null)
                .get(5, TimeUnit.SECONDS);
        assertNotNull(groupId);
    }

    @Test
    @Order(ORDER_HIGH_PRIORITY)
    void addGroupJoinQuestion_shouldReturnQuestionId() throws ExecutionException, InterruptedException {
        CompletableFuture<Long> future = turmsClient.getGroupService().addGroupJoinQuestion(groupId, "question", Arrays.asList("answer1", "answer2"), 10);
        groupJoinQuestionId = future.get();
        assertNotNull(groupJoinQuestionId);
    }

    @Test
    @Order(ORDER_HIGH_PRIORITY)
    void createJoinRequest_shouldReturnJoinRequestId() throws ExecutionException, InterruptedException {
        CompletableFuture<Long> future = turmsClient.getGroupService().createJoinRequest(groupId, "content");
        groupJoinRequestId = future.get();
        assertNotNull(groupJoinRequestId);
    }

    @Test
    @Order(ORDER_HIGH_PRIORITY)
    void addGroupMember_shouldSucceed() throws ExecutionException, InterruptedException {
        CompletableFuture<Void> future = turmsClient.getGroupService().addGroupMember(groupId, GROUP_MEMBER_ID, "name", GroupMemberRole.MEMBER, null);
        assertNull(future.get());
    }

    @Test
    @Order(ORDER_HIGH_PRIORITY)
    void blacklistUser_shouldSucceed() throws ExecutionException, InterruptedException, TimeoutException {
        Void result = turmsClient.getGroupService().blacklistUser(groupId, GROUP_BLACKLISTED_USER_ID)
                .get(5, TimeUnit.SECONDS);
        assertNull(result);
    }

    @Test
    @Order(ORDER_HIGH_PRIORITY)
    void createInvitation_shouldReturnInvitationId() throws ExecutionException, InterruptedException {
        CompletableFuture<Long> future = turmsClient.getGroupService().createInvitation(groupId, GROUP_INVITATION_INVITEE, "content");
        groupInvitationId = future.get();
        assertNotNull(groupInvitationId);
    }

    // Update

    @Test
    @Order(ORDER_MIDDLE_PRIORITY)
    void updateGroup_shouldSucceed() throws ExecutionException, InterruptedException, TimeoutException {
        Void result = turmsClient.getGroupService().updateGroup(groupId, "name", "intro", "announcement", 10, null, null, null, null)
                .get(5, TimeUnit.SECONDS);
        assertNull(result);
    }

    @Test
    @Order(ORDER_LAST - 1)
    void transferOwnership_shouldSucceed() throws ExecutionException, InterruptedException, TimeoutException {
        Void result = turmsClient.getGroupService().transferOwnership(groupId, GROUP_SUCCESSOR, true)
                .get(5, TimeUnit.SECONDS);
        assertNull(result);
    }

    @Test
    @Order(ORDER_MIDDLE_PRIORITY)
    void muteGroup_shouldSucceed() throws ExecutionException, InterruptedException, TimeoutException {
        Void result = turmsClient.getGroupService().muteGroup(groupId, new Date())
                .get(5, TimeUnit.SECONDS);
        assertNull(result);
    }

    @Test
    @Order(ORDER_MIDDLE_PRIORITY)
    void unmuteGroup_shouldSucceed() throws ExecutionException, InterruptedException, TimeoutException {
        Void result = turmsClient.getGroupService().unmuteGroup(groupId)
                .get(5, TimeUnit.SECONDS);
        assertNull(result);
    }

    @Test
    @Order(ORDER_MIDDLE_PRIORITY)
    void updateGroupJoinQuestion_shouldSucceed() throws ExecutionException, InterruptedException, TimeoutException {
        Void result = turmsClient.getGroupService().updateGroupJoinQuestion(groupId, "new-question", Arrays.asList("answer"), null)
                .get(5, TimeUnit.SECONDS);
        assertNull(result);
    }

    @Test
    @Order(ORDER_MIDDLE_PRIORITY)
    void updateGroupMemberInfo_shouldSucceed() throws ExecutionException, InterruptedException, TimeoutException {
        Void result = turmsClient.getGroupService().updateGroupMemberInfo(groupId, GROUP_MEMBER_ID, "myname", null, null)
                .get(5, TimeUnit.SECONDS);
        assertNull(result);
    }

    @Test
    @Order(ORDER_MIDDLE_PRIORITY)
    void muteGroupMember_shouldSucceed() throws ExecutionException, InterruptedException, TimeoutException {
        Void result = turmsClient.getGroupService().muteGroupMember(groupId, GROUP_MEMBER_ID, new Date(System.currentTimeMillis() + 100000))
                .get(5, TimeUnit.SECONDS);
        assertNull(result);
    }

    @Test
    @Order(ORDER_MIDDLE_PRIORITY)
    void unmuteGroupMember_shouldSucceed() throws ExecutionException, InterruptedException, TimeoutException {
        Void result = turmsClient.getGroupService().unmuteGroupMember(groupId, GROUP_MEMBER_ID)
                .get(5, TimeUnit.SECONDS);
        assertNull(result);
    }

    // Query

    @Test
    @Order(ORDER_MIDDLE_PRIORITY)
    void queryGroup_shouldReturnGroupWithVersion() throws ExecutionException, InterruptedException, TimeoutException {
        GroupWithVersion groupWithVersion = turmsClient.getGroupService().queryGroup(groupId, null)
                .get(5, TimeUnit.SECONDS);
        assertEquals(groupId, groupWithVersion.getGroup().getId().getValue());
    }

    @Test
    @Order(ORDER_MIDDLE_PRIORITY)
    void queryJoinedGroupIds_shouldEqualNewGroupId() throws ExecutionException, InterruptedException, TimeoutException {
        Int64ValuesWithVersion joinedGroupIdsWithVersion = turmsClient.getGroupService().queryJoinedGroupIds(null)
                .get(5, TimeUnit.SECONDS);
        assertTrue(joinedGroupIdsWithVersion.getValuesList().contains(groupId));
    }

    @Test
    @Order(ORDER_MIDDLE_PRIORITY)
    void queryJoinedGroupInfos_shouldEqualNewGroupId() throws ExecutionException, InterruptedException, TimeoutException {
        GroupsWithVersion groupWithVersion = turmsClient.getGroupService().queryJoinedGroupInfos(null)
                .get(5, TimeUnit.SECONDS);
        List<Group> groups = groupWithVersion.getGroupsList();
        Set<Long> groupIds = new HashSet<>(groups.size());
        for (Group group : groups) {
            groupIds.add(group.getId().getValue());
        }
        assertTrue(groupIds.contains(groupId));
    }

    @Test
    @Order(ORDER_MIDDLE_PRIORITY)
    void queryBlacklistedUserIds_shouldEqualBlacklistedUserId() throws ExecutionException, InterruptedException, TimeoutException {
        Int64ValuesWithVersion blacklistedUserIdsWithVersion = turmsClient.getGroupService().queryBlacklistedUserIds(groupId, null)
                .get(5, TimeUnit.SECONDS);
        assertEquals(GROUP_BLACKLISTED_USER_ID, blacklistedUserIdsWithVersion.getValues(0));
    }

    @Test
    @Order(ORDER_MIDDLE_PRIORITY)
    void queryBlacklistedUserInfos_shouldEqualBlacklistedUserId() throws ExecutionException, InterruptedException, TimeoutException {
        UsersInfosWithVersion usersInfosWithVersion = turmsClient.getGroupService().queryBlacklistedUserInfos(groupId, null)
                .get(5, TimeUnit.SECONDS);
        Assertions.assertEquals(GROUP_BLACKLISTED_USER_ID, usersInfosWithVersion.getUserInfos(0).getId().getValue());
    }

    @Test
    @Order(ORDER_MIDDLE_PRIORITY)
    void queryInvitations_shouldEqualNewInvitationId() throws ExecutionException, InterruptedException, TimeoutException {
        GroupInvitationsWithVersion groupInvitationsWithVersion = turmsClient.getGroupService().queryInvitations(groupId, null)
                .get(5, TimeUnit.SECONDS);
        Assertions.assertEquals(groupInvitationId, groupInvitationsWithVersion.getGroupInvitations(0).getId().getValue());
    }

    @Test
    @Order(ORDER_MIDDLE_PRIORITY)
    void queryJoinRequests_shouldEqualNewJoinRequestId() throws ExecutionException, InterruptedException, TimeoutException {
        GroupJoinRequestsWithVersion groupJoinRequestsWithVersion = turmsClient.getGroupService().queryJoinRequests(groupId, null)
                .get(5, TimeUnit.SECONDS);
        Assertions.assertEquals(groupJoinRequestId, groupJoinRequestsWithVersion.getGroupJoinRequests(0).getId().getValue());
    }

    @Test
    @Order(ORDER_MIDDLE_PRIORITY)
    void queryGroupJoinQuestionsRequest_shouldEqualNewGroupQuestionId() throws ExecutionException, InterruptedException, TimeoutException {
        GroupJoinQuestionsWithVersion groupJoinQuestionsWithVersion = turmsClient.getGroupService().queryGroupJoinQuestionsRequest(groupId, true, null)
                .get(5, TimeUnit.SECONDS);
        Assertions.assertEquals(groupJoinQuestionId, groupJoinQuestionsWithVersion.getGroupJoinQuestions(0).getId().getValue());
    }

    @Test
    @Order(ORDER_MIDDLE_PRIORITY)
    void queryGroupMembers_shouldEqualNewMemberId() throws ExecutionException, InterruptedException, TimeoutException {
        GroupMembersWithVersion groupMembersWithVersion = turmsClient.getGroupService().queryGroupMembers(groupId, true, null)
                .get(5, TimeUnit.SECONDS);
        Assertions.assertEquals(GROUP_MEMBER_ID, groupMembersWithVersion.getGroupMembers(1).getUserId().getValue());
    }

    @Test
    @Order(ORDER_MIDDLE_PRIORITY)
    void queryGroupMembersByMemberIds_shouldEqualNewMemberId() throws ExecutionException, InterruptedException, TimeoutException {
        GroupMembersWithVersion groupMembersWithVersion = turmsClient.getGroupService().queryGroupMembersByMemberIds(groupId, Collections.singletonList(GROUP_MEMBER_ID), true)
                .get(5, TimeUnit.SECONDS);
        Assertions.assertEquals(GROUP_MEMBER_ID, groupMembersWithVersion.getGroupMembers(0).getUserId().getValue());
    }

    @Test
    @Order(ORDER_MIDDLE_PRIORITY)
    void answerGroupQuestions_shouldReturnAnswerResult() throws InterruptedException, TimeoutException {
        HashMap<Long, String> map = new HashMap<>();
        map.put(groupJoinQuestionId, "answer");
        try {
            GroupJoinQuestionsAnswerResult answerResult = turmsClient.getGroupService()
                    .answerGroupQuestions(map)
                    .get(5, TimeUnit.SECONDS);
            boolean isCorrect = answerResult.getQuestionIdsList().contains(groupJoinQuestionId);
            assertTrue(isCorrect);
        } catch (ExecutionException e) {
            assertTrue(ExceptionUtil.isTurmsStatusCode(e, TurmsStatusCode.MEMBER_CANNOT_ANSWER_GROUP_QUESTION));
        }
    }

    // Delete

    @Test
    @Order(ORDER_LOW_PRIORITY)
    void removeGroupMember_shouldSucceed() throws ExecutionException, InterruptedException, TimeoutException {
        Void result = turmsClient.getGroupService().removeGroupMember(groupId, GROUP_MEMBER_ID)
                .get(5, TimeUnit.SECONDS);
        assertNull(result);
    }

    @Test
    @Order(ORDER_LOWEST_PRIORITY)
    void deleteGroupJoinQuestion_shouldSucceed() throws ExecutionException, InterruptedException, TimeoutException {
        Void result = turmsClient.getGroupService().deleteGroupJoinQuestion(groupJoinQuestionId)
                .get(5, TimeUnit.SECONDS);
        assertNull(result);
    }

    @Test
    @Order(ORDER_LOWEST_PRIORITY)
    void unblacklistUser_shouldSucceed() throws ExecutionException, InterruptedException, TimeoutException {
        Void result = turmsClient.getGroupService().unblacklistUser(groupId, GROUP_BLACKLISTED_USER_ID)
                .get(5, TimeUnit.SECONDS);
        assertNull(result);
    }

    @Test
    @Order(ORDER_LOWEST_PRIORITY)
    void deleteInvitation_shouldSucceedOrThrowDisabledFunction() throws InterruptedException, TimeoutException {
        boolean isSuccessful;
        try {
            turmsClient.getGroupService().deleteInvitation(groupInvitationId)
                    .get(5, TimeUnit.SECONDS);
            isSuccessful = true;
        } catch (ExecutionException e) {
            isSuccessful = ExceptionUtil.isTurmsStatusCode(e, TurmsStatusCode.DISABLED_FUNCTION);
        }
        assertTrue(isSuccessful);
    }

    @Test
    @Order(ORDER_LOWEST_PRIORITY)
    void deleteJoinRequest_shouldSucceedOrThrowDisabledFunction() throws InterruptedException, TimeoutException {
        boolean isSuccessful;
        try {
            turmsClient.getGroupService().deleteJoinRequest(groupJoinRequestId)
                    .get(5, TimeUnit.SECONDS);
            isSuccessful = true;
        } catch (ExecutionException e) {
            isSuccessful = ExceptionUtil.isTurmsStatusCode(e, TurmsStatusCode.DISABLED_FUNCTION);
        }
        assertTrue(isSuccessful);
    }

    @Test
    @Order(ORDER_LOWEST_PRIORITY)
    void quitGroup_shouldSucceed() throws ExecutionException, InterruptedException, TimeoutException {
        Void result = turmsClient.getGroupService().quitGroup(groupId, null, false)
                .get(5, TimeUnit.SECONDS);
        assertNull(result);
    }

    @Test
    @Order(ORDER_LAST)
    void deleteGroup_shouldSucceed() throws ExecutionException, InterruptedException, TimeoutException {
        CompletableFuture<Long> readyToDeleteGroup = turmsClient.getGroupService().createGroup("readyToDelete", null, null, null, null, null);
        Long readyToDeleteGroupId = readyToDeleteGroup.get();
        Void result = turmsClient.getGroupService().deleteGroup(readyToDeleteGroupId)
                .get(5, TimeUnit.SECONDS);
        assertNull(result);
    }
}
