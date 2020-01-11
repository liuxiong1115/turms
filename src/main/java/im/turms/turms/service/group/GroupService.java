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
import im.turms.turms.cluster.TurmsClusterManager;
import im.turms.turms.common.*;
import im.turms.turms.constant.GroupMemberRole;
import im.turms.turms.constant.GroupUpdateStrategy;
import im.turms.turms.exception.TurmsBusinessException;
import im.turms.turms.pojo.bo.common.DateRange;
import im.turms.turms.pojo.bo.common.Int64ValuesWithVersion;
import im.turms.turms.pojo.bo.group.GroupsWithVersion;
import im.turms.turms.pojo.domain.Group;
import im.turms.turms.pojo.domain.GroupMember;
import im.turms.turms.pojo.domain.GroupType;
import im.turms.turms.service.user.UserVersionService;
import org.hibernate.validator.constraints.URL;
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

import static im.turms.turms.common.Constants.*;

// TODO: Allow group owners to change the type of their groups
@Service
@Validated
public class GroupService {
    private final ReactiveMongoTemplate mongoTemplate;
    private final GroupTypeService groupTypeService;
    private final GroupMemberService groupMemberService;
    private final UserVersionService userVersionService;
    private final TurmsClusterManager turmsClusterManager;
    private final GroupVersionService groupVersionService;

    public GroupService(
            GroupMemberService groupMemberService,
            TurmsClusterManager turmsClusterManager,
            GroupTypeService groupTypeService,
            ReactiveMongoTemplate mongoTemplate,
            UserVersionService userVersionService,
            GroupVersionService groupVersionService) {
        this.groupMemberService = groupMemberService;
        this.turmsClusterManager = turmsClusterManager;
        this.groupTypeService = groupTypeService;
        this.mongoTemplate = mongoTemplate;
        this.userVersionService = userVersionService;
        this.groupVersionService = groupVersionService;
    }

    public Mono<Group> createGroup(
            @NotNull Long creatorId,
            @NotNull Long ownerId,
            @Nullable String groupName,
            @Nullable String intro,
            @Nullable String announcement,
            @Nullable @URL String profilePictureUrl,
            @Nullable @Min(value = 0) Integer minimumScore,
            @Nullable Long groupTypeId,
            @Nullable @PastOrPresent Date creationDate,
            @Nullable @PastOrPresent Date deletionDate,
            @Nullable Date muteEndDate,
            @Nullable Boolean isActive) {
        Long groupId = turmsClusterManager.generateRandomId();
        Group group = new Group();
        group.setId(groupId);
        group.setCreatorId(creatorId);
        group.setOwnerId(ownerId);
        group.setName(groupName);
        group.setIntro(intro);
        group.setAnnouncement(announcement);
        group.setProfilePictureUrl(profilePictureUrl);
        group.setMinimumScore(minimumScore);
        group.setTypeId(groupTypeId);
        group.setCreationDate(creationDate);
        group.setDeletionDate(deletionDate);
        group.setMuteEndDate(muteEndDate);
        group.setActive(isActive);
        return mongoTemplate
                .inTransaction()
                .execute(operations -> operations.insert(group)
                        .zipWith(groupMemberService.addGroupMember(
                                group.getId(),
                                creatorId,
                                GroupMemberRole.OWNER,
                                null,
                                new Date(),
                                null,
                                operations))
                        .flatMap(results -> groupVersionService.upsert(groupId)
                                .thenReturn(results.getT1())))
                .retryWhen(TRANSACTION_RETRY)
                .single();
    }

