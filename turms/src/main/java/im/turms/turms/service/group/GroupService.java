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
import im.turms.common.constant.GroupUpdateStrategy;
import im.turms.common.exception.TurmsBusinessException;
import im.turms.common.model.bo.common.Int64ValuesWithVersion;
import im.turms.common.model.bo.group.GroupsWithVersion;
import im.turms.common.util.Validator;
import im.turms.turms.builder.QueryBuilder;
import im.turms.turms.builder.UpdateBuilder;
import im.turms.turms.constant.Common;
import im.turms.turms.manager.TurmsClusterManager;
import im.turms.turms.pojo.bo.DateRange;
import im.turms.turms.pojo.domain.Group;
import im.turms.turms.pojo.domain.GroupMember;
import im.turms.turms.pojo.domain.GroupType;
import im.turms.turms.pojo.domain.UserPermissionGroup;
import im.turms.turms.service.user.UserPermissionGroupService;
import im.turms.turms.service.user.UserVersionService;
import im.turms.turms.util.ProtoUtil;
import org.springframework.beans.factory.annotation.Qualifier;
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
import javax.validation.constraints.Min;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.PastOrPresent;
import java.util.*;
import java.util.stream.Collectors;

import static im.turms.turms.constant.Common.*;
import static im.turms.turms.pojo.domain.GroupMember.Fields.ID_GROUP_ID;
import static im.turms.turms.pojo.domain.GroupMember.Fields.ID_USER_ID;

@Service
@Validated
public class GroupService {
    private final ReactiveMongoTemplate mongoTemplate;
    private final GroupTypeService groupTypeService;
    private final GroupMemberService groupMemberService;
    private final UserVersionService userVersionService;
    private final TurmsClusterManager turmsClusterManager;
    private final GroupVersionService groupVersionService;
    private final UserPermissionGroupService userPermissionGroupService;

    public GroupService(
            GroupMemberService groupMemberService,
            TurmsClusterManager turmsClusterManager,
            GroupTypeService groupTypeService,
            @Qualifier("groupMongoTemplate") ReactiveMongoTemplate mongoTemplate,
            UserVersionService userVersionService,
            GroupVersionService groupVersionService,
            UserPermissionGroupService userPermissionGroupService) {
        this.groupMemberService = groupMemberService;
        this.turmsClusterManager = turmsClusterManager;
        this.groupTypeService = groupTypeService;
        this.mongoTemplate = mongoTemplate;
        this.userVersionService = userVersionService;
        this.groupVersionService = groupVersionService;
        this.userPermissionGroupService = userPermissionGroupService;
    }

    public Mono<Group> createGroup(
            @NotNull Long creatorId,
            @NotNull Long ownerId,
            @Nullable String groupName,
            @Nullable String intro,
            @Nullable String announcement,
            @Nullable @Min(value = 0) Integer minimumScore,
            @Nullable Long groupTypeId,
            @Nullable @PastOrPresent Date creationDate,
            @Nullable @PastOrPresent Date deletionDate,
            @Nullable Date muteEndDate,
            @Nullable Boolean isActive) {
        isActive = isActive != null ? isActive : turmsClusterManager.getTurmsProperties().getGroup().isActivateGroupWhenCreated();
        Long groupId = turmsClusterManager.generateRandomId();
        Group group = new Group(groupId, groupTypeId, creatorId, ownerId, groupName, intro,
                announcement, minimumScore, creationDate, deletionDate, muteEndDate, isActive);
        return mongoTemplate
                .inTransaction()
                .execute(operations -> {
                    Date now = new Date();
                    return operations.insert(group)
                            .zipWith(groupMemberService.addGroupMember(
                                    group.getId(),
                                    creatorId,
                                    GroupMemberRole.OWNER,
                                    null,
                                    now,
                                    null,
                                    operations))
                            .flatMap(results -> groupVersionService.upsert(groupId, now)
                                    .thenReturn(results.getT1()));
                })
                .retryWhen(TRANSACTION_RETRY)
                .singleOrEmpty();
    }

