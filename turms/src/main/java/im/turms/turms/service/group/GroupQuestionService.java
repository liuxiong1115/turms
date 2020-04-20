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

package im.turms.turms.service.group;

import com.google.protobuf.Int64Value;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;
import im.turms.common.TurmsStatusCode;
import im.turms.common.constant.GroupMemberRole;
import im.turms.common.exception.TurmsBusinessException;
import im.turms.common.model.bo.group.GroupJoinQuestionsAnswerResult;
import im.turms.common.model.bo.group.GroupJoinQuestionsWithVersion;
import im.turms.common.util.Validator;
import im.turms.turms.annotation.constraint.GroupQuestionIdAndAnswerConstraint;
import im.turms.turms.builder.QueryBuilder;
import im.turms.turms.builder.UpdateBuilder;
import im.turms.turms.manager.TurmsClusterManager;
import im.turms.turms.pojo.bo.GroupQuestionIdAndAnswer;
import im.turms.turms.pojo.domain.GroupJoinQuestion;
import im.turms.turms.util.ProtoUtil;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;

import javax.annotation.Nullable;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static im.turms.turms.constant.Common.ID;

@Service
@Validated
public class GroupQuestionService {
    private final TurmsClusterManager turmsClusterManager;
    private final ReactiveMongoTemplate mongoTemplate;
    private final GroupMemberService groupMemberService;
    private final GroupService groupService;
    private final GroupVersionService groupVersionService;

    public GroupQuestionService(ReactiveMongoTemplate mongoTemplate, TurmsClusterManager turmsClusterManager, GroupMemberService groupMemberService, GroupVersionService groupVersionService, GroupService groupService) {
        this.mongoTemplate = mongoTemplate;
        this.turmsClusterManager = turmsClusterManager;
        this.groupMemberService = groupMemberService;
        this.groupVersionService = groupVersionService;
        this.groupService = groupService;
    }

    public Mono<Integer> checkGroupQuestionAnswerAndCountScore(
            @NotNull Long questionId,
            @NotNull String answer,
            @Nullable Long groupId) {
        Query query = new Query()
                .addCriteria(Criteria.where(ID).is(questionId))
                .addCriteria(Criteria.where(GroupJoinQuestion.Fields.answers).in(answer));
        if (groupId != null) {
            query.addCriteria(Criteria.where(GroupJoinQuestion.Fields.groupId).is(groupId));
        }
        return mongoTemplate.findOne(query, GroupJoinQuestion.class)
                .map(GroupJoinQuestion::getScore);
    }

    /**
     * group join questions ids -> score
     */
    public Mono<Pair<List<Long>, Integer>> checkGroupQuestionAnswersAndCountScore(
            @NotEmpty Set<@GroupQuestionIdAndAnswerConstraint GroupQuestionIdAndAnswer> questionIdAndAnswers,
            @Nullable Long groupId) {
        List<Mono<Pair<Long, Integer>>> checks = new ArrayList<>(questionIdAndAnswers.size());
        for (GroupQuestionIdAndAnswer entry : questionIdAndAnswers) {
            checks.add(checkGroupQuestionAnswerAndCountScore(entry.getId(), entry.getAnswer(), groupId)
                    .map(score -> Pair.of(entry.getId(), score)));
        }
        return Flux.merge(checks)
                .collectList()
                .map(pairs -> {
                    List<Long> questionsIds = new ArrayList<>(pairs.size());
                    int score = 0;
                    for (Pair<Long, Integer> pair : pairs) {
                        questionsIds.add(pair.getLeft());
                        score += pair.getRight();
                    }
                    return Pair.of(questionsIds, score);
                });
    }

    public Mono<GroupJoinQuestionsAnswerResult> checkGroupQuestionAnswerAndJoin(
            @NotNull Long requesterId,
            @NotEmpty Set<@GroupQuestionIdAndAnswerConstraint GroupQuestionIdAndAnswer> questionIdAndAnswers) {
        Long firstQuestionId = questionIdAndAnswers.iterator().next().getId();
        return queryGroupId(firstQuestionId)
                .flatMap(groupId -> groupMemberService.isBlacklisted(groupId, requesterId)
                        .flatMap(isBlacklisted -> {
                            if (isBlacklisted != null && isBlacklisted) {
                                return Mono.error(TurmsBusinessException.get(TurmsStatusCode.USER_HAS_BEEN_BLACKLISTED));
                            } else {
                                return groupMemberService.exists(groupId, requesterId);
                            }
                        })
                        .flatMap(isGroupMember -> {
                            if (isGroupMember != null && isGroupMember) {
                                return Mono.error(TurmsBusinessException.get(TurmsStatusCode.ALREADY_GROUP_MEMBER));
                            } else {
                                return groupService.isGroupActiveAndNotDeleted(groupId);
                            }
                        })
                        .flatMap(isActive -> {
                            if (isActive != null && isActive) {
                                return checkGroupQuestionAnswersAndCountScore(questionIdAndAnswers, groupId);
                            } else {
                                return Mono.error(TurmsBusinessException.get(TurmsStatusCode.NOT_ACTIVE));
                            }
                        })
                        .flatMap(idsAndScore -> groupService.queryGroupMinimumScore(groupId)
                                .flatMap(minimumScore -> {
                                    if (idsAndScore.getRight() >= minimumScore) {
                                        return groupMemberService.addGroupMember(
                                                groupId,
                                                requesterId,
                                                GroupMemberRole.MEMBER,
                                                null,
                                                null,
                                                null,
                                                null)
                                                .thenReturn(true);
                                    } else {
                                        return Mono.error(TurmsBusinessException.get(TurmsStatusCode.GUESTS_HAVE_BEEN_MUTED));
                                    }
                                })
                                .map(joined -> GroupJoinQuestionsAnswerResult
                                        .newBuilder()
                                        .setJoined(joined)
                                        .addAllQuestionsIds(idsAndScore.getKey())
                                        .setScore(idsAndScore.getRight())
                                        .build())));
    }

