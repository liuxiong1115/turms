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

import im.turms.common.TurmsStatusCode;
import im.turms.common.constant.GroupMemberRole;
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
import im.turms.turms.annotation.websocket.TurmsRequestMapping;
import im.turms.turms.manager.TurmsClusterManager;
import im.turms.turms.pojo.bo.GroupQuestionIdAndAnswer;
import im.turms.turms.pojo.bo.RequestResult;
import im.turms.turms.pojo.bo.TurmsRequestWrapper;
import im.turms.turms.service.group.*;
import org.springframework.stereotype.Controller;
import reactor.core.publisher.Mono;

import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static im.turms.common.model.dto.request.TurmsRequest.KindCase.*;

@Controller
public class WsGroupController {
    private final GroupService groupService;
    private final GroupBlacklistService groupBlacklistService;
    private final GroupQuestionService groupQuestionService;
    private final GroupInvitationService groupInvitationService;
    private final GroupJoinRequestService groupJoinRequestService;
    private final GroupMemberService groupMemberService;
    private final TurmsClusterManager turmsClusterManager;

    public WsGroupController(GroupService groupService, GroupBlacklistService groupBlacklistService, GroupQuestionService groupQuestionService, GroupInvitationService groupInvitationService, GroupJoinRequestService groupJoinRequestService, GroupMemberService groupMemberService, TurmsClusterManager turmsClusterManager) {
        this.groupService = groupService;
        this.groupBlacklistService = groupBlacklistService;
        this.groupQuestionService = groupQuestionService;
        this.groupInvitationService = groupInvitationService;
        this.groupJoinRequestService = groupJoinRequestService;
        this.groupMemberService = groupMemberService;
        this.turmsClusterManager = turmsClusterManager;
    }

    @TurmsRequestMapping(CREATE_GROUP_REQUEST)
    public Function<TurmsRequestWrapper, Mono<RequestResult>> handleCreateGroupRequest() {
        return turmsRequestWrapper -> {
            CreateGroupRequest request = turmsRequestWrapper.getTurmsRequest().getCreateGroupRequest();
            String intro = request.hasIntro() ? request.getIntro().getValue() : null;
            String announcement = request.hasAnnouncement() ? request.getAnnouncement().getValue() : null;
            Integer minimumScore = request.hasMinimumScore() ? request.getMinimumScore().getValue() : null;
            Long groupTypeId = request.hasGroupTypeId() ? request.getGroupTypeId().getValue() : null;
            Date muteEndDate = request.hasMuteEndDate() ? new Date(request.getMuteEndDate().getValue()) : null;
            return groupService.authAndCreateGroup(
                    turmsRequestWrapper.getUserId(),
                    turmsRequestWrapper.getUserId(),
                    request.getName(),
                    intro,
                    announcement,
                    minimumScore,
                    groupTypeId,
                    muteEndDate,
                    null,
                    null,
                    turmsClusterManager.getTurmsProperties().getGroup().isActivateGroupWhenCreated())
                    .map(group -> RequestResult.create(group.getId()));
        };
    }

    @TurmsRequestMapping(DELETE_GROUP_REQUEST)
    public Function<TurmsRequestWrapper, Mono<RequestResult>> handleDeleteGroupRequest() {
        return turmsRequestWrapper -> {
            DeleteGroupRequest request = turmsRequestWrapper.getTurmsRequest().getDeleteGroupRequest();
            return groupMemberService
                    .isOwner(turmsRequestWrapper.getUserId(), request.getGroupId())
                    .flatMap(authenticated -> {
                        if (authenticated == null || !authenticated) {
                            return Mono.just(RequestResult.create(TurmsStatusCode.UNAUTHORIZED));
                        }
                        if (!turmsClusterManager.getTurmsProperties().getNotification().isNotifyMembersAfterGroupDeleted()) {
                            return groupService.deleteGroupsAndGroupMembers(
                                    Set.of(request.getGroupId()),
                                    null)
                                    .map(RequestResult::okIfTrue);
                        }
                        return groupService.queryGroupMembersIds(request.getGroupId())
                                .collect(Collectors.toSet())
                                .flatMap(membersIds -> groupService.deleteGroupsAndGroupMembers(Set.of(request.getGroupId()), null)
                                        .map(deleted -> {
                                            if (deleted != null && deleted) {
                                                if (membersIds.isEmpty()) {
                                                    return RequestResult.ok();
                                                } else {
                                                    return RequestResult.create(
                                                            membersIds,
                                                            turmsRequestWrapper.getTurmsRequest());
                                                }
                                            } else {
                                                return RequestResult.fail();
                                            }
                                        }));
                    });
        };
    }