    public Mono<Group> authAndCreateGroup(
            @NotNull Long creatorId,
            @NotNull Long ownerId,
            @Nullable String groupName,
            @Nullable String intro,
            @Nullable String announcement,
            @Nullable @Min(value = 0) Integer minimumScore,
            @Nullable Long groupTypeId,
            @Nullable @PastOrPresent Date creationDate,
            @Nullable @PastOrPresent Date deletionDate,
            @Nullable Date muteEndDate,
            @Nullable Boolean isActive) {
        if (creationDate != null && deletionDate != null && deletionDate.before(creationDate)) {
            throw TurmsBusinessException.get(TurmsStatusCode.ILLEGAL_ARGUMENTS, "The deletion date must not be before the creation date");
        }
        if (groupTypeId == null) {
            groupTypeId = DEFAULT_GROUP_TYPE_ID;
        }
        Long finalGroupTypeId = groupTypeId;
        return isAllowedToCreateGroupAndHaveGroupType(creatorId, groupTypeId)
                .flatMap(allowed -> {
                    if (allowed != null && allowed) {
                        return createGroup(creatorId,
                                ownerId,
                                groupName,
                                intro,
                                announcement,
                                minimumScore,
                                finalGroupTypeId,
                                creationDate,
                                deletionDate,
                                muteEndDate,
                                isActive);
                    } else {
                        return Mono.error(TurmsBusinessException.get(TurmsStatusCode.OWNED_RESOURCE_LIMIT_REACHED));
                    }
                });
    }

    public Mono<Boolean> deleteGroupsAndGroupMembers(
            @Nullable Set<Long> groupIds,
            @Nullable Boolean shouldDeleteLogically) {
        if (shouldDeleteLogically == null) {
            shouldDeleteLogically = turmsClusterManager.getTurmsProperties()
                    .getGroup().isDeleteGroupLogicallyByDefault();
        }
        boolean finalShouldDeleteLogically = shouldDeleteLogically;
        return mongoTemplate.inTransaction()
                .execute(operations -> {
                    Query query = QueryBuilder
                            .newBuilder()
                            .addInIfNotNull(ID, groupIds)
                            .buildQuery();
                    Mono<Boolean> updateOrRemoveMono;
                    if (finalShouldDeleteLogically) {
                        Update update = new Update().set(Group.Fields.DELETION_DATE, new Date());
                        updateOrRemoveMono = operations.updateMulti(query, update, Group.class)
                                .map(UpdateResult::wasAcknowledged);
                    } else {
                        updateOrRemoveMono = operations.remove(query, Group.class)
                                .map(DeleteResult::wasAcknowledged);
                    }
                    return updateOrRemoveMono.flatMap(acknowledged -> {
                        if (acknowledged != null && acknowledged) {
                            return groupMemberService.deleteAllGroupMembers(groupIds, operations, false)
                                    .then(groupVersionService.delete(groupIds, operations));
                        } else {
                            return Mono.just(false);
                        }
                    });
                })
                .retryWhen(TRANSACTION_RETRY)
                .singleOrEmpty();
    }

    public Flux<Group> queryGroups(
            @Nullable Set<Long> ids,
            @Nullable Set<Long> typeIds,
            @Nullable Set<Long> creatorIds,
            @Nullable Set<Long> ownerIds,
            @Nullable Boolean isActive,
            @Nullable DateRange creationDateRange,
            @Nullable DateRange deletionDateRange,
            @Nullable DateRange muteEndDateRange,
            @Nullable Set<Long> memberIds,
            @Nullable Integer page,
            @Nullable Integer size) {
        return getGroupIdsFromGroupIdsAndMemberIds(ids, memberIds)
                .switchIfEmpty(Common.emptySetMono())
                .flatMapMany(groupIds -> {
                    Query query = QueryBuilder
                            .newBuilder()
                            .addInIfNotNull(ID, groupIds)
                            .addInIfNotNull(Group.Fields.TYPE_ID, typeIds)
                            .addInIfNotNull(Group.Fields.CREATOR_ID, creatorIds)
                            .addInIfNotNull(Group.Fields.OWNER_ID, ownerIds)
                            .addIsIfNotNull(Group.Fields.IS_ACTIVE, isActive)
                            .addBetweenIfNotNull(Group.Fields.CREATION_DATE, creationDateRange)
                            .addBetweenIfNotNull(Group.Fields.DELETION_DATE, deletionDateRange)
                            .addBetweenIfNotNull(Group.Fields.MUTE_END_DATE, muteEndDateRange)
                            .paginateIfNotNull(page, size);
                    return mongoTemplate.find(query, Group.class);
                });
    }

    public Mono<Long> queryGroupTypeId(@NotNull Long groupId) {
        Query query = new Query().addCriteria(Criteria.where(ID).is(groupId));
        return mongoTemplate.findOne(query, Group.class)
                .map(Group::getTypeId);
    }

