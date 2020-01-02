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
import com.mongodb.client.result.DeleteResult;
import im.turms.turms.annotation.constraint.UserRelationshipKeyConstraint;
import im.turms.turms.common.*;
import im.turms.turms.exception.TurmsBusinessException;
import im.turms.turms.pojo.bo.common.DateRange;
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
import org.springframework.validation.annotation.Validated;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.annotation.Nullable;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.PastOrPresent;
import java.util.*;
import java.util.stream.Collectors;

import static im.turms.turms.common.Constants.*;

@Service
@Validated
public class UserRelationshipService {
    private final UserVersionService userVersionService;
    private final UserRelationshipGroupService userRelationshipGroupService;
    private final ReactiveMongoTemplate mongoTemplate;

    public UserRelationshipService(UserVersionService userVersionService, ReactiveMongoTemplate mongoTemplate, UserRelationshipGroupService userRelationshipGroupService) {
        this.userVersionService = userVersionService;
        this.mongoTemplate = mongoTemplate;
        this.userRelationshipGroupService = userRelationshipGroupService;
    }

    public Mono<Boolean> deleteOneSidedRelationships(
            @NotNull Long ownerId,
            @NotEmpty Set<Long> relatedUsersIds) {
        return mongoTemplate.inTransaction()
                .execute(operations -> {
                    Query query = new Query()
                            .addCriteria(Criteria.where(ID_OWNER_ID).is(ownerId))
                            .addCriteria(Criteria.where(ID_RELATED_USER_ID).in(relatedUsersIds));
                    return operations.remove(query, UserRelationship.class)
                            .flatMap(result -> {
                                if (result.wasAcknowledged()) {
                                    return userRelationshipGroupService.deleteRelatedUsersFromAllRelationshipGroups(ownerId, relatedUsersIds, operations, true)
                                            .then(userVersionService.updateRelationshipsVersion(ownerId, null))
                                            .thenReturn(true);
                                } else {
                                    return Mono.just(false);
                                }
                            });
                })
                .retryBackoff(MONGO_TRANSACTION_RETRIES_NUMBER, MONGO_TRANSACTION_BACKOFF)
                .single();
    }

    public Mono<Boolean> deleteAllRelationships(
            @NotEmpty Set<Long> usersIds,
            @Nullable ReactiveMongoOperations operations,
            boolean updateVersion) {
        Query query = new Query()
                .addCriteria(new Criteria().orOperator(
                        Criteria.where(ID_OWNER_ID).in(usersIds),
                        Criteria.where(ID_RELATED_USER_ID).in(usersIds)));
        if (updateVersion) {
            if (operations != null) {
                return operations.remove(query, UserRelationship.class)
                        .flatMap(result -> {
                            if (result.wasAcknowledged()) {
                                return userVersionService.updateRelationshipsVersion(usersIds, operations);
                            } else {
                                return Mono.just(false);
                            }
                        });
            } else {
                return mongoTemplate.inTransaction()
                        .execute(newOperations -> newOperations.remove(query, UserRelationship.class)
                                .flatMap(result -> {
                                    if (result.wasAcknowledged()) {
                                        return userVersionService.updateRelationshipsVersion(usersIds, newOperations);
                                    } else {
                                        return Mono.just(false);
                                    }
                                }))
                        .retryBackoff(MONGO_TRANSACTION_RETRIES_NUMBER, MONGO_TRANSACTION_BACKOFF)
                        .single();
            }
        } else {
            ReactiveMongoOperations mongoOperations = operations != null ? operations : mongoTemplate;
            return mongoOperations.remove(query, UserRelationship.class).map(DeleteResult::wasAcknowledged);
        }
    }

