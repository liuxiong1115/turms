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

package im.turms.turms.service.user;

import com.hazelcast.replicatedmap.ReplicatedMap;
import com.mongodb.client.result.DeleteResult;
import im.turms.common.TurmsStatusCode;
import im.turms.common.exception.TurmsBusinessException;
import im.turms.common.util.Validator;
import im.turms.turms.annotation.cluster.PostHazelcastInitialized;
import im.turms.turms.builder.QueryBuilder;
import im.turms.turms.builder.UpdateBuilder;
import im.turms.turms.manager.TurmsClusterManager;
import im.turms.turms.pojo.domain.UserPermissionGroup;
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
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import static im.turms.turms.constant.Common.*;

@Service
@Validated
public class UserPermissionGroupService {
    private static final UserPermissionGroup EMPTY_USER_PERMISSION_GROUP = new UserPermissionGroup();
    private static ReplicatedMap<Long, UserPermissionGroup> userPermissionGroupMap;
    private final ReactiveMongoTemplate mongoTemplate;
    private final TurmsClusterManager turmsClusterManager;
    private final UserService userService;

    public UserPermissionGroupService(
            @Qualifier("userMongoTemplate") ReactiveMongoTemplate mongoTemplate,
            @Lazy TurmsClusterManager turmsClusterManager,
            UserService userService) {
        this.mongoTemplate = mongoTemplate;
        this.turmsClusterManager = turmsClusterManager;
        this.userService = userService;
    }

    @PostHazelcastInitialized
    public Function<TurmsClusterManager, Void> initUserPermissionGroups() {
        return turmsClusterManager -> {
            userPermissionGroupMap = turmsClusterManager.getHazelcastInstance().getReplicatedMap(HAZELCAST_USER_PERMISSION_GROUPS_MAP);
            if (userPermissionGroupMap.isEmpty()) {
                loadAllUserPermissionGroups().subscribe();
            }
            userPermissionGroupMap.putIfAbsent(
                    DEFAULT_USER_PERMISSION_GROUP_ID,
                    new UserPermissionGroup(
                            DEFAULT_USER_PERMISSION_GROUP_ID,
                            Set.of(DEFAULT_GROUP_TYPE_ID),
                            Integer.MAX_VALUE,
                            Integer.MAX_VALUE,
                            Collections.emptyMap()));
            return null;
        };
    }

    public Flux<UserPermissionGroup> loadAllUserPermissionGroups() {
        return mongoTemplate.find(new Query(), UserPermissionGroup.class)
                .doOnNext(userPermissionGroup -> userPermissionGroupMap.put(userPermissionGroup.getId(), userPermissionGroup));
    }

    public UserPermissionGroup getDefaultUserPermissionGroup() {
        return userPermissionGroupMap.get(DEFAULT_USER_PERMISSION_GROUP_ID);
    }

    public Flux<UserPermissionGroup> queryUserPermissionGroups(
            @Nullable Integer page,
            @Nullable Integer size) {
        Query query = QueryBuilder
                .newBuilder()
                .paginateIfNotNull(page, size);
        return mongoTemplate.find(query, UserPermissionGroup.class)
                .concatWithValues(getDefaultUserPermissionGroup());
    }

    public Mono<UserPermissionGroup> addUserPermissionGroup(
            @Nullable Long id,
            @NotNull Set<Long> creatableGroupTypeIds,
            @NotNull Integer ownedGroupLimit,
            @NotNull Integer ownedGroupLimitForEachGroupType,
            @NotNull Map<Long, Integer> groupTypeLimitMap) {
        if (id == null) {
            id = turmsClusterManager.generateRandomId();
        }
        UserPermissionGroup userPermissionGroup = new UserPermissionGroup(
                id,
                creatableGroupTypeIds,
                ownedGroupLimit,
                ownedGroupLimitForEachGroupType,
                groupTypeLimitMap);
        userPermissionGroupMap.put(id, userPermissionGroup);
        return mongoTemplate.insert(userPermissionGroup);
    }

