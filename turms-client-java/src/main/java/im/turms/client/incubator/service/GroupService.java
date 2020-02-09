package im.turms.client.incubator.service;

import im.turms.client.incubator.TurmsClient;
import im.turms.client.incubator.model.GroupWithVersion;
import im.turms.client.incubator.util.MapUtil;
import im.turms.turms.common.Validator;
import im.turms.turms.constant.GroupMemberRole;
import im.turms.turms.pojo.bo.common.Int64ValuesWithVersion;
import im.turms.turms.pojo.bo.group.*;
import im.turms.turms.pojo.bo.user.UsersInfosWithVersion;
import im.turms.turms.pojo.request.group.*;
import im.turms.turms.pojo.request.group.blacklist.CreateGroupBlacklistedUserRequest;
import im.turms.turms.pojo.request.group.blacklist.DeleteGroupBlacklistedUserRequest;
import im.turms.turms.pojo.request.group.blacklist.QueryGroupBlacklistedUsersIdsRequest;
import im.turms.turms.pojo.request.group.blacklist.QueryGroupBlacklistedUsersInfosRequest;
import im.turms.turms.pojo.request.group.enrollment.*;
import im.turms.turms.pojo.request.group.member.CreateGroupMemberRequest;
import im.turms.turms.pojo.request.group.member.DeleteGroupMemberRequest;
import im.turms.turms.pojo.request.group.member.QueryGroupMembersRequest;
import im.turms.turms.pojo.request.group.member.UpdateGroupMemberRequest;

