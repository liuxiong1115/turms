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
import im.turms.turms.manager.TurmsClusterManager;
import im.turms.turms.pojo.bo.AdminInfo;
import im.turms.turms.pojo.domain.Admin;
import im.turms.turms.util.TurmsPasswordUtil;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.RandomStringUtils;
import org.hibernate.validator.constraints.Length;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.annotation.Nullable;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.PastOrPresent;
import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import static im.turms.turms.constant.Common.*;

@Log4j2
@Service
@Validated
public class AdminService {
    /**
     * Use hard-coded account because modifying the account of admins is not allowed.
     */
    public static String ROOT_ADMIN_ACCOUNT = "turms";
    //Account -> AdminInfo
    private static ReplicatedMap<String, AdminInfo> adminMap;
    private static final int MIN_ACCOUNT_LIMIT = 1;
    private static final int MIN_PASSWORD_LIMIT = 1;
    private static final int MIN_NAME_LIMIT = 1;
    public static final int MAX_ACCOUNT_LIMIT = 32;
    public static final int MAX_PASSWORD_LIMIT = 32;
    public static final int MAX_NAME_LIMIT = 32;
    private final TurmsPasswordUtil turmsPasswordUtil;
    private final ReactiveMongoTemplate mongoTemplate;
    private final AdminRoleService adminRoleService;

    public AdminService(TurmsPasswordUtil turmsPasswordUtil, @Qualifier("adminMongoTemplate") ReactiveMongoTemplate mongoTemplate, AdminRoleService adminRoleService) {
        this.turmsPasswordUtil = turmsPasswordUtil;
        this.mongoTemplate = mongoTemplate;
        this.adminRoleService = adminRoleService;
    }

    @PostHazelcastInitialized
    public Function<TurmsClusterManager, Void> initAdminsCache() {
        return clusterManager -> {
            adminMap = clusterManager.getHazelcastInstance().getReplicatedMap(HAZELCAST_ADMINS_MAP);
            if (adminMap.isEmpty()) {
                loadAllAdmins();
            }
            rootAdminExists().subscribe(exists -> {
                if (!exists) {
                    String rawPassword = RandomStringUtils.randomAlphanumeric(32);
                    addAdmin(ROOT_ADMIN_ACCOUNT,
                            rawPassword,
                            ADMIN_ROLE_ROOT_ID,
                            RandomStringUtils.randomAlphabetic(8),
                            new Date(),
                            false)
                            .doOnNext(admin -> log.info("Root admin: {}", Map.of(
                                    "Account", ROOT_ADMIN_ACCOUNT,
                                    "Raw Password", rawPassword)))
                            .subscribe();
                }
            });
            return null;
        };
    }

    public Mono<Boolean> rootAdminExists() {
        Query query = new Query()
                .addCriteria(Criteria.where(Admin.Fields.ROLE_ID).is(ADMIN_ROLE_ROOT_ID));
        return mongoTemplate.exists(query, Admin.class);
    }

