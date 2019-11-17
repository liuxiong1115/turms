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
import im.turms.turms.annotation.cluster.PostHazelcastInitialized;
import im.turms.turms.cluster.TurmsClusterManager;
import im.turms.turms.common.Constants;
import im.turms.turms.common.UpdateBuilder;
import im.turms.turms.constant.AdminPermission;
import im.turms.turms.pojo.domain.AdminRole;
import org.springframework.context.annotation.Lazy;
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
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static im.turms.turms.common.Constants.*;

@Service
public class AdminRoleService {
    private static ReplicatedMap<Long, AdminRole> roles;
    private final ReactiveMongoTemplate mongoTemplate;
    private final AdminService adminService;

    public AdminRoleService(ReactiveMongoTemplate mongoTemplate, @Lazy AdminService adminService) {
        this.mongoTemplate = mongoTemplate;
        this.adminService = adminService;
    }

    @PostHazelcastInitialized
    public Function<TurmsClusterManager, Void> initAdminRolesCache() {
        return turmsClusterManager -> {
            roles = turmsClusterManager.getHazelcastInstance().getReplicatedMap(Constants.HAZELCAST_ROLES_MAP);
            if (roles.size() == 0) {
                loadAllRoles();
            }
            roles.putIfAbsent(
                    ADMIN_ROLE_ROOT_ID,
                    new AdminRole(
                            ADMIN_ROLE_ROOT_ID,
                            ADMIN_ROLE_ROOT_NAME,
                            AdminPermission.all(),
                            Integer.MAX_VALUE));
            return null;
        };
    }

    public void loadAllRoles() {
        mongoTemplate.find(new Query(), AdminRole.class)
                .doOnNext(role -> roles.put(role.getRoleId(), role))
                .subscribe();
    }

    public Mono<AdminRole> addAdminRole(
            @NotNull Long id,
            @NotNull String name,
            @NotEmpty Set<AdminPermission> permissions,
            @NotNull Integer rank) {
        AdminRole adminRole = new AdminRole(id, name, permissions, rank);
        return addAdminRole(adminRole);
    }

    public Mono<AdminRole> addAdminRole(@NotNull AdminRole adminRole) {
        return mongoTemplate.insert(adminRole).map(role -> {
            roles.put(adminRole.getRoleId(), role);
            return role;
        });
    }

    public Mono<Boolean> deleteAdminRoles(@NotEmpty Set<Long> rolesIds) {
        rolesIds.remove(ADMIN_ROLE_ROOT_ID);
        Query query = new Query().addCriteria(Criteria.where(Constants.ID).in(rolesIds));
        return mongoTemplate.remove(query, AdminRole.class)
                .map(result -> {
                    if (result.wasAcknowledged()) {
                        for (Long id : rolesIds) {
                            roles.remove(id);
                        }
                        return true;
                    } else {
                        return false;
                    }
                });
    }

    public Mono<Boolean> updateAdminRole(
            @NotNull Long roleId,
            @Nullable String newName,
            @Nullable Set<AdminPermission> permissions,
            @Nullable Integer rank) {
        Query query = new Query().addCriteria(Criteria.where(Constants.ID).is(roleId));
        Update update = UpdateBuilder.newBuilder()
                .setIfNotNull(AdminRole.Fields.name, newName)
                .setIfNotNull(AdminRole.Fields.permissions, permissions)
                .setIfNotNull(AdminRole.Fields.rank, rank)
                .build();
        return mongoTemplate.updateFirst(query, update, AdminRole.class)
                .map(result -> {
                    if (result.wasAcknowledged()) {
                        AdminRole adminRole = roles.get(roleId);
                        if (adminRole != null) {
                            adminRole.setName(newName);
                            adminRole.setPermissions(permissions);
                            adminRole.setRank(rank);
                        } else {
                            queryAndUpdateRole(roleId);
                        }
                        return true;
                    } else {
                        return false;
                    }
                });
    }

    public Mono<Boolean> addPermissions(
            @NotNull Long roleId,
            @NotEmpty Set<AdminPermission> permissions) {
        Query query = new Query().addCriteria(Criteria.where(Constants.ID).is(roleId));
        Update update = new Update().addToSet(AdminRole.Fields.permissions).each(permissions);
        return mongoTemplate.updateFirst(query, update, AdminRole.class)
                .flatMap(result -> {
                    if (result.wasAcknowledged()) {
                        AdminRole adminRole = roles.get(roleId);
                        if (adminRole != null) {
                            adminRole.getPermissions().addAll(permissions);
                            return Mono.just(true);
                        } else {
                            return queryAndUpdateRole(roleId)
                                    .map(role -> {
                                        role.getPermissions().addAll(permissions);
                                        return true;
                                    });
                        }
                    } else {
                        return Mono.just(false);
                    }
                });
    }

