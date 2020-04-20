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
import im.turms.common.constant.RequestStatus;
import im.turms.common.exception.TurmsBusinessException;
import im.turms.common.model.bo.group.GroupInvitationsWithVersion;
import im.turms.common.util.Validator;
import im.turms.turms.annotation.constraint.RequestStatusConstraint;
import im.turms.turms.builder.QueryBuilder;
import im.turms.turms.builder.UpdateBuilder;
import im.turms.turms.manager.TurmsClusterManager;
import im.turms.turms.pojo.bo.DateRange;
import im.turms.turms.pojo.domain.GroupInvitation;
import im.turms.turms.property.TurmsProperties;
import im.turms.turms.service.user.UserVersionService;
import im.turms.turms.util.ProtoUtil;
import im.turms.turms.util.RequestStatusUtil;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.annotation.Nullable;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.PastOrPresent;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Set;
import java.util.stream.Collectors;

import static im.turms.turms.constant.Common.EXPIRED_GROUP_INVITATIONS_CLEANER_CRON;
import static im.turms.turms.constant.Common.ID;
import static org.springframework.data.mongodb.core.query.Criteria.where;

@Service
@Validated
public class GroupInvitationService {
    private final TurmsClusterManager turmsClusterManager;
    private final TurmsProperties turmsProperties;
    private final GroupMemberService groupMemberService;
    private final GroupVersionService groupVersionService;
    private final UserVersionService userVersionService;
    private final ReactiveMongoTemplate mongoTemplate;

    public GroupInvitationService(TurmsProperties turmsProperties, GroupMemberService groupMemberService, @Lazy TurmsClusterManager turmsClusterManager, ReactiveMongoTemplate mongoTemplate, UserVersionService userVersionService, GroupVersionService groupVersionService) {
        this.turmsProperties = turmsProperties;
        this.groupMemberService = groupMemberService;
        this.turmsClusterManager = turmsClusterManager;
        this.mongoTemplate = mongoTemplate;
        this.userVersionService = userVersionService;
        this.groupVersionService = groupVersionService;
    }

    @Scheduled(cron = EXPIRED_GROUP_INVITATIONS_CLEANER_CRON)
    public void groupInvitationsCleaner() {
        if (turmsClusterManager.isCurrentMemberMaster()) {
            if (turmsClusterManager.getTurmsProperties().getGroup()
                    .isDeleteExpiredGroupInvitationsAutomatically()) {
                removeAllExpiredGroupInvitations().subscribe();
            } else {
                updateExpiredRequestsStatus().subscribe();
            }
        }
    }

    public Mono<Boolean> removeAllExpiredGroupInvitations() {
        Date now = new Date();
        Query query = new Query()
                .addCriteria(Criteria.where(GroupInvitation.Fields.expirationDate).lt(now));
        return mongoTemplate.remove(query, GroupInvitation.class)
                .map(DeleteResult::wasAcknowledged);
    }

    /**
     * Warning: Only use expirationDate to check whether a request is expired.
     * Because of the excessive resource consumption, the request status of requests
     * won't be expiry immediately when reaching the expiration date.
     */
    public Mono<Boolean> updateExpiredRequestsStatus() {
        Date now = new Date();
        Query query = new Query()
                .addCriteria(Criteria.where(GroupInvitation.Fields.expirationDate).lt(now))
                .addCriteria(Criteria.where(GroupInvitation.Fields.status).is(RequestStatus.PENDING));
        Update update = new Update().set(GroupInvitation.Fields.status, RequestStatus.EXPIRED);
        return mongoTemplate.updateMulti(query, update, GroupInvitation.class)
                .map(UpdateResult::wasAcknowledged);
    }

    public Mono<GroupInvitation> authAndCreateGroupInvitation(
            @NotNull Long groupId,
            @NotNull Long inviterId,
            @NotNull Long inviteeId,
            @NotNull String content) {
        int contentLimit = turmsProperties.getGroup().getGroupInvitationContentLimit();
        if (content.length() > contentLimit) {
            throw TurmsBusinessException.get(TurmsStatusCode.ILLEGAL_ARGUMENTS, String.format("The content has exceeded the character limit (%d)", contentLimit));
        }
        return groupMemberService
                .isAllowedToInviteOrAdd(groupId, inviterId, null)
                .flatMap(strategy -> {
                    if (!strategy.isInvitable()) {
                        return Mono.error(TurmsBusinessException.get(TurmsStatusCode.UNAUTHORIZED));
                    }
                    return groupMemberService
                            .isAllowedToBeInvited(groupId, inviteeId)
                            .flatMap(allowedToBeInvited -> {
                                if (allowedToBeInvited == null || !allowedToBeInvited) {
                                    return Mono.error(TurmsBusinessException.get(TurmsStatusCode.TARGET_USERS_UNAUTHORIZED));
                                }
                                if (strategy.getGroupInvitationStrategy().requireAcceptance()) {
                                    return createGroupInvitation(null, groupId, inviterId, inviteeId, content,
                                            RequestStatus.PENDING, null, null, null);
                                } else {
                                    return Mono.error(TurmsBusinessException.get(TurmsStatusCode.REDUNDANT_REQUEST));
                                }
                            });
                });
    }

