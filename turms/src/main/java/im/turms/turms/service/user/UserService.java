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

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;
import im.turms.common.TurmsCloseStatus;
import im.turms.common.TurmsStatusCode;
import im.turms.common.constant.ProfileAccessStrategy;
import im.turms.common.exception.TurmsBusinessException;
import im.turms.common.util.Validator;
import im.turms.turms.annotation.constraint.ProfileAccessConstraint;
import im.turms.turms.builder.QueryBuilder;
import im.turms.turms.builder.UpdateBuilder;
import im.turms.turms.constant.CloseStatusFactory;
import im.turms.turms.manager.TurmsClusterManager;
import im.turms.turms.pojo.bo.DateRange;
import im.turms.turms.pojo.domain.User;
import im.turms.turms.pojo.domain.UserLoginLog;
import im.turms.turms.pojo.domain.UserOnlineUserNumber;
import im.turms.turms.service.group.GroupMemberService;
import im.turms.turms.service.user.onlineuser.OnlineUserService;
import im.turms.turms.service.user.relationship.UserRelationshipGroupService;
import im.turms.turms.service.user.relationship.UserRelationshipService;
import im.turms.turms.util.AggregationUtil;
import im.turms.turms.util.TurmsPasswordUtil;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.annotation.Nullable;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.PastOrPresent;
import java.time.Duration;
import java.util.*;

import static im.turms.turms.constant.Common.*;

@Component
@Validated
public class UserService {
    private final GroupMemberService groupMemberService;
    private final UserRelationshipService userRelationshipService;
    private final UserRelationshipGroupService userRelationshipGroupService;
    private final UserVersionService userVersionService;
    private final OnlineUserService onlineUserService;
    private final TurmsClusterManager turmsClusterManager;
    private final TurmsPasswordUtil turmsPasswordUtil;
    private final ReactiveMongoTemplate mongoTemplate;
    private final Cache<Long, User> userInfoForLoginCache;

    public UserService(
            UserRelationshipService userRelationshipService,
            GroupMemberService groupMemberService,
            TurmsPasswordUtil turmsPasswordUtil,
            TurmsClusterManager turmsClusterManager,
            UserVersionService userVersionService,
            @Qualifier("userMongoTemplate") ReactiveMongoTemplate mongoTemplate,
            @Lazy OnlineUserService onlineUserService,
            UserRelationshipGroupService userRelationshipGroupService) {
        this.userRelationshipService = userRelationshipService;
        this.groupMemberService = groupMemberService;
        this.turmsPasswordUtil = turmsPasswordUtil;
        this.turmsClusterManager = turmsClusterManager;
        this.userVersionService = userVersionService;
        this.mongoTemplate = mongoTemplate;
        this.onlineUserService = onlineUserService;
        this.userRelationshipGroupService = userRelationshipGroupService;
        // Note that only cache for seconds
        // so that won't get in trouble
        userInfoForLoginCache = Caffeine.newBuilder()
                .maximumSize(1024)
                .expireAfterWrite(Duration.ofSeconds(3))
                .build();
    }

    public Mono<Void> cacheRequiredUserInfoForLogin(Long userId) {
        User user = userInfoForLoginCache.getIfPresent(userId);
        if (user == null) {
            Query query = new Query()
                    .addCriteria(Criteria.where(ID).is(userId));
            return mongoTemplate.findOne(query, User.class)
                    .doOnNext(usr -> userInfoForLoginCache.put(usr.getId(), usr))
                    .then();
        } else {
            return Mono.empty();
        }
    }

    /**
     * AuthenticateAdmin the user through the ServerHttpRequest object during handshake.
     * WARNING: Because during handshake the WebSocket APIs on Browser can only allowed to set the cookie value,
     *
     * @return return the userId If the user information is matched.
     * return null If the userId and the token are unmatched.
     */
    public Mono<Boolean> authenticate(
            @NotNull Long userId,
            @NotNull String rawPassword) {
        Query query = new Query()
                .addCriteria(Criteria.where(ID).is(userId));
        query.fields().include(User.Fields.PASSWORD);
        return mongoTemplate.findOne(query, User.class)
                .map(user -> {
                    String encodedPassword = user.getPassword();
                    return encodedPassword != null && turmsPasswordUtil.matchesUserPassword(rawPassword, encodedPassword);
                })
                .defaultIfEmpty(false);
    }

    public Mono<Boolean> isActiveAndNotDeleted(@NotNull Long userId) {
        Query query = new Query()
                .addCriteria(Criteria.where(ID).is(userId))
                .addCriteria(Criteria.where(User.Fields.IS_ACTIVE).is(true))
                .addCriteria(Criteria.where(User.Fields.DELETION_DATE).is(null));
        return mongoTemplate.exists(query, User.class);
    }

