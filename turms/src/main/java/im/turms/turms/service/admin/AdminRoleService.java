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

package im.turms.turms.service.admin;

import com.hazelcast.replicatedmap.ReplicatedMap;
import im.turms.common.TurmsStatusCode;
import im.turms.common.exception.TurmsBusinessException;
import im.turms.common.util.Validator;
import im.turms.turms.annotation.cluster.PostHazelcastInitialized;
import im.turms.turms.annotation.constraint.NoWhitespaceConstraint;
import im.turms.turms.builder.QueryBuilder;
import im.turms.turms.builder.UpdateBuilder;
import im.turms.turms.constant.AdminPermission;
import im.turms.turms.constant.Common;
import im.turms.turms.manager.TurmsClusterManager;
import im.turms.turms.pojo.domain.AdminRole;
import org.apache.commons.lang3.tuple.Triple;
import org.hibernate.validator.constraints.Length;
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
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static im.turms.turms.constant.Common.*;

@Service
@Validated
public class AdminRoleService {
    private static ReplicatedMap<Long, AdminRole> roles;
    private static final int MIN_ROLE_NAME_LIMIT = 1;
    private static final int MAX_ROLE_NAME_LIMIT = 32;
    private final ReactiveMongoTemplate mongoTemplate;
    private final AdminService adminService;

    public AdminRoleService(@Qualifier("adminMongoTemplate") ReactiveMongoTemplate mongoTemplate, @Lazy AdminService adminService) {
        this.mongoTemplate = mongoTemplate;
        this.adminService = adminService;
    }

    @PostHazelcastInitialized
    public Function<TurmsClusterManager, Void> initAdminRolesCache() {
        return turmsClusterManager -> {
            roles = turmsClusterManager.getHazelcastInstance().getReplicatedMap(Common.HAZELCAST_ROLES_MAP);
            if (roles.isEmpty()) {
                loadAllRoles();
            }
            roles.putIfAbsent(
                    ADMIN_ROLE_ROOT_ID,
                    new AdminRole(
                            ADMIN_ROLE_ROOT_ID,
                            ADMIN_ROLE_ROOT_NAME,
                            AdminPermission.ALL,
                            Integer.MAX_VALUE));
            return null;
        };
    }

    public void loadAllRoles() {
        mongoTemplate.find(new Query(), AdminRole.class)
                .doOnNext(role -> roles.put(role.getId(), role))
                .subscribe();
    }

    public Mono<AdminRole> authAndAddAdminRole(
            @NotNull String requesterAccount,
            @NotNull Long roleId,
            @NotNull @NoWhitespaceConstraint @Length(min = MIN_ROLE_NAME_LIMIT, max = MAX_ROLE_NAME_LIMIT) String name,
            @NotEmpty Set<AdminPermission> permissions,
            @NotNull Integer rank) {
        return isAdminHigherThanRank(requesterAccount, rank)
                .flatMap(isHigher -> {
                    if (isHigher) {
                        return adminHasPermissions(requesterAccount, permissions)
                                .flatMap(hasPermissions -> {
                                    if (hasPermissions) {
                                        return addAdminRole(roleId, name, permissions, rank);
                                    } else {
                                        return Mono.error(TurmsBusinessException.get(TurmsStatusCode.UNAUTHORIZED));
                                    }
                                });
                    } else {
                        return Mono.error(TurmsBusinessException.get(TurmsStatusCode.UNAUTHORIZED));
                    }
                });
    }

    public Mono<AdminRole> addAdminRole(
            @NotNull Long id,
            @NotNull @NoWhitespaceConstraint @Length(min = MIN_ROLE_NAME_LIMIT, max = MAX_ROLE_NAME_LIMIT) String name,
            @NotEmpty Set<AdminPermission> permissions,
            @NotNull Integer rank) {
        AdminRole adminRole = new AdminRole(id, name, permissions, rank);
        if (adminRole.getId().equals(ADMIN_ROLE_ROOT_ID)) {
            throw TurmsBusinessException.get(TurmsStatusCode.UNAUTHORIZED);
        }
        return mongoTemplate.insert(adminRole).map(role -> {
            roles.put(adminRole.getId(), role);
            return role;
        });
    }

    public Mono<Boolean> authAndDeleteAdminRoles(
            @NotNull String requesterAccount,
            @NotEmpty Set<Long> roleIds) {
        Long highestRoleId = null;
        for (Long roleId : roleIds) {
            if (highestRoleId == null || highestRoleId < roleId) {
                highestRoleId = roleId;
            }
        }
        return isAdminHigherThanRole(requesterAccount, highestRoleId)
                .flatMap(isHigher -> {
                    if (isHigher) {
                        return deleteAdminRoles(roleIds);
                    } else {
                        return Mono.error(TurmsBusinessException.get(TurmsStatusCode.UNAUTHORIZED));
                    }
                });
    }