    @TurmsRequestMapping(QUERY_GROUP_REQUEST)
    public Function<TurmsRequestWrapper, Mono<RequestResult>> handleQueryGroupRequest() {
        return turmsRequestWrapper -> {
            QueryGroupRequest request = turmsRequestWrapper.getTurmsRequest().getQueryGroupRequest();
            Date lastUpdatedDate = request.hasLastUpdatedDate() ?
                    new Date(request.getLastUpdatedDate().getValue()) : null;
            return groupService.queryGroupWithVersion(request.getGroupId(), lastUpdatedDate)
                    .map(groupsWithVersion -> RequestResult.create(TurmsNotification.Data.newBuilder()
                            .setGroupsWithVersion(groupsWithVersion)
                            .build()));
        };
    }

    @TurmsRequestMapping(QUERY_JOINED_GROUPS_IDS_REQUEST)
    public Function<TurmsRequestWrapper, Mono<RequestResult>> handleQueryJoinedGroupsIdsRequest() {
        return turmsRequestWrapper -> {
            QueryJoinedGroupsIdsRequest request = turmsRequestWrapper.getTurmsRequest()
                    .getQueryJoinedGroupsIdsRequest();
            Date lastUpdatedDate = request.hasLastUpdatedDate() ? new Date(request.getLastUpdatedDate().getValue()) : null;
            return groupService.queryJoinedGroupsIdsWithVersion(
                    turmsRequestWrapper.getUserId(),
                    lastUpdatedDate)
                    .map(idsWithVersion -> RequestResult.create(TurmsNotification.Data
                            .newBuilder()
                            .setIdsWithVersion(idsWithVersion)
                            .build()));
        };
    }

    @TurmsRequestMapping(QUERY_JOINED_GROUPS_INFOS_REQUEST)
    public Function<TurmsRequestWrapper, Mono<RequestResult>> handleQueryJoinedGroupsRequest() {
        return turmsRequestWrapper -> {
            QueryJoinedGroupsInfosRequest request = turmsRequestWrapper.getTurmsRequest()
                    .getQueryJoinedGroupsInfosRequest();
            Date lastUpdatedDate = request.hasLastUpdatedDate() ? new Date(request.getLastUpdatedDate().getValue()) : null;
            return groupService.queryJoinedGroupsWithVersion(
                    turmsRequestWrapper.getUserId(),
                    lastUpdatedDate)
                    .map(groupsWithVersion -> RequestResult.create(TurmsNotification.Data
                            .newBuilder()
                            .setGroupsWithVersion(groupsWithVersion)
                            .build()));
        };
    }

