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
import im.turms.turms.cluster.TurmsClusterManager;
import im.turms.turms.common.ProtoUtil;
import im.turms.turms.common.TurmsStatusCode;
import im.turms.turms.common.UpdateBuilder;
import im.turms.turms.constant.GroupInvitationStrategy;
import im.turms.turms.constant.GroupMemberRole;
import im.turms.turms.exception.TurmsBusinessException;
import im.turms.turms.pojo.bo.InvitableAndInvitationStrategy;
import im.turms.turms.pojo.bo.UserOnlineInfo;
import im.turms.turms.pojo.domain.GroupBlacklistedUser;
import im.turms.turms.pojo.domain.GroupMember;
import im.turms.turms.pojo.domain.UserPermissionGroup;
import im.turms.turms.pojo.response.GroupMembersWithVersion;
import im.turms.turms.service.user.onlineuser.OnlineUserService;
import org.springframework.context.annotation.Lazy;
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
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static im.turms.turms.common.Constants.*;

@Service
public class GroupMemberService {
    private static final UserPermissionGroup EMPTY_USER_GROUP_TYPE_PERMISSION = new UserPermissionGroup();
    private final ReactiveMongoTemplate mongoTemplate;
    private final GroupService groupService;
    private final GroupTypeService groupTypeService;
    private final GroupVersionService groupVersionService;
    private final OnlineUserService onlineUserService;
    private final TurmsClusterManager turmsClusterManager;

    public GroupMemberService(ReactiveMongoTemplate mongoTemplate, @Lazy GroupService groupService, @Lazy GroupTypeService groupTypeService, GroupVersionService groupVersionService, @Lazy OnlineUserService onlineUserService, @Lazy TurmsClusterManager turmsClusterManager) {
        this.mongoTemplate = mongoTemplate;
        this.groupService = groupService;
        this.groupTypeService = groupTypeService;
        this.groupVersionService = groupVersionService;
        this.onlineUserService = onlineUserService;
        this.turmsClusterManager = turmsClusterManager;
    }

    //TODO: member's name
    public Mono<Boolean> addGroupMember(
            @NotNull Long groupId,
            @NotNull Long userId,
            @NotNull GroupMemberRole groupMemberRole,
            @Nullable ReactiveMongoOperations operations) {
        GroupMember groupMember = new GroupMember(
                groupId,
                userId,
                null,
                groupMemberRole,
                new Date(),
                null);
        ReactiveMongoOperations mongoOperations = operations != null ? operations : mongoTemplate;
        return mongoOperations.insert(groupMember)
                .zipWith(groupVersionService.updateMembersVersion(groupId))
                .thenReturn(true);
    }

    public Mono<Boolean> authAndAddGroupMember(
            @NotNull Long requesterId,
            @NotNull Long groupId,
            @NotNull Long userId,
            @NotNull GroupMemberRole groupMemberRole,
            @Nullable ReactiveMongoOperations operations) {
        return isAllowedToInviteOrAdd(groupId, requesterId)
                .flatMap(allowed -> {
                    if (allowed.isInvitable()) {
                        return Mono.zip(isBlacklisted(groupId, userId),
                                groupService.isGroupActive(groupId))
                                .flatMap(results -> {
                                    if (!results.getT1() && results.getT2()) {
                                        return addGroupMember(groupId, userId, groupMemberRole, operations);
                                    } else {
                                        return Mono.error(TurmsBusinessException.get(TurmsStatusCode.TARGET_USERS_UNAUTHORIZED));
                                    }
                                });
                    } else {
                        return Mono.error(TurmsBusinessException.get(TurmsStatusCode.UNAUTHORIZED));
                    }
                });
    }

