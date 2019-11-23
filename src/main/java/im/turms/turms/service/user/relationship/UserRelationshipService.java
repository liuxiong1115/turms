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

package im.turms.turms.service.user.relationship;

import com.google.protobuf.Int64Value;
import im.turms.turms.cluster.TurmsClusterManager;
import im.turms.turms.common.ProtoUtil;
import im.turms.turms.common.QueryBuilder;
import im.turms.turms.common.TurmsStatusCode;
import im.turms.turms.common.UpdateBuilder;
import im.turms.turms.exception.TurmsBusinessException;
import im.turms.turms.pojo.bo.common.Int64ValuesWithVersion;
import im.turms.turms.pojo.bo.user.UserRelationshipsWithVersion;
import im.turms.turms.pojo.domain.UserRelationship;
import im.turms.turms.pojo.domain.UserRelationshipGroupMember;
import im.turms.turms.service.user.UserVersionService;
import org.springframework.data.mongodb.core.ReactiveMongoOperations;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.annotation.Nullable;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.*;
import java.util.stream.Collectors;

import static im.turms.turms.common.Constants.*;

@Service
public class UserRelationshipService {
    private final UserVersionService userVersionService;
    private final UserRelationshipGroupService userRelationshipGroupService;
    private final TurmsClusterManager turmsClusterManager;
    private final ReactiveMongoTemplate mongoTemplate;

    public UserRelationshipService(TurmsClusterManager turmsClusterManager, UserVersionService userVersionService, ReactiveMongoTemplate mongoTemplate, UserRelationshipGroupService userRelationshipGroupService) {
        this.turmsClusterManager = turmsClusterManager;
        this.userVersionService = userVersionService;
        this.mongoTemplate = mongoTemplate;
        this.userRelationshipGroupService = userRelationshipGroupService;
    }

    public Mono<Boolean> deleteOneSidedRelationships(
            @NotNull Long ownerId,
            @NotEmpty Set<Long> relatedUsersIds) {
        Query query = new Query()
                .addCriteria(Criteria.where(ID_OWNER_ID).is(ownerId))
                .addCriteria(Criteria.where(ID_RELATED_USER_ID).in(relatedUsersIds));
        return mongoTemplate.remove(query, UserRelationship.class)
                .flatMap(result -> {
                    if (result.wasAcknowledged()) {
                        return userVersionService.updateRelationshipsVersion(ownerId)
                                .thenReturn(true);
                    } else {
                        return Mono.just(false);
                    }
                });
    }

    public Mono<Boolean> deleteAllRelatedRelationships(
            @NotEmpty Set<Long> usersIds,
            @Nullable ReactiveMongoOperations operations) {
        Query query = new Query()
                .addCriteria(new Criteria().orOperator(
                        Criteria.where(ID_OWNER_ID).in(usersIds),
                        Criteria.where(ID_RELATED_USER_ID).in(usersIds)));
        if (operations != null) {
            return operations.remove(query, UserRelationship.class)
                    .flatMap(result -> {
                        if (result.wasAcknowledged()) {
                            return userVersionService.delete(usersIds, operations);
                        } else {
                            return Mono.just(false);
                        }
                    });
        } else {
            return mongoTemplate.inTransaction()
                    .execute(newOperations -> newOperations.remove(query, UserRelationship.class)
                            .flatMap(result -> {
                                if (result.wasAcknowledged()) {
                                    return userVersionService.delete(usersIds, newOperations);
                                } else {
                                    return Mono.just(false);
                                }
                            }))
                    .retryBackoff(MONGO_TRANSACTION_RETRIES_NUMBER, MONGO_TRANSACTION_BACKOFF)
                    .single();
        }
    }