    public Mono<Integer> queryGroupMinimumScore(@NotNull Long groupId) {
        Query query = new Query().addCriteria(Criteria.where(ID).is(groupId));
        return mongoTemplate.findOne(query, Group.class)
                .map(Group::getMinimumScore);
    }

    public Mono<Boolean> authAndTransferGroupOwnership(
            @NotNull Long requesterId,
            @NotNull Long groupId,
            @NotNull Long successorId,
            boolean quitAfterTransfer,
            @Nullable ReactiveMongoOperations operations) {
        return groupMemberService
                .isOwner(requesterId, groupId)
                .flatMap(isOwner -> {
                    if (isOwner != null && isOwner) {
                        return checkAndTransferGroupOwnership(requesterId, groupId, successorId, quitAfterTransfer, operations);
                    } else {
                        return Mono.error(TurmsBusinessException.get(TurmsStatusCode.UNAUTHORIZED));
                    }
                });
    }

    public Mono<Long> queryGroupOwnerId(@NotNull Long groupId) {
        Query query = new Query().addCriteria(Criteria.where(ID).is(groupId));
        query.fields().include(Group.Fields.OWNER_ID);
        return mongoTemplate.findOne(query, Group.class)
                .map(Group::getOwnerId);
    }

    private Mono<Boolean> checkAndTransferGroupOwnership(
            @Nullable Long helperCurrentOwnerId,
            @NotNull Long groupId,
            @NotNull Long successorId,
            boolean quitAfterTransfer,
            @Nullable ReactiveMongoOperations operations) {
        Mono<Long> queryOwnerIdMono;
        if (helperCurrentOwnerId != null) {
            queryOwnerIdMono = Mono.just(helperCurrentOwnerId);
        } else {
            queryOwnerIdMono = queryGroupOwnerId(groupId);
        }
        return queryOwnerIdMono.flatMap(ownerId -> groupMemberService
                .isGroupMember(groupId, successorId)
                .flatMap(isGroupMember -> {
                    if (isGroupMember == null || !isGroupMember) {
                        return Mono.error(TurmsBusinessException.get(TurmsStatusCode.SUCCESSOR_NOT_GROUP_MEMBER));
                    }
                    return queryGroupTypeId(groupId);
                })
                .flatMap(groupTypeId ->
                        isAllowedToCreateGroupAndHaveGroupType(successorId, groupTypeId)
                                .flatMap(allowed -> {
                                    if (allowed == null || !allowed) {
                                        return Mono.error(TurmsBusinessException
                                                .get(TurmsStatusCode.OWNED_RESOURCE_LIMIT_REACHED));
                                    }
                                    Mono<Boolean> deleteOrUpdateOwnerMono;
                                    if (quitAfterTransfer) {
                                        deleteOrUpdateOwnerMono = groupMemberService.deleteGroupMembers(
                                                groupId, Set.of(ownerId), operations, false);
                                    } else {
                                        deleteOrUpdateOwnerMono = groupMemberService.updateGroupMember(
                                                groupId,
                                                ownerId,
                                                null,
                                                GroupMemberRole.MEMBER,
                                                null,
                                                null,
                                                operations,
                                                false);
                                    }
                                    Mono<Boolean> update = groupMemberService.updateGroupMember(
                                            groupId,
                                            successorId,
                                            null,
                                            GroupMemberRole.OWNER,
                                            null,
                                            null,
                                            operations,
                                            true);
                                    return Mono.zip(deleteOrUpdateOwnerMono, update)
                                            .map(results -> results.getT1() && results.getT2());
                                })));
    }

    public Mono<GroupType> queryGroupType(@NotNull Long groupId) {
        Query query = new Query().addCriteria(Criteria.where(ID).is(groupId));
        query.fields().include(Group.Fields.TYPE_ID);
        return mongoTemplate.findOne(query, Group.class)
                .flatMap(group -> groupTypeService.queryGroupType(group.getTypeId()));
    }

