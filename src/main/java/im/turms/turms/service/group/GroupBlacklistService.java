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
import im.turms.turms.common.ProtoUtil;
import im.turms.turms.common.TurmsStatusCode;
import im.turms.turms.exception.TurmsBusinessException;
import im.turms.turms.pojo.bo.common.Int64ValuesWithVersion;
import im.turms.turms.pojo.bo.user.UserInfo;
import im.turms.turms.pojo.bo.user.UsersInfosWithVersion;
import im.turms.turms.pojo.domain.GroupBlacklistedUser;
import im.turms.turms.pojo.domain.User;
import im.turms.turms.service.user.UserService;
import org.springframework.data.mongodb.core.ReactiveMongoOperations;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;
import java.util.Date;
import java.util.stream.Collectors;

import static im.turms.turms.common.Constants.*;

@Service
public class GroupBlacklistService {
    private final GroupMemberService groupMemberService;
    private final GroupVersionService groupVersionService;
    private final UserService userService;
    private final ReactiveMongoTemplate mongoTemplate;

    public GroupBlacklistService(
            GroupMemberService groupMemberService,
            ReactiveMongoTemplate mongoTemplate,
            GroupVersionService groupVersionService,
            UserService userService) {
        this.groupMemberService = groupMemberService;
        this.mongoTemplate = mongoTemplate;
        this.groupVersionService = groupVersionService;
        this.userService = userService;
    }

    public Mono<Boolean> blacklistUser(
            @NotNull Long requesterId,
            @NotNull Long groupId,
            @NotNull Long blacklistedUserId,
            @Nullable ReactiveMongoOperations operations) {
        return groupMemberService.isOwnerOrManager(requesterId, groupId)
                .flatMap(authenticated -> {
                    if (authenticated != null && authenticated) {
                        return groupMemberService.isGroupMember(groupId, blacklistedUserId);
                    } else {
                        return Mono.error(TurmsBusinessException.get(TurmsStatusCode.UNAUTHORIZED));
                    }
                })
                .flatMap(isGroupMember -> {
                    GroupBlacklistedUser blacklistedUser = new GroupBlacklistedUser(
                            groupId, blacklistedUserId, new Date(), requesterId);
                    if (isGroupMember != null && isGroupMember) {
                        Mono<Boolean> updateVersion = groupVersionService.updateVersion(
                                groupId,
                                false,
                                true,
                                true,
                                false,
                                false);
                        if (operations != null) {
                            Mono<Boolean> delete = groupMemberService.deleteGroupMember(groupId, blacklistedUserId, operations);
                            return Mono.zip(delete, operations.insert(blacklistedUser), updateVersion)
                                    .thenReturn(true);
                        } else {
                            return mongoTemplate
                                    .inTransaction()
                                    .execute(newOperations ->
                                            Mono.zip(groupMemberService.deleteGroupMember(groupId, blacklistedUserId, newOperations),
                                                    newOperations.insert(blacklistedUser),
                                                    updateVersion)
                                                    .thenReturn(true))
                                    .retryBackoff(MONGO_TRANSACTION_RETRIES_NUMBER, MONGO_TRANSACTION_BACKOFF)
                                    .single();
                        }
                    } else {
                        Mono<Boolean> updateVersion = groupVersionService.updateBlacklistVersion(groupId);
                        ReactiveMongoOperations mongoOperations = operations != null ? operations : mongoTemplate;
                        return Mono.zip(mongoOperations.insert(blacklistedUser),
                                updateVersion)
                                .thenReturn(true);
                    }
                });
    }