    @TurmsRequestMapping(UPDATE_GROUP_REQUEST)
    public Function<TurmsRequestWrapper, Mono<RequestResult>> handleUpdateGroupRequest() {
        return turmsRequestWrapper -> {
            UpdateGroupRequest request = turmsRequestWrapper.getTurmsRequest().getUpdateGroupRequest();
            Integer minimumScore = request.hasMinimumScore() ? request.getMinimumScore().getValue() : null;
            Long groupTypeId = request.hasGroupTypeId() ? request.getGroupTypeId().getValue() : null;
            Long successorId = request.hasSuccessorId() ? request.getSuccessorId().getValue() : null;
            String groupName = request.hasGroupName() ? request.getGroupName().getValue() : null;
            String intro = request.hasIntro() ? request.getIntro().getValue() : null;
            String announcement = request.hasAnnouncement() ? request.getAnnouncement().getValue() : null;
            Date muteEndDate = request.hasMuteEndDate() ? new Date(request.getMuteEndDate().getValue()) : null;
            boolean quitAfterTransfer = request.hasQuitAfterTransfer() && request.getQuitAfterTransfer().getValue();
            return groupService.authAndUpdateGroup(
                    turmsRequestWrapper.getUserId(),
                    request.getGroupId(),
                    groupTypeId,
                    null,
                    null,
                    groupName,
                    intro,
                    announcement,
                    minimumScore,
                    null,
                    null,
                    null,
                    muteEndDate,
                    successorId,
                    quitAfterTransfer)
                    .flatMap(updated -> {
                        if (updated != null && updated) {
                            if (turmsClusterManager.getTurmsProperties().getNotification().isNotifyMembersAfterGroupUpdated()) {
                                return groupMemberService.queryGroupMembersIds(request.getGroupId())
                                        .collect(Collectors.toSet())
                                        .map(membersIds -> RequestResult.create(
                                                membersIds,
                                                turmsRequestWrapper.getTurmsRequest()));
                            } else {
                                return Mono.just(RequestResult.ok());
                            }
                        } else {
                            return Mono.just(RequestResult.fail());
                        }
                    });
        };
    }

    @TurmsRequestMapping(CREATE_GROUP_BLACKLISTED_USER_REQUEST)
    public Function<TurmsRequestWrapper, Mono<RequestResult>> handleCreateGroupBlacklistedUserRequest() {
        return turmsRequestWrapper -> {
            CreateGroupBlacklistedUserRequest request = turmsRequestWrapper.getTurmsRequest()
                    .getCreateGroupBlacklistedUserRequest();
            return groupBlacklistService.blacklistUser(
                    turmsRequestWrapper.getUserId(),
                    request.getGroupId(),
                    request.getBlacklistedUserId(),
                    null)
                    .map(success -> {
                        if (success != null && success
                                && turmsClusterManager.getTurmsProperties().getNotification().isNotifyUserAfterBlacklistedByGroup()) {
                            return RequestResult.create(
                                    request.getBlacklistedUserId(),
                                    turmsRequestWrapper.getTurmsRequest());
                        }
                        return RequestResult.okIfTrue(success);
                    });
        };
    }

    @TurmsRequestMapping(DELETE_GROUP_BLACKLISTED_USER_REQUEST)
    public Function<TurmsRequestWrapper, Mono<RequestResult>> handleDeleteGroupBlacklistedUserRequest() {
        return turmsRequestWrapper -> {
            DeleteGroupBlacklistedUserRequest request = turmsRequestWrapper.getTurmsRequest()
                    .getDeleteGroupBlacklistedUserRequest();
            return groupBlacklistService.unblacklistUser(
                    turmsRequestWrapper.getUserId(),
                    request.getGroupId(),
                    request.getUnblacklistedUserId(),
                    null,
                    true)
                    .map(success -> {
                        if (success != null && success
                                && turmsClusterManager.getTurmsProperties().getNotification().isNotifyUserAfterUnblacklistedByGroup()) {
                            return RequestResult.create(
                                    request.getUnblacklistedUserId(),
                                    turmsRequestWrapper.getTurmsRequest());
                        }
                        return RequestResult.okIfTrue(success);
                    });
        };
    }