    public Mono<GroupJoinQuestion> authAndCreateGroupJoinQuestion(
            @NotNull Long requesterId,
            @NotNull Long groupId,
            @NotNull String question,
            @NotEmpty Set<String> answers,
            @NotNull Integer score) {
        if (score < 0) {
            throw TurmsBusinessException.get(TurmsStatusCode.ILLEGAL_ARGUMENTS, "The score must be greater than or equal to 0");
        }
        return groupMemberService.isAllowedToCreateJoinQuestion(requesterId, groupId)
                .flatMap(allowed -> {
                    if (allowed != null && allowed) {
                        return createGroupJoinQuestion(groupId, question, answers, score);
                    } else {
                        return Mono.error(TurmsBusinessException.get(TurmsStatusCode.UNAUTHORIZED));
                    }
                });
    }

    public Mono<GroupJoinQuestion> createGroupJoinQuestion(
            @NotNull Long groupId,
            @NotNull String question,
            @NotEmpty Set<String> answers,
            @NotNull Integer score) {
        GroupJoinQuestion groupJoinQuestion = new GroupJoinQuestion(
                turmsClusterManager.generateRandomId(),
                groupId,
                question,
                answers,
                score);
        return mongoTemplate.insert(groupJoinQuestion)
                .zipWith(groupVersionService.updateJoinQuestionsVersion(groupId))
                .map(Tuple2::getT1);
    }

    public Mono<Long> queryGroupId(@NotNull Long questionId) {
        Query query = new Query().addCriteria(Criteria.where(ID).is(questionId));
        query.fields().include(GroupJoinQuestion.Fields.groupId);
        return mongoTemplate.findOne(query, GroupJoinQuestion.class)
                .map(GroupJoinQuestion::getGroupId);
    }

    public Mono<Boolean> authAndDeleteGroupJoinQuestion(
            @NotNull Long requesterId,
            @NotNull Long questionId) {
        return queryGroupId(questionId)
                .flatMap(groupId -> groupMemberService.isOwnerOrManager(requesterId, groupId)
                        .flatMap(authenticated -> {
                            if (authenticated != null && authenticated) {
                                Query query = new Query().addCriteria(Criteria.where(ID).is(questionId));
                                return mongoTemplate.remove(query, GroupJoinQuestion.class)
                                        .flatMap(result -> {
                                            if (result.wasAcknowledged()) {
                                                return groupVersionService.updateJoinQuestionsVersion(groupId)
                                                        .thenReturn(true);
                                            } else {
                                                return Mono.just(false);
                                            }
                                        });
                            } else {
                                return Mono.error(TurmsBusinessException.get(TurmsStatusCode.UNAUTHORIZED));
                            }
                        }));
    }

    public Flux<GroupJoinQuestion> queryGroupJoinQuestions(
            @Nullable Set<Long> ids,
            @Nullable Set<Long> groupIds,
            @Nullable Integer page,
            @Nullable Integer size,
            boolean withAnswers) {
        Query query = QueryBuilder
                .newBuilder()
                .addInIfNotNull(ID, ids)
                .addInIfNotNull(GroupJoinQuestion.Fields.groupId, groupIds)
                .paginateIfNotNull(page, size);
        if (!withAnswers) {
            query.fields().exclude(GroupJoinQuestion.Fields.answers);
        }
        return mongoTemplate.find(query, GroupJoinQuestion.class);
    }

    public Mono<Long> countGroupJoinQuestions(@Nullable Set<Long> ids, @Nullable Set<Long> groupIds) {
        Query query = QueryBuilder
                .newBuilder()
                .addInIfNotNull(ID, ids)
                .addInIfNotNull(GroupJoinQuestion.Fields.groupId, groupIds)
                .buildQuery();
        return mongoTemplate.count(query, GroupJoinQuestion.class);
    }