    public Mono<Boolean> isAllowedToSendMessageToTarget(
            @NotNull Boolean isGroupMessage,
            @NotNull Boolean isSystemMessage,
            @NotNull Long requesterId,
            @NotNull Long targetId) {
        if (isSystemMessage) {
            return Mono.just(true);
        }
        if (isGroupMessage) {
            return groupMemberService.isAllowedToSendMessage(targetId, requesterId);
        } else {
            if (requesterId.equals(targetId)) {
                if (turmsClusterManager.getTurmsProperties()
                        .getMessage().isAllowSendingMessagesToOneself()) {
                    return Mono.just(true);
                } else {
                    return Mono.error(TurmsBusinessException.get(TurmsStatusCode.DISABLED_FUNCTION));
                }
            } else {
                if (turmsClusterManager.getTurmsProperties().getMessage().isAllowSendingMessagesToStranger()) {
                    if (turmsClusterManager.getTurmsProperties().getMessage().isCheckIfTargetActiveAndNotDeleted()) {
                        return isActiveAndNotDeleted(targetId)
                                .zipWith(userRelationshipService.isNotBlocked(targetId, requesterId))
                                .map(results -> results.getT1() && results.getT2());
                    } else {
                        return userRelationshipService.isNotBlocked(targetId, requesterId);
                    }
                } else {
                    return userRelationshipService.isRelatedAndAllowed(targetId, requesterId);
                }
            }
        }
    }

    public Mono<User> addUser(
            @Nullable Long id,
            @Nullable String rawPassword,
            @Nullable String name,
            @Nullable String intro,
            @Nullable @ProfileAccessConstraint ProfileAccessStrategy profileAccess,
            @Nullable Long permissionGroupId,
            @Nullable @PastOrPresent Date registrationDate,
            @Nullable Boolean isActive) {
        Date now = new Date();
        id = id != null ? id : turmsClusterManager.generateRandomId();
        rawPassword = rawPassword != null ? rawPassword : RandomStringUtils.randomAlphanumeric(16);
        name = name != null ? name : "";
        intro = intro != null ? intro : "";
        profileAccess = profileAccess != null ? profileAccess : ProfileAccessStrategy.ALL;
        permissionGroupId = permissionGroupId != null ? permissionGroupId : DEFAULT_USER_PERMISSION_GROUP_ID;
        isActive = isActive != null ? isActive : turmsClusterManager.getTurmsProperties().getUser().isActivateUserWhenAdded();
        Date date = registrationDate != null ? registrationDate : now;
        User user = new User(
                id,
                turmsPasswordUtil.encodeUserPassword(rawPassword),
                name,
                intro,
                profileAccess,
                permissionGroupId,
                date,
                null,
                isActive,
                now);
        Long finalId = id;
        return mongoTemplate.inTransaction()
                .execute(operations -> operations.save(user)
                        .then(userRelationshipGroupService.createRelationshipGroup(finalId, 0, "", now, operations))
                        .then(userVersionService.upsertEmptyUserVersion(user.getId(), date, operations))
                        .thenReturn(user))
                .retryWhen(TRANSACTION_RETRY)
                .singleOrEmpty();
    }

    public Mono<Boolean> isAllowToQueryUserProfile(
            @NotNull Long requesterId,
            @NotNull Long targetUserId) {
        Query query = new Query().addCriteria(Criteria.where(ID).is(targetUserId));
        query.fields().include(User.Fields.PROFILE_ACCESS);
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
            @NotNull Long userId,
            boolean shouldQueryDeletedRecords) {
        return authAndQueryUsersProfiles(requesterId, Set.of(userId), shouldQueryDeletedRecords).singleOrEmpty();
    }

    public Flux<User> authAndQueryUsersProfiles(
            @NotNull Long requesterId,
            @NotEmpty Set<Long> userIds,
            boolean shouldQueryDeletedRecords) {
        List<Mono<Boolean>> monos = new ArrayList<>(userIds.size());
        for (Long userId : userIds) {
            monos.add(isAllowToQueryUserProfile(requesterId, userId)
                    .switchIfEmpty(Mono.error(TurmsBusinessException.get(TurmsStatusCode.TARGET_USERS_NOT_EXIST))));
        }
        return Mono.zip(monos, objects -> objects)
                .flatMapMany(results -> {
                    for (Object result : results) {
                        if (!(boolean) result) {
                            throw TurmsBusinessException.get(TurmsStatusCode.UNAUTHORIZED);
                        }
                    }
                    return queryUsersProfiles(userIds, shouldQueryDeletedRecords);
                });
    }