    public Mono<Boolean> updateGroupsInformation(
            @NotEmpty Set<Long> groupIds,
            @Nullable Long typeId,
            @Nullable Long creatorId,
            @Nullable Long ownerId,
            @Nullable String name,
            @Nullable String intro,
            @Nullable String announcement,
            @Nullable @Min(0) Integer minimumScore,
            @Nullable Boolean isActive,
            @Nullable @PastOrPresent Date creationDate,
            @Nullable @PastOrPresent Date deletionDate,
            @Nullable Date muteEndDate,
            @Nullable ReactiveMongoOperations operations) {
        if (Validator.areAllNull(typeId, creatorId, ownerId, name, intro, announcement,
                minimumScore, isActive, creationDate, deletionDate, muteEndDate)) {
            return Mono.just(true);
        }
        Query query = new Query().addCriteria(Criteria.where(ID).in(groupIds));
        Update update = UpdateBuilder.newBuilder()
                .setIfNotNull(Group.Fields.TYPE_ID, typeId)
                .setIfNotNull(Group.Fields.CREATOR_ID, creatorId)
                .setIfNotNull(Group.Fields.OWNER_ID, ownerId)
                .setIfNotNull(Group.Fields.NAME, name)
                .setIfNotNull(Group.Fields.INTRO, intro)
                .setIfNotNull(Group.Fields.ANNOUNCEMENT, announcement)
                .setIfNotNull(Group.Fields.MINIMUM_SCORE, minimumScore)
                .setIfNotNull(Group.Fields.IS_ACTIVE, isActive)
                .setIfNotNull(Group.Fields.CREATION_DATE, creationDate)
                .setIfNotNull(Group.Fields.DELETION_DATE, deletionDate)
                .setIfNotNull(Group.Fields.MUTE_END_DATE, muteEndDate)
                .build();
        ReactiveMongoOperations mongoOperations = operations != null ? operations : mongoTemplate;
        return mongoOperations.updateMulti(query, update, Group.class)
                .flatMap(result -> {
                    if (result.wasAcknowledged()) {
                        ArrayList<Mono<Boolean>> list = new ArrayList<>(groupIds.size());
                        for (Long groupId : groupIds) {
                            list.add(groupVersionService.updateInformation(groupId));
                        }
                        return Flux.merge(list).then(Mono.just(true));
                    } else {
                        return Mono.just(false);
                    }
                });
    }

    public Mono<Boolean> authAndUpdateGroupInformation(
            @Nullable Long requesterId,
            @NotNull Long groupId,
            @Nullable Long typeId,
            @Nullable Long creatorId,
            @Nullable Long ownerId,
            @Nullable String name,
            @Nullable String intro,
            @Nullable String announcement,
            @Nullable @Min(0) Integer minimumScore,
            @Nullable Boolean isActive,
            @Nullable @PastOrPresent Date creationDate,
            @Nullable @PastOrPresent Date deletionDate,
            @Nullable Date muteEndDate,
            @Nullable ReactiveMongoOperations operations) {
        if (Validator.areAllNull(typeId, creatorId, ownerId, name, intro, announcement,
                minimumScore, isActive, creationDate, deletionDate, muteEndDate)) {
            return Mono.just(true);
        }
        return queryGroupType(groupId)
                .flatMap(groupType -> {
                    GroupUpdateStrategy groupUpdateStrategy = groupType.getGroupInfoUpdateStrategy();
                    if (groupUpdateStrategy == GroupUpdateStrategy.ALL) {
                        return Mono.just(true);
                    } else {
                        switch (groupUpdateStrategy) {
                            case OWNER:
                                return groupMemberService.isOwner(requesterId, groupId);
                            case OWNER_MANAGER:
                                return groupMemberService.isOwnerOrManager(requesterId, groupId);
                            case OWNER_MANAGER_MEMBER:
                                return groupMemberService.isOwnerOrManagerOrMember(requesterId, groupId);
                            default:
                                return Mono.just(false);
                        }
                    }
                })
                .flatMap(authenticated -> {
                    if (authenticated != null && authenticated) {
                        return updateGroupsInformation(Set.of(groupId), typeId, creatorId, ownerId, name, intro,
                                announcement, minimumScore, isActive, creationDate, deletionDate, muteEndDate, operations);
                    } else {
                        return Mono.error(TurmsBusinessException.get(TurmsStatusCode.UNAUTHORIZED));
                    }
                });
    }

    private Mono<Long> countUserOwnedGroupNumber(@NotNull Long ownerId) {
        Query query = new Query().addCriteria(Criteria.where(Group.Fields.OWNER_ID).is(ownerId));
        return mongoTemplate.count(query, Group.class);
    }