    public Mono<Boolean> unblacklistUser(
            @NotNull Long requesterId,
            @NotNull Long groupId,
            @NotNull Long unblacklistedUserId,
            @Nullable ReactiveMongoOperations operations) {
        return groupMemberService
                .isOwnerOrManager(requesterId, groupId)
                .flatMap(authenticated -> {
                    if (authenticated != null && authenticated) {
                        ReactiveMongoOperations mongoOperations = operations != null ? operations : mongoTemplate;
                        Query query = new Query()
                                .addCriteria(Criteria.where(ID_GROUP_ID).is(groupId))
                                .addCriteria(Criteria.where(ID_USER_ID).is(unblacklistedUserId));
                        return mongoOperations.remove(query, GroupBlacklistedUser.class)
                                .flatMap(result -> {
                                    if (result.wasAcknowledged()) {
                                        return groupVersionService.updateBlacklistVersion(groupId)
                                                .thenReturn(true);
                                    } else {
                                        return Mono.just(false);
                                    }
                                });
                    } else {
                        return Mono.error(TurmsBusinessException.get(TurmsStatusCode.UNAUTHORIZED));
                    }
                });
    }

    public Flux<Long> queryGroupBlacklistedUsersIds(@NotNull Long groupId) {
        Query query = new Query().addCriteria(Criteria.where(ID_GROUP_ID).is(groupId));
        query.fields().include(ID_USER_ID);
        return mongoTemplate
                .find(query, GroupBlacklistedUser.class)
                .map(groupBlacklistedUser -> groupBlacklistedUser.getKey().getUserId());
    }

    public Flux<GroupBlacklistedUser> queryGroupBlacklistedUsers(@NotNull Long groupId) {
        Query query = new Query().addCriteria(Criteria.where(ID_GROUP_ID).is(groupId));
        return mongoTemplate.find(query, GroupBlacklistedUser.class);
    }

    public Mono<Int64ValuesWithVersion> queryGroupBlacklistedUsersIdsWithVersion(
            @NotNull Long groupId,
            @Nullable Date lastUpdatedDate) {
        return groupVersionService
                .queryBlacklistVersion(groupId)
                .defaultIfEmpty(MAX_DATE)
                .flatMap(version -> {
                    if (lastUpdatedDate == null || lastUpdatedDate.before(version)) {
                        return queryGroupBlacklistedUsersIds(groupId)
                                .collect(Collectors.toSet())
                                .map(ids -> {
                                    if (ids.isEmpty()) {
                                        throw TurmsBusinessException.get(TurmsStatusCode.NOT_FOUND);
                                    }
                                    return Int64ValuesWithVersion
                                            .newBuilder()
                                            .setLastUpdatedDate(Int64Value.newBuilder().setValue(version.getTime()).build())
                                            .addAllValues(ids)
                                            .build();
                                });
                    } else {
                        return Mono.error(TurmsBusinessException.get(TurmsStatusCode.ALREADY_UP_TO_DATE));
                    }
                });
    }

    public Mono<UsersInfosWithVersion> queryGroupBlacklistedUsersInfosWithVersion(
            @NotNull Long groupId,
            @Nullable Date lastUpdatedDate) {
        return groupVersionService
                .queryBlacklistVersion(groupId)
                .defaultIfEmpty(MAX_DATE)
                .flatMap(version -> {
                    if (lastUpdatedDate == null || lastUpdatedDate.before(version)) {
                        return queryGroupBlacklistedUsersIds(groupId)
                                .collect(Collectors.toSet())
                                .map(ids -> {
                                    if (ids.isEmpty()) {
                                        throw TurmsBusinessException.get(TurmsStatusCode.NOT_FOUND);
                                    }
                                    return ids;
                                })
                                .flatMapMany(userService::queryUsersProfiles)
                                .collect(Collectors.toSet())
                                .map(users -> {
                                    if (users.isEmpty()) {
                                        throw TurmsBusinessException.get(TurmsStatusCode.NOT_FOUND);
                                    }
                                    UsersInfosWithVersion.Builder builder = UsersInfosWithVersion.newBuilder();
                                    builder.setLastUpdatedDate(Int64Value.newBuilder().setValue(version.getTime()).build());
                                    for (User user : users) {
                                        UserInfo userInfo = ProtoUtil.userProfile2proto(user).build();
                                        builder.addUserInfos(userInfo);
                                    }
                                    return builder.build();
                                });
                    } else {
                        return Mono.error(TurmsBusinessException.get(TurmsStatusCode.ALREADY_UP_TO_DATE));
                    }
                });
    }
}