    public Mono<Boolean> deleteAdminRoles(@NotEmpty Set<Long> roleIds) {
        if (roleIds.contains(ADMIN_ROLE_ROOT_ID)) {
            throw TurmsBusinessException.get(TurmsStatusCode.UNAUTHORIZED);
        }
        Query query = new Query().addCriteria(Criteria.where(Common.ID).in(roleIds));
        return mongoTemplate.remove(query, AdminRole.class)
                .map(result -> {
                    if (result.wasAcknowledged()) {
                        for (Long id : roleIds) {
                            roles.remove(id);
                        }
                        return true;
                    } else {
                        return false;
                    }
                });
    }

    public Mono<Boolean> authAndUpdateAdminRole(
            @NotNull String requesterAccount,
            @NotEmpty Set<Long> roleIds,
            @Nullable @NoWhitespaceConstraint @Length(min = MIN_ROLE_NAME_LIMIT, max = MAX_ROLE_NAME_LIMIT) String newName,
            @Nullable Set<AdminPermission> permissions,
            @Nullable Integer rank) {
        Long highestRoleId = null;
        for (Long roleId : roleIds) {
            if (highestRoleId == null || highestRoleId < roleId) {
                highestRoleId = roleId;
            }
        }
        return isAdminHigherThanRole(requesterAccount, highestRoleId)
                .flatMap(isHigher -> {
                    if (isHigher) {
                        if (permissions != null) {
                            return adminHasPermissions(requesterAccount, permissions)
                                    .flatMap(hasPermissions -> {
                                        if (hasPermissions) {
                                            return updateAdminRole(roleIds, newName, permissions, rank);
                                        } else {
                                            return Mono.error(TurmsBusinessException.get(TurmsStatusCode.UNAUTHORIZED));
                                        }
                                    });
                        } else {
                            return updateAdminRole(roleIds, newName, null, rank);
                        }
                    } else {
                        return Mono.error(TurmsBusinessException.get(TurmsStatusCode.UNAUTHORIZED));
                    }
                });
    }

    public Mono<Boolean> updateAdminRole(
            @NotEmpty Set<Long> roleIds,
            @Nullable @NoWhitespaceConstraint @Length(min = MIN_ROLE_NAME_LIMIT, max = MAX_ROLE_NAME_LIMIT) String newName,
            @Nullable Set<AdminPermission> permissions,
            @Nullable Integer rank) {
        if (Validator.areAllFalsy(newName, permissions, rank)) {
            return Mono.just(true);
        }
        if (roleIds.contains(ADMIN_ROLE_ROOT_ID)) {
            throw TurmsBusinessException.get(TurmsStatusCode.UNAUTHORIZED);
        }
        Query query = new Query().addCriteria(Criteria.where(Common.ID).in(roleIds));
        Update update = UpdateBuilder.newBuilder()
                .setIfNotNull(AdminRole.Fields.name, newName)
                .setIfNotNull(AdminRole.Fields.permissions, permissions)
                .setIfNotNull(AdminRole.Fields.rank, rank)
                .build();
        return mongoTemplate.updateMulti(query, update, AdminRole.class)
                .map(result -> {
                    if (result.wasAcknowledged()) {
                        for (Long roleId : roleIds) {
                            AdminRole adminRole = roles.get(roleId);
                            if (adminRole != null) {
                                adminRole.setName(newName);
                                adminRole.setPermissions(permissions);
                                adminRole.setRank(rank);
                            } else {
                                queryAndCacheRole(roleId);
                            }
                        }
                        return true;
                    } else {
                        return false;
                    }
                });
    }

    public AdminRole getRootRole() {
        return roles.get(ADMIN_ROLE_ROOT_ID);
    }

    public Flux<AdminRole> queryAdminRoles(
            @Nullable Set<Long> ids,
            @Nullable Set<String> names,
            @Nullable Set<AdminPermission> includedPermissions,
            @Nullable Set<Integer> ranks,
            @Nullable Integer page,
            @Nullable Integer size) {
        Query query = QueryBuilder
                .newBuilder()
                .addInIfNotNull(ID, ids)
                .addInIfNotNull(AdminRole.Fields.name, names)
                .addInIfNotNull(AdminRole.Fields.permissions, includedPermissions)
                .addInIfNotNull(AdminRole.Fields.rank, ranks)
                .paginateIfNotNull(page, size);
        return Flux.from(mongoTemplate.find(query, AdminRole.class)
                .concatWithValues(getRootRole()));
    }