    public Mono<GroupsWithVersion> queryGroupWithVersion(
            @NotNull Long groupId,
            @Nullable Date lastUpdatedDate) {
        return groupVersionService.queryInfoVersion(groupId)
                .flatMap(version -> {
                    if (lastUpdatedDate == null || lastUpdatedDate.before(version)) {
                        return mongoTemplate.findById(groupId, Group.class)
                                .map(group -> GroupsWithVersion.newBuilder()
                                        .addGroups(ProtoUtil.group2proto(group))
                                        .setLastUpdatedDate(Int64Value.newBuilder().setValue(version.getTime()).build())
                                        .build());
                    } else {
                        return Mono.error(TurmsBusinessException.get(TurmsStatusCode.ALREADY_UP_TO_DATE));
                    }
                })
                .switchIfEmpty(Mono.error(TurmsBusinessException.get(TurmsStatusCode.ALREADY_UP_TO_DATE)));
    }

    private Flux<Group> queryGroups(@NotEmpty List<Long> groupsIds) {
        Query query = new Query().addCriteria(Criteria.where(ID).in(groupsIds));
        return mongoTemplate.find(query, Group.class);
    }

    public Flux<Long> queryJoinedGroupsIds(@NotNull Long memberId) {
        Query query = new Query().addCriteria(Criteria.where(ID_USER_ID).is(memberId));
        query.fields().include(ID_GROUP_ID);
        return mongoTemplate
                .find(query, GroupMember.class)
                .map(groupMember -> groupMember.getKey().getGroupId());
    }

    public Flux<Group> queryJoinedGroups(@NotNull Long memberId) {
        return queryJoinedGroupsIds(memberId)
                .collectList()
                .flatMapMany(groupIds -> groupIds.isEmpty()
                        ? Flux.empty()
                        : this.queryGroups(groupIds));
    }

    public Mono<Int64ValuesWithVersion> queryJoinedGroupsIdsWithVersion(
            @NotNull Long memberId,
            @Nullable Date lastUpdatedDate) {
        return userVersionService
                .queryJoinedGroupVersion(memberId)
                .flatMap(version -> {
                    if (lastUpdatedDate == null || lastUpdatedDate.before(version)) {
                        return queryJoinedGroupsIds(memberId)
                                .collectList()
                                .map(ids -> {
                                    if (ids.isEmpty()) {
                                        throw TurmsBusinessException.get(TurmsStatusCode.NO_CONTENT);
                                    }
                                    return Int64ValuesWithVersion
                                            .newBuilder()
                                            .addAllValues(ids)
                                            .setLastUpdatedDate(Int64Value.newBuilder().setValue(version.getTime()).build())
                                            .build();
                                });
                    } else {
                        return Mono.error(TurmsBusinessException.get(TurmsStatusCode.ALREADY_UP_TO_DATE));
                    }
                })
                .switchIfEmpty(Mono.error(TurmsBusinessException.get(TurmsStatusCode.ALREADY_UP_TO_DATE)));
    }

    public Mono<GroupsWithVersion> queryJoinedGroupsWithVersion(
            @NotNull Long memberId,
            @Nullable Date lastUpdatedDate) {
        return userVersionService
                .queryJoinedGroupVersion(memberId)
                .flatMap(version -> {
                    if (lastUpdatedDate == null || lastUpdatedDate.before(version)) {
                        return queryJoinedGroups(memberId)
                                .collectList()
                                .map(groups -> {
                                    if (groups.isEmpty()) {
                                        throw TurmsBusinessException.get(TurmsStatusCode.NO_CONTENT);
                                    }
                                    GroupsWithVersion.Builder builder = GroupsWithVersion.newBuilder();
                                    for (Group group : groups) {
                                        builder.addGroups(ProtoUtil.group2proto(group));
                                    }
                                    return builder
                                            .setLastUpdatedDate(Int64Value.newBuilder().setValue(version.getTime()).build())
                                            .build();
                                });
                    } else {
                        return Mono.error(TurmsBusinessException.get(TurmsStatusCode.ALREADY_UP_TO_DATE));
                    }
                })
                .switchIfEmpty(Mono.error(TurmsBusinessException.get(TurmsStatusCode.ALREADY_UP_TO_DATE)));
    }