    public Mono<Boolean> authAndDeleteGroupMember(
            @NotNull Long requesterId,
            @NotNull Long groupId,
            @NotNull Long deleteMemberId,
            @Nullable Long successorId,
            @Nullable Boolean quitAfterTransfer) {
        if (successorId != null) {
            quitAfterTransfer = quitAfterTransfer != null ? quitAfterTransfer : false;
            return groupService.authAndTransferGroupOwnership(
                    requesterId, groupId, successorId, quitAfterTransfer, null);
        }
        if (requesterId.equals(deleteMemberId)) {
            return isOwner(deleteMemberId, groupId)
                    .flatMap(isOwner -> {
                        if (isOwner != null && isOwner) {
                            // Because successorId is null
                            return Mono.error(TurmsBusinessException.get(TurmsStatusCode.ILLEGAL_ARGUMENTS));
                        } else {
                            return deleteGroupMember(groupId, deleteMemberId, null);
                        }
                    });
        } else {
            return isOwnerOrManager(requesterId, groupId)
                    .flatMap(isOwnerOrManager -> {
                        if (isOwnerOrManager != null && isOwnerOrManager) {
                            return deleteGroupMember(groupId, deleteMemberId, null);
                        } else {
                            return Mono.error(TurmsBusinessException.get(TurmsStatusCode.UNAUTHORIZED));
                        }
                    });
        }
    }

    public Mono<Boolean> deleteGroupMember(
            @NotNull Long groupId,
            @NotNull Long deleteMemberId,
            @Nullable ReactiveMongoOperations operations) {
        Query query = new Query()
                .addCriteria(Criteria.where(ID_GROUP_ID).is(groupId))
                .addCriteria(Criteria.where(ID_USER_ID).is(deleteMemberId));
        ReactiveMongoOperations mongoOperations = operations != null ? operations : mongoTemplate;
        return mongoOperations.remove(query, GroupMember.class)
                .flatMap(result -> {
                    if (result.wasAcknowledged()) {
                        return groupVersionService.updateMembersVersion(groupId)
                                .thenReturn(true);
                    } else {
                        return Mono.just(false);
                    }
                });
    }

    public Mono<Boolean> updateGroupMember(
            @NotNull Long groupId,
            @NotNull Long memberId,
            @Nullable String name,
            @Nullable GroupMemberRole role,
            @Nullable Date muteEndDate,
            @Nullable ReactiveMongoOperations operations) {
        if (name == null && role == null && muteEndDate == null) {
            throw TurmsBusinessException.get(TurmsStatusCode.ILLEGAL_ARGUMENTS);
        }
        Query query = new Query()
                .addCriteria(Criteria.where(ID_GROUP_ID).is(groupId))
                .addCriteria(Criteria.where(ID_USER_ID).is(memberId));
        Update update = UpdateBuilder.newBuilder()
                .setIfNotNull(GroupMember.Fields.name, name)
                .setIfNotNull(GroupMember.Fields.role, role)
                .build();
        if (muteEndDate != null) {
            if (muteEndDate.before(new Date())) {
                update.unset(GroupMember.Fields.muteEndDate);
            } else {
                update.set(GroupMember.Fields.muteEndDate, muteEndDate);
            }
        }
        ReactiveMongoOperations mongoOperations = operations != null ? operations : mongoTemplate;
        return mongoOperations.updateFirst(query, update, GroupMember.class)
                .flatMap(result -> {
                    if (result.wasAcknowledged()) {
                        return groupVersionService.updateMembersVersion(groupId)
                                .thenReturn(true);
                    } else {
                        return Mono.just(false);
                    }
                });
    }

    public Flux<Long> getMembersIdsByGroupId(@NotNull Long groupId) {
        Query query = new Query()
                .addCriteria(Criteria.where(ID_GROUP_ID).is(groupId));
        query.fields().include(ID_USER_ID);
        return mongoTemplate.find(query, GroupMember.class)
                .map(groupMember -> groupMember.getKey().getUserId());
    }

    public Mono<Boolean> isGroupMember(@NotNull Long groupId, @NotNull Long userId) {
        Query query = new Query()
                .addCriteria(Criteria.where(ID_GROUP_ID).is(groupId))
                .addCriteria(Criteria.where(ID_USER_ID).is(userId));
        return mongoTemplate.exists(query, GroupMember.class);
    }