    public Mono<Long> countAdminRoles(
            @Nullable Set<Long> ids,
            @Nullable Set<String> names,
            @Nullable Set<AdminPermission> includedPermissions,
            @Nullable Set<Integer> ranks) {
        Query query = QueryBuilder
                .newBuilder()
                .addInIfNotNull(ID, ids)
                .addInIfNotNull(AdminRole.Fields.name, names)
                .addInIfNotNull(AdminRole.Fields.permissions, includedPermissions)
                .addInIfNotNull(AdminRole.Fields.rank, ranks)
                .buildQuery();
        return mongoTemplate.count(query, AdminRole.class)
                .map(number -> number + 1);
    }

    public Flux<Integer> queryRanksByAccounts(@NotEmpty Set<String> accounts) {
        return adminService.queryRolesIds(accounts)
                .collect(Collectors.toSet())
                .flatMapMany(this::queryRanksByRoles);
    }

    public Mono<Integer> queryRankByAccount(@NotNull String account) {
        return adminService.queryRoleId(account)
                .flatMap(this::queryRankByRole);
    }

    public Mono<Integer> queryRankByRole(@NotNull Long roleId) {
        if (roleId.equals(ADMIN_ROLE_ROOT_ID)) {
            return Mono.just(getRootRole().getRank());
        } else {
            Query query = new Query();
            query.addCriteria(Criteria.where(ID).is(roleId));
            query.fields().include(AdminRole.Fields.rank);
            return mongoTemplate.findOne(query, AdminRole.class)
                    .map(AdminRole::getRank);
        }
    }

    public Flux<Integer> queryRanksByRoles(@NotEmpty Set<Long> rolesIds) {
        boolean containsRoot = rolesIds.contains(ADMIN_ROLE_ROOT_ID);
        if (containsRoot && rolesIds.size() == 1) {
            return Flux.just(getRootRole().getRank());
        } else {
            Query query = new Query();
            query.addCriteria(Criteria.where(ID).in(rolesIds));
            query.fields().include(AdminRole.Fields.rank);
            Flux<AdminRole> roleFlux = mongoTemplate.find(query, AdminRole.class);
            if (containsRoot) {
                roleFlux = roleFlux.concatWithValues(getRootRole());
            }
            return roleFlux.map(AdminRole::getRank);
        }
    }

    public Mono<Boolean> isAdminHigherThanRole(
            @NotNull String account,
            @NotNull Long roleId) {
        return Mono.zip(queryRankByAccount(account), queryRankByRole(roleId))
                .map(tuple -> tuple.getT1() > tuple.getT2())
                .defaultIfEmpty(false);
    }

    public Mono<Boolean> isAdminHigherThanRank(
            @NotNull String account,
            @NotNull Integer rank) {
        return queryRankByAccount(account)
                .map(adminRank -> adminRank > rank)
                .defaultIfEmpty(false);
    }

    private Mono<Boolean> adminHasPermissions(@NotNull String account, @NotNull Set<AdminPermission> permissions) {
        if (permissions.isEmpty()) {
            return Mono.just(true);
        } else {
            return queryPermissions(account)
                    .map(adminPermissions -> adminPermissions.containsAll(permissions))
                    .defaultIfEmpty(false);
        }
    }

    /**
     * @return isAdminHigherThanAdmins, admin rank, admins ranks
     */
    public Mono<Triple<Boolean, Integer, Set<Integer>>> isAdminHigherThanAdmins(
            @NotNull String account,
            @NotEmpty Set<String> accounts) {
        return queryRankByAccount(account)
                .flatMap(rank -> queryRanksByAccounts(accounts)
                        .collect(Collectors.toSet())
                        .map(ranks -> {
                            for (Integer targetRank : ranks) {
                                if (targetRank >= rank) {
                                    return Triple.of(false, rank, ranks);
                                }
                            }
                            return Triple.of(true, rank, ranks);
                        }));
    }

    public Mono<AdminRole> queryAndCacheRole(@NotNull Long roleId) {
        if (roleId.equals(ADMIN_ROLE_ROOT_ID)) {
            return Mono.just(getRootRole());
        } else {
            return mongoTemplate.findById(roleId, AdminRole.class)
                    .map(role -> {
                        roles.put(roleId, role);
                        return role;
                    });
        }
    }

    public Mono<Set<AdminPermission>> queryPermissions(@NotNull String account) {
        return adminService.queryRoleId(account)
                .flatMap(this::queryPermissions);
    }

    public Mono<Set<AdminPermission>> queryPermissions(@NotNull Long roleId) {
        AdminRole role = roles.get(roleId);
        if (role != null) {
            return Mono.just(role.getPermissions());
        } else {
            return queryAndCacheRole(roleId)
                    .map(AdminRole::getPermissions);
        }
    }

    public Mono<Boolean> hasPermission(@NotNull Long roleId, @NotNull AdminPermission permission) {
        return queryPermissions(roleId)
                .map(permissions -> permissions.contains(permission))
                .defaultIfEmpty(false);
    }
}