    public Mono<GroupInvitation> createGroupInvitation(
            @Nullable Long id,
            @NotNull Long groupId,
            @NotNull Long inviterId,
            @NotNull Long inviteeId,
            @NotNull String content,
            @Nullable @RequestStatusConstraint RequestStatus status,
            @Nullable @PastOrPresent Date creationDate,
            @Nullable @PastOrPresent Date responseDate,
            @Nullable Date expirationDate) {
        id = id != null ? id : turmsClusterManager.generateRandomId();
        if (creationDate == null) {
            creationDate = new Date();
        }
        if (expirationDate == null) {
            int groupInvitationTimeToLiveHours = turmsProperties.getGroup()
                    .getGroupInvitationTimeToLiveHours();
            if (groupInvitationTimeToLiveHours == 0) {
                expirationDate = Date.from(Instant.now()
                        .plus(groupInvitationTimeToLiveHours, ChronoUnit.HOURS));
            }
        }
        if (status == null) {
            status = RequestStatus.PENDING;
        }
        GroupInvitation groupInvitation = new GroupInvitation(id, groupId, inviterId, inviteeId, content, status, creationDate, responseDate, expirationDate);
        return mongoTemplate.insert(groupInvitation)
                .flatMap(invitation -> groupVersionService.updateGroupInvitationsVersion(groupId)
                        .then(userVersionService.updateSentGroupInvitationsVersion(inviterId))
                        .then(userVersionService.updateReceivedGroupInvitationsVersion(inviteeId))
                        .thenReturn(invitation));
    }

    public Mono<GroupInvitation> queryGroupIdAndStatus(@NotNull Long invitationId) {
        Query query = new Query().addCriteria(where(ID).is(invitationId));
        query.fields()
                .include(GroupInvitation.Fields.groupId)
                .include(GroupInvitation.Fields.status);
        return mongoTemplate.findOne(query, GroupInvitation.class)
                .map(groupInvitation -> {
                    Date expirationDate = groupInvitation.getExpirationDate();
                    if (expirationDate != null
                            && groupInvitation.getStatus() == RequestStatus.PENDING
                            && expirationDate.getTime() < System.currentTimeMillis()) {
                        return groupInvitation.toBuilder().status(RequestStatus.EXPIRED).build();
                    } else {
                        return groupInvitation;
                    }
                });
    }

    public Mono<Boolean> recallPendingGroupInvitation(
            @NotNull Long requesterId,
            @NotNull Long invitationId) {
        if (!turmsClusterManager.getTurmsProperties()
                .getGroup().isAllowRecallingPendingGroupInvitationByOwnerAndManager()) {
            throw TurmsBusinessException.get(TurmsStatusCode.DISABLED_FUNCTION);
        }
        return queryGroupIdAndStatus(invitationId)
                .flatMap(invitation -> {
                    if (invitation.getStatus() != RequestStatus.PENDING) {
                        return Mono.error(TurmsBusinessException.get(TurmsStatusCode.RESOURCES_HAVE_BEEN_HANDLED));
                    }
                    return groupMemberService.isOwnerOrManager(requesterId, invitation.getGroupId())
                            .flatMap(authenticated -> {
                                if (authenticated == null || !authenticated) {
                                    return Mono.error(TurmsBusinessException.get(TurmsStatusCode.UNAUTHORIZED));
                                }
                                Query query = new Query().addCriteria(where(ID).is(invitationId));
                                Update update = new Update()
                                        .set(GroupInvitation.Fields.status, RequestStatus.CANCELED);
                                return mongoTemplate.updateFirst(query, update, GroupInvitation.class)
                                        .flatMap(result -> {
                                            if (result.wasAcknowledged()) {
                                                return groupVersionService
                                                        .updateGroupInvitationsVersion(invitation.getGroupId())
                                                        .thenReturn(true);
                                            } else {
                                                return Mono.just(false);
                                            }
                                        });
                            });
                });
    }

    public Flux<GroupInvitation> queryGroupInvitationsByInviteeId(@NotNull Long inviteeId) {
        Query query = new Query()
                .addCriteria(where(GroupInvitation.Fields.inviteeId).is(inviteeId));
        return queryExpirableData(query);
    }

