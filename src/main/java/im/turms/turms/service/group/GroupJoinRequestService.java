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
import com.hazelcast.cp.lock.FencedLock;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;
import im.turms.turms.annotation.cluster.PostHazelcastInitialized;
import im.turms.turms.cluster.TurmsClusterManager;
import im.turms.turms.common.ProtoUtil;
import im.turms.turms.common.TurmsStatusCode;
import im.turms.turms.constant.RequestStatus;
import im.turms.turms.exception.TurmsBusinessException;
import im.turms.turms.pojo.domain.GroupJoinRequest;
import im.turms.turms.pojo.response.GroupJoinRequestsWithVersion;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.function.Function;
import java.util.stream.Collectors;

import static im.turms.turms.common.Constants.*;
import static org.springframework.data.mongodb.core.query.Criteria.where;

@Service
public class GroupJoinRequestService {
    private final TurmsClusterManager turmsClusterManager;
    private final ReactiveMongoTemplate mongoTemplate;
    private final GroupService groupService;
    private final GroupVersionService groupVersionService;
    private final GroupMemberService groupMemberService;

    public GroupJoinRequestService(ReactiveMongoTemplate mongoTemplate, @Lazy TurmsClusterManager turmsClusterManager, GroupVersionService groupVersionService, GroupMemberService groupMemberService, @Lazy GroupService groupService) {
        this.mongoTemplate = mongoTemplate;
        this.turmsClusterManager = turmsClusterManager;
        this.groupVersionService = groupVersionService;
        this.groupMemberService = groupMemberService;
        this.groupService = groupService;
    }

    @PostHazelcastInitialized
    public Function<TurmsClusterManager, Void> initExpiryGroupJoinRequestsCleaner() {
        return (clusterManager -> {
            TASK_SCHEDULER.schedule(() -> {
                FencedLock lock = clusterManager.getHazelcastInstance().getCPSubsystem().getLock(HAZELCAST_EXPIRY_GROUP_JOIN_REQUESTS_CLEANER_LOCK);
                if (lock.tryLock()) {
                    try {
                        if (clusterManager.getTurmsProperties().getGroup()
                                .isDeleteExpiryGroupJoinRequests()) {
                            removeAllExpiryGroupJoinRequests().subscribe();
                        } else {
                            updateExpiryRequestsStatus().subscribe();
                        }
                    } finally {
                        lock.unlock();
                    }
                }
            }, new CronTrigger(EXPIRY_GROUP_JOIN_REQUESTS_CLEANER_CRON));
            return null;
        });
    }

    public Mono<Boolean> removeAllExpiryGroupJoinRequests() {
        Date now = new Date();
        Query query = new Query()
                .addCriteria(Criteria.where(GroupJoinRequest.Fields.expirationDate).lt(now));
        return mongoTemplate.remove(query, GroupJoinRequest.class)
                .map(DeleteResult::wasAcknowledged);
    }

