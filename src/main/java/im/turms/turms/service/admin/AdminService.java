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
import im.turms.turms.common.*;
import im.turms.turms.constant.AdminPermission;
import im.turms.turms.exception.TurmsBusinessException;
import im.turms.turms.pojo.bo.admin.AdminInfo;
import im.turms.turms.pojo.domain.Admin;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.annotation.Nullable;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static im.turms.turms.common.Constants.*;

@Service
public class AdminService {
    public static String ROOT_ADMIN_ACCOUNT;
    //Account -> AdminInfo
    private static ReplicatedMap<String, AdminInfo> adminMap;
    private final TurmsPasswordUtil turmsPasswordUtil;
    private final ReactiveMongoTemplate mongoTemplate;
    private final AdminRoleService adminRoleService;

    public AdminService(TurmsPasswordUtil turmsPasswordUtil, ReactiveMongoTemplate mongoTemplate, AdminRoleService adminRoleService) {
        this.turmsPasswordUtil = turmsPasswordUtil;
        this.mongoTemplate = mongoTemplate;
        this.adminRoleService = adminRoleService;
    }

    @PostHazelcastInitialized
    public Function<TurmsClusterManager, Void> initAdminsCache() {
        return clusterManager -> {
            adminMap = clusterManager.getHazelcastInstance().getReplicatedMap(HAZELCAST_ADMINS_MAP);
            if (adminMap.size() == 0) {
                loadAllAdmins();
            }
            countRootAdmins().subscribe(number -> {
                if (number == 0) {
                    String account = RandomStringUtils.randomAlphabetic(16);
                    String rawPassword = RandomStringUtils.randomAlphanumeric(32);
                    addAdmin(account,
                            rawPassword,
                            ADMIN_ROLE_ROOT_ID,
                            RandomStringUtils.randomAlphabetic(8),
                            new Date(),
                            false)
                            .doOnSuccess(admin -> {
                                ROOT_ADMIN_ACCOUNT = account;
                                TurmsLogger.logJson("Root admin", Map.of(
                                        "Account", account,
                                        "Raw Password", rawPassword));
                            })
                            .subscribe();
                }
            });
            return null;
        };
    }

    public Mono<Long> countRootAdmins() {
        Query query = new Query()
                .addCriteria(Criteria.where(Admin.Fields.roleId).is(ADMIN_ROLE_ROOT_ID));
        return mongoTemplate.count(query, Admin.class);
    }

    public Mono<Admin> authAndAddAdmin(
            @NotNull String requester,
            @Nullable String account,
            @Nullable String rawPassword,
            @NotNull Long roleId,
            @Nullable String name,
            @Nullable Date registrationDate,
            boolean upsert) {
        Validator.throwIfAllNull(requester, roleId);
        return adminRoleService.isAdminHigherThanRole(requester, roleId)
                .flatMap(isHigher -> {
                    if (isHigher) {
                        return addAdmin(account, rawPassword, roleId, name, registrationDate, upsert);
                    } else {
                        return Mono.error(TurmsBusinessException.get(TurmsStatusCode.UNAUTHORIZED));
                    }
                })
                .switchIfEmpty(Mono.error(TurmsBusinessException.get(TurmsStatusCode.UNAUTHORIZED)));
    }

    public Mono<Admin> addAdmin(
            @Nullable String account,
            @Nullable String rawPassword,
            @NotNull Long roleId,
            @Nullable String name,
            @Nullable Date registrationDate,
            boolean upsert) {
        account = account != null ? account : RandomStringUtils.randomAlphabetic(16);
        String password = StringUtils.hasText(rawPassword) ?
                turmsPasswordUtil.encodeAdminPassword(rawPassword) :
                turmsPasswordUtil.encodeAdminPassword(RandomStringUtils.randomAlphabetic(10));
        name = StringUtils.hasText(name) ? name : RandomStringUtils.randomAlphabetic(8);
        registrationDate = registrationDate != null ? registrationDate : new Date();
        Admin admin = new Admin(account, password, name, roleId, registrationDate);
        AdminInfo adminInfo = new AdminInfo(admin, rawPassword);
        String finalAccount = account;
        if (upsert) {
            return mongoTemplate.save(admin).doOnSuccess(result -> adminMap.put(finalAccount, adminInfo));
        } else {
            return mongoTemplate.insert(admin).doOnSuccess(result -> adminMap.put(finalAccount, adminInfo));
        }
    }

