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

import com.hazelcast.replicatedmap.ReplicatedMap;
import com.mongodb.client.result.DeleteResult;
import im.turms.common.TurmsStatusCode;
import im.turms.common.constant.GroupInvitationStrategy;
import im.turms.common.constant.GroupJoinStrategy;
import im.turms.common.constant.GroupUpdateStrategy;
import im.turms.common.exception.TurmsBusinessException;
import im.turms.common.util.Validator;
import im.turms.turms.annotation.cluster.PostHazelcastInitialized;
import im.turms.turms.annotation.constraint.NoWhitespaceConstraint;
import im.turms.turms.builder.QueryBuilder;
import im.turms.turms.builder.UpdateBuilder;
import im.turms.turms.manager.TurmsClusterManager;
import im.turms.turms.pojo.domain.GroupType;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.annotation.Nullable;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.Set;
import java.util.function.Function;

import static im.turms.turms.constant.Common.*;

@Service
@Validated
public class GroupTypeService {
    private static final GroupType EMPTY_GROUP_TYPE = new GroupType();
    private static ReplicatedMap<Long, GroupType> groupTypeMap;
    private final ReactiveMongoTemplate mongoTemplate;
    private final TurmsClusterManager turmsClusterManager;

    public GroupTypeService(
            @Qualifier("groupMongoTemplate") ReactiveMongoTemplate mongoTemplate,
            @Lazy TurmsClusterManager turmsClusterManager) {
        this.mongoTemplate = mongoTemplate;
        this.turmsClusterManager = turmsClusterManager;
    }

    @PostHazelcastInitialized
    public Function<TurmsClusterManager, Void> initGroupTypes() {
        return turmsClusterManager -> {
            groupTypeMap = turmsClusterManager.getHazelcastInstance().getReplicatedMap(HAZELCAST_GROUP_TYPES_MAP);
            if (groupTypeMap.isEmpty()) {
                loadAllGroupTypes().subscribe();
            }
            groupTypeMap.putIfAbsent(
                    DEFAULT_GROUP_TYPE_ID,
                    new GroupType(
                            DEFAULT_GROUP_TYPE_ID,
                            DEFAULT_GROUP_TYPE_NAME,
                            500,
                            GroupInvitationStrategy.OWNER_MANAGER_MEMBER_REQUIRING_ACCEPTANCE,
                            GroupJoinStrategy.DECLINE_ANY_REQUEST,
                            GroupUpdateStrategy.OWNER_MANAGER,
                            GroupUpdateStrategy.OWNER_MANAGER,
                            false,
                            true,
                            true,
                            true));
            return null;
        };
    }

    public Flux<GroupType> loadAllGroupTypes() {
        return mongoTemplate.find(new Query(), GroupType.class)
                .doOnNext(groupType -> groupTypeMap.put(groupType.getId(), groupType));
    }

    public GroupType getDefaultGroupType() {
        return groupTypeMap.get(DEFAULT_GROUP_TYPE_ID);
    }

    public Flux<GroupType> queryGroupTypes(
            @Nullable Integer page,
            @Nullable Integer size) {
        Query query = QueryBuilder
                .newBuilder()
                .paginateIfNotNull(page, size);
        return mongoTemplate.find(query, GroupType.class)
                .concatWithValues(getDefaultGroupType());
    }

    public Mono<GroupType> addGroupType(
            @NotNull @NoWhitespaceConstraint String name,
            @NotNull @Min(1) Integer groupSizeLimit,
            @NotNull GroupInvitationStrategy groupInvitationStrategy,
            @NotNull GroupJoinStrategy groupJoinStrategy,
            @NotNull GroupUpdateStrategy groupInfoUpdateStrategy,
            @NotNull GroupUpdateStrategy memberInfoUpdateStrategy,
            @NotNull Boolean guestSpeakable,
            @NotNull Boolean selfInfoUpdatable,
            @NotNull Boolean enableReadReceipt,
            @NotNull Boolean messageEditable) {
        Long id = turmsClusterManager.generateRandomId();
        GroupType groupType = new GroupType(
                id,
                name,
                groupSizeLimit,
                groupInvitationStrategy,
                groupJoinStrategy,
                groupInfoUpdateStrategy,
                memberInfoUpdateStrategy,
                guestSpeakable,
                selfInfoUpdatable,
                enableReadReceipt,
                messageEditable);
        groupTypeMap.put(id, groupType);
        return mongoTemplate.insert(groupType);
    }