    public Flux<User> queryUsersProfiles(@NotEmpty Set<Long> userIds, boolean shouldQueryDeletedRecords) {
        Query query = QueryBuilder
                .newBuilder()
                .add(Criteria.where(ID).in(userIds))
                .addIsNullIfFalse(User.Fields.DELETION_DATE, shouldQueryDeletedRecords)
                .buildQuery();
        query.fields()
                .include(ID)
                .include(User.Fields.NAME)
                .include(User.Fields.INTRO)
                .include(User.Fields.REGISTRATION_DATE)
                .include(User.Fields.PROFILE_ACCESS)
                .include(User.Fields.PERMISSION_GROUP_ID)
                .include(User.Fields.IS_ACTIVE);
        return mongoTemplate.find(query, User.class);
    }

    public Mono<Long> queryUserPermissionGroupId(@NotNull Long userId) {
        Query query = new Query().addCriteria(Criteria.where(ID).is(userId));
        query.fields().include(User.Fields.PERMISSION_GROUP_ID);
        return mongoTemplate.findOne(query, User.class)
                .map(User::getPermissionGroupId);
    }

    public Mono<Boolean> deleteUsers(
            @NotEmpty Set<Long> userIds,
            @Nullable Boolean shouldDeleteLogically) {
        Query query = new Query().addCriteria(Criteria.where(ID).in(userIds));
        Mono<Boolean> deleteOrUpdateMono;
        if (shouldDeleteLogically) {
            Date now = new Date();
            Update update = new Update()
                    .set(User.Fields.DELETION_DATE, now)
                    .set(User.Fields.LAST_UPDATED_DATE, now);
            deleteOrUpdateMono = mongoTemplate.updateMulti(query, update, User.class)
                    .map(UpdateResult::wasAcknowledged);
        } else {
            deleteOrUpdateMono = mongoTemplate.inTransaction()
                    .execute(operations -> operations.remove(query, User.class)
                            .map(DeleteResult::wasAcknowledged)
                            .flatMap(acknowledged -> {
                                if (acknowledged != null && acknowledged) {
                                    return userRelationshipService.deleteAllRelationships(userIds, operations, false)
                                            .then(userRelationshipGroupService.deleteAllRelationshipGroups(userIds, operations, false))
                                            .then(userVersionService.delete(userIds, operations))
                                            .thenReturn(true);
                                } else {
                                    return Mono.just(false);
                                }
                            }))
                    .retryWhen(TRANSACTION_RETRY)
                    .singleOrEmpty();
        }
        return deleteOrUpdateMono.flatMap(success -> {
            if (success) {
                return onlineUserService.setUsersOffline(userIds, CloseStatusFactory.get(TurmsCloseStatus.USER_IS_DELETED_OR_INACTIVATED))
                        .then(Mono.just(true));
            } else {
                return Mono.just(false);
            }
        });
    }

    public Mono<Boolean> userExists(@NotNull Long userId, boolean shouldQueryDeletedRecords) {
        Query query = QueryBuilder
                .newBuilder()
                .add(Criteria.where(ID).is(userId))
                .addIsNullIfFalse(User.Fields.DELETION_DATE, shouldQueryDeletedRecords)
                .buildQuery();
        return mongoTemplate.exists(query, User.class);
    }

    public Mono<Boolean> updateUser(
            @NotNull Long userId,
            @Nullable String rawPassword,
            @Nullable String name,
            @Nullable String intro,
            @Nullable @ProfileAccessConstraint ProfileAccessStrategy profileAccessStrategy,
            @Nullable Long permissionGroupId,
            @Nullable Boolean isActive,
            @Nullable @PastOrPresent Date registrationDate) {
        return updateUsers(Collections.singleton(userId),
                rawPassword,
                name,
                intro,
                profileAccessStrategy,
                permissionGroupId,
                registrationDate,
                isActive);
    }

    public Flux<User> queryUsers(
            @Nullable Collection<Long> userIds,
            @Nullable DateRange registrationDateRange,
            @Nullable DateRange deletionDateRange,
            @Nullable Boolean isActive,
            @Nullable Integer page,
            @Nullable Integer size,
            boolean shouldQueryDeletedRecords) {
        Query query = QueryBuilder
                .newBuilder()
                .addInIfNotNull(ID, userIds)
                .addBetweenIfNotNull(User.Fields.REGISTRATION_DATE, registrationDateRange)
                .addBetweenIfNotNull(User.Fields.DELETION_DATE, deletionDateRange)
                .addIsIfNotNull(User.Fields.IS_ACTIVE, isActive)
                .addIsNullIfFalse(User.Fields.DELETION_DATE, shouldQueryDeletedRecords)
                .paginateIfNotNull(page, size);
        return mongoTemplate.find(query, User.class);
    }

