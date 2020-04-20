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
import im.turms.common.model.bo.group.GroupJoinRequestsWithVersion;
import im.turms.common.util.Validator;
import im.turms.turms.annotation.constraint.RequestStatusConstraint;
import im.turms.turms.builder.QueryBuilder;
import im.turms.turms.builder.UpdateBuilder;
import im.turms.turms.manager.TurmsClusterManager;
import im.turms.turms.pojo.bo.DateRange;
import im.turms.turms.pojo.domain.GroupJoinRequest;
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
import reactor.util.function.Tuple2;

import javax.annotation.Nullable;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.PastOrPresent;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Set;

import static im.turms.turms.constant.Common.EXPIRED_GROUP_JOIN_REQUESTS_CLEANER_CRON;
import static im.turms.turms.constant.Common.ID;
import static org.springframework.data.mongodb.core.query.Criteria.where;

@Service
@Validated
public class GroupJoinRequestService {
    private final TurmsClusterManager turmsClusterManager;
    private final ReactiveMongoTemplate mongoTemplate;
    private final GroupService groupService;
    private final GroupVersionService groupVersionService;
    private final GroupMemberService groupMemberService;
    private final UserVersionService userVersionService;

    public GroupJoinRequestService(ReactiveMongoTemplate mongoTemplate, @Lazy TurmsClusterManager turmsClusterManager, GroupVersionService groupVersionService, GroupMemberService groupMemberService, @Lazy GroupService groupService, UserVersionService userVersionService) {
        this.mongoTemplate = mongoTemplate;
        this.turmsClusterManager = turmsClusterManager;
        this.groupVersionService = groupVersionService;
        this.groupMemberService = groupMemberService;
        this.groupService = groupService;
        this.userVersionService = userVersionService;
    }

    @Scheduled(cron = EXPIRED_GROUP_JOIN_REQUESTS_CLEANER_CRON)
    public void expiredGroupJoinRequestsCleaner() {
        if (turmsClusterManager.isCurrentMemberMaster()) {
            if (turmsClusterManager.getTurmsProperties().getGroup()
                    .isDeleteExpiredGroupJoinRequestsAutomatically()) {
                removeAllExpiredGroupJoinRequests().subscribe();
            } else {
                updateExpiredRequestsStatus().subscribe();
            }
        }
    }

    public Mono<Boolean> removeAllExpiredGroupJoinRequests() {
        Date now = new Date();
        Query query = new Query()
                .addCriteria(Criteria.where(GroupJoinRequest.Fields.expirationDate).lt(now));
        return mongoTemplate.remove(query, GroupJoinRequest.class)
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
                .addCriteria(Criteria.where(GroupJoinRequest.Fields.expirationDate).lt(now))
                .addCriteria(Criteria.where(GroupJoinRequest.Fields.status).is(RequestStatus.PENDING));
        Update update = new Update().set(GroupJoinRequest.Fields.status, RequestStatus.EXPIRED);
        return mongoTemplate.updateMulti(query, update, GroupJoinRequest.class)
                .map(UpdateResult::wasAcknowledged);
    }

    public Mono<GroupJoinRequest> authAndCreateGroupJoinRequest(
            @NotNull Long requesterId,
            @NotNull Long groupId,
            @NotNull String content) {
        int contentLimit = turmsClusterManager.getTurmsProperties()
                .getGroup().getGroupJoinRequestContentLimit();
        if (content.length() > contentLimit) {
            throw TurmsBusinessException.get(TurmsStatusCode.ILLEGAL_ARGUMENTS, String.format("The content has exceeded the character limit (%d)", contentLimit));
        }
        return groupMemberService.isBlacklisted(groupId, requesterId)
                .flatMap(isBlacklisted -> {
                    if (isBlacklisted != null && isBlacklisted) {
                        return Mono.error(TurmsBusinessException.get(TurmsStatusCode.UNAUTHORIZED));
                    } else {
                        return groupService.isGroupActiveAndNotDeleted(groupId)
                                .flatMap(isActive -> {
                                    if (isActive == null || !isActive) {
                                        return Mono.error(TurmsBusinessException.get(TurmsStatusCode.NOT_ACTIVE));
                                    }
                                    Date expirationDate = null;
                                    int hours = turmsClusterManager.getTurmsProperties().getGroup()
                                            .getGroupJoinRequestTimeToLiveHours();
                                    if (hours != 0) {
                                        expirationDate = Date.from(Instant.now().plus(hours, ChronoUnit.HOURS));
                                    }
                                    long id = turmsClusterManager.generateRandomId();
                                    GroupJoinRequest groupJoinRequest = new GroupJoinRequest(
                                            id,
                                            content,
                                            RequestStatus.PENDING,
                                            new Date(),
                                            null,
                                            expirationDate,
                                            groupId,
                                            requesterId,
                                            null);
                                    return mongoTemplate.insert(groupJoinRequest)
                                            .flatMap(request -> groupVersionService.updateJoinRequestsVersion(groupId)
                                                    .then(userVersionService.updateSentGroupJoinRequestsVersion(requesterId))
                                                    .thenReturn(request));
                                });
                    }
                });
    }