    public Mono<Boolean> updateGroups(
            @NotEmpty Set<Long> groupIds,
            @Nullable Long typeId,
            @Nullable Long creatorId,
            @Nullable Long ownerId,
            @Nullable String name,
            @Nullable String intro,
            @Nullable String announcement,
            @Nullable @Min(0) Integer minimumScore,
            @Nullable Boolean isActive,
            @Nullable @PastOrPresent Date creationDate,
            @Nullable @PastOrPresent Date deletionDate,
            @Nullable Date muteEndDate,
            @Nullable Long successorId,
            @NotNull Boolean quitAfterTransfer) {
        if (Validator.areAllNull(
                typeId,
                creatorId,
                ownerId,
                name,
                intro,
                announcement,
                minimumScore,
                isActive,
                creationDate,
                deletionDate,
                muteEndDate,
                successorId)) {
            return Mono.just(true);
        }
        return mongoTemplate
                .inTransaction()
                .execute(operations -> {
                    List<Mono<Boolean>> monos = new LinkedList<>();
                    if (successorId != null) {
                        for (Long groupId : groupIds) {
                            Mono<Boolean> transferMono = checkAndTransferGroupOwnership(
                                    null, groupId, successorId, quitAfterTransfer, operations);
                            monos.add(transferMono);
                        }
                    }
                    if (!Validator.areAllNull(typeId, creatorId, ownerId, name, intro, announcement,
                            minimumScore, isActive, creationDate, deletionDate, muteEndDate)) {
                        monos.add(updateGroupsInformation(
                                groupIds, typeId, creatorId, ownerId, name, intro, announcement,
                                minimumScore, isActive, creationDate, deletionDate, muteEndDate,
                                operations));
                    }
                    if (monos.isEmpty()) {
                        return Mono.just(true);
                    } else {
                        return Mono.when(monos).thenReturn(true);
                    }
                })
                .retryWhen(TRANSACTION_RETRY)
                .singleOrEmpty();
    }

    public Mono<Boolean> authAndUpdateGroup(
            @NotNull Long requesterId,
            @NotNull Long groupId,
            @Nullable Long typeId,
            @Nullable Long creatorId,
            @Nullable Long ownerId,
            @Nullable String name,
            @Nullable String intro,
            @Nullable String announcement,
            @Nullable @Min(0) Integer minimumScore,
            @Nullable Boolean isActive,
            @Nullable @PastOrPresent Date creationDate,
            @Nullable @PastOrPresent Date deletionDate,
            @Nullable Date muteEndDate,
            @Nullable Long successorId,
            boolean quitAfterTransfer) {
        Mono<Boolean> authorizeMono = Mono.just(true);
        if (successorId != null || typeId != null) {
            authorizeMono = groupMemberService.isOwner(requesterId, groupId);
            if (typeId != null) {
                authorizeMono = authorizeMono.flatMap(
                        authorized -> {
                            if (authorized != null && authorized) {
                                return isAllowedToCreateGroupAndHaveGroupType(requesterId, typeId);
                            } else {
                                return Mono.error(TurmsBusinessException.get(TurmsStatusCode.UNAUTHORIZED));
                            }
                        });
            }
        }
        return authorizeMono.flatMap(authorized -> {
            if (authorized != null && authorized) {
                return mongoTemplate.inTransaction()
                        .execute(operations -> {
                            List<Mono<Boolean>> monos = new LinkedList<>();
                            if (successorId != null) {
                                Mono<Boolean> transferMono = authAndTransferGroupOwnership(
                                        requesterId, groupId, successorId, quitAfterTransfer, operations);
                                monos.add(transferMono);
                            }
                            if (!Validator.areAllNull(typeId, creatorId, ownerId, name, intro, announcement,
                                    minimumScore, isActive, creationDate, deletionDate, muteEndDate)) {
                                Mono<Boolean> updateMono = authAndUpdateGroupInformation(
                                        requesterId, groupId, typeId, creatorId, ownerId, name, intro, announcement,
                                        minimumScore, isActive, creationDate, deletionDate, muteEndDate, operations);
                                monos.add(updateMono);
                            }
                            if (monos.isEmpty()) {
                                return Mono.just(true);
                            } else {
                                return Mono.when(monos).thenReturn(true);
                            }
                        })
                        .retryWhen(TRANSACTION_RETRY)
                        .singleOrEmpty();
            } else {
                return Mono.error(TurmsBusinessException.get(TurmsStatusCode.UNAUTHORIZED));
            }
        });
    }