    public Mono<Boolean> deletePermissions(
            @NotNull Long roleId,
            @NotEmpty Set<AdminPermission> permissions) {
        Query query = new Query()
                .addCriteria(Criteria.where(Constants.ID).is(roleId));
        Update update = new Update().pullAll(AdminRole.Fields.permissions, permissions.toArray());
        return mongoTemplate.updateFirst(query, update, AdminRole.class)
                .flatMap(result -> {
                    if (result.wasAcknowledged()) {
                        AdminRole adminRole = roles.get(roleId);
                        if (adminRole != null) {
                            adminRole.getPermissions().removeAll(permissions);
                            return Mono.just(true);
                        } else {
                            return queryAndUpdateRole(roleId)
                                    .map(role -> {
                                        role.getPermissions().removeAll(permissions);
                                        return true;
                                    });
                        }
                    } else {
                        return Mono.just(false);
                    }
                });
    }

    public AdminRole getRootRole() {
        return roles.get(ADMIN_ROLE_ROOT_ID);
    }

    public Flux<AdminRole> queryAllAdminRoles() {
        return Flux.from(mongoTemplate.findAll(AdminRole.class)
                .concatWithValues(getRootRole()));
    }

    public Flux<AdminRole> queryAdminRoles(
            @Nullable Set<Long> ids,
            @Nullable Set<String> names,
            @Nullable Set<AdminPermission> includedPermissions,
            @Nullable Set<Integer> ranks) {
        Query query = new Query();
        if (ids != null && !ids.isEmpty()) {
            query.addCriteria(Criteria.where(ID).in(ids));
        }
        if (names != null && !names.isEmpty()) {
            query.addCriteria(Criteria.where(AdminRole.Fields.name).in(names));
        }
        if (includedPermissions != null && !includedPermissions.isEmpty()) {
            query.addCriteria(Criteria.where(AdminRole.Fields.permissions).in(includedPermissions));
        }
        if (ranks != null && !ranks.isEmpty()) {
            query.addCriteria(Criteria.where(AdminRole.Fields.rank).in(ranks));
        }
        return Flux.from(mongoTemplate.find(query, AdminRole.class)
                .concatWithValues(getRootRole()));
    }

    public Flux<Integer> queryRanksByAccounts(@NotNull Set<String> accounts) {
        return adminService.queryRolesIds(accounts)
                .collect(Collectors.toSet())
                .flatMapMany(this::queryRanksByRoles);
    }

    public Mono<Integer> queryRankByAccount(@NotNull String account) {
        return adminService.queryRoleId(account)
                .flatMap(this::queryRankByRole);
    }

    public Mono<Integer> queryRankByRole(@NotNull Long roleId) {
        if (roleId == ADMIN_ROLE_ROOT_ID) {
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
        return queryRankByAccount(account)
                .flatMap(rank -> queryRankByRole(roleId)
                        .map(roleRank -> rank > roleRank));
    }

    public Mono<Boolean> isAdminHigherThanAdmins(
            @NotNull String account,
            @NotNull Set<String> accounts) {
        return queryRankByAccount(account)
                .flatMap(rank -> queryRanksByAccounts(accounts)
                        .collect(Collectors.toSet())
                        .map(ranks -> {
                            for (Integer targetRank : ranks) {
                                if (targetRank >= rank) {
                                    return false;
                                }
                            }
                            return true;
                        }));
    }

    public Mono<AdminRole> queryAndUpdateRole(@NotNull Long roleId) {
        if (roleId == ADMIN_ROLE_ROOT_ID) {
            return Mono.just(getRootRole());
        } else {
            return mongoTemplate.findById(roleId, AdminRole.class)
                    .map(role -> {
                        roles.put(roleId, role);
                        return role;
                    });
        }
    }

    public Mono<Set<AdminPermission>> queryPermissions(@NotNull Long roleId) {
        AdminRole role = roles.get(roleId);
        if (role != null) {
            return Mono.just(role.getPermissions());
        } else {
            return queryAndUpdateRole(roleId)
                    .map(AdminRole::getPermissions);
        }
    }

    public Mono<Boolean> hasPermission(@NotNull Long roleId, @NotEmpty AdminPermission permission) {
        return queryPermissions(roleId)
                .map(permissions -> permissions.contains(permission))
                .defaultIfEmpty(false);
    }
}