import javax.annotation.Nullable;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class GroupService {
    private TurmsClient turmsClient;

    public GroupService(TurmsClient turmsClient) {
        this.turmsClient = turmsClient;
    }

    public CompletableFuture<Long> createGroup(
            @NotNull String name,
            @Nullable String intro,
            @Nullable String announcement,
            @Nullable String profilePictureUrl,
            @Nullable Integer minimumScore,
            @Nullable Date muteEndDate,
            @Nullable Long groupTypeId) {
        Validator.throwIfAnyFalsy(name);
        return this.turmsClient.getDriver()
                .send(CreateGroupRequest.newBuilder(), MapUtil.of(
                        "name", name,
                        "intro", intro,
                        "announcement", announcement,
                        "minimum_score", minimumScore,
                        "mute_end_date", muteEndDate,
                        "profile_picture_url", profilePictureUrl,
                        "group_type_id", groupTypeId))
                .thenApply(notification -> notification.getData().getIds().getValuesList().get(0));
    }

    public CompletableFuture<Void> deleteGroup(long groupId) {
        return turmsClient.getDriver()
                .send(DeleteGroupRequest.newBuilder(), MapUtil.of("group_id", groupId))
                .thenApply(turmsNotification -> null);
    }

    public CompletableFuture<Void> updateGroup(
            long groupId,
            @Nullable String groupName,
            @Nullable String intro,
            @Nullable String announcement,
            @Nullable String profilePictureUrl,
            @Nullable Integer minimumScore,
            @Nullable Long groupTypeId,
            @Nullable Date muteEndDate,
            @Nullable Long successorId,
            @Nullable Boolean quitAfterTransfer) {
        if (Validator.areAllFalsy(groupName, intro, announcement, profilePictureUrl, minimumScore, groupTypeId,
                muteEndDate, successorId)) {
            return CompletableFuture.completedFuture(null);
        }
        return turmsClient.getDriver()
                .send(UpdateGroupRequest.newBuilder(), MapUtil.of(
                        "group_id", groupId,
                        "group_name", groupName,
                        "intro", intro,
                        "announcement", announcement,
                        "profile_picture_url", profilePictureUrl,
                        "mute_end_date", muteEndDate,
                        "minimum_score", minimumScore,
                        "group_type_id", groupTypeId,
                        "successor_id", successorId,
                        "quit_after_transfer", quitAfterTransfer))
                .thenApply(turmsNotification -> null);
    }

    public CompletableFuture<Void> transferOwnership(long groupId, long successorId, Boolean quitAfterTransfer) {
        if (quitAfterTransfer == null) {
            quitAfterTransfer = false;
        }
        return this.updateGroup(groupId, null, null, null, null, null, null, null, successorId, quitAfterTransfer);
    }

    public CompletableFuture<Void> muteGroup(long groupId, @NotNull Date muteEndDate) {
        Validator.throwIfAnyFalsy(muteEndDate);
        return updateGroup(groupId, null, null, null, null, null, null, muteEndDate, null, null);
    }

    public CompletableFuture<Void> unmuteGroup(long groupId) {
        return this.muteGroup(groupId, new Date(0));
    }

    public CompletableFuture<GroupWithVersion> queryGroup(long groupId, @Nullable Date lastUpdatedDate) {
        return turmsClient.getDriver()
                .send(QueryGroupRequest.newBuilder(), MapUtil.of(
                        "group_id", groupId,
                        "last_updated_date", lastUpdatedDate))
                .thenApply(GroupWithVersion::from);
    }

    public CompletableFuture<Int64ValuesWithVersion> queryJoinedGroupsIds(@Nullable Date lastUpdatedDate) {
        return turmsClient.getDriver()
                .send(QueryJoinedGroupsIdsRequest.newBuilder(), MapUtil.of(
                        "last_updated_date", lastUpdatedDate))
                .thenApply(notification -> notification.getData().getIdsWithVersion());
    }

    public CompletableFuture<GroupsWithVersion> queryJoinedGroupsInfos(@Nullable Date lastUpdatedDate) {
        return turmsClient.getDriver()
                .send(QueryJoinedGroupsInfosRequest.newBuilder(), MapUtil.of(
                        "last_updated_date", lastUpdatedDate))
                .thenApply(notification -> notification.getData().getGroupsWithVersion());
    }

    public CompletableFuture<Long> addGroupJoinQuestion(
            long groupId,
            @NotNull String question,
            @NotEmpty List<String> answers,
            int score) {
        Validator.throwIfAnyFalsy(question, answers);
        return turmsClient.getDriver()
                .send(CreateGroupJoinQuestionRequest.newBuilder(), MapUtil.of(
                        "group_id", groupId,
                        "question", question,
                        "answers", answers,
                        "score", score))
                .thenApply(notification -> notification.getData().getIds().getValuesList().get(0));
    }

    public CompletableFuture<Void> deleteGroupJoinQuestion(long questionId) {
        return turmsClient.getDriver()
                .send(DeleteGroupJoinQuestionRequest.newBuilder(), MapUtil.of(
                        "question_id", questionId))
                .thenApply(notification -> null);
    }

    public CompletableFuture<Void> updateGroupJoinQuestion(
            long questionId,
            @Nullable String question,
            @Nullable List<String> answers,
            @Nullable Integer score) {
        if (Validator.areAllNull(question, answers, score)) {
            return CompletableFuture.completedFuture(null);
        }
        return turmsClient.getDriver()
                .send(UpdateGroupJoinQuestionRequest.newBuilder(), MapUtil.of(
                        "question_id", questionId,
                        "question", question,
                        "answers", answers,
                        "score", score))
                .thenApply(notification -> null);
    }

    // Group Blacklist
    public CompletableFuture<Void> blacklistUser(long groupId, long userId) {
        return turmsClient.getDriver()
                .send(CreateGroupBlacklistedUserRequest.newBuilder(), MapUtil.of(
                        "blacklisted_user_id", userId,
                        "group_id", groupId))
                .thenApply(notification -> null);
    }

    public CompletableFuture<Void> unblacklistUser(long groupId, long userId) {
        return turmsClient.getDriver()
                .send(DeleteGroupBlacklistedUserRequest.newBuilder(), MapUtil.of(
                        "group_id", groupId,
                        "unblacklisted_user_id", userId))
                .thenApply(notification -> null);
    }

    public CompletableFuture<Int64ValuesWithVersion> queryBlacklistedUsersIds(
            long groupId,
            @Nullable Date lastUpdatedDate) {
        return turmsClient.getDriver()
                .send(QueryGroupBlacklistedUsersIdsRequest.newBuilder(), MapUtil.of(
                        "group_id", groupId,
                        "last_updated_date", lastUpdatedDate))
                .thenApply(notification -> notification.getData().getIdsWithVersion());
    }

    public CompletableFuture<UsersInfosWithVersion> queryBlacklistedUsersInfos(
            long groupId,
            @Nullable Date lastUpdatedDate) {
        return turmsClient.getDriver()
                .send(QueryGroupBlacklistedUsersInfosRequest.newBuilder(), MapUtil.of(
                        "group_id", groupId,
                        "last_updated_date", lastUpdatedDate))
                .thenApply(notification -> notification.getData().getUsersInfosWithVersion());
    }

    // Group Enrollment
    public CompletableFuture<Long> createInvitation(
            long groupId,
            long inviteeId,
            @NotNull String content) {
        Validator.throwIfAnyFalsy(content);
        return turmsClient.getDriver()
                .send(CreateGroupInvitationRequest.newBuilder(), MapUtil.of(
                        "group_id", groupId,
                        "invitee_id", inviteeId,
                        "content", content))
                .thenApply(notification -> notification.getData().getIds().getValuesList().get(0));
    }

    public CompletableFuture<Void> deleteInvitation(long invitationId) {
        return turmsClient.getDriver()
                .send(DeleteGroupInvitationRequest.newBuilder(), MapUtil.of(
                        "invitation_id", invitationId))
                .thenApply(notification -> null);
    }

    public CompletableFuture<GroupInvitationsWithVersion> queryInvitations(long groupId, @Nullable Date lastUpdatedDate) {
        return turmsClient.getDriver()
                .send(QueryGroupInvitationsRequest.newBuilder(), MapUtil.of(
                        "group_id", groupId,
                        "last_updated_date", lastUpdatedDate))
                .thenApply(notification -> notification.getData().getGroupInvitationsWithVersion());
    }

    public CompletableFuture<Long> createJoinRequest(long groupId, @NotNull String content) {
        Validator.throwIfAnyFalsy(content);
        return turmsClient.getDriver()
                .send(CreateGroupJoinRequestRequest.newBuilder(), MapUtil.of(
                        "group_id", groupId,
                        "content", content))
                .thenApply(notification -> notification.getData().getIds().getValuesList().get(0));
    }

    public CompletableFuture<Void> deleteJoinRequest(long requestId) {
        return turmsClient.getDriver()
                .send(DeleteGroupJoinRequestRequest.newBuilder(), MapUtil.of(
                        "request_id", requestId))
                .thenApply(notification -> null);
    }

    public CompletableFuture<GroupJoinRequestsWithVersion> queryJoinRequests(
            long groupId, @Nullable Date lastUpdatedDate) {
        return turmsClient.getDriver()
                .send(QueryGroupJoinRequestsRequest.newBuilder(), MapUtil.of(
                        "group_id", groupId,
                        "last_updated_date", lastUpdatedDate))
                .thenApply(notification -> notification.getData().getGroupJoinRequestsWithVersion());
    }

    /**
     * Note: Only the owner and managers have the right to fetch questions with answers
     */
    public CompletableFuture<GroupJoinQuestionsWithVersion> queryGroupJoinQuestionsRequest(
            long groupId,
            boolean withAnswers,
            @Nullable Date lastUpdatedDate) {
        return turmsClient.getDriver()
                .send(QueryGroupJoinQuestionsRequest.newBuilder(), MapUtil.of(
                        "group_id", groupId,
                        "with_answers", withAnswers,
                        "last_updated_date", lastUpdatedDate))
                .thenApply(notification -> notification.getData().getGroupJoinQuestionsWithVersion());
    }

    public CompletableFuture<Boolean> answerGroupQuestions(@NotEmpty Map<Long, String> questionIdAndAnswerMap) {
        Validator.throwIfEmpty(questionIdAndAnswerMap);
        return turmsClient.getDriver()
                .send(CheckGroupJoinQuestionsAnswersRequest.newBuilder(), MapUtil.of(
                        "question_id_and_answer", questionIdAndAnswerMap))
                .thenApply(notification -> notification.getData().getSuccess().getValue());
    }

    // Group Member
    public CompletableFuture<Void> addGroupMember(
            long groupId,
            long userId,
            @Nullable String name,
            @Nullable GroupMemberRole role,
            @Nullable Date muteEndDate) {
        return turmsClient.getDriver()
                .send(CreateGroupMemberRequest.newBuilder(), MapUtil.of(
                        "group_id", groupId,
                        "user_id", userId,
                        "name", name,
                        "role", role,
                        "mute_end_date", muteEndDate))
                .thenApply(notification -> null);

    }


    public CompletableFuture<Void> quitGroup(
            long groupId,
            @Nullable Long successorId,
            @Nullable Boolean quitAfterTransfer) {
        return turmsClient.getDriver()
                .send(DeleteGroupMemberRequest.newBuilder(), MapUtil.of(
                        "group_id", groupId,
                        "group_member_id", turmsClient.getUserService().getUserId(),
                        "successor_id", successorId,
                        "quit_after_transfer", quitAfterTransfer))
                .thenApply(notification -> null);
    }

    public CompletableFuture<Void> removeGroupMember(long groupId, long memberId) {
        return turmsClient.getDriver()
                .send(DeleteGroupMemberRequest.newBuilder(), MapUtil.of(
                        "group_id", groupId,
                        "group_member_id", memberId))
                .thenApply(notification -> null);
    }

    public CompletableFuture<Void> updateGroupMemberInfo(
            long groupId,
            long memberId,
            @Nullable String name,
            @Nullable GroupMemberRole role,
            @Nullable Date muteEndDate) {
        if (Validator.areAllNull(name, role, muteEndDate)) {
            return CompletableFuture.completedFuture(null);
        }
        return turmsClient.getDriver()
                .send(UpdateGroupMemberRequest.newBuilder(), MapUtil.of(
                        "group_id", groupId,
                        "member_id", memberId,
                        "name", name,
                        "role", role,
                        "mute_end_date", muteEndDate))
                .thenApply(notification -> null);
    }

    public CompletableFuture<Void> muteGroupMember(
            long groupId,
            long memberId,
            @NotNull Date muteEndDate) {
        Validator.throwIfAnyFalsy(muteEndDate);
        return this.updateGroupMemberInfo(groupId, memberId, null, null, muteEndDate);
    }

    public CompletableFuture<Void> unmuteGroupMember(long groupId, long memberId) {
        return this.muteGroupMember(groupId, memberId, new Date(0));
    }

    public CompletableFuture<GroupMembersWithVersion> queryGroupMembers(
            long groupId,
            boolean withStatus,
            @Nullable Date lastUpdatedDate) {
        return turmsClient.getDriver()
                .send(QueryGroupMembersRequest.newBuilder(), MapUtil.of(
                        "group_id", groupId,
                        "last_updated_date", lastUpdatedDate,
                        "with_status", withStatus))
                .thenApply(notification -> notification.getData().getGroupMembersWithVersion());
    }

    public CompletableFuture<GroupMembersWithVersion> queryGroupMembersByMembersIds(
            long groupId,
            @NotEmpty List<Long> membersIds,
            boolean withStatus) {
        Validator.throwIfAnyFalsy(membersIds);
        return turmsClient.getDriver()
                .send(QueryGroupMembersRequest.newBuilder(), MapUtil.of(
                        "group_id", groupId,
                        "group_members_ids", membersIds,
                        "with_status", withStatus))
                .thenApply(notification -> notification.getData().getGroupMembersWithVersion());
    }
}