    public Mono<Boolean> deleteOneSidedRelationship(
            @NotNull Long ownerId,
            @NotNull Long relatedUserId,
            @Nullable ReactiveMongoOperations operations) {
        Query query = new Query()
                .addCriteria(Criteria.where(ID_OWNER_ID).is(ownerId))
                .addCriteria(Criteria.where(ID_RELATED_USER_ID).is(relatedUserId));
        if (operations != null) {
            return operations.remove(query, UserRelationship.class)
                    .zipWith(userRelationshipGroupService.deleteRelatedUserFromAllRelationshipGroups(
                            ownerId, relatedUserId, operations))
                    .zipWith(userVersionService.updateRelationshipsVersion(ownerId))
                    .thenReturn(true);
        } else {
            return mongoTemplate.inTransaction()
                    .execute(newOperations -> newOperations.remove(query, UserRelationship.class)
                            .zipWith(userRelationshipGroupService.deleteRelatedUserFromAllRelationshipGroups(
                                    ownerId, relatedUserId, operations))
                            .zipWith(userVersionService.updateRelationshipsVersion(ownerId))
                            .thenReturn(true))
                    .retryBackoff(MONGO_TRANSACTION_RETRIES_NUMBER, MONGO_TRANSACTION_BACKOFF)
                    .single();
        }
    }

    public Mono<Boolean> deleteTwoSidedRelationships(
            @NotNull Long userOneId,
            @NotNull Long userTwoId) {
        return mongoTemplate.inTransaction()
                .execute(operations -> deleteOneSidedRelationship(userOneId, userTwoId, operations)
                        .zipWith(deleteOneSidedRelationship(userTwoId, userOneId, operations))
                        .map(results -> results.getT1() && results.getT2()))
                .retryBackoff(MONGO_TRANSACTION_RETRIES_NUMBER, MONGO_TRANSACTION_BACKOFF)
                .single();
    }

    private Flux<UserRelationship> queryRelationships(@NotEmpty Set<Long> ownersIds) {
        Query query = new Query().addCriteria(Criteria.where(ID_OWNER_ID).in(ownersIds));
        return mongoTemplate.find(query, UserRelationship.class);
    }

    private Flux<Long> queryRelatedUsersIds(
            @NotNull Long ownerId,
            @NotNull Integer groupIndex) {
        Query query = new Query()
                .addCriteria(Criteria.where(ID_OWNER_ID).is(ownerId))
                .addCriteria(Criteria.where(ID_GROUP_INDEX).is(groupIndex));
        query.fields().include(ID_RELATED_USER_ID);
        return mongoTemplate.find(query, UserRelationshipGroupMember.class)
                .map(member -> member.getKey().getRelatedUserId());
    }

    public Mono<Int64ValuesWithVersion> queryRelatedUsersIdsWithVersion(
            @NotNull Long ownerId,
            @NotNull Integer groupIndex,
            @NotNull Boolean isBlocked,
            @Nullable Date lastUpdatedDate) {
        return userVersionService.queryRelationshipsLastUpdatedDate(ownerId)
                .defaultIfEmpty(MAX_DATE)
                .flatMap(date -> {
                    if (lastUpdatedDate == null || lastUpdatedDate.before(date)) {
                        return queryRelatedUsersIds(ownerId, groupIndex, isBlocked)
                                .collect(Collectors.toSet())
                                .map(ids -> Int64ValuesWithVersion.newBuilder()
                                        .setLastUpdatedDate(Int64Value.newBuilder().setValue(date.getTime()).build())
                                        .addAllValues(ids)
                                        .build());
                    } else {
                        return Mono.error(TurmsBusinessException.get(TurmsStatusCode.ALREADY_UP_TO_DATE));
                    }
                });
    }

    public Mono<UserRelationshipsWithVersion> queryRelationshipsWithVersion(
            @NotNull Long ownerId,
            @Nullable Set<Long> relatedUsersIds,
            @Nullable Integer groupIndex,
            @Nullable Boolean isBlocked,
            @Nullable Date lastUpdatedDate) {
        return userVersionService.queryRelationshipsLastUpdatedDate(ownerId)
                .defaultIfEmpty(MAX_DATE)
                .flatMap(date -> {
                    if (lastUpdatedDate == null || lastUpdatedDate.before(date)) {
                        return queryRelationships(ownerId, relatedUsersIds, groupIndex, isBlocked)
                                .collect(Collectors.toSet())
                                .map(relationships -> {
                                    UserRelationshipsWithVersion.Builder builder = UserRelationshipsWithVersion.newBuilder();
                                    builder.setLastUpdatedDate(Int64Value.newBuilder().setValue(date.getTime()).build());
                                    for (UserRelationship relationship : relationships) {
                                        im.turms.turms.pojo.bo.user.UserRelationship userRelationship = ProtoUtil.relationship2proto(relationship).build();
                                        builder.addUserRelationships(userRelationship);
                                    }
                                    return builder.build();
                                });
                    } else {
                        return Mono.error(TurmsBusinessException.get(TurmsStatusCode.ALREADY_UP_TO_DATE));
                    }
                });
    }