    private Mono<GroupJoinRequest> queryRequesterIdAndStatusAndGroupId(@NotNull Long requestId) {
        Query query = new Query().addCriteria(Criteria.where(ID).is(requestId));
        query.fields()
                .include(GroupJoinRequest.Fields.requesterId)
                .include(GroupJoinRequest.Fields.status)
                .include(GroupJoinRequest.Fields.groupId);
        return mongoTemplate.findOne(query, GroupJoinRequest.class)
                .map(groupJoinRequest -> {
                    Date expirationDate = groupJoinRequest.getExpirationDate();
                    if (expirationDate != null
                            && groupJoinRequest.getStatus() == RequestStatus.PENDING
                            && expirationDate.getTime() < System.currentTimeMillis()) {
                        return groupJoinRequest.toBuilder().status(RequestStatus.EXPIRED).build();
                    } else {
                        return groupJoinRequest;
                    }
                });
    }

    public Mono<Boolean> recallPendingGroupJoinRequest(@NotNull Long requesterId, @NotNull Long requestId) {
        if (!turmsClusterManager.getTurmsProperties().getGroup().isAllowRecallingJoinRequestSentByOneself()) {
            throw TurmsBusinessException.get(TurmsStatusCode.DISABLED_FUNCTION);
        }
        return queryRequesterIdAndStatusAndGroupId(requestId)
                .flatMap(request -> {
                    if (request.getStatus() != RequestStatus.PENDING) {
                        return Mono.error(TurmsBusinessException.get(TurmsStatusCode.RESOURCES_HAVE_BEEN_HANDLED));
                    }
                    if (!request.getRequesterId().equals(requesterId)) {
                        return Mono.error(TurmsBusinessException.get(TurmsStatusCode.UNAUTHORIZED));
                    }
                    Query query = new Query().addCriteria(where(ID).is(requestId));
                    Update update = new Update()
                            .set(GroupJoinRequest.Fields.status, RequestStatus.CANCELED)
                            .set(GroupJoinRequest.Fields.responderId, requesterId);
                    return mongoTemplate.updateFirst(query, update, GroupJoinRequest.class)
                            .flatMap(result -> {
                                if (result.wasAcknowledged()) {
                                    return groupVersionService.updateJoinRequestsVersion(request.getGroupId())
                                            .zipWith(userVersionService.updateSentGroupJoinRequestsVersion(requesterId))
                                            .thenReturn(true);
                                } else {
                                    return Mono.just(false);
                                }
                            });
                });
    }