    public Mono<Boolean> isAllowedToCreateGroupAndHaveGroupType(
            @NotNull Long requesterId,
            @NotNull Long groupTypeId) {
        Mono<UserPermissionGroup> userPermissionGroupMono = userPermissionGroupService.queryUserPermissionGroupByUserId(requesterId);
        return userPermissionGroupMono
                .flatMap(userPermissionGroup -> isAllowedToCreateGroup(requesterId, userPermissionGroup)
                        .flatMap(isAllowedToCreateGroup -> {
                            if (isAllowedToCreateGroup) {
                                return isAllowedHaveGroupType(requesterId, groupTypeId, userPermissionGroup);
                            } else {
                                return Mono.just(false);
                            }
                        }));
    }

    public Mono<Boolean> isAllowedToCreateGroup(
            @NotNull Long requesterId,
            @Nullable UserPermissionGroup auxiliaryUserPermissionGroup) {
        Mono<UserPermissionGroup> userPermissionGroupMono;
        if (auxiliaryUserPermissionGroup != null) {
            userPermissionGroupMono = Mono.just(auxiliaryUserPermissionGroup);
        } else {
            userPermissionGroupMono = userPermissionGroupService.queryUserPermissionGroupByUserId(requesterId);
        }
        return userPermissionGroupMono
                .flatMap(userPermissionGroup -> {
                    if (userPermissionGroup.getOwnedGroupLimit() == Integer.MAX_VALUE) {
                        return Mono.just(true);
                    } else {
                        return countOwnedGroups(requesterId)
                                .map(ownedGroupsNumber -> {
                                    if (ownedGroupsNumber < userPermissionGroup.getOwnedGroupLimit()) {
                                        return true;
                                    } else {
                                        throw TurmsBusinessException.get(TurmsStatusCode.OWNED_RESOURCE_LIMIT_REACHED);
                                    }
                                });
                    }
                })
                .switchIfEmpty(Mono.error(TurmsBusinessException.get(TurmsStatusCode.OWNED_RESOURCE_LIMIT_REACHED)));
    }

    public Mono<Boolean> isAllowedHaveGroupType(
            @NotNull Long requesterId,
            @NotNull Long groupTypeId,
            @Nullable UserPermissionGroup auxiliaryUserPermissionGroup) {
        return groupTypeService.groupTypeExists(groupTypeId)
                .flatMap(existed -> {
                    if (existed != null && existed) {
                        if (auxiliaryUserPermissionGroup != null) {
                            return Mono.just(auxiliaryUserPermissionGroup);
                        } else {
                            return userPermissionGroupService.queryUserPermissionGroupByUserId(requesterId);
                        }
                    } else {
                        return Mono.error(TurmsBusinessException.get(TurmsStatusCode.TYPE_NOT_EXISTS));
                    }
                })
                .flatMap(userPermissionGroup -> {
                    if (userPermissionGroup.getCreatableGroupTypeIds().contains(groupTypeId)) {
                        if (userPermissionGroup.getOwnedGroupLimitForEachGroupType() == Integer.MAX_VALUE
                                && (userPermissionGroup.getGroupTypeLimits() == null
                                || userPermissionGroup.getGroupTypeLimits().getOrDefault(groupTypeId, Integer.MAX_VALUE) == Integer.MAX_VALUE)) {
                            return Mono.just(true);
                        } else {
                            return countOwnedGroups(requesterId, groupTypeId)
                                    .map(ownedGroupsNumber -> {
                                        if (ownedGroupsNumber < userPermissionGroup.getOwnedGroupLimitForEachGroupType()
                                                && userPermissionGroup.getGroupTypeLimits().getOrDefault(groupTypeId, Integer.MAX_VALUE) < Integer.MAX_VALUE) {
                                            return true;
                                        } else {
                                            throw TurmsBusinessException.get(TurmsStatusCode.OWNED_RESOURCE_LIMIT_REACHED);
                                        }
                                    });
                        }
                    } else {
                        return Mono.error(TurmsBusinessException.get(TurmsStatusCode.UNAUTHORIZED));
                    }
                });
    }

    public Mono<Long> countOwnedGroups(@NotNull Long ownerId) {
        Query query = new Query()
                .addCriteria(Criteria.where(Group.Fields.OWNER_ID).is(ownerId));
        return mongoTemplate.count(query, Group.class);
    }

    public Mono<Long> countOwnedGroups(
            @NotNull Long ownerId,
            @NotNull Long groupTypeId) {
        Query query = new Query()
                .addCriteria(Criteria.where(Group.Fields.OWNER_ID).is(ownerId))
                .addCriteria(Criteria.where(Group.Fields.TYPE_ID).is(groupTypeId));
        return mongoTemplate.count(query, Group.class);
    }