    public Flux<Long> queryRelatedUsersIds(
            @NotNull Long ownerId,
            @Nullable Boolean isBlocked) {
        Query query = QueryBuilder.newBuilder()
                .add(Criteria.where(ID_OWNER_ID).is(ownerId))
                .addIsIfNotNull(UserRelationship.Fields.isBlocked, isBlocked)
                .buildQuery();
        query.fields().include(ID_RELATED_USER_ID);
        return mongoTemplate.find(query, UserRelationship.class)
                .map(userRelationship -> userRelationship.getKey().getRelatedUserId());
    }

    public Flux<Long> queryRelatedUsersIds(
            @NotNull Long ownerId,
            @Nullable Integer groupIndex,
            @Nullable Boolean isBlocked) {
        if (groupIndex != null && isBlocked != null) {
            return Mono.zip(
                    queryRelatedUsersIds(ownerId, groupIndex).collect(Collectors.toSet()),
                    queryRelatedUsersIds(ownerId, isBlocked).collect(Collectors.toSet()))
                    .flatMapIterable(tuple -> {
                        tuple.getT1().retainAll(tuple.getT2());
                        return tuple.getT1();
                    });
        } else if (groupIndex != null) {
            return queryRelatedUsersIds(ownerId, groupIndex);
        } else {
            return queryRelatedUsersIds(ownerId, isBlocked);
        }
    }

    private Flux<UserRelationship> queryRelationshipsWithoutGroupIndex(
            @NotNull Long ownerId,
            @Nullable Set<Long> relatedUsersIds,
            @Nullable Boolean isBlocked) {
        QueryBuilder builder = QueryBuilder.newBuilder()
                .add(Criteria.where(ID_OWNER_ID).is(ownerId))
                .addInIfNotNull(ID_RELATED_USER_ID, relatedUsersIds)
                .addIsIfNotNull(UserRelationship.Fields.isBlocked, isBlocked);
        return mongoTemplate.find(builder.buildQuery(), UserRelationship.class);
    }

    public Mono<UserRelationship> queryRelationship(
            @NotNull Long ownerId,
            @NotNull Long userId) {
        Query query = new Query()
                .addCriteria(Criteria.where(ID_OWNER_ID).is(ownerId))
                .addCriteria(Criteria.where(ID_RELATED_USER_ID).is(userId));
        return mongoTemplate.findOne(query, UserRelationship.class);
    }

    public Flux<UserRelationship> queryRelationships(
            @NotNull Long ownerId,
            @Nullable Set<Long> relatedUsersIds,
            @Nullable Integer groupIndex,
            @Nullable Boolean isBlocked) {
        if (groupIndex != null && (relatedUsersIds != null || isBlocked != null)) {
            return Mono.zip(
                    queryRelatedUsersIds(ownerId, groupIndex).collect(Collectors.toSet()),
                    queryRelationshipsWithoutGroupIndex(ownerId, relatedUsersIds, isBlocked).collect(Collectors.toSet()))
                    .flatMapIterable(results -> results.getT2().stream()
                            .filter(userRelationship -> results.getT1().contains(userRelationship.getKey().getRelatedUserId()))
                            .collect(Collectors.toSet()));
        } else if (groupIndex != null) {
            return queryRelatedUsersIds(ownerId, groupIndex).collect(Collectors.toSet())
                    .flatMapMany(this::queryRelationships);
        } else {
            return queryRelationshipsWithoutGroupIndex(ownerId, relatedUsersIds, isBlocked);
        }
    }