    public Mono<Boolean> deleteGroupJoinQuestions(@Nullable Set<Long> ids) {
        Query query = QueryBuilder
                .newBuilder()
                .addInIfNotNull(ID, ids)
                .buildQuery();
        return mongoTemplate.remove(query, GroupJoinQuestion.class)
                .map(DeleteResult::wasAcknowledged);
    }

    public Mono<GroupJoinQuestionsWithVersion> queryGroupJoinQuestionsWithVersion(
            @NotNull Long requesterId,
            @NotNull Long groupId,
            boolean withAnswers,
            @Nullable Date lastUpdatedDate) {
        Mono<Boolean> authenticated;
        if (withAnswers) {
            authenticated = groupMemberService.isOwnerOrManager(requesterId, groupId);
        } else {
            authenticated = Mono.just(true);
        }
        return authenticated
                .flatMap(isAuthenticated -> {
                    if (isAuthenticated != null && isAuthenticated) {
                        return groupVersionService.queryGroupJoinQuestionsVersion(groupId);
                    } else {
                        return Mono.error(TurmsBusinessException.get(TurmsStatusCode.UNAUTHORIZED));
                    }
                })
                .flatMap(version -> {
                    if (lastUpdatedDate == null || lastUpdatedDate.before(version)) {
                        return queryGroupJoinQuestions(null, Set.of(groupId), null, null, false)
                                .collect(Collectors.toSet())
                                .map(groupJoinQuestions -> {
                                    if (groupJoinQuestions.isEmpty()) {
                                        throw TurmsBusinessException.get(TurmsStatusCode.NO_CONTENT);
                                    }
                                    GroupJoinQuestionsWithVersion.Builder builder = GroupJoinQuestionsWithVersion.newBuilder();
                                    builder.setLastUpdatedDate(Int64Value.newBuilder().setValue(version.getTime()).build());
                                    for (GroupJoinQuestion question : groupJoinQuestions) {
                                        im.turms.common.model.bo.group.GroupJoinQuestion.Builder questionBuilder = ProtoUtil.groupJoinQuestion2proto(question);
                                        builder.addGroupJoinQuestions(questionBuilder.build());
                                    }
                                    return builder.build();
                                });
                    } else {
                        return Mono.error(TurmsBusinessException.get(TurmsStatusCode.ALREADY_UP_TO_DATE));
                    }
                })
                .switchIfEmpty(Mono.error(TurmsBusinessException.get(TurmsStatusCode.ALREADY_UP_TO_DATE)));
    }

    public Mono<Boolean> authAndUpdateGroupJoinQuestion(
            @NotNull Long requesterId,
            @NotNull Long questionId,
            @Nullable String question,
            @Nullable Set<String> answers,
            @Nullable Integer score) {
        if (Validator.areAllNull(question, answers, score)) {
            return Mono.just(true);
        }
        if (score != null && score < 0) {
            throw TurmsBusinessException.get(TurmsStatusCode.ILLEGAL_ARGUMENTS, "The score must be greater than or equal to 0");
        }
        return queryGroupId(questionId)
                .flatMap(groupId -> groupMemberService.isOwnerOrManager(requesterId, groupId)
                        .flatMap(authenticated -> {
                            if (authenticated != null && authenticated) {
                                Query query = new Query().addCriteria(Criteria.where(ID).is(questionId));
                                Update update = UpdateBuilder.newBuilder()
                                        .setIfNotNull(GroupJoinQuestion.Fields.question, question)
                                        .setIfNotNull(GroupJoinQuestion.Fields.answers, answers)
                                        .setIfNotNull(GroupJoinQuestion.Fields.score, score)
                                        .build();
                                return mongoTemplate.updateFirst(query, update, GroupJoinQuestion.class)
                                        .flatMap(result -> {
                                            if (result.wasAcknowledged()) {
                                                return groupVersionService.updateJoinQuestionsVersion(groupId)
                                                        .thenReturn(true);
                                            } else {
                                                return Mono.just(false);
                                            }
                                        });
                            } else {
                                return Mono.error(TurmsBusinessException.get(TurmsStatusCode.UNAUTHORIZED));
                            }
                        }));
    }

    public Mono<Boolean> updateGroupJoinQuestions(
            @NotEmpty Set<Long> ids,
            @Nullable Long groupId,
            @Nullable String question,
            @Nullable Set<String> answers,
            @Nullable Integer score) {
        if (Validator.areAllFalsy(groupId, question, answers, score)) {
            return Mono.just(true);
        }
        Query query = new Query().addCriteria(Criteria.where(ID).in(ids));
        Update update = UpdateBuilder.newBuilder()
                .setIfNotNull(GroupJoinQuestion.Fields.groupId, groupId)
                .setIfNotNull(GroupJoinQuestion.Fields.question, question)
                .setIfNotNull(GroupJoinQuestion.Fields.answers, answers)
                .setIfNotNull(GroupJoinQuestion.Fields.score, score)
                .build();
        return mongoTemplate.updateMulti(query, update, GroupJoinQuestion.class)
                .map(UpdateResult::wasAcknowledged);
    }
}