    @TurmsRequestMapping(QUERY_GROUP_BLACKLISTED_USERS_IDS_REQUEST)
    public Function<TurmsRequestWrapper, Mono<RequestResult>> handleQueryGroupBlacklistedUsersIdsRequest() {
        return turmsRequestWrapper -> {
            QueryGroupBlacklistedUsersIdsRequest request = turmsRequestWrapper.getTurmsRequest()
                    .getQueryGroupBlacklistedUsersIdsRequest();
            Date lastUpdatedDate = request.hasLastUpdatedDate() ?
                    new Date(request.getLastUpdatedDate().getValue()) : null;
            return groupBlacklistService.queryGroupBlacklistedUsersIdsWithVersion(
                    request.getGroupId(),
                    lastUpdatedDate)
                    .map(version -> RequestResult.create(TurmsNotification.Data
                            .newBuilder()
                            .setIdsWithVersion(version)
                            .build()));
        };
    }

    @TurmsRequestMapping(QUERY_GROUP_BLACKLISTED_USERS_INFOS_REQUEST)
    public Function<TurmsRequestWrapper, Mono<RequestResult>> handleQueryGroupBlacklistedUsersInfosRequest() {
        return turmsRequestWrapper -> {
            QueryGroupBlacklistedUsersInfosRequest request = turmsRequestWrapper.getTurmsRequest()
                    .getQueryGroupBlacklistedUsersInfosRequest();
            Date lastUpdatedDate = request.hasLastUpdatedDate() ? new Date(request.getLastUpdatedDate().getValue()) : null;
            return groupBlacklistService.queryGroupBlacklistedUsersInfosWithVersion(
                    request.getGroupId(),
                    lastUpdatedDate)
                    .map(version -> RequestResult.create(TurmsNotification.Data
                            .newBuilder()
                            .setUsersInfosWithVersion(version)
                            .build()));
        };
    }

    @TurmsRequestMapping(CHECK_GROUP_JOIN_QUESTIONS_ANSWERS_REQUEST)
    public Function<TurmsRequestWrapper, Mono<RequestResult>> handleCheckGroupQuestionAnswerRequest() {
        return turmsRequestWrapper -> {
            CheckGroupJoinQuestionsAnswersRequest request = turmsRequestWrapper.getTurmsRequest()
                    .getCheckGroupJoinQuestionsAnswersRequest();
            HashSet<GroupQuestionIdAndAnswer> set = new HashSet<>(request.getQuestionIdAndAnswerCount());
            for (Map.Entry<Long, String> entry : request.getQuestionIdAndAnswerMap().entrySet()) {
                set.add(new GroupQuestionIdAndAnswer(entry.getKey(), entry.getValue()));
            }
            return groupQuestionService.checkGroupQuestionAnswerAndJoin(turmsRequestWrapper.getUserId(), set)
                    .map(answerResult -> RequestResult.create(TurmsNotification.Data.newBuilder()
                            .setGroupJoinQuestionAnswerResult(answerResult).build()));
        };
    }

    @TurmsRequestMapping(CREATE_GROUP_INVITATION_REQUEST)
    public Function<TurmsRequestWrapper, Mono<RequestResult>> handleCreateGroupInvitationRequestRequest() {
        return turmsRequestWrapper -> {
            CreateGroupInvitationRequest request = turmsRequestWrapper.getTurmsRequest()
                    .getCreateGroupInvitationRequest();
            return groupInvitationService.authAndCreateGroupInvitation(
                    request.getGroupId(),
                    turmsRequestWrapper.getUserId(),
                    request.getInviteeId(),
                    request.getContent())
                    .map(invitation -> {
                        if (turmsClusterManager.getTurmsProperties().getNotification().isNotifyUserAfterInvitedByGroup()) {
                            return RequestResult.create(
                                    invitation.getId(),
                                    request.getInviteeId(),
                                    turmsRequestWrapper.getTurmsRequest());
                        }
                        return RequestResult.ok();
                    });
        };
    }