    public Mono<Admin> authAndAddAdmin(
            @NotNull String requesterAccount,
            @Nullable @NoWhitespaceConstraint @Length(min = MIN_ACCOUNT_LIMIT, max = MAX_ACCOUNT_LIMIT) String account,
            @Nullable @NoWhitespaceConstraint @Length(min = MIN_PASSWORD_LIMIT, max = MAX_PASSWORD_LIMIT) String rawPassword,
            @NotNull Long roleId,
            @Nullable @NoWhitespaceConstraint @Length(min = MIN_NAME_LIMIT, max = MAX_NAME_LIMIT) String name,
            @Nullable @PastOrPresent Date registrationDate,
            boolean upsert) {
        if (roleId.equals(ADMIN_ROLE_ROOT_ID)) {
            throw TurmsBusinessException.get(TurmsStatusCode.UNAUTHORIZED);
        }
        return adminRoleService.isAdminHigherThanRole(requesterAccount, roleId)
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
            @Nullable @NoWhitespaceConstraint @Length(min = MIN_ACCOUNT_LIMIT, max = MAX_ACCOUNT_LIMIT) String account,
            @Nullable @NoWhitespaceConstraint @Length(min = MIN_PASSWORD_LIMIT, max = MAX_PASSWORD_LIMIT) String rawPassword,
            @NotNull Long roleId,
            @Nullable @NoWhitespaceConstraint @Length(min = MIN_NAME_LIMIT, max = MAX_NAME_LIMIT) String name,
            @Nullable @PastOrPresent Date registrationDate,
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
            return mongoTemplate.save(admin).doOnNext(result -> adminMap.put(finalAccount, adminInfo));
        } else {
            return mongoTemplate.insert(admin).doOnNext(result -> adminMap.put(finalAccount, adminInfo));
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

    public Mono<Boolean> authenticate(
            @NotNull @NoWhitespaceConstraint String account,
            @NotNull @NoWhitespaceConstraint String rawPassword) {
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

    public Mono<Admin> queryAdmin(@NotNull String account) {
        AdminInfo adminInfo = adminMap.get(account);
        if (adminInfo != null) {
            return Mono.just(adminInfo.getAdmin());
        } else {
            return mongoTemplate.findById(account, Admin.class)
                    .doOnNext(admin -> {
                        adminMap.put(account, new AdminInfo(admin, null));
                    });
        }
    }

    public void loadAllAdmins() {
        mongoTemplate.find(new Query(), Admin.class)
                .doOnNext(admin -> {
                    adminMap.put(admin.getAccount(), new AdminInfo(admin, null));
                })
                .subscribe();
    }

    public Flux<Admin> queryAdmins(
            @Nullable Set<String> accounts,
            @Nullable Set<Long> roleIds,
            boolean withPassword,
            @Nullable Integer page,
            @Nullable Integer size) {
        Query query = QueryBuilder.newBuilder()
                .addInIfNotNull(ID, accounts)
                .addInIfNotNull(Admin.Fields.ROLE_ID, roleIds)
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
            @NotNull String requesterAccount,
            @NotEmpty Set<String> accounts) {
        if (accounts.contains(ROOT_ADMIN_ACCOUNT)) {
            throw TurmsBusinessException.get(TurmsStatusCode.UNAUTHORIZED);
        }
        return adminRoleService.isAdminHigherThanAdmins(requesterAccount, accounts)
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
        return mongoTemplate.remove(query, Admin.class)
                .map(result -> {
                    if (result.wasAcknowledged()) {
                        for (String account : accounts) {
                            adminMap.remove(account);
                        }
                    }
                    return result.wasAcknowledged();
                });
    }

    public Mono<Boolean> authAndUpdateAdmins(
            @NotNull String requesterAccount,
            @NotEmpty Set<String> targetAccounts,
            @Nullable @NoWhitespaceConstraint @Length(min = MIN_PASSWORD_LIMIT, max = MAX_PASSWORD_LIMIT) String rawPassword,
            @Nullable @NoWhitespaceConstraint @Length(min = MIN_NAME_LIMIT, max = MAX_NAME_LIMIT) String name,
            @Nullable Long roleId) {
        if (Validator.areAllNull(rawPassword, name, roleId)) {
            return Mono.just(true);
        }
        boolean onlyUpdateOneself = targetAccounts.size() == 1 && targetAccounts.iterator().next().equals(requesterAccount);
        if (onlyUpdateOneself) {
            if (roleId == null) {
                return updateAdmins(targetAccounts, rawPassword, name, null);
            } else {
                throw TurmsBusinessException.get(TurmsStatusCode.UNAUTHORIZED);
            }
        } else {
            return adminRoleService.isAdminHigherThanAdmins(requesterAccount, targetAccounts)
                    .flatMap(triple -> {
                        if (triple.getLeft()) {
                            if (roleId != null) {
                                return adminRoleService.queryRankByRole(roleId)
                                        .flatMap(targetRoleRank -> {
                                            if (triple.getMiddle() > targetRoleRank) {
                                                return updateAdmins(targetAccounts, rawPassword, name, roleId);
                                            } else {
                                                return Mono.error(TurmsBusinessException.get(TurmsStatusCode.UNAUTHORIZED));
                                            }
                                        });
                            } else {
                                return updateAdmins(targetAccounts, rawPassword, name, null);
                            }
                        } else {
                            return Mono.error(TurmsBusinessException.get(TurmsStatusCode.UNAUTHORIZED));
                        }
                    })
                    .switchIfEmpty(Mono.error(TurmsBusinessException.get(TurmsStatusCode.UNAUTHORIZED)));
        }
    }

    public Mono<Boolean> updateAdmins(
            @NotEmpty Set<String> targetAccounts,
            @Nullable @NoWhitespaceConstraint @Length(min = MIN_PASSWORD_LIMIT, max = MAX_PASSWORD_LIMIT) String rawPassword,
            @Nullable @NoWhitespaceConstraint @Length(min = MIN_NAME_LIMIT, max = MAX_NAME_LIMIT) String name,
            @Nullable Long roleId) {
        if (Validator.areAllNull(rawPassword, name, roleId)) {
            return Mono.just(true);
        }
        Query query = new Query();
        query.addCriteria(Criteria.where(ID).in(targetAccounts));
        String password = turmsPasswordUtil.encodeAdminPassword(rawPassword);
        Update update = UpdateBuilder
                .newBuilder()
                .setIfNotNull(Admin.Fields.PASSWORD, password)
                .setIfNotNull(Admin.Fields.NAME, name)
                .setIfNotNull(Admin.Fields.ROLE_ID, roleId)
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

    public Mono<Long> countAdmins(@Nullable Set<String> accounts, @Nullable Set<Long> roleIds) {
        Query query = QueryBuilder.newBuilder()
                .addInIfNotNull(ID, accounts)
                .addInIfNotNull(Admin.Fields.ROLE_ID, roleIds)
                .buildQuery();
        return mongoTemplate.count(query, Admin.class);
    }
}