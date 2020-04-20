package im.turms.client.service;

import helper.ExceptionUtil;
import im.turms.client.TurmsClient;
import im.turms.client.model.GroupWithVersion;
import im.turms.common.TurmsStatusCode;
import im.turms.common.constant.GroupMemberRole;
import im.turms.common.model.bo.common.Int64ValuesWithVersion;
import im.turms.common.model.bo.group.*;
import im.turms.common.model.bo.user.UsersInfosWithVersion;
import org.junit.jupiter.api.*;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import static helper.Constants.*;
import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class GroupServiceST {
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
        turmsClient = new TurmsClient(WS_URL, null, null, STORAGE_SERVER_URL);
        CompletableFuture<Void> future = turmsClient.getDriver().connect(1, "123");
        future.get(5, TimeUnit.SECONDS);
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
    public void constructor_shouldReturnNotNullGroupServiceInstance() {
        assertNotNull(turmsClient.getGroupService());
    }

    // Create

    @Test
    @Order(ORDER_HIGHEST_PRIORITY)
    public void createGroup_shouldReturnGroupId() throws ExecutionException, InterruptedException, TimeoutException {
        groupId = turmsClient.getGroupService().createGroup("name", "intro", "announcement", 10, null, null)
                .get(5, TimeUnit.SECONDS);
        assertNotNull(groupId);
    }

    @Test
    @Order(ORDER_HIGH_PRIORITY)
    public void addGroupJoinQuestion_shouldReturnQuestionId() throws ExecutionException, InterruptedException {
        CompletableFuture<Long> future = turmsClient.getGroupService().addGroupJoinQuestion(groupId, "question", List.of("answer1", "answer2"), 10);
        groupJoinQuestionId = future.get();
        assertNotNull(groupJoinQuestionId);
    }

    @Test
    @Order(ORDER_HIGH_PRIORITY)
    public void createJoinRequest_shouldReturnJoinRequestId() throws ExecutionException, InterruptedException {
        CompletableFuture<Long> future = turmsClient.getGroupService().createJoinRequest(groupId, "content");
        groupJoinRequestId = future.get();
        assertNotNull(groupJoinRequestId);
    }

    @Test
    @Order(ORDER_HIGH_PRIORITY)
    public void addGroupMember_shouldSucceed() throws ExecutionException, InterruptedException {
        CompletableFuture<Void> future = turmsClient.getGroupService().addGroupMember(groupId, GROUP_MEMBER_ID, "name", GroupMemberRole.MEMBER, null);
        assertNull(future.get());
    }

    @Test
    @Order(ORDER_HIGH_PRIORITY)
    public void blacklistUser_shouldSucceed() throws ExecutionException, InterruptedException, TimeoutException {
        Void result = turmsClient.getGroupService().blacklistUser(groupId, GROUP_BLACKLISTED_USER_ID)
                .get(5, TimeUnit.SECONDS);
        assertNull(result);
    }

    @Test
    @Order(ORDER_HIGH_PRIORITY)
    public void createInvitation_shouldReturnInvitationId() throws ExecutionException, InterruptedException {
        CompletableFuture<Long> future = turmsClient.getGroupService().createInvitation(groupId, GROUP_INVITATION_INVITEE, "content");
        groupInvitationId = future.get();
        assertNotNull(groupInvitationId);
    }

    // Update

    @Test
    @Order(ORDER_MIDDLE_PRIORITY)
    public void updateGroup_shouldSucceed() throws ExecutionException, InterruptedException, TimeoutException {
        Void result = turmsClient.getGroupService().updateGroup(groupId, "name", "intro", "announcement", 10, null, null, null, null)
                .get(5, TimeUnit.SECONDS);
        assertNull(result);
    }

    @Test
    @Order(ORDER_LAST - 1)
    public void transferOwnership_shouldSucceed() throws ExecutionException, InterruptedException, TimeoutException {
        Void result = turmsClient.getGroupService().transferOwnership(groupId, GROUP_SUCCESSOR, true)
                .get(5, TimeUnit.SECONDS);
        assertNull(result);
    }

    @Test
    @Order(ORDER_MIDDLE_PRIORITY)
    public void muteGroup_shouldSucceed() throws ExecutionException, InterruptedException, TimeoutException {
        Void result = turmsClient.getGroupService().muteGroup(groupId, new Date())
                .get(5, TimeUnit.SECONDS);
        assertNull(result);
    }

    @Test
    @Order(ORDER_MIDDLE_PRIORITY)
    public void unmuteGroup_shouldSucceed() throws ExecutionException, InterruptedException, TimeoutException {
        Void result = turmsClient.getGroupService().unmuteGroup(groupId)
                .get(5, TimeUnit.SECONDS);
        assertNull(result);
    }

    @Test
    @Order(ORDER_MIDDLE_PRIORITY)
    public void updateGroupJoinQuestion_shouldSucceed() throws ExecutionException, InterruptedException, TimeoutException {
        Void result = turmsClient.getGroupService().updateGroupJoinQuestion(groupId, "new-question", List.of("answer"), null)
                .get(5, TimeUnit.SECONDS);
        assertNull(result);
    }

    @Test
    @Order(ORDER_MIDDLE_PRIORITY)
    public void updateGroupMemberInfo_shouldSucceed() throws ExecutionException, InterruptedException, TimeoutException {
        Void result = turmsClient.getGroupService().updateGroupMemberInfo(groupId, GROUP_MEMBER_ID, "myname", null, null)
                .get(5, TimeUnit.SECONDS);
        assertNull(result);
    }

    @Test
    @Order(ORDER_MIDDLE_PRIORITY)
    public void muteGroupMember_shouldSucceed() throws ExecutionException, InterruptedException, TimeoutException {
        Void result = turmsClient.getGroupService().muteGroupMember(groupId, GROUP_MEMBER_ID, new Date(System.currentTimeMillis() + 100000))
                .get(5, TimeUnit.SECONDS);
        assertNull(result);
    }

    @Test
    @Order(ORDER_MIDDLE_PRIORITY)
    public void unmuteGroupMember_shouldSucceed() throws ExecutionException, InterruptedException, TimeoutException {
        Void result = turmsClient.getGroupService().unmuteGroupMember(groupId, GROUP_MEMBER_ID)
                .get(5, TimeUnit.SECONDS);
        assertNull(result);
    }

    // Query

    @Test
    @Order(ORDER_MIDDLE_PRIORITY)
    public void queryGroup_shouldReturnGroupWithVersion() throws ExecutionException, InterruptedException, TimeoutException {
        GroupWithVersion groupWithVersion = turmsClient.getGroupService().queryGroup(groupId, null)
                .get(5, TimeUnit.SECONDS);
        assertEquals(groupId, groupWithVersion.getGroup().getId().getValue());
    }

    @Test
    @Order(ORDER_MIDDLE_PRIORITY)
    public void queryJoinedGroupsIds_shouldEqualNewGroupId() throws ExecutionException, InterruptedException, TimeoutException {
        Int64ValuesWithVersion joinedGroupsIdsWithVersion = turmsClient.getGroupService().queryJoinedGroupsIds(null)
                .get(5, TimeUnit.SECONDS);
        assertTrue(joinedGroupsIdsWithVersion.getValuesList().contains(groupId));
    }

    @Test
    @Order(ORDER_MIDDLE_PRIORITY)
    public void queryJoinedGroupsInfos_shouldEqualNewGroupId() throws ExecutionException, InterruptedException, TimeoutException {
        GroupsWithVersion groupWithVersion = turmsClient.getGroupService().queryJoinedGroupsInfos(null)
                .get(5, TimeUnit.SECONDS);
        Set<Long> groupIds = groupWithVersion.getGroupsList().stream()
                .map(group -> group.getId().getValue())
                .collect(Collectors.toSet());
        assertTrue(groupIds.contains(groupId));
    }

    @Test
    @Order(ORDER_MIDDLE_PRIORITY)
    public void queryBlacklistedUsersIds_shouldEqualBlacklistedUserId() throws ExecutionException, InterruptedException, TimeoutException {
        Int64ValuesWithVersion blacklistedUsersIdsWithVersion = turmsClient.getGroupService().queryBlacklistedUsersIds(groupId, null)
                .get(5, TimeUnit.SECONDS);
        assertEquals(GROUP_BLACKLISTED_USER_ID, blacklistedUsersIdsWithVersion.getValues(0));
    }

    @Test
    @Order(ORDER_MIDDLE_PRIORITY)
    public void queryBlacklistedUsersInfos_shouldEqualBlacklistedUserId() throws ExecutionException, InterruptedException, TimeoutException {
        UsersInfosWithVersion usersInfosWithVersion = turmsClient.getGroupService().queryBlacklistedUsersInfos(groupId, null)
                .get(5, TimeUnit.SECONDS);
        assertEquals(GROUP_BLACKLISTED_USER_ID, usersInfosWithVersion.getUserInfos(0).getId().getValue());
    }

    @Test
    @Order(ORDER_MIDDLE_PRIORITY)
    public void queryInvitations_shouldEqualNewInvitationId() throws ExecutionException, InterruptedException, TimeoutException {
        GroupInvitationsWithVersion groupInvitationsWithVersion = turmsClient.getGroupService().queryInvitations(groupId, null)
                .get(5, TimeUnit.SECONDS);
        assertEquals(groupInvitationId, groupInvitationsWithVersion.getGroupInvitations(0).getId().getValue());
    }

    @Test
    @Order(ORDER_MIDDLE_PRIORITY)
    public void queryJoinRequests_shouldEqualNewJoinRequestId() throws ExecutionException, InterruptedException, TimeoutException {
        GroupJoinRequestsWithVersion groupJoinRequestsWithVersion = turmsClient.getGroupService().queryJoinRequests(groupId, null)
                .get(5, TimeUnit.SECONDS);
        assertEquals(groupJoinRequestId, groupJoinRequestsWithVersion.getGroupJoinRequests(0).getId().getValue());
    }

    @Test
    @Order(ORDER_MIDDLE_PRIORITY)
    public void queryGroupJoinQuestionsRequest_shouldEqualNewGroupQuestionId() throws ExecutionException, InterruptedException, TimeoutException {
        GroupJoinQuestionsWithVersion groupJoinQuestionsWithVersion = turmsClient.getGroupService().queryGroupJoinQuestionsRequest(groupId, true, null)
                .get(5, TimeUnit.SECONDS);
        assertEquals(groupJoinQuestionId, groupJoinQuestionsWithVersion.getGroupJoinQuestions(0).getId().getValue());
    }

    @Test
    @Order(ORDER_MIDDLE_PRIORITY)
    public void queryGroupMembers_shouldEqualNewMemberId() throws ExecutionException, InterruptedException, TimeoutException {
        GroupMembersWithVersion groupMembersWithVersion = turmsClient.getGroupService().queryGroupMembers(groupId, true, null)
                .get(5, TimeUnit.SECONDS);
        assertEquals(GROUP_MEMBER_ID, groupMembersWithVersion.getGroupMembers(1).getUserId().getValue());
    }

    @Test
    @Order(ORDER_MIDDLE_PRIORITY)
    public void queryGroupMembersByMembersIds_shouldEqualNewMemberId() throws ExecutionException, InterruptedException, TimeoutException {
        GroupMembersWithVersion groupMembersWithVersion = turmsClient.getGroupService().queryGroupMembersByMembersIds(groupId, List.of(GROUP_MEMBER_ID), true)
                .get(5, TimeUnit.SECONDS);
        assertEquals(GROUP_MEMBER_ID, groupMembersWithVersion.getGroupMembers(0).getUserId().getValue());
    }

    @Test
    @Order(ORDER_MIDDLE_PRIORITY)
    public void answerGroupQuestions_shouldReturnAnswerResult() throws InterruptedException, TimeoutException {
        Map<Long, String> map = Map.of(groupJoinQuestionId, "answer");
        try {
            GroupJoinQuestionsAnswerResult answerResult = turmsClient.getGroupService()
                    .answerGroupQuestions(map)
                    .get(5, TimeUnit.SECONDS);
            boolean isCorrect = answerResult.getQuestionsIdsList().contains(groupJoinQuestionId);
            assertTrue(isCorrect);
        } catch (ExecutionException e) {
            assertTrue(ExceptionUtil.isTurmsStatusCode(e, TurmsStatusCode.ALREADY_GROUP_MEMBER));
        }
    }

    // Delete

    @Test
    @Order(ORDER_LOW_PRIORITY)
    public void removeGroupMember_shouldSucceed() throws ExecutionException, InterruptedException, TimeoutException {
        Void result = turmsClient.getGroupService().removeGroupMember(groupId, GROUP_MEMBER_ID)
                .get(5, TimeUnit.SECONDS);
        assertNull(result);
    }

    @Test
    @Order(ORDER_LOWEST_PRIORITY)
    public void deleteGroupJoinQuestion_shouldSucceed() throws ExecutionException, InterruptedException, TimeoutException {
        Void result = turmsClient.getGroupService().deleteGroupJoinQuestion(groupJoinQuestionId)
                .get(5, TimeUnit.SECONDS);
        assertNull(result);
    }

    @Test
    @Order(ORDER_LOWEST_PRIORITY)
    public void unblacklistUser_shouldSucceed() throws ExecutionException, InterruptedException, TimeoutException {
        Void result = turmsClient.getGroupService().unblacklistUser(groupId, GROUP_BLACKLISTED_USER_ID)
                .get(5, TimeUnit.SECONDS);
        assertNull(result);
    }

    @Test
    @Order(ORDER_LOWEST_PRIORITY)
    public void deleteInvitation_shouldSucceedOrThrowDisabledFunction() throws InterruptedException, TimeoutException {
        boolean isSuccess;
        try {
            turmsClient.getGroupService().deleteInvitation(groupInvitationId)
                    .get(5, TimeUnit.SECONDS);
            isSuccess = true;
        } catch (ExecutionException e) {
            isSuccess = ExceptionUtil.isTurmsStatusCode(e, TurmsStatusCode.DISABLED_FUNCTION);
        }
        assertTrue(isSuccess);
    }

    @Test
    @Order(ORDER_LOWEST_PRIORITY)
    public void deleteJoinRequest_shouldSucceedOrThrowDisabledFunction() throws InterruptedException, TimeoutException {
        boolean isSuccess;
        try {
            turmsClient.getGroupService().deleteJoinRequest(groupJoinRequestId)
                    .get(5, TimeUnit.SECONDS);
            isSuccess = true;
        } catch (ExecutionException e) {
            isSuccess = ExceptionUtil.isTurmsStatusCode(e, TurmsStatusCode.DISABLED_FUNCTION);
        }
        assertTrue(isSuccess);
    }

    @Test
    @Order(ORDER_LOWEST_PRIORITY)
    public void quitGroup_shouldSucceed() throws ExecutionException, InterruptedException, TimeoutException {
        Void result = turmsClient.getGroupService().quitGroup(groupId, null, false)
                .get(5, TimeUnit.SECONDS);
        assertNull(result);
    }

    @Test
    @Order(ORDER_LAST)
    public void deleteGroup_shouldSucceed() throws ExecutionException, InterruptedException, TimeoutException {
        CompletableFuture<Long> readyToDeleteGroup = turmsClient.getGroupService().createGroup("readyToDelete", null, null, null, null, null);
        Long readyToDeleteGroupId = readyToDeleteGroup.get();
        Void result = turmsClient.getGroupService().deleteGroup(readyToDeleteGroupId)
                .get(5, TimeUnit.SECONDS);
        assertNull(result);
    }
}