    public Mono<GroupJoinRequestsWithVersion> queryGroupJoinRequestsWithVersion(
            @NotNull Long userId,
            @Nullable Long groupId,
            @Nullable Date lastUpdatedDate) {
        boolean searchRequestsByGroupId = groupId != null;
        Mono<Date> versionMono = searchRequestsByGroupId ?
                groupMemberService.isOwnerOrManager(userId, groupId)
                        .flatMap(authenticated -> {
                            if (authenticated != null && authenticated) {
                                return groupVersionService.queryGroupJoinRequestsVersion(groupId);
                            } else {
                                return Mono.error(TurmsBusinessException.get(TurmsStatusCode.UNAUTHORIZED));
                            }
                        })
                : userVersionService.queryGroupJoinRequestsVersion(userId);
        return versionMono
                .flatMap(version -> {
                    if (lastUpdatedDate == null || lastUpdatedDate.before(version)) {
                        Flux<GroupJoinRequest> requestFlux = searchRequestsByGroupId
                                ? queryGroupJoinRequestsByGroupId(groupId)
                                : queryGroupJoinRequestsByRequesterId(userId);
                        return requestFlux
                                .collectList()
                                .map(groupJoinRequests -> {
                                    if (!groupJoinRequests.isEmpty()) {
                                        GroupJoinRequestsWithVersion.Builder builder = GroupJoinRequestsWithVersion.newBuilder();
                                        for (GroupJoinRequest groupJoinRequest : groupJoinRequests) {
                                            builder.addGroupJoinRequests(ProtoUtil.groupJoinRequest2proto(groupJoinRequest).build());
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

    public Flux<GroupJoinRequest> queryGroupJoinRequestsByGroupId(@NotNull Long groupId) {
        Query query = new Query().addCriteria(where(GroupJoinRequest.Fields.groupId).is(groupId));
        return queryExpirableData(query);
    }

    public Flux<GroupJoinRequest> queryGroupJoinRequestsByRequesterId(@NotNull Long requesterId) {
        Query query = new Query().addCriteria(where(GroupJoinRequest.Fields.requesterId).is(requesterId));
        return queryExpirableData(query);
    }

    public Mono<Long> queryGroupId(@NotNull Long requestId) {
        Query query = new Query().addCriteria(where(ID).is(requestId));
        query.fields().include(GroupJoinRequest.Fields.groupId);
        return mongoTemplate.findOne(query, GroupJoinRequest.class)
                .map(GroupJoinRequest::getGroupId);
    }


    public Flux<GroupJoinRequest> queryJoinRequests(
            @Nullable Set<Long> ids,
            @Nullable Set<Long> groupIds,
            @Nullable Set<Long> requesterIds,
            @Nullable Set<Long> responderIds,
            @Nullable Set<@RequestStatusConstraint RequestStatus> statuses,
            @Nullable DateRange creationDateRange,
            @Nullable DateRange responseDateRange,
            @Nullable DateRange expirationDateRange,
            @Nullable Integer page,
            @Nullable Integer size) {
        Query query = QueryBuilder
                .newBuilder()
                .addInIfNotNull(ID, ids)
                .addInIfNotNull(GroupJoinRequest.Fields.groupId, groupIds)
                .addInIfNotNull(GroupJoinRequest.Fields.requesterId, requesterIds)
                .addInIfNotNull(GroupJoinRequest.Fields.responderId, responderIds)
                .addInIfNotNull(GroupJoinRequest.Fields.status, statuses)
                .addBetweenIfNotNull(GroupJoinRequest.Fields.creationDate, creationDateRange)
                .addBetweenIfNotNull(GroupJoinRequest.Fields.responseDate, responseDateRange)
                .addBetweenIfNotNull(GroupJoinRequest.Fields.expirationDate, expirationDateRange)
                .paginateIfNotNull(page, size);
        return mongoTemplate.find(query, GroupJoinRequest.class);
    }

    public Mono<Long> countJoinRequests(
            @Nullable Set<Long> ids,
            @Nullable Set<Long> groupId,
            @Nullable Set<Long> requesterId,
            @Nullable Set<Long> responderId,
            @Nullable Set<@RequestStatusConstraint RequestStatus> status,
            @Nullable DateRange creationDateRange,
            @Nullable DateRange responseDateRange,
            @Nullable DateRange expirationDateRange) {
        Query query = QueryBuilder
                .newBuilder()
                .addInIfNotNull(ID, ids)
                .addIsIfNotNull(GroupJoinRequest.Fields.groupId, groupId)
                .addIsIfNotNull(GroupJoinRequest.Fields.requesterId, requesterId)
                .addIsIfNotNull(GroupJoinRequest.Fields.responderId, responderId)
                .addIsIfNotNull(GroupJoinRequest.Fields.status, status)
                .addBetweenIfNotNull(GroupJoinRequest.Fields.creationDate, creationDateRange)
                .addBetweenIfNotNull(GroupJoinRequest.Fields.responseDate, responseDateRange)
                .addBetweenIfNotNull(GroupJoinRequest.Fields.expirationDate, expirationDateRange)
                .buildQuery();
        return mongoTemplate.count(query, GroupJoinRequest.class);
    }

    public Mono<Boolean> deleteJoinRequests(@Nullable Set<Long> ids) {
        Query query = QueryBuilder
                .newBuilder()
                .addInIfNotNull(ID, ids)
                .buildQuery();
        return mongoTemplate.remove(query, GroupJoinRequest.class)
                .map(DeleteResult::wasAcknowledged);
    }

    public Mono<Boolean> updateJoinRequests(
            @NotEmpty Set<Long> ids,
            @Nullable Long requesterId,
            @Nullable Long responderId,
            @Nullable String content,
            @Nullable @RequestStatusConstraint RequestStatus status,
            @Nullable @PastOrPresent Date creationDate,
            @Nullable @PastOrPresent Date responseDate,
            @Nullable Date expirationDate) {
        if (Validator.areAllNull(requesterId, responderId, content, status, creationDate, expirationDate)) {
            return Mono.just(true);
        }
        Query query = new Query().addCriteria(where(ID).in(ids));
        Update update = UpdateBuilder
                .newBuilder()
                .setIfNotNull(GroupJoinRequest.Fields.requesterId, requesterId)
                .setIfNotNull(GroupJoinRequest.Fields.responderId, responderId)
                .setIfNotNull(GroupJoinRequest.Fields.content, content)
                .setIfNotNull(GroupJoinRequest.Fields.status, status)
                .setIfNotNull(GroupJoinRequest.Fields.creationDate, creationDate)
                .setIfNotNull(GroupJoinRequest.Fields.expirationDate, expirationDate)
                .build();
        RequestStatusUtil.updateResponseDateBasedOnStatus(update, status, responseDate);
        return mongoTemplate.updateMulti(query, update, GroupJoinRequest.class)
                .map(UpdateResult::wasAcknowledged);
    }

    public Mono<GroupJoinRequest> createGroupJoinRequest(
            @Nullable Long id,
            @NotNull Long groupId,
            @NotNull Long requesterId,
            @NotNull Long responderId,
            @NotNull String content,
            @Nullable @RequestStatusConstraint RequestStatus status,
            @Nullable @PastOrPresent Date creationDate,
            @Nullable @PastOrPresent Date responseDate,
            @Nullable Date expirationDate) {
        Date now = new Date();
        id = id != null ? id : turmsClusterManager.generateRandomId();
        if (creationDate == null) {
            creationDate = now;
        }
        if (expirationDate == null) {
            int timeToLiveHours = turmsClusterManager.getTurmsProperties().getGroup()
                    .getGroupJoinRequestTimeToLiveHours();
            if (timeToLiveHours == 0) {
                expirationDate = Date.from(Instant.now().plus(timeToLiveHours, ChronoUnit.HOURS));
            }
        }
        if (status == null) {
            status = RequestStatus.PENDING;
        }
        responseDate = RequestStatusUtil.getResponseDateBasedOnStatus(status, responseDate, now);
        GroupJoinRequest groupJoinRequest = new GroupJoinRequest(id, content, status, creationDate, responseDate,
                expirationDate, groupId, requesterId, responderId);
        return Mono.zip(mongoTemplate.insert(groupJoinRequest),
                groupVersionService.updateJoinRequestsVersion(groupId),
                userVersionService.updateSentGroupJoinRequestsVersion(responderId))
                .map(Tuple2::getT1);
    }

    private Flux<GroupJoinRequest> queryExpirableData(Query query) {
        return mongoTemplate.find(query, GroupJoinRequest.class)
                .map(groupJoinRequest -> {
                    Date expirationDate = groupJoinRequest.getExpirationDate();
                    if (expirationDate != null
                            && groupJoinRequest.getStatus() == RequestStatus.PENDING
                            && expirationDate.getTime() < System.currentTimeMillis()) {
                        return groupJoinRequest.toBuilder().status(RequestStatus.EXPIRED).build();
                    } else {
                        return groupJoinRequest;
                    }
                });
    }
}