    @TurmsRequestMapping(CREATE_GROUP_JOIN_REQUEST_REQUEST)
    public Function<TurmsRequestWrapper, Mono<RequestResult>> handleCreateGroupJoinRequestRequest() {
        return turmsRequestWrapper -> {
            CreateGroupJoinRequestRequest request = turmsRequestWrapper.getTurmsRequest()
                    .getCreateGroupJoinRequestRequest();
            return groupJoinRequestService.authAndCreateGroupJoinRequest(
                    turmsRequestWrapper.getUserId(),
                    request.getGroupId(),
                    request.getContent())
                    .flatMap(joinRequest -> {
                        if (turmsClusterManager.getTurmsProperties().getNotification().isNotifyOwnerAndManagersAfterReceivingJoinRequest()) {
                            return groupMemberService.queryGroupManagersAndOwnerId(request.getGroupId())
                                    .collect(Collectors.toSet())
                                    .map(recipientsIds -> {
                                        if (recipientsIds.isEmpty()) {
                                            return RequestResult.ok();
                                        } else {
                                            return RequestResult.create(
                                                    joinRequest.getId(),
                                                    recipientsIds,
                                                    false,
                                                    turmsRequestWrapper.getTurmsRequest());
                                        }
                                    });
                        }
                        return Mono.just(RequestResult.ok());
                    });
        };
    }

    @TurmsRequestMapping(CREATE_GROUP_JOIN_QUESTION_REQUEST)
    public Function<TurmsRequestWrapper, Mono<RequestResult>> handleCreateGroupQuestionRequest() {
        return turmsRequestWrapper -> {
            CreateGroupJoinQuestionRequest request = turmsRequestWrapper.getTurmsRequest()
                    .getCreateGroupJoinQuestionRequest();
            if (request.getAnswersCount() == 0) {
                return Mono.just(RequestResult.fail());
            } else {
                Set<String> answers = new HashSet<>(request.getAnswersList());
                int score = request.getScore();
                if (score >= 0) {
                    return groupQuestionService.authAndCreateGroupJoinQuestion(
                            turmsRequestWrapper.getUserId(),
                            request.getGroupId(),
                            request.getQuestion(),
                            answers,
                            score)
                            .map(question -> RequestResult.create(question.getId()));
                } else {
                    return Mono.just(RequestResult.create(TurmsStatusCode.ILLEGAL_ARGUMENTS, "The score must be greater than or equal to 0"));
                }
            }
        };
    }

    @TurmsRequestMapping(DELETE_GROUP_INVITATION_REQUEST)
    public Function<TurmsRequestWrapper, Mono<RequestResult>> handleDeleteGroupInvitationRequest() {
        return turmsRequestWrapper -> {
            DeleteGroupInvitationRequest request = turmsRequestWrapper.getTurmsRequest().getDeleteGroupInvitationRequest();
            return groupInvitationService.queryInviteeIdByInvitationId(request.getInvitationId())
                    .flatMap(inviteeId -> groupInvitationService.recallPendingGroupInvitation(
                            turmsRequestWrapper.getUserId(),
                            request.getInvitationId())
                            .map(recalled -> {
                                if (recalled != null && recalled
                                        && turmsClusterManager.getTurmsProperties().getNotification().isNotifyInviteeAfterGroupInvitationRecalled()) {
                                    return RequestResult.create(
                                            inviteeId,
                                            turmsRequestWrapper.getTurmsRequest());
                                }
                                return RequestResult.okIfTrue(recalled);
                            }));
        };
    }

    //TODO
    @TurmsRequestMapping(DELETE_GROUP_JOIN_REQUEST_REQUEST)
    public Function<TurmsRequestWrapper, Mono<RequestResult>> handleDeleteGroupJoinRequestRequest() {
        return turmsRequestWrapper -> {
            DeleteGroupJoinRequestRequest request = turmsRequestWrapper.getTurmsRequest()
                    .getDeleteGroupJoinRequestRequest();
            return groupJoinRequestService.recallPendingGroupJoinRequest(
                    turmsRequestWrapper.getUserId(),
                    request.getRequestId())
                    .flatMap(recalled -> {
                        if (recalled != null && recalled
                                && turmsClusterManager.getTurmsProperties().getNotification().isNotifyOwnerAndManagersAfterGroupJoinRequestRecalled()) {
                            return groupJoinRequestService.queryGroupId(request.getRequestId())
                                    .flatMap(groupId -> groupMemberService.queryGroupManagersAndOwnerId(groupId)
                                            .collect(Collectors.toSet())
                                            .map(ids -> {
                                                if (ids.isEmpty()) {
                                                    return RequestResult.ok();
                                                } else {
                                                    return RequestResult.create(
                                                            ids,
                                                            turmsRequestWrapper.getTurmsRequest());
                                                }
                                            }));
                        }
                        return Mono.just(RequestResult.okIfTrue(recalled));
                    });
        };
    }

