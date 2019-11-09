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
import im.turms.turms.cluster.TurmsClusterManager;
import im.turms.turms.common.ProtoUtil;
import im.turms.turms.common.TurmsStatusCode;
import im.turms.turms.common.UpdateBuilder;
import im.turms.turms.constant.GroupMemberRole;
import im.turms.turms.exception.TurmsBusinessException;
import im.turms.turms.pojo.bo.group.GroupJoinQuestionsAnswerResult;
import im.turms.turms.pojo.bo.group.GroupJoinQuestionsWithVersion;
import im.turms.turms.pojo.domain.GroupJoinQuestion;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;

import javax.annotation.Nullable;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.*;
import java.util.stream.Collectors;

import static im.turms.turms.common.Constants.ID;
import static im.turms.turms.common.Constants.MAX_DATE;

@Service
public class GroupJoinQuestionService {
    private final TurmsClusterManager turmsClusterManager;
    private final ReactiveMongoTemplate mongoTemplate;
    private final GroupMemberService groupMemberService;
    private final GroupService groupService;
    private final GroupVersionService groupVersionService;

    public GroupJoinQuestionService(ReactiveMongoTemplate mongoTemplate, TurmsClusterManager turmsClusterManager, GroupMemberService groupMemberService, GroupVersionService groupVersionService, GroupService groupService) {
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
            @NotEmpty Map<Long, String> questionIdAndAnswerMap,
            @Nullable Long groupId) {
        List<Mono<Pair<Long, Integer>>> checks = new ArrayList<>(questionIdAndAnswerMap.entrySet().size());
        for (Map.Entry<Long, String> entry : questionIdAndAnswerMap.entrySet()) {
            checks.add(checkGroupQuestionAnswerAndCountScore(entry.getKey(), entry.getValue(), groupId)
                    .map(score -> Pair.of(entry.getKey(), score)));
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
            @NotEmpty Map<Long, String> questionIdAndAnswerMap) {
        Long firstQuestionId = questionIdAndAnswerMap.keySet().toArray(new Long[0])[0];
        return queryGroupId(firstQuestionId)
                .flatMap(groupId -> groupMemberService.isBlacklisted(groupId, requesterId)
                        .flatMap(isBlacklisted -> {
                            if (isBlacklisted != null && isBlacklisted) {
                                return Mono.error(TurmsBusinessException.get(TurmsStatusCode.UNAUTHORIZED));
                            } else {
                                return groupMemberService.exists(groupId, requesterId);
                            }
                        })
                        .flatMap(exists -> {
                            if (exists != null && exists) {
                                return Mono.error(TurmsBusinessException.get(TurmsStatusCode.RESOURCES_HAVE_BEEN_HANDLED));
                            } else {
                                return groupService.isGroupActive(groupId);
                            }
                        })
                        .flatMap(isActive -> {
                            if (isActive != null && isActive) {
                                return checkGroupQuestionAnswersAndCountScore(questionIdAndAnswerMap, groupId);
                            } else {
                                return Mono.error(TurmsBusinessException.get(TurmsStatusCode.DISABLE_FUNCTION));
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
                                                null, //TODO: GroupType: allow add a mute end date for new members
                                                null)
                                                .thenReturn(true);
                                    } else {
                                        return Mono.just(false);
                                    }
                                })
                                .map(joined -> GroupJoinQuestionsAnswerResult
                                        .newBuilder()
                                        .setJoined(joined)
                                        .addAllQuestionsIds(idsAndScore.getKey())
                                        .setScore(idsAndScore.getRight())
                                        .build())));
    }

    public Mono<GroupJoinQuestion> createGroupJoinQuestion(
            @NotNull Long requesterId,
            @NotNull Long groupId,
            @NotNull String question,
            @NotEmpty List<String> answers,
            @NotNull Integer score) {
        if (score < 0) {
            throw TurmsBusinessException.get(TurmsStatusCode.ILLEGAL_ARGUMENTS);
        }
        return groupMemberService.isAllowedToCreateJoinQuestion(requesterId, groupId)
                .flatMap(allowed -> {
                    if (allowed != null && allowed) {
                        GroupJoinQuestion groupJoinQuestion = new GroupJoinQuestion(
                                turmsClusterManager.generateRandomId(),
                                groupId,
                                question,
                                answers,
                                score);
                        return mongoTemplate.insert(groupJoinQuestion)
                                .zipWith(groupVersionService.updateJoinQuestionsVersion(groupId))
                                .map(Tuple2::getT1);
                    } else {
                        return Mono.error(TurmsBusinessException.get(TurmsStatusCode.UNAUTHORIZED));
                    }
                });
    }

    public Mono<Long> queryGroupId(@NotNull Long questionId) {
        Query query = new Query().addCriteria(Criteria.where(ID).is(questionId));
        query.fields().include(GroupJoinQuestion.Fields.groupId);
        return mongoTemplate.findOne(query, GroupJoinQuestion.class)
                .map(GroupJoinQuestion::getGroupId);
    }

    public Mono<Boolean> deleteGroupJoinQuestion(
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

    public Flux<GroupJoinQuestion> queryGroupJoinQuestions(@NotNull Long groupId, boolean withAnswers) {
        Query query = new Query().addCriteria(Criteria.where(GroupJoinQuestion.Fields.groupId).is(groupId));
        if (!withAnswers) {
            query.fields().exclude(GroupJoinQuestion.Fields.answers);
        }
        return mongoTemplate.find(query, GroupJoinQuestion.class);
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
                        return groupVersionService.queryGroupJoinQuestionsVersion(groupId)
                                .defaultIfEmpty(MAX_DATE);
                    } else {
                        return Mono.error(TurmsBusinessException.get(TurmsStatusCode.UNAUTHORIZED));
                    }
                })
                .flatMap(version -> {
                    if (lastUpdatedDate == null || lastUpdatedDate.before(version)) {
                        return queryGroupJoinQuestions(groupId, false)
                                .collect(Collectors.toSet())
                                .map(groupJoinQuestions -> {
                                    if (groupJoinQuestions.isEmpty()) {
                                        throw TurmsBusinessException.get(TurmsStatusCode.NOT_FOUND);
                                    }
                                    GroupJoinQuestionsWithVersion.Builder builder = GroupJoinQuestionsWithVersion.newBuilder();
                                    builder.setLastUpdatedDate(Int64Value.newBuilder().setValue(version.getTime()).build());
                                    for (GroupJoinQuestion question : groupJoinQuestions) {
                                        im.turms.turms.pojo.bo.group.GroupJoinQuestion.Builder questionBuilder = ProtoUtil.groupJoinQuestion2proto(question);
                                        builder.addGroupJoinQuestions(questionBuilder.build());
                                    }
                                    return builder.build();
                                });
                    } else {
                        return Mono.error(TurmsBusinessException.get(TurmsStatusCode.ALREADY_UP_TO_DATE));
                    }
                });
    }

    public Mono<Boolean> updateGroupJoinQuestion(
            @NotNull Long requesterId,
            @NotNull Long questionId,
            @Nullable String question,
            @Nullable Set<String> answers,
            @Nullable Integer score) {
        if ((question == null && answers == null && score == null) || (score != null && score < 0)) {
            throw TurmsBusinessException.get(TurmsStatusCode.ILLEGAL_ARGUMENTS);
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
}