    public Mono<Boolean> friendTwoUsers(
            @NotNull Long userOneId,
            @NotNull Long userTwoId,
            @Nullable ReactiveMongoOperations operations) {
        Date now = new Date();
        if (operations != null) {
            return Mono.zip(upsertOneSidedRelationship(
                    userOneId, userTwoId, false,
                    DEFAULT_RELATIONSHIP_GROUP_INDEX, null, now, true, operations)
                    , upsertOneSidedRelationship(userTwoId, userOneId, false,
                            DEFAULT_RELATIONSHIP_GROUP_INDEX, null, now, true, operations))
                    .thenReturn(true);
        } else {
            return mongoTemplate.inTransaction()
                    .execute(newOperations -> friendTwoUsers(userOneId, userTwoId, newOperations)
                            .map(objects -> objects))
                    .retryBackoff(MONGO_TRANSACTION_RETRIES_NUMBER, MONGO_TRANSACTION_BACKOFF)
                    .single();
        }
    }

    public Mono<Boolean> unfriendTwoUsers(
            @NotNull Long ownerId,
            @NotNull Long relatedUserId,
            @Nullable ReactiveMongoOperations operations) {
        if (operations != null) {
            return Mono.zip(deleteOneSidedRelationship(ownerId, relatedUserId, operations),
                    deleteOneSidedRelationship(relatedUserId, ownerId, operations))
                    .map(results -> results.getT1() && results.getT2());
        } else {
            return mongoTemplate.inTransaction()
                    .execute(newOperations -> unfriendTwoUsers(ownerId, relatedUserId, newOperations))
                    .retryBackoff(MONGO_TRANSACTION_RETRIES_NUMBER, MONGO_TRANSACTION_BACKOFF)
                    .single();
        }
    }

    public Mono<Boolean> upsertOneSidedRelationship(
            @NotNull Long ownerId,
            @NotNull Long relatedUserId,
            @Nullable Boolean isBlocked,
            @Nullable Integer newGroupIndex,
            @Nullable Integer deleteGroupIndex,
            @Nullable Date establishmentDate,
            boolean upsert,
            @Nullable ReactiveMongoOperations operations) {
        UserRelationship userRelationship = new UserRelationship();
        userRelationship.setKey(new UserRelationship.Key(ownerId, relatedUserId));
        userRelationship.setIsBlocked(isBlocked != null && isBlocked);
        userRelationship.setEstablishmentDate(establishmentDate != null ? establishmentDate : new Date());
        List<Mono<?>> monos = new LinkedList<>();
        ReactiveMongoOperations mongoOperations = operations != null ? operations : mongoTemplate;
        if (upsert) {
            monos.add(mongoOperations.save(userRelationship));
        } else {
            monos.add(mongoOperations.insert(userRelationship));
        }
        if (newGroupIndex != null && deleteGroupIndex != null && !newGroupIndex.equals(deleteGroupIndex)) {
            Query query = new Query()
                    .addCriteria(Criteria.where(ID_OWNER_ID).is(ownerId))
                    .addCriteria(Criteria.where(ID_RELATED_USER_ID).is(relatedUserId))
                    .addCriteria(Criteria.where(ID_GROUP_INDEX).is(deleteGroupIndex));
            Update update = new Update().set(ID_GROUP_INDEX, newGroupIndex);
            monos.add(mongoTemplate.findAndModify(query, update, UserRelationshipGroupMember.class));
        } else {
            if (newGroupIndex != null) {
                Mono<Boolean> add = userRelationshipGroupService.addRelatedUserToRelationshipGroups(
                        ownerId, Collections.singleton(newGroupIndex), relatedUserId, operations);
                monos.add(add);
            }
            if (deleteGroupIndex != null) {
                Mono<Boolean> delete = userRelationshipGroupService.removeRelatedUserFromRelationshipGroup
                        (ownerId, relatedUserId, deleteGroupIndex,
                                newGroupIndex != null ? newGroupIndex : DEFAULT_RELATIONSHIP_GROUP_INDEX);
                monos.add(delete);
            }
        }
        return Mono.zip(monos, objects -> objects)
                .onErrorReturn(EMPTY_ARRAY)
                .map(objects -> EMPTY_ARRAY != objects);
    }

