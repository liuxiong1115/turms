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

import com.hazelcast.core.ReplicatedMap;
import com.mongodb.client.result.DeleteResult;
import im.turms.turms.annotation.cluster.PostHazelcastInitialized;
import im.turms.turms.cluster.TurmsClusterManager;
import im.turms.turms.common.UpdateBuilder;
import im.turms.turms.constant.GroupInvitationStrategy;
import im.turms.turms.constant.GroupJoinStrategy;
import im.turms.turms.constant.GroupUpdateStrategy;
import im.turms.turms.pojo.domain.Group;
import im.turms.turms.pojo.domain.GroupType;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;
import java.util.function.Function;

import static im.turms.turms.common.Constants.*;

@Service
public class GroupTypeService {
    private static final GroupType EMPTY_GROUP_TYPE = new GroupType();
    private static ReplicatedMap<Long, GroupType> groupTypeMap;
    private final ReactiveMongoTemplate mongoTemplate;
    private final TurmsClusterManager turmsClusterManager;

    public GroupTypeService(ReactiveMongoTemplate mongoTemplate, @Lazy TurmsClusterManager turmsClusterManager) {
        this.mongoTemplate = mongoTemplate;
        this.turmsClusterManager = turmsClusterManager;
    }

    @PostHazelcastInitialized
    public static Function<TurmsClusterManager, Void> initGroupTypes() {
        return turmsClusterManager -> {
            groupTypeMap = turmsClusterManager.getHazelcastInstance().getReplicatedMap(HAZELCAST_GROUP_TYPES_MAP);
            groupTypeMap.putIfAbsent(
                    DEFAULT_GROUP_TYPE_ID,
                    new GroupType(
                            DEFAULT_GROUP_TYPE_ID,
                            DEFAULT_GROUP_TYPE_NAME,
                            500,
                            GroupInvitationStrategy.OWNER_MANAGER_MEMBER,
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

    public Flux<GroupType> getGroupTypes() {
        return mongoTemplate.findAll(GroupType.class);
    }

    public Mono<GroupType> addGroupType(
            @NotNull String name,
            @NotNull Integer groupSizeLimit,
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

    public Mono<Boolean> updateGroupType(
            @NotNull Long id,
            @Nullable String name,
            @Nullable Integer groupSizeLimit,
            @Nullable GroupInvitationStrategy groupInvitationStrategy,
            @Nullable GroupJoinStrategy groupJoinStrategy,
            @Nullable GroupUpdateStrategy groupInfoUpdateStrategy,
            @Nullable GroupUpdateStrategy memberInfoUpdateStrategy,
            @Nullable Boolean guestSpeakable,
            @Nullable Boolean selfInfoUpdatable,
            @Nullable Boolean enableReadReceipt,
            @Nullable Boolean messageEditable) {
        Query query = new Query().addCriteria(Criteria.where(ID).is(id));
        Update update = UpdateBuilder.newBuilder()
                .setIfNotNull(GroupType.Fields.name, name)
                .setIfNotNull(GroupType.Fields.groupSizeLimit, groupSizeLimit)
                .setIfNotNull(GroupType.Fields.invitationStrategy, groupInvitationStrategy)
                .setIfNotNull(GroupType.Fields.joinStrategy, groupJoinStrategy)
                .setIfNotNull(GroupType.Fields.groupInfoUpdateStrategy, groupInfoUpdateStrategy)
                .setIfNotNull(GroupType.Fields.memberInfoUpdateStrategy, memberInfoUpdateStrategy)
                .setIfNotNull(GroupType.Fields.guestSpeakable, guestSpeakable)
                .setIfNotNull(GroupType.Fields.selfInfoUpdatable, selfInfoUpdatable)
                .setIfNotNull(GroupType.Fields.enableReadReceipt, enableReadReceipt)
                .setIfNotNull(GroupType.Fields.messageEditable, messageEditable)
                .build();
        return mongoTemplate.findAndModify(query, update, GroupType.class)
                .doOnSuccess(groupType -> groupTypeMap.put(id, groupType))
                .thenReturn(true);
    }

    public Mono<Boolean> deleteGroupType(@NotNull Long groupTypeId) {
        Query query = new Query().addCriteria(Criteria.where(ID).is(groupTypeId));
        groupTypeMap.remove(groupTypeId);
        return mongoTemplate.remove(query, GroupType.class).map(DeleteResult::wasAcknowledged);
    }

    public Mono<GroupType> queryGroupType(@NotNull Long groupTypeId) {
        GroupType groupType = groupTypeMap.get(groupTypeId);
        if (groupType != null) {
            return Mono.just(groupType);
        } else {
            return mongoTemplate.findById(groupTypeId, GroupType.class)
                    .doOnSuccess(type -> groupTypeMap.put(groupTypeId, type));
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

    public Mono<Long> queryGroupTypeIdByGroupId(@NotNull Long groupId) {
        Query query = new Query().addCriteria(Criteria.where(ID).is(groupId));
        query.fields().include(Group.Fields.typeId);
        return mongoTemplate.findOne(query, Group.class)
                .map(Group::getTypeId);
    }

    public Mono<GroupType> queryGroupTypeByGroupId(@NotNull Long groupId) {
        return queryGroupTypeIdByGroupId(groupId)
                .flatMap(this::queryGroupType);
    }
}