    public Mono<Boolean> deleteOneSidedRelationships(@NotEmpty Set<UserRelationship.@UserRelationshipKeyConstraint Key> keys) {
        return MapUtil.fluxMerge(multimap -> {
            for (UserRelationship.Key key : keys) {
                multimap.put(key.getOwnerId(), key.getRelatedUserId());
            }
            return null;
        }, (monos, key, values) -> {
            monos.add(deleteOneSidedRelationships(key, values));
            return null;
        });
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
                            ownerId, relatedUserId, operations, true))
                    .zipWith(userVersionService.updateRelationshipsVersion(ownerId, operations))
                    .thenReturn(true);
        } else {
            return mongoTemplate.inTransaction()
                    .execute(newOperations -> newOperations.remove(query, UserRelationship.class)
                            .zipWith(userRelationshipGroupService.deleteRelatedUserFromAllRelationshipGroups(
                                    ownerId, relatedUserId, newOperations, true))
                            .zipWith(userVersionService.updateRelationshipsVersion(ownerId, newOperations))
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

    private Flux<Long> queryMembersRelatedUsersIds(
            @Nullable Set<Long> ownerIds,
            @Nullable Set<Integer> groupIndexes,
            @Nullable Integer page,
            @Nullable Integer size) {
        Query query = QueryBuilder
                .newBuilder()
                .addInIfNotNull(ID_OWNER_ID, ownerIds)
                .addInIfNotNull(ID_GROUP_INDEX, groupIndexes)
                .paginateIfNotNull(page, size);
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
                        return queryMembersRelatedUsersIds(Set.of(ownerId), Set.of(groupIndex), isBlocked)
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
                        return queryRelationships(
                                Set.of(ownerId),
                                relatedUsersIds,
                                groupIndex != null ? Set.of(groupIndex) : null,
                                isBlocked,
                                null,
                                null,
                                null)
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
            @Nullable Set<Long> ownerIds,
            @Nullable Boolean isBlocked) {
        Query query = QueryBuilder.newBuilder()
                .addInIfNotNull(ID_OWNER_ID, ownerIds)
                .addIsIfNotNull(UserRelationship.Fields.isBlocked, isBlocked)
                .buildQuery();
        query.fields().include(ID_RELATED_USER_ID);
        return mongoTemplate.find(query, UserRelationship.class)
                .map(userRelationship -> userRelationship.getKey().getRelatedUserId());
    }

    public Flux<Long> queryMembersRelatedUsersIds(
            @Nullable Set<Long> ownerIds,
            @Nullable Set<Integer> groupIndexes,
            @Nullable Boolean isBlocked) {
        if (groupIndexes != null && isBlocked != null) {
            return Mono.zip(
                    queryMembersRelatedUsersIds(ownerIds, groupIndexes, null, null).collect(Collectors.toSet()),
                    queryRelatedUsersIds(ownerIds, isBlocked).collect(Collectors.toSet()))
                    .flatMapIterable(tuple -> {
                        tuple.getT1().retainAll(tuple.getT2());
                        return tuple.getT1();
                    });
        } else if (groupIndexes != null) {
            return queryMembersRelatedUsersIds(ownerIds, groupIndexes, null, null);
        } else {
            return queryRelatedUsersIds(ownerIds, isBlocked);
        }
    }

    private Flux<UserRelationship> queryRelationships(
            @Nullable Set<Long> ownerIds,
            @Nullable Set<Long> relatedUsersIds,
            @Nullable Boolean isBlocked,
            @Nullable DateRange establishmentDateRange,
            @Nullable Integer page,
            @Nullable Integer size) {
        Query query = QueryBuilder.newBuilder()
                .addInIfNotNull(ID_OWNER_ID, ownerIds)
                .addInIfNotNull(ID_RELATED_USER_ID, relatedUsersIds)
                .addBetweenIfNotNull(UserRelationship.Fields.establishmentDate, establishmentDateRange)
                .addIsIfNotNull(UserRelationship.Fields.isBlocked, isBlocked)
                .paginateIfNotNull(page, size);
        return mongoTemplate.find(query, UserRelationship.class);
    }

    public Flux<UserRelationship> queryRelationships(
            @Nullable Set<Long> ownerIds,
            @Nullable Set<Long> relatedUsersIds,
            @Nullable Set<Integer> groupIndexes,
            @Nullable Boolean isBlocked,
            @Nullable DateRange establishmentDateRange,
            @Nullable Integer page,
            @Nullable Integer size) {
        boolean queryByGroupIndexes = groupIndexes != null;
        boolean queryByRelationshipInfo = relatedUsersIds != null || isBlocked != null || establishmentDateRange != null;
        if (queryByGroupIndexes && queryByRelationshipInfo) {
            if (relatedUsersIds != null && relatedUsersIds.isEmpty()) {
                return Flux.empty();
            }
            return queryMembersRelatedUsersIds(ownerIds, groupIndexes, null, null)
                    .collect(Collectors.toSet())
                    .flatMapMany(usersIds -> {
                        if (relatedUsersIds != null) {
                            usersIds.retainAll(relatedUsersIds);
                        }
                        return queryRelationships(ownerIds, usersIds, isBlocked, establishmentDateRange, page, size);
                    });
        } else if (queryByGroupIndexes) {
            return queryMembersRelationships(ownerIds, groupIndexes, page, size);
        } else {
            return queryRelationships(ownerIds, relatedUsersIds, isBlocked, establishmentDateRange, page, size);
        }
    }

    public Flux<UserRelationship> queryMembersRelationships(
            @Nullable Set<Long> ownerIds,
            @Nullable Set<Integer> groupIndexes,
            @Nullable Integer page,
            @Nullable Integer size) {
        return queryMembersRelatedUsersIds(ownerIds, groupIndexes, null)
                .collect(Collectors.toSet())
                .flatMapMany(relatedUsersIds -> {
                    if (relatedUsersIds.isEmpty()) {
                        return Flux.empty();
                    }
                    Query query = QueryBuilder
                            .newBuilder()
                            .addInIfNotNull(ID_OWNER_ID, ownerIds)
                            .addInIfNotNull(ID_RELATED_USER_ID, relatedUsersIds)
                            .paginateIfNotNull(page, size);
                    return mongoTemplate.find(query, UserRelationship.class);
                });
    }

    public Mono<Long> countRelationships(
            @Nullable Set<Long> ownerIds,
            @Nullable Set<Long> relatedUsersIds,
            @Nullable Set<Integer> groupIndexes,
            @Nullable Boolean isBlocked) {
        boolean queryByGroupIndexes = groupIndexes != null;
        boolean queryByRelationshipInfo = relatedUsersIds != null || isBlocked != null;
        if (queryByGroupIndexes && queryByRelationshipInfo) {
            if (relatedUsersIds != null && relatedUsersIds.isEmpty()) {
                return Mono.just(0L);
            }
            return queryMembersRelatedUsersIds(ownerIds, groupIndexes, null, null)
                    .collect(Collectors.toSet())
                    .flatMap(usersIds -> {
                        if (relatedUsersIds != null) {
                            usersIds.retainAll(relatedUsersIds);
                        }
                        return countRelationships(ownerIds, usersIds, isBlocked);
                    });
        } else if (queryByGroupIndexes) {
            return countMembersRelationships(ownerIds, groupIndexes);
        } else {
            return countRelationships(ownerIds, relatedUsersIds, isBlocked);
        }
    }

    private Mono<Long> countMembersRelationships(
            @Nullable Set<Long> ownerIds,
            @Nullable Set<Integer> groupIndexes) {
        Query query = QueryBuilder
                .newBuilder()
                .addInIfNotNull(ID_OWNER_ID, ownerIds)
                .addInIfNotNull(ID_GROUP_INDEX, groupIndexes)
                .buildQuery();
        return mongoTemplate.count(query, UserRelationshipGroupMember.class);
    }

    public Mono<Long> countRelationships(
            @Nullable Set<Long> ownerIds,
            @Nullable Set<Long> relatedUsersIds,
            @Nullable Boolean isBlocked) {
        Query query = QueryBuilder
                .newBuilder()
                .addInIfNotNull(ID_OWNER_ID, ownerIds)
                .addInIfNotNull(ID_RELATED_USER_ID, relatedUsersIds)
                .addIsIfNotNull(UserRelationship.Fields.isBlocked, isBlocked)
                .buildQuery();
        return mongoTemplate.count(query, UserRelationship.class);
    }

    public Mono<Boolean> friendTwoUsers(
            @NotNull Long userOneId,
            @NotNull Long userTwoId,
            @Nullable ReactiveMongoOperations operations) {
        if (userOneId.equals(userTwoId)) {
            throw TurmsBusinessException.get(TurmsStatusCode.ILLEGAL_ARGUMENTS);
        }
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

    public Mono<Boolean> upsertOneSidedRelationship(
            @NotNull Long ownerId,
            @NotNull Long relatedUserId,
            @Nullable Boolean isBlocked,
            @Nullable Integer newGroupIndex,
            @Nullable Integer deleteGroupIndex,
            @Nullable @PastOrPresent Date establishmentDate,
            boolean upsert,
            @Nullable ReactiveMongoOperations operations) {
        if (ownerId.equals(relatedUserId)) {
            throw TurmsBusinessException.get(TurmsStatusCode.ILLEGAL_ARGUMENTS);
        }
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

    public Mono<Boolean> updateUserOneSidedRelationships(
            @NotEmpty Set<UserRelationship.@UserRelationshipKeyConstraint Key> keys,
            @Nullable Long newOwnerId,
            @Nullable Boolean isBlocked,
            @Nullable @PastOrPresent Date establishmentDate) {
        if (newOwnerId != null) {
            for (UserRelationship.Key key : keys) {
                if (newOwnerId.equals(key.getRelatedUserId())) {
                    throw TurmsBusinessException.get(TurmsStatusCode.ILLEGAL_ARGUMENTS);
                }
            }
        }
        return MapUtil.fluxMerge(map -> {
            for (UserRelationship.Key key : keys) {
                map.put(key.getOwnerId(), key.getRelatedUserId());
            }
            return null;
        }, (monos, key, values) -> {
            monos.add(updateUserOneSidedRelationships(key, values, newOwnerId, isBlocked, establishmentDate));
            return null;
        });
    }

    public Mono<Boolean> updateUserOneSidedRelationships(
            @NotNull Long ownerId,
            @NotEmpty Set<Long> relatedUsersIds,
            @Nullable Long newOwnerId,
            @Nullable Boolean isBlocked,
            @Nullable @PastOrPresent Date establishmentDate) {
        Validator.throwIfAllNull(newOwnerId, isBlocked, establishmentDate);
        if (newOwnerId != null) {
            for (Long relatedUsersId : relatedUsersIds) {
                if (newOwnerId.equals(relatedUsersId)) {
                    throw TurmsBusinessException.get(TurmsStatusCode.ILLEGAL_ARGUMENTS);
                }
            }
        }
        Query query = new Query()
                .addCriteria(Criteria.where(ID_OWNER_ID).is(ownerId))
                .addCriteria(Criteria.where(ID_RELATED_USER_ID).in(relatedUsersIds));
        Update update = UpdateBuilder.newBuilder()
                .setIfNotNull(ID_OWNER_ID, newOwnerId)
                .setIfNotNull(UserRelationship.Fields.isBlocked, isBlocked)
                .setIfNotNull(UserRelationship.Fields.establishmentDate, establishmentDate)
                .build();
        return mongoTemplate.updateMulti(query, update, UserRelationship.class)
                .zipWith(userVersionService.updateRelationshipsVersion(ownerId, null))
                .map(result -> result.getT1().wasAcknowledged());
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
}