    public Mono<Long> queryRoleId(@NotNull String account) {
        return queryAdmin(account).map(Admin::getRoleId);
    }

    public Flux<Long> queryRolesIds(@NotEmpty Set<String> accounts) {
        Set<Long> rolesIds = new HashSet<>(accounts.size());
        for (String account : accounts) {
            AdminInfo adminInfo = adminMap.get(account);
            if (adminInfo != null) {
                rolesIds.add(adminInfo.getAdmin().getRoleId());
            }
        }
        if (rolesIds.size() == accounts.size()) {
            return Flux.fromIterable(rolesIds);
        } else {
            return queryAdmins(accounts, null, true, null, null)
                    .map(Admin::getRoleId);
        }
    }

    public Mono<Boolean> isAdminAuthorized(
            @NotNull String account,
            @NotNull AdminPermission permission) {
        return queryRoleId(account)
                .flatMap(roleId -> adminRoleService.hasPermission(roleId, permission))
                .switchIfEmpty(Mono.just(false));
    }

    public Mono<Boolean> isAdminAuthorized(
            @NotNull ServerWebExchange exchange,
            @NotNull String account,
            @NotNull AdminPermission permission) {
        boolean isQueryingOneselfInfo = isQueryingOneselfInfo(exchange, account, permission);
        if (isQueryingOneselfInfo) {
            return Mono.just(true);
        } else {
            return isAdminAuthorized(account, permission);
        }
    }

    private boolean isQueryingOneselfInfo(
            @NotNull ServerWebExchange exchange,
            @NotNull String account,
            @NotNull AdminPermission permission) {
        if (permission == AdminPermission.ADMIN_QUERY) {
            String accounts = exchange.getRequest().getQueryParams().getFirst("accounts");
            return accounts != null && accounts.equals(account);
        } else {
            return false;
        }
    }

    public Mono<Boolean> authenticate(@NotNull String account, @NotNull String rawPassword) {
        AdminInfo adminInfo = adminMap.get(account);
        if (adminInfo != null && adminInfo.getRawPassword() != null) {
            return Mono.just(adminInfo.getRawPassword().equals(rawPassword));
        } else {
            return queryAdmin(account)
                    .map(admin -> {
                        boolean valid = turmsPasswordUtil.matchesAdminPassword(rawPassword, admin.getPassword());
                        if (valid) {
                            adminMap.get(admin.getAccount()).setRawPassword(rawPassword);
                        }
                        return valid;
                    })
                    .defaultIfEmpty(false);
        }
    }

    public Mono<Boolean> deleteAdmin(@NotNull String account) {
        Query query = new Query().addCriteria(Criteria.where(ID).is(account));
        return mongoTemplate.remove(query, Admin.class).map(result -> {
            if (result.wasAcknowledged()) {
                adminMap.remove(account);
            }
            return result.wasAcknowledged();
        });
    }

    public Mono<Admin> queryAdmin(@NotNull String account) {
        AdminInfo adminInfo = adminMap.get(account);
        if (adminInfo != null) {
            return Mono.just(adminInfo.getAdmin());
        } else {
            return mongoTemplate.findById(account, Admin.class)
                    .doOnSuccess(admin -> adminMap.put(account, new AdminInfo(admin, null)));
        }
    }

    public void loadAllAdmins() {
        mongoTemplate.find(new Query(), Admin.class)
                .doOnNext(admin -> adminMap.put(admin.getAccount(), new AdminInfo(admin, null)))
                .subscribe();
    }

    public Flux<Admin> queryAdmins(
            @Nullable Set<String> accounts,
            @Nullable Long roleId,
            boolean withPassword,
            @Nullable Integer page,
            @Nullable Integer size) {
        Query query = QueryBuilder.newBuilder()
                .addInIfNotNull(ID, accounts)
                .addIsIfNotNull(Admin.Fields.roleId, roleId)
                .paginateIfNotNull(page, size);
        return mongoTemplate.find(query, Admin.class).map(admin -> {
            if (withPassword) {
                return admin;
            } else {
                Admin clone = admin.clone();
                clone.setPassword(null);
                return clone;
            }
        });
    }


    public Mono<Boolean> authAndDeleteAdmins(
            @NotNull String requester,
            @NotEmpty Set<String> accounts) {
        return adminRoleService.isAdminHigherThanAdmins(requester, accounts)
                .flatMap(triple -> {
                    if (triple.getLeft()) {
                        return deleteAdmins(accounts);
                    } else {
                        return Mono.error(TurmsBusinessException.get(TurmsStatusCode.UNAUTHORIZED));
                    }
                })
                .switchIfEmpty(Mono.error(TurmsBusinessException.get(TurmsStatusCode.UNAUTHORIZED)));
    }