    public Mono<Boolean> isBlacklisted(@NotNull Long groupId, @NotNull Long userId) {
        Query query = new Query()
                .addCriteria(Criteria.where(ID_GROUP_ID).is(groupId))
                .addCriteria(Criteria.where(ID_USER_ID).is(userId));
        return mongoTemplate.exists(query, GroupBlacklistedUser.class);
    }

    public Mono<InvitableAndInvitationStrategy> isAllowedToInviteOrAdd(
            @NotNull Long groupId,
            @NotNull Long inviterId) {
        return groupService.queryGroupType(groupId)
                .flatMap(groupType -> {
                    GroupInvitationStrategy groupInvitationStrategy = groupType.getInvitationStrategy();
                    if (groupInvitationStrategy == GroupInvitationStrategy.ALL
                            || groupInvitationStrategy == GroupInvitationStrategy.ALL_REQUIRING_ACCEPTANCE) {
                        return Mono.just(new InvitableAndInvitationStrategy(true, groupInvitationStrategy));
                    } else {
                        return queryGroupMemberRole(inviterId, groupId)
                                .map(groupMemberRole -> {
                                    switch (groupInvitationStrategy) {
                                        case OWNER:
                                        case OWNER_REQUIRING_ACCEPTANCE:
                                            return groupMemberRole == GroupMemberRole.OWNER;
                                        case OWNER_MANAGER:
                                        case OWNER_MANAGER_REQUIRING_ACCEPTANCE:
                                            return groupMemberRole == GroupMemberRole.OWNER
                                                    || groupMemberRole == GroupMemberRole.MANAGER;
                                        case OWNER_MANAGER_MEMBER:
                                        case OWNER_MANAGER_MEMBER_REQUIRING_ACCEPTANCE:
                                            return groupMemberRole == GroupMemberRole.OWNER
                                                    || groupMemberRole == GroupMemberRole.MANAGER
                                                    || groupMemberRole == GroupMemberRole.MEMBER;
                                        default:
                                            return false;
                                    }
                                })
                                .map(allowed -> new InvitableAndInvitationStrategy(allowed, groupInvitationStrategy))
                                .defaultIfEmpty(new InvitableAndInvitationStrategy(false, groupInvitationStrategy));
                    }
                });
    }

    public Mono<Boolean> isAllowedToBeInvited(@NotNull Long groupId, @NotNull Long inviteeId) {
        return isGroupMember(groupId, inviteeId)
                .flatMap(isGroupMember -> {
                    if (isGroupMember == null || !isGroupMember) {
                        return isBlacklisted(groupId, inviteeId)
                                .map(isBlacklisted -> !isBlacklisted);
                    } else {
                        return Mono.just(false);
                    }
                });
    }

    // Note: a blacklisted user is never a group member
    public Mono<Boolean> isAllowedToSendMessage(@NotNull Long groupId, @NotNull Long senderId) {
        return isGroupMember(groupId, senderId)
                .flatMap(authenticated -> {
                    if (authenticated != null && authenticated) {
                        return groupService.isGroupMutedOrInactive(groupId)
                                .flatMap(groupMutedOrInactive -> {
                                    if (groupMutedOrInactive) {
                                        return Mono.just(false);
                                    } else {
                                        return isMemberMuted(groupId, senderId)
                                                .map(muted -> !muted);
                                    }
                                });
                    } else {
                        return groupService.queryGroupType(groupId)
                                .flatMap(type -> {
                                    Boolean speakable = type.getGuestSpeakable();
                                    if (speakable != null && speakable) {
                                        return groupService.isGroupMutedOrInactive(groupId)
                                                .flatMap(groupMutedOrInactive -> {
                                                    if (groupMutedOrInactive) {
                                                        return Mono.just(false);
                                                    } else {
                                                        return isBlacklisted(groupId, senderId)
                                                                .map(isBlacklisted -> !isBlacklisted);
                                                    }
                                                });
                                    } else {
                                        return Mono.just(false);
                                    }
                                })
                                .defaultIfEmpty(false);
                    }
                });
    }

