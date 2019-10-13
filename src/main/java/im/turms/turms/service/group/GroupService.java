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
import im.turms.turms.common.ProtoUtil;
import im.turms.turms.common.QueryBuilder;
import im.turms.turms.common.TurmsStatusCode;
import im.turms.turms.common.UpdateBuilder;
import im.turms.turms.constant.ChatType;
import im.turms.turms.constant.GroupMemberRole;
import im.turms.turms.constant.GroupUpdateStrategy;
import im.turms.turms.exception.TurmsBusinessException;
import im.turms.turms.pojo.domain.Group;
import im.turms.turms.pojo.domain.GroupMember;
import im.turms.turms.pojo.domain.GroupType;
import im.turms.turms.pojo.domain.Message;
import im.turms.turms.pojo.response.GroupsWithVersion;
import im.turms.turms.pojo.response.Int64ValuesWithVersion;
import im.turms.turms.service.user.UserVersionService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.mongodb.core.ReactiveMongoOperations;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.annotation.Nullable;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import static im.turms.turms.common.Constants.*;

// TODO: Allow group owners to change the type of their groups
@Service
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
            @Nullable String groupName,
            @Nullable String intro,
            @Nullable String announcement,
            @Nullable String profilePictureUrl,
            @Nullable Long groupTypeId) {
        Long groupId = turmsClusterManager.generateRandomId();
        Group group = new Group();
        group.setId(groupId);
        group.setCreatorId(creatorId);
        group.setOwnerId(creatorId);
        group.setName(groupName);
        group.setIntro(intro);
        group.setAnnouncement(announcement);
        group.setProfilePictureUrl(profilePictureUrl);
        group.setTypeId(groupTypeId);
        return mongoTemplate
                .inTransaction()
                .execute(operations -> operations.insert(group)
                        .zipWith(groupMemberService.addGroupMember(
                                group.getId(),
                                creatorId,
                                GroupMemberRole.OWNER,
                                operations))
                        .flatMap(results -> groupVersionService.upsert(groupId)
                                .thenReturn(results.getT1())))
                .single();
    }

    public Mono<Group> authAndCreateGroup(
            @NotNull Long creatorId,
            @Nullable String groupName,
            @Nullable String intro,
            @Nullable String announcement,
            @Nullable String profilePictureUrl,
            @Nullable Long groupTypeId) {
        if (groupTypeId == null) {
            groupTypeId = DEFAULT_GROUP_TYPE_ID;
        }
        Long finalGroupTypeId = groupTypeId;
        return groupTypeService.groupTypeExists(groupTypeId)
                .flatMap(existed -> {
                    if (existed != null && existed) {
                        return countUserOwnedGroupNumber(creatorId);
                    } else {
                        return Mono.error(TurmsBusinessException.get(TurmsStatusCode.NOT_FOUND));
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
                        return createGroup(creatorId, groupName, intro, announcement, profilePictureUrl, finalGroupTypeId);
                    } else {
                        return Mono.error(TurmsBusinessException.get(TurmsStatusCode.OWNED_RESOURCE_LIMIT_REACHED));
                    }
                });
    }

    public Mono<Boolean> deleteGroupAndGroupMembers(
            @NotNull Long groupId,
            @Nullable Boolean useLogicalDeletion) {
        if (useLogicalDeletion == null) {
            useLogicalDeletion = turmsClusterManager.getTurmsProperties()
                    .getGroup().isLogicallyDeleteGroupByDefault();
        }
        boolean finalUseLogicalDeletion = useLogicalDeletion;
        return mongoTemplate.inTransaction().execute(operations -> {
            Query query = new Query().addCriteria(Criteria.where(ID).is(groupId));
            Mono<Boolean> updateOrRemoveMono;
            if (finalUseLogicalDeletion) {
                Update update = new Update().set(Group.Fields.deletionDate, new Date());
                updateOrRemoveMono = operations.updateFirst(query, update, Group.class)
                        .map(UpdateResult::wasAcknowledged);
            } else {
                updateOrRemoveMono = operations.remove(query, Group.class)
                        .map(DeleteResult::wasAcknowledged);
            }
            return updateOrRemoveMono.flatMap(acknowledged -> {
                if (acknowledged != null && acknowledged) {
                    return groupMemberService.deleteAllGroupMembers(groupId, operations)
                            .then(groupVersionService.delete(groupId, operations));
                } else {
                    return Mono.just(false);
                }
            });
        }).single();
    }

    public Mono<Boolean> authAndDeleteGroupAndGroupMembers(
            @NotNull Long requesterId,
            @NotNull Long groupId,
            @Nullable Boolean useLogicalDeletion) {
        return groupMemberService
                .isOwner(requesterId, groupId)
                .flatMap(authenticated -> {
                    if (authenticated != null && authenticated) {
                        return deleteGroupAndGroupMembers(groupId, useLogicalDeletion);
                    } else {
                        return Mono.error(TurmsBusinessException.get(TurmsStatusCode.UNAUTHORIZED));
                    }
                });
    }

    public Flux<Group> queryGroups(@NotNull Integer page, @NotNull Integer size) {
        Query query = new Query().with(PageRequest.of(page, size));
        return mongoTemplate.find(query, Group.class);
    }

    public Mono<Long> queryGroupTypeId(@NotNull Long groupId) {
        Query query = new Query().addCriteria(Criteria.where(ID).is(groupId));
        return mongoTemplate.findOne(query, Group.class)
                .map(Group::getTypeId);
    }

    public Mono<Boolean> authAndTransferGroupOwnership(
            @NotNull Long requesterId,
            @NotNull Long groupId,
            @NotNull Long successorId,
            boolean quitAfterTransfer,
            ReactiveMongoOperations operations) {
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
            @Nullable Long currentOwnerId,
            @NotNull Long groupId,
            @NotNull Long successorId,
            boolean quitAfterTransfer,
            @Nullable ReactiveMongoOperations operations) {
        Mono<Long> queryOwnerIdMono;
        if (currentOwnerId != null) {
            queryOwnerIdMono = Mono.just(currentOwnerId);
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
                                        deleteOrUpdateOwnerMono = groupMemberService.deleteGroupMember(groupId, currentOwnerId, operations);
                                    } else {
                                        deleteOrUpdateOwnerMono = groupMemberService.updateGroupMember(
                                                groupId,
                                                currentOwnerId,
                                                null,
                                                GroupMemberRole.MEMBER,
                                                null,
                                                operations);
                                    }
                                    Mono<Boolean> update = groupMemberService.updateGroupMember(
                                            groupId,
                                            successorId,
                                            null,
                                            GroupMemberRole.OWNER,
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

    public Mono<Boolean> updateGroupInformation(
            @NotNull Long groupId,
            @Nullable String name,
            @Nullable String profileUrl,
            @Nullable String intro,
            @Nullable String announcement,
            @Nullable Long typeId,
            @Nullable ReactiveMongoOperations operations) {
        Query query = new Query().addCriteria(Criteria.where(ID).is(groupId));
        Update update = UpdateBuilder.newBuilder()
                .setIfNotNull(Group.Fields.name, name)
                .setIfNotNull(Group.Fields.intro, intro)
                .setIfNotNull(Group.Fields.announcement, announcement)
                .setIfNotNull(Group.Fields.profilePictureUrl, profileUrl)
                .setIfNotNull(Group.Fields.typeId, typeId)
                .build();
        ReactiveMongoOperations mongoOperations = operations != null ? operations : mongoTemplate;
        return mongoOperations.updateFirst(query, update, Group.class)
                .flatMap(result -> {
                    if (result.wasAcknowledged()) {
                        return groupVersionService.updateInformation(groupId)
                                .thenReturn(true);
                    } else {
                        return Mono.just(false);
                    }
                });
    }

    public Mono<Boolean> authAndUpdateGroupInformation(
            @Nullable Long requesterId,
            @NotNull Long groupId,
            @Nullable String name,
            @Nullable String profileUrl,
            @Nullable String intro,
            @Nullable String announcement,
            @Nullable Long typeId,
            @Nullable ReactiveMongoOperations operations) {
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
                        return updateGroupInformation(groupId, name, profileUrl, intro, announcement, typeId, operations);
                    } else {
                        return Mono.error(TurmsBusinessException.get(TurmsStatusCode.UNAUTHORIZED));
                    }
                });
    }

    private Mono<Long> countUserOwnedGroupNumber(@NotNull Long ownerId) {
        Query query = new Query().addCriteria(Criteria.where(Group.Fields.ownerId).is(ownerId));
        return mongoTemplate.count(query, Group.class);
    }

    public Flux<Group> queryUserJoinedGroup(@NotNull Long userId) {
        return groupMemberService
                .queryUserJoinedGroupsIds(userId)
                .collectList()
                .flatMapMany(groupsIds -> {
                    if (groupsIds.isEmpty()) {
                        throw TurmsBusinessException.get(TurmsStatusCode.NOT_FOUND);
                    }
                    Query query = new Query().addCriteria(Criteria.where(ID).in(groupsIds));
                    return mongoTemplate.find(query, Group.class);
                });
    }

    public Mono<Group> queryGroupById(@NotNull Long groupId) {
        return mongoTemplate.findById(groupId, Group.class);
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
                                        throw TurmsBusinessException.get(TurmsStatusCode.NOT_FOUND);
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
                                        throw TurmsBusinessException.get(TurmsStatusCode.NOT_FOUND);
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

    public Mono<Boolean> updateGroup(
            @NotNull Long groupId,
            @Nullable Date muteEndDate,
            @Nullable String groupName,
            @Nullable String url,
            @Nullable String intro,
            @Nullable String announcement,
            @Nullable Long groupTypeId,
            @Nullable Long successorId,
            boolean quitAfterTransfer) {
        return mongoTemplate
                .inTransaction()
                .execute(operations -> {
                    List<Mono<Boolean>> monos = new LinkedList<>();
                    if (successorId != null) {
                        Mono<Boolean> transferMono = checkAndTransferGroupOwnership(
                                null, groupId, successorId, quitAfterTransfer, operations);
                        monos.add(transferMono);
                    }
                    if (muteEndDate != null || groupName != null || url != null
                            || intro != null || announcement != null || groupTypeId != null) {
                        monos.add(updateGroupInformation(
                                groupId,
                                groupName,
                                url,
                                intro,
                                announcement,
                                groupTypeId,
                                operations));
                    }
                    if (monos.isEmpty()) {
                        throw TurmsBusinessException.get(TurmsStatusCode.ILLEGAL_ARGUMENTS);
                    } else {
                        return Mono.zip(monos, objects -> objects).thenReturn(true);
                    }
                })
                .single();
    }

    public Mono<Boolean> authAndUpdateGroup(
            @NotNull Long requesterId,
            @NotNull Long groupId,
            @Nullable Date muteEndDate,
            @Nullable String groupName,
            @Nullable String url,
            @Nullable String intro,
            @Nullable String announcement,
            @Nullable Long groupTypeId,
            @Nullable Long successorId,
            boolean quitAfterTransfer) {
        Mono<Boolean> authorizeMono = Mono.just(true);
        if (successorId != null || groupTypeId != null) {
            authorizeMono = groupMemberService.isOwner(requesterId, groupId);
            if (groupTypeId != null) {
                authorizeMono = authorizeMono.flatMap(
                        authorized -> {
                            if (authorized != null && authorized) {
                                return groupMemberService.isAllowedToHaveGroupType(requesterId, groupTypeId);
                            } else {
                                return Mono.error(TurmsBusinessException.get(TurmsStatusCode.UNAUTHORIZED));
                            }
                        });
            }
        }
        return authorizeMono.flatMap(authorized -> {
            if (authorized != null && authorized) {
                return mongoTemplate.inTransaction().execute(operations -> {
                    List<Mono<Boolean>> monos = new LinkedList<>();
                    if (successorId != null) {
                        Mono<Boolean> transferMono = authAndTransferGroupOwnership(
                                requesterId, groupId, successorId, quitAfterTransfer, operations);
                        monos.add(transferMono);
                    }
                    if (muteEndDate != null || groupName != null || url != null
                            || intro != null || announcement != null || groupTypeId != null) {
                        Mono<Boolean> updateMono = authAndUpdateGroupInformation(
                                requesterId,
                                groupId,
                                groupName,
                                url,
                                intro,
                                announcement,
                                groupTypeId,
                                operations);
                        monos.add(updateMono);
                    }
                    if (monos.isEmpty()) {
                        return Mono.just(true);
                    } else {
                        return Mono.zip(monos, objects -> objects).thenReturn(true);
                    }
                }).single();
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

    public Mono<Long> countOwnedGroups(
            @Nullable Date creationDateStart,
            @Nullable Date creationDateEnd) {
        Query query = QueryBuilder.newBuilder()
                .addBetweenIfNotNull(Group.Fields.creationDate, creationDateStart, creationDateEnd)
                .buildQuery();
        return mongoTemplate.count(query, Group.class);
    }

    public Mono<Long> countDeletedGroups(
            @Nullable Date deletionDateStart,
            @Nullable Date deletionDateEnd) {
        Query query = QueryBuilder.newBuilder()
                .addBetweenIfNotNull(Group.Fields.deletionDate, deletionDateStart, deletionDateEnd)
                .buildQuery();
        return mongoTemplate.count(query, Group.class);
    }

    public Mono<Long> countGroupsThatSentMessages(
            @NotNull Date deliveryMessageDateStart,
            @NotNull Date deliveryMessageDateEnd) {
        Criteria criteria = QueryBuilder.newBuilder()
                .addIfNotNull(Criteria.where(Message.Fields.chatType).is(ChatType.GROUP), ChatType.GROUP)
                .addBetweenIfNotNull(Message.Fields.deliveryDate,
                        deliveryMessageDateStart,
                        deliveryMessageDateEnd)
                .buildCriteria();
        Aggregation aggregation = Aggregation.newAggregation(
                Aggregation.match(criteria), Aggregation.group().count().as("count"));
        return mongoTemplate.aggregate(aggregation, Message.class, Long.class).single();
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

    public Mono<Boolean> isGroupMutedOrInactive(@NotNull Long groupId) {
        Query query = new Query()
                .addCriteria(Criteria.where(ID_GROUP_ID).is(groupId))
                .addCriteria(Criteria.where(Group.Fields.muteEndDate).gt(new Date()))
                .addCriteria(Criteria.where(Group.Fields.active).ne(true));
        return mongoTemplate.exists(query, Group.class);
    }

    public Mono<Boolean> isGroupActive(@NotNull Long groupId) {
        Query query = new Query()
                .addCriteria(Criteria.where(Group.Fields.active).is(true));
        return mongoTemplate.exists(query, Group.class);
    }
}
