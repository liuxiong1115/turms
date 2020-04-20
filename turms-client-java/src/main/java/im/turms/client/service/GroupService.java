package im.turms.client.service;

import im.turms.client.TurmsClient;
import im.turms.client.model.GroupWithVersion;
import im.turms.client.util.MapUtil;
import im.turms.client.util.NotificationUtil;
import im.turms.common.TurmsStatusCode;
import im.turms.common.constant.GroupMemberRole;
import im.turms.common.exception.TurmsBusinessException;
import im.turms.common.model.bo.common.Int64ValuesWithVersion;
import im.turms.common.model.bo.group.*;
import im.turms.common.model.bo.user.UsersInfosWithVersion;
import im.turms.common.model.dto.notification.TurmsNotification;
import im.turms.common.model.dto.request.group.*;
import im.turms.common.model.dto.request.group.blacklist.CreateGroupBlacklistedUserRequest;
import im.turms.common.model.dto.request.group.blacklist.DeleteGroupBlacklistedUserRequest;
import im.turms.common.model.dto.request.group.blacklist.QueryGroupBlacklistedUsersIdsRequest;
import im.turms.common.model.dto.request.group.blacklist.QueryGroupBlacklistedUsersInfosRequest;
import im.turms.common.model.dto.request.group.enrollment.*;
import im.turms.common.model.dto.request.group.member.CreateGroupMemberRequest;
import im.turms.common.model.dto.request.group.member.DeleteGroupMemberRequest;
import im.turms.common.model.dto.request.group.member.QueryGroupMembersRequest;
import im.turms.common.model.dto.request.group.member.UpdateGroupMemberRequest;
import im.turms.common.util.Validator;

