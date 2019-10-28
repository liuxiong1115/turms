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

import com.google.protobuf.Int64Value;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;
import im.turms.turms.cluster.TurmsClusterManager;
import im.turms.turms.common.*;
import im.turms.turms.constant.ChatType;
import im.turms.turms.constant.ProfileAccessStrategy;
import im.turms.turms.exception.TurmsBusinessException;
import im.turms.turms.pojo.domain.GroupInvitation;
import im.turms.turms.pojo.domain.User;
import im.turms.turms.pojo.domain.UserLoginLog;
import im.turms.turms.pojo.domain.UserOnlineUserNumber;
import im.turms.turms.pojo.response.GroupInvitationsWithVersion;
import im.turms.turms.service.group.GroupInvitationService;
import im.turms.turms.service.group.GroupMemberService;
import im.turms.turms.service.user.relationship.UserRelationshipService;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.annotation.Nullable;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.*;
import java.util.stream.Collectors;

import static im.turms.turms.common.Constants.ID;
import static im.turms.turms.common.Constants.MAX_DATE;

@Component
public class UserService {
    private static final String REGISTERED_USERS = "registeredUsers";
    private static final String DELETED_USERS = "deletedUsers";
    private static final String USERS_WHO_SENT_MESSAGES = "usersWhoSentMessages";
    private static final String LOGGED_IN_USERS = "loggedInUsers";
    private static final String MAX_ONLINE_USERS = "maxOnlineUsers";
    private static final String USERS = "users";
    private final GroupMemberService groupMemberService;
    private final GroupInvitationService groupInvitationService;
    private final UserRelationshipService userRelationshipService;
    private final UserVersionService userVersionService;
    private final TurmsClusterManager turmsClusterManager;
    private final TurmsPasswordUtil turmsPasswordUtil;
    private final ReactiveMongoTemplate mongoTemplate;

    public UserService(UserRelationshipService userRelationshipService, GroupMemberService groupMemberService, TurmsPasswordUtil turmsPasswordUtil, TurmsClusterManager turmsClusterManager, UserVersionService userVersionService, ReactiveMongoTemplate mongoTemplate, GroupInvitationService groupInvitationService) {
        this.userRelationshipService = userRelationshipService;
        this.groupMemberService = groupMemberService;
        this.turmsPasswordUtil = turmsPasswordUtil;
        this.turmsClusterManager = turmsClusterManager;
        this.userVersionService = userVersionService;
        this.mongoTemplate = mongoTemplate;
        this.groupInvitationService = groupInvitationService;
    }

    /**
     * AuthenticateAdmin the user through the ServerHttpRequest object during handshake.
     * WARNING: Because during handshake the WebSocket APIs on Browser can only allowed to set the cookie value,
     *
     * @return return the userId If the user information is matched.
     * return null If the userId and the token are unmatched.
     */
    public Mono<Boolean> authenticate(@NotNull Long userId, @NotNull String password) {
        Query query = new Query()
                .addCriteria(Criteria.where(ID).is(userId))
                .addCriteria(Criteria.where(User.Fields.password).is(password));
        return mongoTemplate.exists(query, User.class);
    }

    public Mono<Boolean> isActive(@NotNull Long userId) {
        Query query = new Query()
                .addCriteria(Criteria.where(ID).is(userId))
                .addCriteria(Criteria.where(User.Fields.active).is(true));
        return mongoTemplate.exists(query, User.class);
    }

    public Mono<Boolean> isAllowedToSendMessageToTarget(
            @NotNull ChatType chatType,
            @NotNull Long requesterId,
            @NotNull Long targetId) {
        switch (chatType) {
            case PRIVATE:
                if (requesterId.equals(targetId)) {
                    return Mono.just(turmsClusterManager.getTurmsProperties()
                            .getMessage().isAllowSendingMessagesToOneself());
                }
                return isActive(requesterId)
                        .flatMap(isActive -> {
                            if (isActive == null || !isActive) {
                                return Mono.error(TurmsBusinessException.get(TurmsStatusCode.UNAUTHORIZED));
                            } else {
                                if (turmsClusterManager.getTurmsProperties().getMessage().isAllowSendingMessagesToStranger()) {
                                    if (turmsClusterManager.getTurmsProperties().getMessage().isCheckIfTargetExists()) {
                                        return userExists(targetId)
                                                .zipWith(userRelationshipService.isNotBlocked(targetId, requesterId))
                                                .map(results -> results.getT1() && results.getT2());
                                    } else {
                                        return userRelationshipService.isNotBlocked(targetId, requesterId);
                                    }
                                } else {
                                    return userRelationshipService.isRelatedAndAllowed(targetId, requesterId);
                                }
                            }
                });
            case GROUP:
                return groupMemberService.isAllowedToSendMessage(targetId, requesterId);
            case SYSTEM:
                // TODO: 0.9.0
            case UNRECOGNIZED:
            default:
                return Mono.just(false);
        }
    }