    public Flux<GroupInvitation> queryGroupInvitationsByInviterId(@NotNull Long inviterId) {
        Query query = new Query()
                .addCriteria(where(GroupInvitation.Fields.inviterId).is(inviterId));
        return queryExpirableData(query);
    }

    public Flux<GroupInvitation> queryGroupInvitationsByInviteeIdOrInviterId(@NotNull Long userId) {
        Query query = new Query()
                .addCriteria(where(GroupInvitation.Fields.inviteeId).is(userId)
                        .orOperator(where(GroupInvitation.Fields.inviterId).is(userId)));
        return queryExpirableData(query);
    }

    public Flux<GroupInvitation> queryGroupInvitationsByGroupId(@NotNull Long groupId) {
        Query query = new Query()
                .addCriteria(where(GroupInvitation.Fields.groupId).is(groupId));
        return queryExpirableData(query);
    }

    public Mono<GroupInvitationsWithVersion> queryUserGroupInvitationsWithVersion(
            @NotNull Long userId,
            boolean areSentByUser,
            @Nullable Date lastUpdatedDate) {
        Mono<Date> versionMono = areSentByUser
                ? userVersionService.querySentGroupInvitationsLastUpdatedDate(userId)
                : userVersionService.queryReceivedGroupInvitationsLastUpdatedDate(userId);
        return versionMono
                .flatMap(version -> {
                    if (lastUpdatedDate == null || lastUpdatedDate.before(version)) {
                        Flux<GroupInvitation> invitationFlux = areSentByUser
                                ? queryGroupInvitationsByInviterId(userId)
                                : queryGroupInvitationsByInviteeId(userId);
                        return invitationFlux
                                .collectList()
                                .map(groupInvitations -> {
                                    if (!groupInvitations.isEmpty()) {
                                        GroupInvitationsWithVersion.Builder builder = GroupInvitationsWithVersion.newBuilder();
                                        for (GroupInvitation groupInvitation : groupInvitations) {
                                            builder.addGroupInvitations(ProtoUtil.groupInvitation2proto(groupInvitation));
                                        }
                                        return builder
                                                .setLastUpdatedDate(Int64Value.newBuilder().setValue(version.getTime()).build())
                                                .build();
                                    } else {
                                        throw TurmsBusinessException.get(TurmsStatusCode.NO_CONTENT);
                                    }
                                });
                    } else {
                        return Mono.error(TurmsBusinessException.get(TurmsStatusCode.ALREADY_UP_TO_DATE));
                    }
                })
                .switchIfEmpty(Mono.error(TurmsBusinessException.get(TurmsStatusCode.ALREADY_UP_TO_DATE)));
    }

    public Mono<GroupInvitationsWithVersion> queryGroupInvitationsWithVersion(
            @NotNull Long userId,
            @NotNull Long groupId,
            @Nullable Date lastUpdatedDate) {
        return groupMemberService.isOwnerOrManager(userId, groupId)
                .flatMap(authenticated -> {
                    if (authenticated == null || !authenticated) {
                        return Mono.error(TurmsBusinessException.get(TurmsStatusCode.UNAUTHORIZED));
                    }
                    return groupVersionService.queryGroupInvitationsVersion(groupId)
                            .flatMap(version -> {
                                if (lastUpdatedDate == null || lastUpdatedDate.before(version)) {
                                    return queryGroupInvitationsByGroupId(groupId)
                                            .collect(Collectors.toSet())
                                            .map(groupInvitations -> {
                                                if (groupInvitations.isEmpty()) {
                                                    throw TurmsBusinessException.get(TurmsStatusCode.NO_CONTENT);
                                                }
                                                GroupInvitationsWithVersion.Builder builder = GroupInvitationsWithVersion.newBuilder();
                                                builder.setLastUpdatedDate(Int64Value.newBuilder().setValue(version.getTime()).build());
                                                for (GroupInvitation invitation : groupInvitations) {
                                                    builder.addGroupInvitations(ProtoUtil.groupInvitation2proto(invitation).build());
                                                }
                                                return builder.build();
                                            });
                                } else {
                                    return Mono.error(TurmsBusinessException.get(TurmsStatusCode.ALREADY_UP_TO_DATE));
                                }
                            })
                            .switchIfEmpty(Mono.error(TurmsBusinessException.get(TurmsStatusCode.ALREADY_UP_TO_DATE)));
                });
    }

    public Mono<Long> queryInviteeIdByInvitationId(@NotNull Long invitationId) {
        Query query = new Query().addCriteria(where(ID).is(invitationId));
        query.fields().include(GroupInvitation.Fields.inviteeId);
        return mongoTemplate.findOne(query, GroupInvitation.class)
                .map(GroupInvitation::getInviteeId);
    }