    public Mono<Boolean> isMemberMuted(@NotNull Long groupId, @NotNull Long userId) {
        Query query = new Query()
                .addCriteria(Criteria.where(ID_GROUP_ID).is(groupId))
                .addCriteria(Criteria.where(ID_USER_ID).is(userId))
                .addCriteria(Criteria.where(GroupMember.Fields.muteEndDate).gt(new Date()));
        return mongoTemplate.exists(query, GroupMember.class);
    }

    public Mono<GroupMemberRole> queryGroupMemberRole(@NotNull Long userId, @NotNull Long groupId) {
        Query query = new Query()
                .addCriteria(Criteria.where(ID_USER_ID).is(userId))
                .addCriteria(Criteria.where(ID_GROUP_ID).is(groupId));
        query.fields().include(GroupMember.Fields.role);
        return mongoTemplate.findOne(query, GroupMember.class)
                .map(GroupMember::getRole);
    }

    public Mono<Boolean> isOwner(@NotNull Long userId, @NotNull Long groupId) {
        Mono<GroupMemberRole> role = queryGroupMemberRole(userId, groupId);
        return role.map(memberRole -> memberRole == GroupMemberRole.OWNER)
                .defaultIfEmpty(false);
    }

    public Mono<Boolean> isOwnerOrManager(@NotNull Long userId, @NotNull Long groupId) {
        Mono<GroupMemberRole> role = queryGroupMemberRole(userId, groupId);
        return role.map(memberRole -> memberRole == GroupMemberRole.OWNER
                || memberRole == GroupMemberRole.MANAGER)
                .defaultIfEmpty(false);
    }

    public Mono<Boolean> isOwnerOrManagerOrMember(@NotNull Long userId, @NotNull Long groupId) {
        Mono<GroupMemberRole> role = queryGroupMemberRole(userId, groupId);
        return role.map(memberRole -> memberRole == GroupMemberRole.OWNER
                || memberRole == GroupMemberRole.MANAGER
                || memberRole == GroupMemberRole.MEMBER)
                .defaultIfEmpty(false);
    }

    public Flux<Long> queryUserJoinedGroupsIds(@NotNull Long userId) {
        Query query = new Query().addCriteria(Criteria.where(ID_USER_ID).is(userId));
        query.fields().include(ID_GROUP_ID);
        return mongoTemplate.find(query, GroupMember.class)
                .map(groupMember -> groupMember.getKey().getGroupId());
    }

    public Mono<Set<Long>> queryUserJoinedGroupsMembersIds(@NotNull Long userId) {
        return queryUserJoinedGroupsIds(userId)
                .collect(Collectors.toSet())
                .flatMap(groupsIds -> {
                    return queryGroupMembersIds(groupsIds)
                            .collect(Collectors.toSet());
                });
    }

    //TODO: Creatable group types for admins
    public Mono<Boolean> isAllowedToHaveGroupType(
            @NotNull Long requesterId,
            @NotNull Long groupTypeId) {
        Query query = new Query()
                .addCriteria(Criteria.where(ID_USER_ID).is(requesterId))
                .addCriteria(Criteria.where(ID_GROUP_TYPE_ID));
        query.fields().include(UserPermissionGroup.Fields.groupTypeLimit);
        return mongoTemplate.findOne(query, UserPermissionGroup.class)
                .defaultIfEmpty(EMPTY_USER_GROUP_TYPE_PERMISSION)
                .flatMap(permission -> groupService.countOwnedGroups(requesterId, groupTypeId)
                        .map(ownedGroupsNumber -> {
                            int defaultLimit = turmsClusterManager.getTurmsProperties().getGroup()
                                    .getUserOwnedLimitForEachGroupTypeByDefault();
                            if (EMPTY_USER_GROUP_TYPE_PERMISSION == permission) {
                                return ownedGroupsNumber < defaultLimit;
                            } else {
                                return ownedGroupsNumber < permission.getGroupTypeLimit().getOrDefault(
                                        groupTypeId, defaultLimit);
                            }
                        }));
    }