    @TurmsRequestMapping(DELETE_GROUP_JOIN_QUESTION_REQUEST)
    public Function<TurmsRequestWrapper, Mono<RequestResult>> handleDeleteGroupJoinQuestionRequest() {
        return turmsRequestWrapper -> {
            DeleteGroupJoinQuestionRequest request = turmsRequestWrapper.getTurmsRequest()
                    .getDeleteGroupJoinQuestionRequest();
            return groupQuestionService.authAndDeleteGroupJoinQuestion(
                    turmsRequestWrapper.getUserId(),
                    request.getQuestionId())
                    .map(RequestResult::okIfTrue);
        };
    }

    //TODO: by to user
    @TurmsRequestMapping(QUERY_GROUP_INVITATIONS_REQUEST)
    public Function<TurmsRequestWrapper, Mono<RequestResult>> handleQueryGroupInvitationsRequest() {
        return turmsRequestWrapper -> {
            QueryGroupInvitationsRequest request = turmsRequestWrapper.getTurmsRequest()
                    .getQueryGroupInvitationsRequest();
            long groupId = request.getGroupId();
            Date lastUpdatedDate = request.hasLastUpdatedDate() ?
                    new Date(request.getLastUpdatedDate().getValue()) : null;
            return groupInvitationService.queryGroupInvitationsWithVersion(
                    turmsRequestWrapper.getUserId(),
                    groupId,
                    lastUpdatedDate)
                    .map(groupInvitationsWithVersion -> RequestResult.create(
                            TurmsNotification.Data.newBuilder()
                                    .setGroupInvitationsWithVersion(groupInvitationsWithVersion)
                                    .build()));
        };
    }

    @TurmsRequestMapping(QUERY_GROUP_JOIN_REQUESTS_REQUEST)
    public Function<TurmsRequestWrapper, Mono<RequestResult>> handleQueryGroupJoinRequestsRequest() {
        return turmsRequestWrapper -> {
            QueryGroupJoinRequestsRequest request = turmsRequestWrapper.getTurmsRequest()
                    .getQueryGroupJoinRequestsRequest();
            Date lastUpdatedDate = request.hasLastUpdatedDate() ?
                    new Date(request.getLastUpdatedDate().getValue()) : null;
            return groupJoinRequestService.queryGroupJoinRequestsWithVersion(
                    turmsRequestWrapper.getUserId(),
                    request.getGroupId(),
                    lastUpdatedDate)
                    .map(groupJoinRequestsWithVersion -> RequestResult.create(TurmsNotification.Data.newBuilder()
                            .setGroupJoinRequestsWithVersion(groupJoinRequestsWithVersion)
                            .build()));
        };
    }

    @TurmsRequestMapping(QUERY_GROUP_JOIN_QUESTIONS_REQUEST)
    public Function<TurmsRequestWrapper, Mono<RequestResult>> handleQueryGroupJoinQuestionsRequest() {
        return turmsRequestWrapper -> {
            QueryGroupJoinQuestionsRequest request = turmsRequestWrapper.getTurmsRequest()
                    .getQueryGroupJoinQuestionsRequest();
            Date lastUpdatedDate = request.hasLastUpdatedDate() ? new Date(request.getLastUpdatedDate().getValue()) : null;
            return groupQuestionService.queryGroupJoinQuestionsWithVersion(
                    turmsRequestWrapper.getUserId(),
                    request.getGroupId(),
                    request.getWithAnswers(),
                    lastUpdatedDate)
                    .map(groupJoinQuestionsWithVersion -> RequestResult.create(TurmsNotification.Data.newBuilder()
                            .setGroupJoinQuestionsWithVersion(groupJoinQuestionsWithVersion)
                            .build()));
        };
    }