    public Flux<Long> queryUsersIdsOnBlacklist(@NotNull Long ownerId) {
        Query query = new Query()
                .addCriteria(Criteria.where(ID_OWNER_ID).is(ownerId))
                .addCriteria(Criteria.where(UserRelationship.Fields.isBlocked).is(true));
        query.fields().include(ID_RELATED_USER_ID);
        return mongoTemplate.find(query, UserRelationship.class)
                .map(userRelationship -> userRelationship.getKey().getRelatedUserId());
    }

    public Mono<Boolean> removeUserFromBlacklist(@NotNull Long ownerId, @NotNull Long relatedUserId) {
        Query query = new Query()
                .addCriteria(Criteria.where(ID_OWNER_ID).is(ownerId))
                .addCriteria(Criteria.where(ID_RELATED_USER_ID).is(relatedUserId));
        Update update = new Update().set(UserRelationship.Fields.isBlocked, false);
        return mongoTemplate.updateFirst(query, update, UserRelationship.class)
                .zipWith(userVersionService.updateRelationshipsVersion(ownerId))
                .map(result -> result.getT1().wasAcknowledged());
    }

    public Mono<Boolean> isBlocked(@NotNull Long ownerId, @NotNull Long relatedUserId) {
        Query query = new Query()
                .addCriteria(Criteria.where(ID_OWNER_ID).is(ownerId))
                .addCriteria(Criteria.where(ID_RELATED_USER_ID).is(relatedUserId))
                .addCriteria(Criteria.where(UserRelationship.Fields.isBlocked).is(true));
        return mongoTemplate.exists(query, UserRelationship.class);
    }

    public Mono<Boolean> isNotBlocked(@NotNull Long ownerId, @NotNull Long relatedUserId) {
        Query query = new Query()
                .addCriteria(Criteria.where(ID_OWNER_ID).is(ownerId))
                .addCriteria(Criteria.where(ID_RELATED_USER_ID).is(relatedUserId))
                .addCriteria(Criteria.where(UserRelationship.Fields.isBlocked).is(true));
        return mongoTemplate.exists(query, UserRelationship.class)
                .map(isBlocked -> !isBlocked);
    }

    public Mono<Boolean> isRelatedAndAllowed(@NotNull Long ownerId, @NotNull Long relatedUserId) {
        Query query = new Query().addCriteria(Criteria.where(ID_OWNER_ID).is(ownerId))
                .addCriteria(Criteria.where(ID_RELATED_USER_ID).is(relatedUserId))
                .addCriteria(Criteria.where(UserRelationship.Fields.isBlocked).is(false));
        return mongoTemplate.exists(query, UserRelationship.class);
    }

    public Flux<Long> queryRelatedUsersIds(
            @NotNull Long ownerId,
            @Nullable Date lastUpdatedDate) {
        return userVersionService.queryRelationshipsLastUpdatedDate(ownerId)
                .flatMapMany(version -> {
                    if (lastUpdatedDate == null || lastUpdatedDate.before(version)) {
                        Query query = new Query()
                                .addCriteria(Criteria.where(ID_OWNER_ID).is(ownerId));
                        query.fields().include(ID_RELATED_USER_ID);
                        return mongoTemplate.find(query, UserRelationship.class)
                                .map(userRelationship -> userRelationship.getKey().getRelatedUserId());
                    } else {
                        throw TurmsBusinessException.get(TurmsStatusCode.ALREADY_UP_TO_DATE);
                    }
                });
    }