    public Mono<Boolean> isAllowedToCreateJoinQuestion(
            @NotNull Long userId,
            @NotNull Long groupId) {
        return isOwnerOrManager(userId, groupId);
    }

    public Flux<Long> queryGroupMembersIds(@NotNull Long groupId) {
        Query query = new Query().addCriteria(Criteria.where(ID_GROUP_ID).is(groupId));
        query.fields().include(ID_USER_ID);
        return mongoTemplate.find(query, GroupMember.class)
                .map(member -> member.getKey().getUserId());
    }

    public Flux<Long> queryGroupMembersIds(@NotEmpty Set<Long> groupsIds) {
        Query query = new Query().addCriteria(Criteria.where(ID_GROUP_ID).in(groupsIds));
        query.fields().include(ID_USER_ID);
        return mongoTemplate.find(query, GroupMember.class)
                .map(member -> member.getKey().getUserId());
    }

    public Flux<GroupMember> queryGroupMembers(@NotNull Long groupId) {
        Query query = new Query().addCriteria(Criteria.where(ID_GROUP_ID).is(groupId));
        return mongoTemplate.find(query, GroupMember.class);
    }

    public Flux<GroupMember> queryGroupMembers(@NotNull Long groupId, @NotEmpty Set<Long> membersIds) {
        Query query = new Query()
                .addCriteria(Criteria.where(ID_GROUP_ID).is(groupId))
                .addCriteria(Criteria.where(ID_USER_ID).in(membersIds));
        return mongoTemplate.find(query, GroupMember.class);
    }

    //TODO: the method may fail and wait to test again after hazelcast provides the serializer for map.
    public Mono<GroupMembersWithVersion> authAndQueryGroupMembers(
            @NotNull Long requesterId,
            @NotNull Long groupId,
            @NotEmpty Set<Long> membersIds,
            boolean withStatus) {
        return isGroupMember(groupId, requesterId)
                .flatMap(isGroupMember -> {
                    if (isGroupMember == null || !isGroupMember) {
                        return Mono.error(TurmsBusinessException.get(TurmsStatusCode.UNAUTHORIZED));
                    }
                    return queryGroupMembers(groupId, membersIds).collectList();
                })
                .flatMap(members -> {
                    if (members.isEmpty()) {
                        return Mono.error(TurmsBusinessException.get(TurmsStatusCode.NOT_FOUND));
                    }
                    GroupMembersWithVersion.Builder builder = GroupMembersWithVersion.newBuilder();
                    if (withStatus) {
                        return fillMembersBuilderWithStatus(members, builder);
                    } else {
                        for (GroupMember member : members) {
                            im.turms.turms.pojo.dto.GroupMember groupMember = ProtoUtil
                                    .groupMember2proto(member).build();
                            builder.addGroupMembers(groupMember);
                        }
                        return Mono.just(builder.build());
                    }
                });
    }

    public Mono<GroupMembersWithVersion> authAndQueryGroupMembersWithVersion(
            @NotNull Long requesterId,
            @NotNull Long groupId,
            @Nullable Date lastUpdatedDate,
            boolean withStatus) {
        return isGroupMember(groupId, requesterId)
                .flatMap(isGroupMember -> {
                    if (isGroupMember != null && isGroupMember) {
                        return groupVersionService.queryMembersVersion(groupId)
                                .defaultIfEmpty(MAX_DATE);
                    } else {
                        return Mono.error(TurmsBusinessException.get(TurmsStatusCode.UNAUTHORIZED));
                    }
                })
                .flatMap(version -> {
                    if (lastUpdatedDate == null || lastUpdatedDate.before(version)) {
                        return queryGroupMembers(groupId)
                                .collectList()
                                .flatMap(members -> {
                                    if (members.isEmpty()) {
                                        return Mono.error(TurmsBusinessException.get(TurmsStatusCode.NOT_FOUND));
                                    }
                                    GroupMembersWithVersion.Builder builder = GroupMembersWithVersion.newBuilder();
                                    if (withStatus) {
                                        return fillMembersBuilderWithStatus(members, builder);
                                    } else {
                                        for (GroupMember member : members) {
                                            im.turms.turms.pojo.dto.GroupMember groupMember = ProtoUtil
                                                    .groupMember2proto(member).build();
                                            builder.addGroupMembers(groupMember);
                                        }
                                        return Mono.just(builder
                                                .setLastUpdatedDate(Int64Value.newBuilder().setValue(version.getTime()).build())
                                                .build());
                                    }
                                });
                    } else {
                        return Mono.error(TurmsBusinessException.get(TurmsStatusCode.ALREADY_UP_TO_DATE));
                    }
                });
    }