    public Mono<Boolean> deleteAdmins(@NotEmpty Set<String> accounts) {
        Query query = new Query()
                .addCriteria(Criteria.where(ID).in(accounts));
        Set<String> rootAdminsAccounts = getRootAdminsAccounts();
        for (String account : accounts) {
            if (rootAdminsAccounts.contains(account)) {
                throw TurmsBusinessException.get(TurmsStatusCode.UNAUTHORIZED);
            }
        }
        if (!rootAdminsAccounts.isEmpty()) {
            return mongoTemplate.remove(query, Admin.class)
                    .map(result -> {
                        if (result.wasAcknowledged()) {
                            for (String account : accounts) {
                                adminMap.remove(account);
                            }
                        }
                        return result.wasAcknowledged();
                    });
        } else {
            return Mono.just(false);
        }
    }

    public Set<String> getRootAdminsAccounts() {
        return adminMap.values()
                .stream()
                .map(adminInfo -> adminInfo.getAdmin().getAccount())
                .collect(Collectors.toSet());
    }

    public Mono<Boolean> authAndUpdateAdmins(
            @NotNull String requester,
            @NotEmpty Set<String> targetAccounts,
            @Nullable String rawPassword,
            @Nullable String name,
            @Nullable Long roleId) {
        if (targetAccounts.size() == 1 && targetAccounts.iterator().next().equals(requester)) {
            if (roleId == null) {
                return updateAdmins(targetAccounts, rawPassword, name, null);
            } else {
                return Mono.error(TurmsBusinessException.get(TurmsStatusCode.UNAUTHORIZED));
            }
        } else {
            return adminRoleService.isAdminHigherThanAdmins(requester, targetAccounts)
                    .flatMap(triple -> {
                        if (triple.getLeft()) {
                            return adminRoleService.queryRankByRole(roleId)
                                    .flatMap(rank -> {
                                        if (triple.getMiddle() > rank) {
                                            return updateAdmins(targetAccounts, rawPassword, name, roleId);
                                        } else {
                                            return Mono.error(TurmsBusinessException.get(TurmsStatusCode.UNAUTHORIZED));
                                        }
                                    });
                        } else {
                            return Mono.error(TurmsBusinessException.get(TurmsStatusCode.UNAUTHORIZED));
                        }
                    })
                    .switchIfEmpty(Mono.error(TurmsBusinessException.get(TurmsStatusCode.UNAUTHORIZED)));
        }
    }

    public Mono<Boolean> updateAdmins(
            @NotEmpty Set<String> targetAccounts,
            @Nullable String rawPassword,
            @Nullable String name,
            @Nullable Long roleId) {
        Validator.throwIfAnyFalsy(targetAccounts);
        Validator.throwIfAllNull(rawPassword, name, roleId);
        Query query = new Query();
        query.addCriteria(Criteria.where(ID).in(targetAccounts));
        String password = turmsPasswordUtil.encodeAdminPassword(rawPassword);
        Update update = UpdateBuilder
                .newBuilder()
                .setIfNotNull(Admin.Fields.password, password)
                .setIfNotNull(Admin.Fields.name, name)
                .setIfNotNull(Admin.Fields.roleId, roleId)
                .build();
        return mongoTemplate.updateMulti(query, update, Admin.class)
                .map(result -> {
                    if (result.wasAcknowledged()) {
                        for (String account : targetAccounts) {
                            AdminInfo adminInfo = adminMap.get(account);
                            if (adminInfo != null) {
                                Admin admin = adminInfo.getAdmin();
                                if (rawPassword != null) {
                                    admin.setPassword(password);
                                    adminInfo.setRawPassword(rawPassword);
                                }
                                if (name != null) {
                                    admin.setName(name);
                                }
                                if (roleId != null) {
                                    admin.setRoleId(roleId);
                                }
                            }
                        }
                    }
                    return result.wasAcknowledged();
                });
    }

    public Mono<Long> countAdmins(@Nullable Set<String> accounts, @Nullable Long roleId) {
        Query query = QueryBuilder.newBuilder()
                .addInIfNotNull(ID, accounts)
                .addIsIfNotNull(Admin.Fields.roleId, roleId)
                .buildQuery();
        return mongoTemplate.count(query, Admin.class);
    }
}