    public Mono<Boolean> updateUserPermissionGroups(
            @NotEmpty Set<Long> ids,
            @Nullable Set<Long> creatableGroupTypeIds,
            @Nullable Integer ownedGroupLimit,
            @Nullable Integer ownedGroupLimitForEachGroupType,
            @Nullable Map<Long, Integer> groupTypeLimitMap) {
        if (Validator.areAllNull(
                creatableGroupTypeIds,
                ownedGroupLimit,
                ownedGroupLimitForEachGroupType,
                groupTypeLimitMap)) {
            return Mono.just(true);
        }
        Query query = new Query().addCriteria(Criteria.where(ID).in(ids));
        Update update = UpdateBuilder.newBuilder()
                .setIfNotNull(UserPermissionGroup.Fields.creatableGroupTypeIds, creatableGroupTypeIds)
                .setIfNotNull(UserPermissionGroup.Fields.ownedGroupLimit, ownedGroupLimit)
                .setIfNotNull(UserPermissionGroup.Fields.ownedGroupLimitForEachGroupType, ownedGroupLimitForEachGroupType)
                .setIfNotNull(UserPermissionGroup.Fields.groupTypeLimits, groupTypeLimitMap)
                .build();
        return mongoTemplate.updateMulti(query, update, UserPermissionGroup.class)
                .flatMap(result -> {
                    if (result.wasAcknowledged()) {
                        return loadAllUserPermissionGroups().then(Mono.just(true));
                    } else {
                        return Mono.just(false);
                    }
                });
    }

    public Mono<Boolean> deleteUserPermissionGroups(@Nullable Set<Long> ids) {
        if (ids != null) {
            if (ids.contains(DEFAULT_USER_PERMISSION_GROUP_ID)) {
                throw TurmsBusinessException.get(TurmsStatusCode.ILLEGAL_ARGUMENTS, "The default user permission group cannot be deleted");
            }
            for (Long id : ids) {
                userPermissionGroupMap.remove(id);
            }
        } else {
            for (Long key : userPermissionGroupMap.keySet()) {
                if (!key.equals(DEFAULT_USER_PERMISSION_GROUP_ID)) {
                    userPermissionGroupMap.remove(key);
                }
            }
        }
        Query query = QueryBuilder
                .newBuilder()
                .addInIfNotNull(ID, ids)
                .buildQuery();
        return mongoTemplate.remove(query, UserPermissionGroup.class).map(DeleteResult::wasAcknowledged);
    }

    public Mono<UserPermissionGroup> queryUserPermissionGroup(@NotNull Long id) {
        UserPermissionGroup userPermissionGroup = userPermissionGroupMap.get(id);
        if (userPermissionGroup != null) {
            return Mono.just(userPermissionGroup);
        } else {
            return mongoTemplate.findById(id, UserPermissionGroup.class)
                    .doOnNext(type -> userPermissionGroupMap.put(id, type));
        }
    }

    public Mono<UserPermissionGroup> queryUserPermissionGroupByUserId(@NotNull Long userId) {
        return userService.queryUserPermissionGroupId(userId)
                .flatMap(this::queryUserPermissionGroup);
    }

    public Mono<Boolean> userPermissionGroupExists(@NotNull Long id) {
        UserPermissionGroup userPermissionGroup = userPermissionGroupMap.get(id);
        if (userPermissionGroup != null) {
            return Mono.just(true);
        } else {
            Query query = new Query().addCriteria(Criteria.where(ID).is(id));
            return mongoTemplate.findOne(query, UserPermissionGroup.class)
                    .defaultIfEmpty(EMPTY_USER_PERMISSION_GROUP)
                    .map(type -> {
                        if (EMPTY_USER_PERMISSION_GROUP == type) {
                            return false;
                        } else {
                            userPermissionGroupMap.put(id, type);
                            return true;
                        }
                    });
        }
    }

    public Mono<Long> countUserPermissionGroups() {
        return mongoTemplate.count(new Query(), UserPermissionGroup.class)
                .map(number -> number + 1);
    }
}