import javax.annotation.Nullable;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class GroupService {
    private final TurmsClient turmsClient;

    public GroupService(TurmsClient turmsClient) {
        this.turmsClient = turmsClient;
    }

    public CompletableFuture<Long> createGroup(
            @NotNull String name,
            @Nullable String intro,
            @Nullable String announcement,
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
                        "group_type_id", groupTypeId))
                .thenApply(NotificationUtil::getFirstId);
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
            @Nullable Integer minimumScore,
            @Nullable Long groupTypeId,
            @Nullable Date muteEndDate,
            @Nullable Long successorId,
            @Nullable Boolean quitAfterTransfer) {
        if (Validator.areAllFalsy(groupName, intro, announcement, minimumScore, groupTypeId,
                muteEndDate, successorId)) {
            return CompletableFuture.completedFuture(null);
        }
        return turmsClient.getDriver()
                .send(UpdateGroupRequest.newBuilder(), MapUtil.of(
                        "group_id", groupId,
                        "group_name", groupName,
                        "intro", intro,
                        "announcement", announcement,
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
        return this.updateGroup(groupId, null, null, null, null, null, null, successorId, quitAfterTransfer);
    }

    public CompletableFuture<Void> muteGroup(long groupId, @NotNull Date muteEndDate) {
        Validator.throwIfAnyFalsy(muteEndDate);
        return updateGroup(groupId, null, null, null, null, null, muteEndDate, null, null);
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
                .thenApply(notification -> {
                    TurmsNotification.Data data = notification.getData();
                    return data.hasIdsWithVersion() ? data.getIdsWithVersion() : null;
                });
    }

    public CompletableFuture<GroupsWithVersion> queryJoinedGroupsInfos(@Nullable Date lastUpdatedDate) {
        return turmsClient.getDriver()
                .send(QueryJoinedGroupsInfosRequest.newBuilder(), MapUtil.of(
                        "last_updated_date", lastUpdatedDate))
                .thenApply(notification -> {
                    TurmsNotification.Data data = notification.getData();
                    return data.hasGroupsWithVersion() ? data.getGroupsWithVersion() : null;
                });
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
                .thenApply(NotificationUtil::getFirstId);
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
                .thenApply(notification -> {
                    TurmsNotification.Data data = notification.getData();
                    return data.hasIdsWithVersion() ? data.getIdsWithVersion() : null;
                });
    }

    public CompletableFuture<UsersInfosWithVersion> queryBlacklistedUsersInfos(
            long groupId,
            @Nullable Date lastUpdatedDate) {
        return turmsClient.getDriver()
                .send(QueryGroupBlacklistedUsersInfosRequest.newBuilder(), MapUtil.of(
                        "group_id", groupId,
                        "last_updated_date", lastUpdatedDate))
                .thenApply(notification -> {
                    TurmsNotification.Data data = notification.getData();
                    return data.hasUsersInfosWithVersion() ? data.getUsersInfosWithVersion() : null;
                });
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
                .thenApply(NotificationUtil::getFirstId);
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
                .thenApply(notification -> {
                    TurmsNotification.Data data = notification.getData();
                    return data.hasGroupInvitationsWithVersion() ? data.getGroupInvitationsWithVersion() : null;
                });
    }

    public CompletableFuture<GroupInvitationsWithVersion> queryInvitations(boolean areSentByMe, @Nullable Date lastUpdatedDate) {
        return turmsClient.getDriver()
                .send(QueryGroupInvitationsRequest.newBuilder(), MapUtil.of(
                        "are_sent_by_me", areSentByMe,
                        "last_updated_date", lastUpdatedDate))
                .thenApply(notification -> {
                    TurmsNotification.Data data = notification.getData();
                    return data.hasGroupInvitationsWithVersion() ? data.getGroupInvitationsWithVersion() : null;
                });
    }

    public CompletableFuture<Long> createJoinRequest(long groupId, @NotNull String content) {
        Validator.throwIfAnyFalsy(content);
        return turmsClient.getDriver()
                .send(CreateGroupJoinRequestRequest.newBuilder(), MapUtil.of(
                        "group_id", groupId,
                        "content", content))
                .thenApply(NotificationUtil::getFirstId);
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
                .thenApply(notification -> {
                    TurmsNotification.Data data = notification.getData();
                    return data.hasGroupJoinRequestsWithVersion() ? data.getGroupJoinRequestsWithVersion() : null;
                });
    }

    public CompletableFuture<GroupJoinRequestsWithVersion> querySentJoinRequests(@Nullable Date lastUpdatedDate) {
        return turmsClient.getDriver()
                .send(QueryGroupJoinRequestsRequest.newBuilder(), MapUtil.of(
                        "last_updated_date", lastUpdatedDate))
                .thenApply(notification -> {
                    TurmsNotification.Data data = notification.getData();
                    return data.hasGroupJoinRequestsWithVersion() ? data.getGroupJoinRequestsWithVersion() : null;
                });
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
                .thenApply(notification -> {
                    TurmsNotification.Data data = notification.getData();
                    return data.hasGroupJoinQuestionsWithVersion() ? data.getGroupJoinQuestionsWithVersion() : null;
                });
    }

    public CompletableFuture<GroupJoinQuestionsAnswerResult> answerGroupQuestions(@NotEmpty Map<Long, String> questionIdAndAnswerMap) {
        Validator.throwIfEmpty(questionIdAndAnswerMap);
        return turmsClient.getDriver()
                .send(CheckGroupJoinQuestionsAnswersRequest.newBuilder(), MapUtil.of(
                        "question_id_and_answer", questionIdAndAnswerMap))
                .thenApply(notification -> {
                    TurmsNotification.Data data = notification.getData();
                    if (data.hasGroupJoinQuestionAnswerResult()) {
                        return data.getGroupJoinQuestionAnswerResult();
                    } else {
                        throw TurmsBusinessException.get(TurmsStatusCode.MISSING_DATA);
                    }
                });
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
                .thenApply(notification -> {
                    TurmsNotification.Data data = notification.getData();
                    return data.hasGroupMembersWithVersion() ? data.getGroupMembersWithVersion() : null;
                });
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
                .thenApply(notification -> {
                    TurmsNotification.Data data = notification.getData();
                    return data.hasGroupMembersWithVersion() ? data.getGroupMembersWithVersion() : null;
                });
    }
}