    /**
     * Warning: Only use expirationDate to check whether a request is expiry.
     * Because of the excessive resource consumption, the request status of requests
     * won't be expiry immediately when reaching the expiration date.
     */
    public Mono<Boolean> updateExpiryRequestsStatus() {
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
        if (content.length() > turmsClusterManager.getTurmsProperties()
                .getGroup().getGroupJoinRequestContentLimit()) {
            throw TurmsBusinessException.get(TurmsStatusCode.ILLEGAL_ARGUMENTS);
        }
        return groupMemberService.isBlacklisted(groupId, requesterId)
                .flatMap(isBlacklisted -> {
                    if (isBlacklisted != null && isBlacklisted) {
                        return Mono.error(TurmsBusinessException.get(TurmsStatusCode.UNAUTHORIZED));
                    } else {
                        return groupService.isGroupActive(groupId)
                                .flatMap(isActive -> {
                                    if (isActive == null || !isActive) {
                                        return Mono.error(TurmsBusinessException.get(TurmsStatusCode.UNAUTHORIZED));
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
                                            new Date(),
                                            content,
                                            RequestStatus.PENDING,
                                            expirationDate,
                                            groupId,
                                            requesterId,
                                            null);
                                    return mongoTemplate.insert(groupJoinRequest)
                                            .zipWith(groupVersionService.updateJoinRequestsVersion(groupId))
                                            .map(Tuple2::getT1);
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
                    if (groupJoinRequest.getStatus() == RequestStatus.PENDING
                            && groupJoinRequest.getExpirationDate().before(new Date())) {
                        groupJoinRequest.setStatus(RequestStatus.EXPIRED);
                    }
                    return groupJoinRequest;
                });
    }

    public Mono<Boolean> recallPendingGroupJoinRequest(@NotNull Long requesterId, @NotNull Long requestId) {
        if (!turmsClusterManager.getTurmsProperties().getGroup().isAllowRecallingJoinRequestSentByOneself()) {
            throw TurmsBusinessException.get(TurmsStatusCode.DISABLE_FUNCTION);
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
                                            .thenReturn(true);
                                } else {
                                    return Mono.just(false);
                                }
                            });
                });
    }

    private Flux<GroupJoinRequest> queryGroupJoinRequests(@NotNull Long groupId) {
        Query query = new Query().addCriteria(where(GroupJoinRequest.Fields.groupId).is(groupId));
        return mongoTemplate.find(query, GroupJoinRequest.class)
                .map(groupJoinRequest -> {
                    if (groupJoinRequest.getStatus() == RequestStatus.PENDING
                            && groupJoinRequest.getExpirationDate().before(new Date())) {
                        groupJoinRequest.setStatus(RequestStatus.EXPIRED);
                    }
                    return groupJoinRequest;
                });
    }

    public Mono<GroupJoinRequestsWithVersion> queryGroupJoinRequestsWithVersion(
            @NotNull Long requesterId,
            @NotNull Long groupId,
            @Nullable Date lastUpdatedDate) {
        return groupMemberService.isOwnerOrManager(requesterId, groupId)
                .flatMap(authenticated -> {
                    if (authenticated != null && authenticated) {
                        return groupVersionService.queryGroupJoinRequestsVersion(groupId)
                                .defaultIfEmpty(MAX_DATE);
                    } else {
                        return Mono.error(TurmsBusinessException.get(TurmsStatusCode.UNAUTHORIZED));
                    }
                })
                .flatMap(version -> {
                    if (lastUpdatedDate == null || lastUpdatedDate.before(version)) {
                        return queryGroupJoinRequests(groupId)
                                .collect(Collectors.toSet())
                                .map(groupJoinRequests -> {
                                    if (groupJoinRequests.isEmpty()) {
                                        throw TurmsBusinessException.get(TurmsStatusCode.NOT_FOUND);
                                    }
                                    GroupJoinRequestsWithVersion.Builder builder = GroupJoinRequestsWithVersion.newBuilder();
                                    builder.setLastUpdatedDate(Int64Value.newBuilder().setValue(version.getTime()).build());
                                    for (GroupJoinRequest groupJoinRequest : groupJoinRequests) {
                                        im.turms.turms.pojo.dto.GroupJoinRequest request = ProtoUtil.groupJoinRequest2proto(groupJoinRequest).build();
                                        builder.addGroupJoinRequests(request);
                                    }
                                    return builder.build();
                                });
                    } else {
                        return Mono.error(TurmsBusinessException.get(TurmsStatusCode.ALREADY_UP_TO_DATE));
                    }
                });
    }

    public Mono<Long> queryGroupId(@NotNull Long requestId) {
        Query query = new Query().addCriteria(where(ID).is(requestId));
        query.fields().include(GroupJoinRequest.Fields.groupId);
        return mongoTemplate.findOne(query, GroupJoinRequest.class)
                .map(GroupJoinRequest::getGroupId);
    }
}