    public Mono<Boolean> upsertOneSidedUserRelationships(
            @NotNull Long ownerId,
            @Nullable Long relatedUserId,
            @Nullable Boolean isBlocked,
            @Nullable Date establishmentDate) {
        if (relatedUserId == null && isBlocked == null && establishmentDate == null) {
            throw TurmsBusinessException.get(TurmsStatusCode.ILLEGAL_ARGUMENTS);
        }
        Query query = QueryBuilder.newBuilder()
                .addIsIfNotNull(ID_OWNER_ID, ownerId)
                .addIsIfNotNull(ID_RELATED_USER_ID, relatedUserId)
                .buildQuery();
        Update update = UpdateBuilder.newBuilder()
                .setIfNotNull(UserRelationship.Fields.isBlocked, isBlocked)
                .setIfNotNull(UserRelationship.Fields.establishmentDate, establishmentDate)
                .build();
        return mongoTemplate.upsert(query, update, UserRelationship.class)
                .zipWith(userVersionService.updateRelationshipsVersion(ownerId))
                .map(result -> result.getT1().wasAcknowledged());
    }

    public Mono<Boolean> updateUserOneSidedRelationships(
            @NotNull Long ownerId,
            @Nullable Set<Long> relatedUsersIds,
            @Nullable Boolean isBlocked,
            @Nullable Date establishmentDate) {
        if (relatedUsersIds == null && isBlocked == null && establishmentDate == null) {
            throw TurmsBusinessException.get(TurmsStatusCode.ILLEGAL_ARGUMENTS);
        }
        Query query = QueryBuilder.newBuilder()
                .add(Criteria.where(ID_OWNER_ID).is(ownerId))
                .addInIfNotNull(ID_RELATED_USER_ID, relatedUsersIds)
                .buildQuery();
        Update update = UpdateBuilder.newBuilder()
                .setIfNotNull(UserRelationship.Fields.isBlocked, isBlocked)
                .setIfNotNull(UserRelationship.Fields.establishmentDate, establishmentDate)
                .build();
        return mongoTemplate.updateMulti(query, update, UserRelationship.class)
                .zipWith(userVersionService.updateRelationshipsVersion(ownerId))
                .map(result -> result.getT1().wasAcknowledged());
    }

    public Mono<Boolean> areStrangersOrAllowed(
            @NotNull Long userOneId,
            @NotNull Long userTwoId) {
        Query queryOne = new Query()
                .addCriteria(Criteria.where(ID_OWNER_ID).is(userOneId))
                .addCriteria(Criteria.where(ID_RELATED_USER_ID).is(userTwoId))
                .addCriteria(Criteria.where(UserRelationship.Fields.isBlocked).is(true));
        Query queryTwo = new Query()
                .addCriteria(Criteria.where(ID_RELATED_USER_ID).is(userOneId))
                .addCriteria(Criteria.where(ID_OWNER_ID).is(userTwoId))
                .addCriteria(Criteria.where(UserRelationship.Fields.isBlocked).is(true));
        return Mono.zip(
                mongoTemplate.exists(queryOne, UserRelationship.class),
                mongoTemplate.exists(queryTwo, UserRelationship.class))
                .map(results -> !results.getT1() && !results.getT2())
                .defaultIfEmpty(false);
    }

    /**
     * For user one, check if user two is a stranger
     */
    public Mono<Boolean> isStranger(
            @NotNull Long userOneId,
            @NotNull Long userTwoId) {
        Query query = new Query()
                .addCriteria(Criteria.where(ID_OWNER_ID).is(userTwoId))
                .addCriteria(Criteria.where(ID_RELATED_USER_ID).is(userOneId))
                .addCriteria(Criteria.where(UserRelationship.Fields.isBlocked).is(null));
        return mongoTemplate.exists(query, UserRelationship.class);
    }

    public Mono<Boolean> hasOneSidedRelationship(
            @NotNull Long ownerId,
            @NotNull Long relatedUserId) {
        Query query = new Query()
                .addCriteria(Criteria.where(ID_OWNER_ID).is(ownerId))
                .addCriteria(Criteria.where(ID_RELATED_USER_ID).is(relatedUserId));
        return mongoTemplate.exists(query, UserRelationship.class);
    }

    public Mono<Boolean> hasTwoSidedRelationship(
            @NotNull Long userOne,
            @NotNull Long userTwo) {
        return Mono.zip(hasOneSidedRelationship(userOne, userTwo),
                hasOneSidedRelationship(userTwo, userOne))
                .map(results -> results.getT1() && results.getT2());
    }
}