    public Mono<Long> countRegisteredUsers(@Nullable DateRange dateRange, boolean shouldQueryDeletedRecords) {
        Query query = QueryBuilder.newBuilder()
                .addBetweenIfNotNull(User.Fields.REGISTRATION_DATE, dateRange)
                .addIsNullIfFalse(User.Fields.DELETION_DATE, shouldQueryDeletedRecords)
                .buildQuery();
        return mongoTemplate.count(query, User.class);
    }

    public Mono<Long> countDeletedUsers(@Nullable DateRange dateRange) {
        Query query = QueryBuilder.newBuilder()
                .addBetweenIfNotNull(User.Fields.DELETION_DATE, dateRange)
                .buildQuery();
        return mongoTemplate.count(query, User.class);
    }

    public Mono<Long> countLoggedInUsers(@Nullable DateRange dateRange, boolean shouldQueryDeletedRecords) {
        Criteria criteria = QueryBuilder.newBuilder()
                .addBetweenIfNotNull(UserLoginLog.Fields.LOGIN_DATE, dateRange)
                .addIsNullIfFalse(User.Fields.DELETION_DATE, shouldQueryDeletedRecords)
                .buildCriteria();
        return AggregationUtil.countDistinct(
                mongoTemplate,
                criteria,
                UserLoginLog.Fields.USER_ID,
                UserLoginLog.class);
    }

    public Mono<Long> countUsers(boolean shouldQueryDeletedRecords) {
        Query query = QueryBuilder
                .newBuilder()
                .addIsNullIfFalse(User.Fields.DELETION_DATE, shouldQueryDeletedRecords)
                .buildQuery();
        return mongoTemplate.count(query, User.class);
    }

    public Mono<Long> countUsers(
            @Nullable Set<Long> userIds,
            @Nullable DateRange registrationDateRange,
            @Nullable DateRange deletionDateRange,
            @Nullable Boolean isActive) {
        Query query = QueryBuilder
                .newBuilder()
                .addInIfNotNull(ID, userIds)
                .addBetweenIfNotNull(User.Fields.REGISTRATION_DATE, registrationDateRange)
                .addBetweenIfNotNull(User.Fields.DELETION_DATE, deletionDateRange)
                .addIsIfNotNull(User.Fields.IS_ACTIVE, isActive)
                .buildQuery();
        return mongoTemplate.count(query, User.class);
    }

    public Mono<Long> countMaxOnlineUsers(@Nullable DateRange dateRange) {
        Query query = QueryBuilder
                .newBuilder()
                .addBetweenIfNotNull(ID, dateRange)
                .max(UserOnlineUserNumber.Fields.NUMBER)
                .buildQuery();
        return mongoTemplate.findOne(query, UserOnlineUserNumber.class)
                .map(entity -> (long) entity.getNumber())
                .defaultIfEmpty(0L);
    }

    public Mono<Boolean> updateUsers(
            @NotEmpty Set<Long> userIds,
            @Nullable String rawPassword,
            @Nullable String name,
            @Nullable String intro,
            @Nullable @ProfileAccessConstraint ProfileAccessStrategy profileAccessStrategy,
            @Nullable Long permissionGroupId,
            @Nullable @PastOrPresent Date registrationDate,
            @Nullable Boolean isActive) {
        if (Validator.areAllFalsy(rawPassword,
                name,
                intro,
                profileAccessStrategy,
                registrationDate,
                isActive)) {
            return Mono.just(true);
        }
        String password = null;
        if (rawPassword != null && !rawPassword.isEmpty()) {
            password = turmsPasswordUtil.encodeUserPassword(rawPassword);
        }
        Query query = new Query().addCriteria(Criteria.where(ID).in(userIds));
        Update update = UpdateBuilder.newBuilder()
                .setIfNotNull(User.Fields.PASSWORD, password)
                .setIfNotNull(User.Fields.NAME, name)
                .setIfNotNull(User.Fields.INTRO, intro)
                .setIfNotNull(User.Fields.PROFILE_ACCESS, profileAccessStrategy)
                .setIfNotNull(User.Fields.PERMISSION_GROUP_ID, permissionGroupId)
                .setIfNotNull(User.Fields.REGISTRATION_DATE, registrationDate)
                .setIfNotNull(User.Fields.IS_ACTIVE, isActive)
                .setIfNotNull(User.Fields.LAST_UPDATED_DATE, new Date())
                .build();
        return mongoTemplate.updateMulti(query, update, User.class)
                .flatMap(result -> {
                    if (result.wasAcknowledged()) {
                        if (isActive != null && !isActive) {
                            return Mono.just(onlineUserService
                                    .setUsersOffline(userIds, CloseStatusFactory.get(TurmsCloseStatus.USER_IS_DELETED_OR_INACTIVATED)))
                                    .thenReturn(true);
                        } else {
                            return Mono.just(true);
                        }
                    } else {
                        return Mono.just(false);
                    }
                });
    }
}