    public Flux<GroupInvitation> queryInvitations(
            @Nullable Set<Long> ids,
            @Nullable Set<Long> groupIds,
            @Nullable Set<Long> inviterIds,
            @Nullable Set<Long> inviteeIds,
            @Nullable Set<RequestStatus> statuses,
            @Nullable DateRange creationDateRange,
            @Nullable DateRange responseDateRange,
            @Nullable DateRange expirationDateRange,
            @Nullable Integer page,
            @Nullable Integer size) {
        Query query = QueryBuilder
                .newBuilder()
                .addInIfNotNull(ID, ids)
                .addInIfNotNull(GroupInvitation.Fields.groupId, groupIds)
                .addInIfNotNull(GroupInvitation.Fields.inviterId, inviterIds)
                .addInIfNotNull(GroupInvitation.Fields.inviteeId, inviteeIds)
                .addInIfNotNull(GroupInvitation.Fields.status, statuses)
                .addBetweenIfNotNull(GroupInvitation.Fields.creationDate, creationDateRange)
                .addBetweenIfNotNull(GroupInvitation.Fields.responseDate, responseDateRange)
                .addBetweenIfNotNull(GroupInvitation.Fields.expirationDate, expirationDateRange)
                .paginateIfNotNull(page, size);
        return mongoTemplate.find(query, GroupInvitation.class);
    }

    public Mono<Long> countInvitations(
            @Nullable Set<Long> ids,
            @Nullable Set<Long> groupIds,
            @Nullable Set<Long> inviterIds,
            @Nullable Set<Long> inviteeIds,
            @Nullable Set<RequestStatus> statuses,
            @Nullable DateRange creationDateRange,
            @Nullable DateRange responseDateRange,
            @Nullable DateRange expirationDateRange) {
        Query query = QueryBuilder
                .newBuilder()
                .addInIfNotNull(ID, ids)
                .addInIfNotNull(GroupInvitation.Fields.groupId, groupIds)
                .addInIfNotNull(GroupInvitation.Fields.inviterId, inviterIds)
                .addInIfNotNull(GroupInvitation.Fields.inviteeId, inviteeIds)
                .addInIfNotNull(GroupInvitation.Fields.status, statuses)
                .addBetweenIfNotNull(GroupInvitation.Fields.creationDate, creationDateRange)
                .addBetweenIfNotNull(GroupInvitation.Fields.responseDate, responseDateRange)
                .addBetweenIfNotNull(GroupInvitation.Fields.expirationDate, expirationDateRange)
                .buildQuery();
        return mongoTemplate.count(query, GroupInvitation.class);
    }

    public Mono<Boolean> deleteInvitations(@Nullable Set<Long> ids) {
        Query query = QueryBuilder
                .newBuilder()
                .addInIfNotNull(ID, ids)
                .buildQuery();
        return mongoTemplate.remove(query, GroupInvitation.class)
                .map(DeleteResult::wasAcknowledged);
    }

    public Mono<Boolean> updateInvitations(
            @NotEmpty Set<Long> ids,
            @Nullable Long inviterId,
            @Nullable Long inviteeId,
            @Nullable String content,
            @Nullable @RequestStatusConstraint RequestStatus status,
            @Nullable @PastOrPresent Date creationDate,
            @Nullable @PastOrPresent Date responseDate,
            @Nullable Date expirationDate) {
        if (Validator.areAllNull(inviterId, inviteeId, content, status, creationDate, expirationDate)) {
            return Mono.just(true);
        }
        Query query = new Query().addCriteria(where(ID).in(ids));
        Update update = UpdateBuilder
                .newBuilder()
                .setIfNotNull(GroupInvitation.Fields.inviterId, inviterId)
                .setIfNotNull(GroupInvitation.Fields.inviteeId, inviteeId)
                .setIfNotNull(GroupInvitation.Fields.content, content)
                .setIfNotNull(GroupInvitation.Fields.status, status)
                .setIfNotNull(GroupInvitation.Fields.creationDate, creationDate)
                .setIfNotNull(GroupInvitation.Fields.expirationDate, expirationDate)
                .build();
        RequestStatusUtil.updateResponseDateBasedOnStatus(update, status, responseDate);
        return mongoTemplate.updateMulti(query, update, GroupInvitation.class)
                .map(UpdateResult::wasAcknowledged);
    }

    private Flux<GroupInvitation> queryExpirableData(Query query) {
        return mongoTemplate.find(query, GroupInvitation.class)
                .map(groupInvitation -> {
                    Date expirationDate = groupInvitation.getExpirationDate();
                    if (expirationDate != null
                            && groupInvitation.getStatus() == RequestStatus.PENDING
                            && expirationDate.getTime() < System.currentTimeMillis()) {
                        return groupInvitation.toBuilder().status(RequestStatus.EXPIRED).build();
                    } else {
                        return groupInvitation;
                    }
                });
    }
}