    @TurmsRequestMapping(UPDATE_GROUP_JOIN_QUESTION_REQUEST)
    public Function<TurmsRequestWrapper, Mono<RequestResult>> handleUpdateGroupJoinQuestionRequest() {
        return turmsRequestWrapper -> {
            UpdateGroupJoinQuestionRequest request = turmsRequestWrapper.getTurmsRequest()
                    .getUpdateGroupJoinQuestionRequest();
            Set<String> answers = request.getAnswersList().isEmpty() ?
                    null : new HashSet<>(request.getAnswersList());
            String question = request.hasQuestion() ? request.getQuestion().getValue() : null;
            Integer score = request.hasScore() ? request.getScore().getValue() : null;
            return groupQuestionService.authAndUpdateGroupJoinQuestion(
                    turmsRequestWrapper.getUserId(),
                    request.getQuestionId(),
                    question,
                    answers,
                    score)
                    .map(RequestResult::okIfTrue);
        };
    }

    @TurmsRequestMapping(CREATE_GROUP_MEMBER_REQUEST)
    public Function<TurmsRequestWrapper, Mono<RequestResult>> handleCreateGroupMemberRequest() {
        return turmsRequestWrapper -> {
            CreateGroupMemberRequest request = turmsRequestWrapper.getTurmsRequest().getCreateGroupMemberRequest();
            String name = request.hasName() ? request.getName().getValue() : null;
            Date muteEndDate = request.hasMuteEndDate() ? new Date(request.getMuteEndDate().getValue()) : null;
            GroupMemberRole role = request.getRole();
            if (role == null || role == GroupMemberRole.UNRECOGNIZED) {
                role = GroupMemberRole.MEMBER;
            } else if (role == GroupMemberRole.OWNER) {
                return Mono.just(RequestResult.create(TurmsStatusCode.ILLEGAL_ARGUMENTS, "The role of the new member must not be OWNER"));
            }
            return groupMemberService.authAndAddGroupMember(
                    turmsRequestWrapper.getUserId(),
                    request.getGroupId(),
                    request.getUserId(),
                    role,
                    name,
                    muteEndDate,
                    null)
                    .map(member -> {
                        if (member != null && turmsClusterManager.getTurmsProperties().getNotification().isNotifyUserAfterAddedToGroupByOthers()) {
                            return RequestResult.create(
                                    request.getUserId(),
                                    turmsRequestWrapper.getTurmsRequest());
                        }
                        return RequestResult.okIfTrue(true);
                    });
        };
    }

    @TurmsRequestMapping(DELETE_GROUP_MEMBER_REQUEST)
    public Function<TurmsRequestWrapper, Mono<RequestResult>> handleDeleteGroupMemberRequest() {
        return turmsRequestWrapper -> {
            DeleteGroupMemberRequest request = turmsRequestWrapper.getTurmsRequest().getDeleteGroupMemberRequest();
            Long successorId = request.hasSuccessorId() ? request.getSuccessorId().getValue() : null;
            Boolean quitAfterTransfer = request.hasQuitAfterTransfer() ? request.getQuitAfterTransfer().getValue() : null;
            return groupMemberService.authAndDeleteGroupMember(
                    turmsRequestWrapper.getUserId(),
                    request.getGroupId(),
                    request.getGroupMemberId(),
                    successorId,
                    quitAfterTransfer)
                    .map(deleted -> {
                        if (deleted != null && deleted
                                && turmsClusterManager.getTurmsProperties().getNotification().isNotifyUserAfterRemovedFromGroupByOthers()
                                && !turmsRequestWrapper.getUserId().equals(request.getGroupMemberId())) {
                            return RequestResult.create(
                                    request.getGroupMemberId(),
                                    turmsRequestWrapper.getTurmsRequest());
                        }
                        return RequestResult.okIfTrue(deleted);
                    });
        };
    }