    public Mono<Long> countCreatedGroups(@Nullable DateRange dateRange) {
        Query query = QueryBuilder.newBuilder()
                .addBetweenIfNotNull(Group.Fields.CREATION_DATE, dateRange)
                .add(Criteria.where(Group.Fields.DELETION_DATE).is(null))
                .buildQuery();
        return mongoTemplate.count(query, Group.class);
    }

    public Mono<Long> countGroups(
            @Nullable Set<Long> ids,
            @Nullable Set<Long> typeIds,
            @Nullable Set<Long> creatorIds,
            @Nullable Set<Long> ownerIds,
            @Nullable Boolean isActive,
            @Nullable DateRange creationDateRange,
            @Nullable DateRange deletionDateRange,
            @Nullable DateRange muteEndDateRange,
            @Nullable Set<Long> memberIds) {
        return getGroupIdsFromGroupIdsAndMemberIds(ids, memberIds)
                .switchIfEmpty(Common.emptySetMono())
                .flatMap(groupIds -> {
                    Query query = QueryBuilder
                            .newBuilder()
                            .addInIfNotNull(ID, groupIds)
                            .addInIfNotNull(Group.Fields.TYPE_ID, typeIds)
                            .addInIfNotNull(Group.Fields.CREATOR_ID, creatorIds)
                            .addInIfNotNull(Group.Fields.OWNER_ID, ownerIds)
                            .addIsIfNotNull(Group.Fields.IS_ACTIVE, isActive)
                            .addBetweenIfNotNull(Group.Fields.CREATION_DATE, creationDateRange)
                            .addBetweenIfNotNull(Group.Fields.DELETION_DATE, deletionDateRange)
                            .addBetweenIfNotNull(Group.Fields.MUTE_END_DATE, muteEndDateRange)
                            .buildQuery();
                    return mongoTemplate.count(query, Group.class);
                });
    }

    public Mono<Long> countDeletedGroups(@Nullable DateRange dateRange) {
        Query query = QueryBuilder.newBuilder()
                .addBetweenIfNotNull(Group.Fields.DELETION_DATE, dateRange)
                .buildQuery();
        return mongoTemplate.count(query, Group.class);
    }

    public Mono<Long> count() {
        return mongoTemplate.count(new Query(), Group.class);
    }

    public Flux<Long> queryGroupMembersIds(@NotNull Long groupId) {
        Query query = new Query().addCriteria(Criteria.where(ID_GROUP_ID).is(groupId));
        query.fields().include(ID_USER_ID);
        return mongoTemplate.find(query, GroupMember.class)
                .map(member -> member.getKey().getUserId());
    }

    public Mono<Boolean> isGroupMuted(@NotNull Long groupId) {
        Query query = new Query()
                .addCriteria(Criteria.where(ID).is(groupId))
                .addCriteria(Criteria.where(Group.Fields.MUTE_END_DATE).gt(new Date()));
        return mongoTemplate.exists(query, Group.class);
    }

    public Mono<Boolean> isGroupActiveAndNotDeleted(@NotNull Long groupId) {
        Query query = new Query()
                .addCriteria(Criteria.where(ID).is(groupId))
                .addCriteria(Criteria.where(Group.Fields.IS_ACTIVE).is(true))
                .addCriteria(Criteria.where(Group.Fields.DELETION_DATE).is(null));
        return mongoTemplate.exists(query, Group.class);
    }

    private Mono<Set<Long>> getGroupIdsFromGroupIdsAndMemberIds(@Nullable Set<Long> ids, @Nullable Set<Long> memberIds) {
        Mono<Set<Long>> idsMono = ids != null ? Mono.just(ids) : Common.emptySetMono();
        if (memberIds != null) {
            Mono<Set<Long>> joinedGroupIdsMono = groupMemberService
                    .queryUsersJoinedGroupsIds(memberIds, null, null)
                    .collect(Collectors.toSet());
            if (idsMono != EMPTY_SET_MONO) {
                idsMono = idsMono.zipWith(joinedGroupIdsMono, (groupIds, joinedGroupIds) -> {
                    groupIds.addAll(joinedGroupIds);
                    return groupIds;
                });
            } else {
                idsMono = joinedGroupIdsMono;
            }
        }
        return idsMono;
    }
}