    public Mono<Boolean> updateGroupTypes(
            @NotEmpty Set<Long> ids,
            @Nullable @NoWhitespaceConstraint String name,
            @Nullable @Min(1) Integer groupSizeLimit,
            @Nullable GroupInvitationStrategy groupInvitationStrategy,
            @Nullable GroupJoinStrategy groupJoinStrategy,
            @Nullable GroupUpdateStrategy groupInfoUpdateStrategy,
            @Nullable GroupUpdateStrategy memberInfoUpdateStrategy,
            @Nullable Boolean guestSpeakable,
            @Nullable Boolean selfInfoUpdatable,
            @Nullable Boolean enableReadReceipt,
            @Nullable Boolean messageEditable) {
        if (Validator.areAllNull(name,
                groupSizeLimit,
                groupInvitationStrategy,
                groupJoinStrategy,
                groupInfoUpdateStrategy,
                memberInfoUpdateStrategy,
                guestSpeakable,
                selfInfoUpdatable,
                enableReadReceipt,
                messageEditable)) {
            return Mono.just(true);
        }
        Query query = new Query().addCriteria(Criteria.where(ID).in(ids));
        Update update = UpdateBuilder.newBuilder()
                .setIfNotNull(GroupType.Fields.NAME, name)
                .setIfNotNull(GroupType.Fields.GROUP_SIZE_LIMIT, groupSizeLimit)
                .setIfNotNull(GroupType.Fields.INVITATION_STRATEGY, groupInvitationStrategy)
                .setIfNotNull(GroupType.Fields.JOIN_STRATEGY, groupJoinStrategy)
                .setIfNotNull(GroupType.Fields.GROUP_INFO_UPDATE_STRATEGY, groupInfoUpdateStrategy)
                .setIfNotNull(GroupType.Fields.MEMBER_INFO_UPDATE_STRATEGY, memberInfoUpdateStrategy)
                .setIfNotNull(GroupType.Fields.GUEST_SPEAKABLE, guestSpeakable)
                .setIfNotNull(GroupType.Fields.SELF_INFO_UPDATABLE, selfInfoUpdatable)
                .setIfNotNull(GroupType.Fields.ENABLE_READ_RECEIPT, enableReadReceipt)
                .setIfNotNull(GroupType.Fields.MESSAGE_EDITABLE, messageEditable)
                .build();
        return mongoTemplate.updateMulti(query, update, GroupType.class)
                .flatMap(result -> {
                    if (result.wasAcknowledged()) {
                        return loadAllGroupTypes().then(Mono.just(true));
                    } else {
                        return Mono.just(false);
                    }
                });
    }

    public Mono<Boolean> deleteGroupTypes(@Nullable Set<Long> groupTypeIds) {
        if (groupTypeIds != null) {
            if (groupTypeIds.contains(DEFAULT_GROUP_TYPE_ID)) {
                throw TurmsBusinessException.get(TurmsStatusCode.ILLEGAL_ARGUMENTS, "The default group type cannot be deleted");
            }
            for (Long id : groupTypeIds) {
                groupTypeMap.remove(id);
            }
        } else {
            for (Long key : groupTypeMap.keySet()) {
                if (!key.equals(DEFAULT_GROUP_TYPE_ID)) {
                    groupTypeMap.remove(key);
                }
            }
        }
        Query query = QueryBuilder
                .newBuilder()
                .addInIfNotNull(ID, groupTypeIds)
                .buildQuery();
        return mongoTemplate.remove(query, GroupType.class).map(DeleteResult::wasAcknowledged);
    }

    public Mono<GroupType> queryGroupType(@NotNull Long groupTypeId) {
        GroupType groupType = groupTypeMap.get(groupTypeId);
        if (groupType != null) {
            return Mono.just(groupType);
        } else {
            return mongoTemplate.findById(groupTypeId, GroupType.class)
                    .doOnNext(type -> groupTypeMap.put(groupTypeId, type));
        }
    }

    public Mono<Boolean> groupTypeExists(@NotNull Long groupTypeId) {
        GroupType groupType = groupTypeMap.get(groupTypeId);
        if (groupType != null) {
            return Mono.just(true);
        } else {
            Query query = new Query().addCriteria(Criteria.where(ID).is(groupTypeId));
            return mongoTemplate.findOne(query, GroupType.class)
                    .defaultIfEmpty(EMPTY_GROUP_TYPE)
                    .map(type -> {
                        if (EMPTY_GROUP_TYPE == type) {
                            return false;
                        } else {
                            groupTypeMap.put(groupTypeId, type);
                            return true;
                        }
                    });
        }
    }

    public Mono<Long> countGroupTypes() {
        return mongoTemplate.count(new Query(), GroupType.class)
                .map(number -> number + 1);
    }
}