    @TurmsRequestMapping(QUERY_GROUP_MEMBERS_REQUEST)
    public Function<TurmsRequestWrapper, Mono<RequestResult>> handleQueryGroupMembersRequest() {
        return turmsRequestWrapper -> {
            QueryGroupMembersRequest request = turmsRequestWrapper.getTurmsRequest().getQueryGroupMembersRequest();
            Date lastUpdatedDate = request.hasLastUpdatedDate() ? new Date(request.getLastUpdatedDate().getValue()) : null;
            Set<Long> membersIds = request.getGroupMembersIdsCount() != 0 ?
                    new HashSet<>(request.getGroupMembersIdsList()) : null;
            boolean withStatus = request.hasWithStatus() && request.getWithStatus().getValue();
            if (request.getGroupMembersIdsCount() > 0) {
                return groupMemberService.authAndQueryGroupMembers(
                        turmsRequestWrapper.getUserId(),
                        request.getGroupId(),
                        membersIds,
                        withStatus)
                        .map(groupMembersWithVersion -> RequestResult.create(
                                TurmsNotification.Data.newBuilder()
                                        .setGroupMembersWithVersion(groupMembersWithVersion)
                                        .build()));
            } else {
                return groupMemberService.authAndQueryGroupMembersWithVersion(
                        turmsRequestWrapper.getUserId(),
                        request.getGroupId(),
                        lastUpdatedDate,
                        withStatus)
                        .map(groupMembersWithVersion -> RequestResult.create(
                                TurmsNotification.Data.newBuilder()
                                        .setGroupMembersWithVersion(groupMembersWithVersion)
                                        .build()));
            }
        };
    }

    @TurmsRequestMapping(UPDATE_GROUP_MEMBER_REQUEST)
    public Function<TurmsRequestWrapper, Mono<RequestResult>> handleUpdateGroupMemberRequest() {
        return turmsRequestWrapper -> {
            UpdateGroupMemberRequest request = turmsRequestWrapper.getTurmsRequest().getUpdateGroupMemberRequest();
            String name = request.hasName() ? request.getName().getValue() : null;
            GroupMemberRole role = request.getRole() != GroupMemberRole.UNRECOGNIZED ? request.getRole() : null;
            Date muteEndDate = request.hasMuteEndDate() ? new Date(request.getMuteEndDate().getValue()) : null;
            return groupMemberService.authAndUpdateGroupMember(
                    turmsRequestWrapper.getUserId(),
                    request.getGroupId(),
                    request.getMemberId(),
                    name,
                    role,
                    muteEndDate)
                    .flatMap(updated -> {
                        if (updated != null && updated) {
                            if (turmsClusterManager.getTurmsProperties().getNotification()
                                    .isNotifyMembersAfterOtherMemberInfoUpdated()) {
                                return groupMemberService.queryGroupMembersIds(request.getGroupId())
                                        .collect(Collectors.toSet())
                                        .map(groupMembersIds -> RequestResult.create(
                                                groupMembersIds,
                                                turmsRequestWrapper.getTurmsRequest()));
                            } else if (!turmsRequestWrapper.getUserId().equals(request.getMemberId())
                                    && turmsClusterManager.getTurmsProperties().getNotification().isNotifyMemberAfterInfoUpdatedByOthers()) {
                                return Mono.just(RequestResult.create(
                                        turmsRequestWrapper.getUserId(),
                                        turmsRequestWrapper.getTurmsRequest()));
                            }
                        }
                        return Mono.just(RequestResult.okIfTrue(updated));
                    });
        };
    }
}