    public Mono<User> addUser(
            @Nullable Long id,
            @Nullable String rawPassword,
            @Nullable String name,
            @Nullable String intro,
            @Nullable String profilePictureUrl,
            @Nullable Date registrationDate,
            @Nullable Boolean active) {
        User user = new User();
        id = id != null ? id : turmsClusterManager.generateRandomId();
        rawPassword = rawPassword != null ? rawPassword : RandomStringUtils.randomAlphanumeric(16);
        name = name != null ? name : "";
        intro = intro != null ? intro : "";
        profilePictureUrl = profilePictureUrl != null ? profilePictureUrl : "";
        registrationDate = registrationDate != null ? registrationDate : new Date();
        active = active != null ? active : true;
        user.setId(id);
        user.setPassword(turmsPasswordUtil.encodeUserPassword(rawPassword));
        user.setName(name);
        user.setIntro(intro);
        user.setProfilePictureUrl(profilePictureUrl);
        user.setRegistrationDate(registrationDate);
        user.setActive(active);
        return mongoTemplate.inTransaction()
                .execute(operations -> operations.insert(user)
                        .then(userVersionService.upsertEmptyUserVersion(user.getId(), operations))
                        .thenReturn(user))
                .single();
    }

    public Flux<GroupInvitation> queryUserGroupInvitations(
            @NotNull Long userId,
            @Nullable Date lastUpdatedDate) {
        return userVersionService.queryGroupInvitationsLastUpdatedDate(userId)
                .defaultIfEmpty(MAX_DATE)
                .flatMapMany(version -> {
                    if (lastUpdatedDate == null || lastUpdatedDate.before(version)) {
                        return groupInvitationService.queryGroupInvitationsByInviteeId(userId);
                    } else {
                        return Mono.error(TurmsBusinessException.get(TurmsStatusCode.ALREADY_UP_TO_DATE));
                    }
                });
    }