    public Mono<Boolean> authAndUpdateGroupMember(
            @NotNull Long requesterId,
            @NotNull Long groupId,
            @NotNull Long memberId,
            @Nullable String name,
            @Nullable GroupMemberRole role,
            @Nullable Date muteEndDate) {
        Mono<Boolean> authorized;
        if (role != null) {
            authorized = isOwner(requesterId, groupId);
        } else if (muteEndDate != null || (name != null && !requesterId.equals(memberId))) {
            authorized = isOwnerOrManager(requesterId, groupId);
        } else {
            throw TurmsBusinessException.get(TurmsStatusCode.ILLEGAL_ARGUMENTS);
        }
        return authorized
                .flatMap(isAuthorized -> {
                    if (isAuthorized != null && isAuthorized) {
                        return updateGroupMember(groupId, memberId, name, role, muteEndDate, null);
                    } else {
                        return Mono.error(TurmsBusinessException.get(TurmsStatusCode.UNAUTHORIZED));
                    }
                });
    }

    public Mono<Boolean> deleteAllGroupMembers(
            @NotNull Long groupId,
            @Nullable ReactiveMongoOperations operations) {
        Query query = new Query()
                .addCriteria(Criteria.where(ID_GROUP_ID).is(groupId));
        ReactiveMongoOperations mongoOperations = operations != null ? operations : mongoTemplate;
        return mongoOperations.remove(query, GroupMember.class)
                .flatMap(result -> {
                    if (result.wasAcknowledged()) {
                        return groupVersionService.updateMembersVersion(groupId)
                                .thenReturn(true);
                    } else {
                        return Mono.just(false);
                    }
                });
    }

    public Flux<Long> queryGroupManagersAndOwnerId(@NotNull Long groupId) {
        Query query = new Query()
                .addCriteria(Criteria.where(ID_GROUP_ID).is(groupId))
                .addCriteria(Criteria.where(GroupMember.Fields.role)
                        .in(GroupMemberRole.MANAGER, GroupMemberRole.OWNER));
        return mongoTemplate.find(query, GroupMember.class)
                .map(member -> member.getKey().getUserId());
    }

    private Mono<GroupMembersWithVersion> fillMembersBuilderWithStatus(
            List<GroupMember> members,
            GroupMembersWithVersion.Builder builder) {
        List<Mono<UserOnlineInfo>> monoList = new ArrayList<>(members.size());
        for (GroupMember member : members) {
            Long userId = member.getKey().getUserId();
            monoList.add(onlineUserService.queryUserOnlineInfo(userId)
                    .defaultIfEmpty(OFFLINE_USER_ONLINE_INFO));
        }
        return Mono.zip(monoList, objects -> objects)
                .map(results -> {
                    for (int i = 0; i < members.size(); i++) {
                        GroupMember member = members.get(i);
                        UserOnlineInfo info = (UserOnlineInfo) results[i];
                        im.turms.turms.pojo.dto.GroupMember groupMember = ProtoUtil
                                .userOnlineInfo2groupMember(member.getKey().getUserId(), info)
                                .build();
                        builder.addGroupMembers(groupMember);
                    }
                    return builder.build();
                });
    }

    public Mono<Boolean> exists(@NotNull Long groupId, @NotNull Long userId) {
        Query query = new Query()
                .addCriteria(Criteria.where(ID_GROUP_ID).is(groupId))
                .addCriteria(Criteria.where(ID_USER_ID).is(userId));
        return mongoTemplate.exists(query, GroupMember.class);
    }
}