    public Mono<Group> authAndCreateGroup(
            @NotNull Long creatorId,
            @NotNull Long ownerId,
            @Nullable String groupName,
            @Nullable String intro,
            @Nullable String announcement,
            @Nullable @URL String profilePictureUrl,
            @Nullable @Min(value = 0) Integer minimumScore,
            @Nullable Long groupTypeId,
            @Nullable @PastOrPresent Date creationDate,
            @Nullable @PastOrPresent Date deletionDate,
            @Nullable Date muteEndDate,
            @Nullable Boolean isActive) {
        if (creationDate != null && deletionDate != null && deletionDate.before(creationDate)) {
            throw TurmsBusinessException.get(TurmsStatusCode.ILLEGAL_ARGUMENTS);
        }
        if (groupTypeId == null) {
            groupTypeId = DEFAULT_GROUP_TYPE_ID;
        }
        Long finalGroupTypeId = groupTypeId;
        return groupTypeService.groupTypeExists(groupTypeId)
                .flatMap(existed -> {
                    if (existed != null && existed) {
                        return countUserOwnedGroupNumber(creatorId);
                    } else {
                        return Mono.error(TurmsBusinessException.get(TurmsStatusCode.NO_CONTENT));
                    }
                })
                .flatMap(ownedGroupNumber -> {
                    if (ownedGroupNumber < turmsClusterManager.getTurmsProperties().getGroup().getUserOwnedGroupLimit()) {
                        return groupMemberService.isAllowedToHaveGroupType(creatorId, finalGroupTypeId);
                    } else {
                        return Mono.error(TurmsBusinessException.get(TurmsStatusCode.OWNED_RESOURCE_LIMIT_REACHED));
                    }
                })
                .flatMap(allowed -> {
                    if (allowed != null && allowed) {
                        return createGroup(creatorId,
                                ownerId,
                                groupName,
                                intro,
                                announcement,
                                profilePictureUrl,
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
                    .getGroup().isShouldDeleteLogicallyGroupByDefault();
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
                        Update update = new Update().set(Group.Fields.deletionDate, new Date());
                        updateOrRemoveMono = operations.updateMulti(query, update, Group.class)
                                .map(UpdateResult::wasAcknowledged);
                    } else {
                        updateOrRemoveMono = operations.remove(query, Group.class)
                                .map(DeleteResult::wasAcknowledged);
                    }
                    return updateOrRemoveMono.flatMap(acknowledged -> {
                        if (acknowledged != null && acknowledged) {
                            return groupMemberService.deleteAllGroupMembers(groupIds, operations)
                                    .then(groupVersionService.delete(groupIds, operations));
                        } else {
                            return Mono.just(false);
                        }
                    });
                })
                .retryWhen(TRANSACTION_RETRY)
                .single();
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
                .switchIfEmpty(Constants.emptySetMono())
                .flatMapMany(groupIds -> {
                    Query query = QueryBuilder
                            .newBuilder()
                            .addInIfNotNull(ID, groupIds)
                            .addInIfNotNull(Group.Fields.typeId, typeIds)
                            .addInIfNotNull(Group.Fields.creatorId, creatorIds)
                            .addInIfNotNull(Group.Fields.ownerId, ownerIds)
                            .addIsIfNotNull(Group.Fields.active, isActive)
                            .addBetweenIfNotNull(Group.Fields.creationDate, creationDateRange)
                            .addBetweenIfNotNull(Group.Fields.deletionDate, deletionDateRange)
                            .addBetweenIfNotNull(Group.Fields.muteEndDate, muteEndDateRange)
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
        query.fields().include(Group.Fields.ownerId);
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
                        groupMemberService.isAllowedToHaveGroupType(successorId, groupTypeId)
                                .flatMap(allowed -> {
                                    if (allowed == null || !allowed) {
                                        return Mono.error(TurmsBusinessException
                                                .get(TurmsStatusCode.OWNED_RESOURCE_LIMIT_REACHED));
                                    }
                                    Mono<Boolean> deleteOrUpdateOwnerMono;
                                    if (quitAfterTransfer) {
                                        deleteOrUpdateOwnerMono = groupMemberService.deleteGroupMembers(
                                                groupId, Set.of(ownerId), operations);
                                    } else {
                                        deleteOrUpdateOwnerMono = groupMemberService.updateGroupMember(
                                                groupId,
                                                ownerId,
                                                null,
                                                GroupMemberRole.MEMBER,
                                                null,
                                                null,
                                                operations);
                                    }
                                    Mono<Boolean> update = groupMemberService.updateGroupMember(
                                            groupId,
                                            successorId,
                                            null,
                                            GroupMemberRole.OWNER,
                                            null,
                                            null,
                                            operations);
                                    return Mono.zip(deleteOrUpdateOwnerMono, update)
                                            .map(results -> results.getT1() && results.getT2());
                                })));
    }

    public Mono<GroupType> queryGroupType(@NotNull Long groupId) {
        Query query = new Query().addCriteria(Criteria.where(ID).is(groupId));
        query.fields().include(Group.Fields.typeId);
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
            @Nullable @URL String profilePictureUrl,
            @Nullable @Min(0) Integer minimumScore,
            @Nullable Boolean isActive,
            @Nullable @PastOrPresent Date creationDate,
            @Nullable @PastOrPresent Date deletionDate,
            @Nullable Date muteEndDate,
            @Nullable ReactiveMongoOperations operations) {
        Validator.throwIfAllNull(typeId, creatorId, ownerId, name, intro, announcement,
                profilePictureUrl, minimumScore, isActive, creationDate, deletionDate, muteEndDate);
        Query query = new Query().addCriteria(Criteria.where(ID).in(groupIds));
        Update update = UpdateBuilder.newBuilder()
                .setIfNotNull(Group.Fields.typeId, typeId)
                .setIfNotNull(Group.Fields.creatorId, creatorId)
                .setIfNotNull(Group.Fields.ownerId, ownerId)
                .setIfNotNull(Group.Fields.name, name)
                .setIfNotNull(Group.Fields.intro, intro)
                .setIfNotNull(Group.Fields.announcement, announcement)
                .setIfNotNull(Group.Fields.profilePictureUrl, profilePictureUrl)
                .setIfNotNull(Group.Fields.minimumScore, minimumScore)
                .setIfNotNull(Group.Fields.active, isActive)
                .setIfNotNull(Group.Fields.creationDate, creationDate)
                .setIfNotNull(Group.Fields.deletionDate, deletionDate)
                .setIfNotNull(Group.Fields.muteEndDate, muteEndDate)
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
            @Nullable @URL String profilePictureUrl,
            @Nullable @Min(0) Integer minimumScore,
            @Nullable Boolean isActive,
            @Nullable @PastOrPresent Date creationDate,
            @Nullable @PastOrPresent Date deletionDate,
            @Nullable Date muteEndDate,
            @Nullable ReactiveMongoOperations operations) {
        Validator.throwIfAllNull(typeId, creatorId, ownerId, name, intro, announcement,
                profilePictureUrl, minimumScore, isActive, creationDate, deletionDate, muteEndDate);
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
                                announcement, profilePictureUrl, minimumScore, isActive, creationDate, deletionDate, muteEndDate, operations);
                    } else {
                        return Mono.error(TurmsBusinessException.get(TurmsStatusCode.UNAUTHORIZED));
                    }
                });
    }

    private Mono<Long> countUserOwnedGroupNumber(@NotNull Long ownerId) {
        Query query = new Query().addCriteria(Criteria.where(Group.Fields.ownerId).is(ownerId));
        return mongoTemplate.count(query, Group.class);
    }

    public Mono<GroupsWithVersion> queryGroupWithVersion(
            @NotNull Long groupId,
            @Nullable Date lastUpdatedDate) {
        return groupVersionService.queryInfoVersion(groupId)
                .defaultIfEmpty(MAX_DATE)
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
                });
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
                .flatMapMany(this::queryGroups);
    }

    public Mono<Int64ValuesWithVersion> queryJoinedGroupsIdsWithVersion(
            @NotNull Long memberId,
            @NotNull Date lastUpdatedDate) {
        return userVersionService
                .queryJoinedGroupVersion(memberId)
                .defaultIfEmpty(MAX_DATE)
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
                });
    }

    public Mono<GroupsWithVersion> queryJoinedGroupsWithVersion(
            @NotNull Long memberId,
            @NotNull Date lastUpdatedDate) {
        return userVersionService
                .queryJoinedGroupVersion(memberId)
                .defaultIfEmpty(MAX_DATE)
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
                });
    }

    public Mono<Boolean> updateGroups(
            @NotEmpty Set<Long> groupIds,
            @Nullable Long typeId,
            @Nullable Long creatorId,
            @Nullable Long ownerId,
            @Nullable String name,
            @Nullable String intro,
            @Nullable String announcement,
            @Nullable @URL String profilePictureUrl,
            @Nullable @Min(0) Integer minimumScore,
            @Nullable Boolean isActive,
            @Nullable @PastOrPresent Date creationDate,
            @Nullable @PastOrPresent Date deletionDate,
            @Nullable Date muteEndDate,
            @Nullable Long successorId,
            boolean quitAfterTransfer) {
        Validator.throwIfAllNull(
                typeId,
                creatorId,
                ownerId,
                name,
                intro,
                announcement,
                profilePictureUrl,
                minimumScore,
                isActive,
                creationDate,
                deletionDate,
                muteEndDate,
                successorId);
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
                            profilePictureUrl, minimumScore, isActive, creationDate, deletionDate, muteEndDate)) {
                        monos.add(updateGroupsInformation(
                                groupIds, typeId, creatorId, ownerId, name, intro, announcement, profilePictureUrl,
                                minimumScore, isActive, creationDate, deletionDate, muteEndDate,
                                operations));
                    }
                    if (monos.isEmpty()) {
                        throw TurmsBusinessException.get(TurmsStatusCode.ILLEGAL_ARGUMENTS);
                    } else {
                        return Mono.zip(monos, objects -> objects).thenReturn(true);
                    }
                })
                .retryWhen(TRANSACTION_RETRY)
                .single();
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
            @Nullable @URL String profilePictureUrl,
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
                                return groupMemberService.isAllowedToHaveGroupType(requesterId, typeId);
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
                                    profilePictureUrl, minimumScore, isActive, creationDate, deletionDate, muteEndDate)) {
                                Mono<Boolean> updateMono = authAndUpdateGroupInformation(
                                        requesterId, groupId, typeId, creatorId, ownerId, name, intro, announcement,
                                        profilePictureUrl, minimumScore, isActive, creationDate, deletionDate, muteEndDate, operations);
                                monos.add(updateMono);
                            }
                            if (monos.isEmpty()) {
                                return Mono.just(true);
                            } else {
                                return Mono.zip(monos, objects -> objects).thenReturn(true);
                            }
                        })
                        .retryWhen(TRANSACTION_RETRY)
                        .single();
            } else {
                return Mono.error(TurmsBusinessException.get(TurmsStatusCode.UNAUTHORIZED));
            }
        });
    }

    public Mono<Long> countOwnedGroups(
            @NotNull Long ownerId,
            @NotNull Long groupTypeId) {
        Query query = new Query()
                .addCriteria(Criteria.where(Group.Fields.ownerId).is(ownerId))
                .addCriteria(Criteria.where(Group.Fields.typeId).is(groupTypeId));
        return mongoTemplate.count(query, Group.class);
    }

    public Mono<Long> countCreatedGroups(@Nullable DateRange dateRange) {
        Query query = QueryBuilder.newBuilder()
                .addBetweenIfNotNull(Group.Fields.creationDate, dateRange)
                .add(Criteria.where(Group.Fields.deletionDate).is(null))
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
                .switchIfEmpty(Constants.emptySetMono())
                .flatMap(groupIds -> {
                    Query query = QueryBuilder
                            .newBuilder()
                            .addInIfNotNull(ID, groupIds)
                            .addInIfNotNull(Group.Fields.typeId, typeIds)
                            .addInIfNotNull(Group.Fields.creatorId, creatorIds)
                            .addInIfNotNull(Group.Fields.ownerId, ownerIds)
                            .addIsIfNotNull(Group.Fields.active, isActive)
                            .addBetweenIfNotNull(Group.Fields.creationDate, creationDateRange)
                            .addBetweenIfNotNull(Group.Fields.deletionDate, deletionDateRange)
                            .addBetweenIfNotNull(Group.Fields.muteEndDate, muteEndDateRange)
                            .buildQuery();
                    return mongoTemplate.count(query, Group.class);
                });
    }

    public Mono<Long> countDeletedGroups(@Nullable DateRange dateRange) {
        Query query = QueryBuilder.newBuilder()
                .addBetweenIfNotNull(Group.Fields.deletionDate, dateRange)
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
                .addCriteria(Criteria.where(ID_GROUP_ID).is(groupId))
                .addCriteria(Criteria.where(Group.Fields.muteEndDate).gt(new Date()));
        return mongoTemplate.exists(query, Group.class);
    }

    public Mono<Boolean> isGroupActiveAndNotDeleted(@NotNull Long groupId) {
        Query query = new Query()
                .addCriteria(Criteria.where(ID).is(groupId))
                .addCriteria(Criteria.where(Group.Fields.active).is(true))
                .addCriteria(Criteria.where(Group.Fields.deletionDate).is(null));
        return mongoTemplate.exists(query, Group.class);
    }

    private Mono<Set<Long>> getGroupIdsFromGroupIdsAndMemberIds(@Nullable Set<Long> ids, @Nullable Set<Long> memberIds) {
        Mono<Set<Long>> idsMono = ids != null ? Mono.just(ids) : Constants.emptySetMono();
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