    public Mono<GroupInvitationsWithVersion> queryUserGroupInvitationsWithVersion(
            @NotNull Long userId,
            @Nullable Date lastUpdatedDate) {
        return userVersionService.queryGroupInvitationsLastUpdatedDate(userId)
                .defaultIfEmpty(MAX_DATE)
                .flatMap(version -> {
                    if (lastUpdatedDate == null || lastUpdatedDate.before(version)) {
                        return groupInvitationService.queryGroupInvitationsByInviteeId(userId)
                                .collect(Collectors.toSet())
                                .map(groupInvitations -> {
                                    GroupInvitationsWithVersion.Builder builder = GroupInvitationsWithVersion.newBuilder();
                                    for (GroupInvitation groupInvitation : groupInvitations) {
                                        builder.addGroupInvitations(ProtoUtil.groupInvitation2proto(groupInvitation));
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

    public Mono<User> queryUser(@NotNull Long userId) {
        return mongoTemplate.findById(userId, User.class);
    }

    public Mono<Boolean> isAllowToQueryUserProfile(
            @NotNull Long requesterId,
            @NotNull Long targetUserId) {
        Query query = new Query().addCriteria(Criteria.where(ID).is(targetUserId));
        query.fields().include(User.Fields.profileAccess);
        return mongoTemplate.findOne(query, User.class)
                .flatMap(user -> {
                    switch (user.getProfileAccess()) {
                        case ALL:
                            return Mono.just(true);
                        case FRIENDS:
                            return userRelationshipService.isRelatedAndAllowed(targetUserId, requesterId);
                        case ALL_EXCEPT_BLACKLISTED_USERS:
                            return userRelationshipService.isNotBlocked(targetUserId, requesterId);
                        case UNRECOGNIZED:
                        default:
                            return Mono.just(false);
                    }
                });
    }

    public Mono<User> authAndQueryUserProfile(
            @NotNull Long requesterId,
            @NotNull Long userId) {
        return authAndQueryUsersProfiles(requesterId, Collections.singleton(userId)).single();
    }

    public Mono<User> queryUserProfile(@NotNull Long userId) {
        return queryUsersProfiles(Collections.singleton(userId))
                .single();
    }

    public Flux<User> authAndQueryUsersProfiles(
            @NotNull Long requesterId,
            @NotEmpty Set<Long> userIds) {
        List<Mono<Boolean>> monos = new ArrayList<>(userIds.size());
        for (Long userId : userIds) {
            monos.add(isAllowToQueryUserProfile(requesterId, userId));
        }
        return Mono.zip(monos, objects -> objects)
                .flatMapMany(results -> {
                    if (!BooleanUtils.and((Boolean[]) results)) {
                        throw TurmsBusinessException.get(TurmsStatusCode.UNAUTHORIZED);
                    }
                    return queryUsersProfiles(userIds);
                });
    }

    public Flux<User> queryUsersProfiles(@NotEmpty Set<Long> userIds) {
        Query query = new Query().addCriteria(Criteria.where(ID).in(userIds));
        query.fields()
                .include(ID)
                .include(User.Fields.name)
                .include(User.Fields.intro)
                .include(User.Fields.profilePictureUrl)
                .include(User.Fields.registrationDate)
                .include(User.Fields.profileAccess)
                .include(User.Fields.active);
        return mongoTemplate.find(query, User.class);
    }

    public Mono<Boolean> deleteUsers(
            @NotEmpty Set<Long> userIds,
            boolean deleteRelationships,
            @Nullable Boolean logicalDelete) {
        Query query = new Query().addCriteria(Criteria.where(ID).in(userIds));
        if (logicalDelete == null) {
            logicalDelete = turmsClusterManager.getTurmsProperties().getUser().isLogicallyDeleteUser();
        }
        if (deleteRelationships) {
            boolean finalLogicallyDeleteUser = logicalDelete;
            return mongoTemplate.inTransaction()
                    .execute(operations -> {
                        Mono<Boolean> updateOrRemove;
                        Update update = new Update().set(User.Fields.deletionDate, new Date());
                        if (finalLogicallyDeleteUser) {
                            updateOrRemove = operations.updateFirst(query, update, User.class)
                                    .map(UpdateResult::wasAcknowledged);
                        } else {
                            updateOrRemove = operations.remove(query, User.class)
                                    .map(DeleteResult::wasAcknowledged);
                        }
                        return updateOrRemove
                                .flatMap(acknowledged -> {
                                    if (acknowledged != null && acknowledged) {
                                        return userRelationshipService.deleteAllRelatedRelationships(userIds, operations);
                                    } else {
                                        return Mono.just(false);
                                    }
                                });
                    })
                    .single();
        } else {
            if (logicalDelete) {
                Update update = new Update().set(User.Fields.deletionDate, new Date());
                return mongoTemplate.updateFirst(query, update, User.class)
                        .map(UpdateResult::wasAcknowledged);
            } else {
                return mongoTemplate.remove(query, User.class)
                        .zipWith(userVersionService.delete(userIds, null)) //TODO -> transaction
                        .map(result -> result.getT1().wasAcknowledged());
            }
        }
    }

    public Mono<Boolean> userExists(@NotNull Long userId) {
        Query query = new Query().addCriteria(Criteria.where(ID).is(userId));
        return mongoTemplate.exists(query, User.class);
    }

    public Mono<Boolean> updateUser(
            @NotNull Long userId,
            @Nullable String rawPassword,
            @Nullable String name,
            @Nullable String intro,
            @Nullable String profilePictureUrl,
            @Nullable ProfileAccessStrategy profileAccessStrategy,
            @Nullable Boolean active,
            @Nullable Date registrationDate) {
        return updateUsers(Collections.singleton(userId),
                rawPassword,
                name,
                intro,
                profilePictureUrl,
                profileAccessStrategy,
                registrationDate,
                active);
    }

    public Flux<User> queryUsers(
            @Nullable Collection<Long> userIds,
            @Nullable Date registrationDateStart,
            @Nullable Date registrationDateEnd,
            @Nullable Date deletionDateStart,
            @Nullable Date deletionDateEnd,
            @Nullable Boolean active,
            int page,
            int size) {
        QueryBuilder builder = QueryBuilder.newBuilder();
        if (userIds != null && !userIds.isEmpty()) {
            builder.add(Criteria.where(ID).in(userIds));
        }
        Query query = builder
                .addBetweenIfNotNull(User.Fields.registrationDate, registrationDateStart, registrationDateEnd)
                .addBetweenIfNotNull(User.Fields.deletionDate, deletionDateStart, deletionDateEnd)
                .addIfNotNull(Criteria.where(User.Fields.active).is(active), active)
                .paginateIfNotNull(page, size);
        return mongoTemplate.find(query, User.class);
    }

    public Mono<Long> countRegisteredUsers(
            @Nullable Date startDate,
            @Nullable Date endDate) {
        Query query = QueryBuilder.newBuilder()
                .addBetweenIfNotNull(User.Fields.registrationDate, startDate, endDate)
                .buildQuery();
        return mongoTemplate.count(query, User.class);
    }

    public Mono<Long> countDeletedUsers(@Nullable Date startDate, @Nullable Date endDate) {
        Query query = QueryBuilder.newBuilder()
                .addBetweenIfNotNull(User.Fields.deletionDate, startDate, endDate)
                .buildQuery();
        return mongoTemplate.count(query, User.class);
    }

    public Mono<Long> countLoggedInUsers(@Nullable Date startDate, @Nullable Date endDate) {
        Criteria criteria = QueryBuilder.newBuilder()
                .addBetweenIfNotNull(UserLoginLog.Fields.loginDate, startDate, endDate)
                .buildCriteria();
        return AggregationUtil.countDistinct(
                mongoTemplate,
                criteria,
                UserLoginLog.Fields.userId,
                UserLoginLog.class);
    }

    public Mono<Long> countUsers() {
        return mongoTemplate.count(new Query(), User.class);
    }

    public Mono<Long> countMaxOnlineUsers(@Nullable Date startDate, @Nullable Date endDate) {
        Query query = QueryBuilder
                .newBuilder()
                .addBetweenIfNotNull(UserOnlineUserNumber.Fields.timestamp, startDate, endDate)
                .max(UserOnlineUserNumber.Fields.number)
                .buildQuery();
        return mongoTemplate.findOne(query, UserOnlineUserNumber.class)
                .map(entity -> (long) entity.getNumber());
    }

    public Mono<Boolean> updateUsers(
            @NotEmpty Set<Long> userIds,
            @Nullable String password,
            @Nullable String name,
            @Nullable String intro,
            @Nullable String profilePictureUrl,
            @Nullable ProfileAccessStrategy profileAccessStrategy,
            @Nullable Date registrationDate,
            @Nullable Boolean active) {
        Query query = new Query().addCriteria(Criteria.where(ID).in(userIds));
        Update update = UpdateBuilder.newBuilder()
                .setIfNotNull(User.Fields.password, password)
                .setIfNotNull(User.Fields.name, name)
                .setIfNotNull(User.Fields.intro, intro)
                .setIfNotNull(User.Fields.profilePictureUrl, profilePictureUrl)
                .setIfNotNull(User.Fields.profileAccess, profileAccessStrategy)
                .setIfNotNull(User.Fields.registrationDate, registrationDate)
                .setIfNotNull(User.Fields.active, active)
                .build();
        return mongoTemplate.updateMulti(query, update, User.class)
                .flatMap(result -> {
                    if (result.wasAcknowledged()) {
                        List<Mono<Boolean>> monos = new ArrayList<>(userIds.size());
                        for (Long userId : userIds) {
                            monos.add(userVersionService.updateInformationVersion(userId));
                        }
                        return Mono.zip(monos, objects -> objects)
                                .thenReturn(true);
                    } else {
                        return Mono.just(false);
                    }
                });
    }

    @Deprecated
    private void notifyUserStatusChange() {
        throw new UnsupportedOperationException("Server should not to notify users of the changes of others' statues actively because of the huge cost of resources");
    }
}
