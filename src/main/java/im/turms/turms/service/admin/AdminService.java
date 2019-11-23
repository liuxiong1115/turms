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
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;
import im.turms.turms.annotation.cluster.PostHazelcastInitialized;
import im.turms.turms.cluster.TurmsClusterManager;
import im.turms.turms.common.*;
import im.turms.turms.constant.AdminPermission;
import im.turms.turms.exception.TurmsBusinessException;
import im.turms.turms.pojo.domain.Admin;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.annotation.Nullable;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static im.turms.turms.common.Constants.*;

@Service
public class AdminService {
    public static String ROOT_ADMIN_ACCOUNT;
    private static final String DESC_ACCOUNT = "Account";
    private static final String DESC_RAW_PASSWORD = "Raw Password";
    //Account -> Admin
    private static ReplicatedMap<String, Admin> admins;
    private final TurmsClusterManager turmsClusterManager;
    private final TurmsPasswordUtil turmsPasswordUtil;
    private final ReactiveMongoTemplate mongoTemplate;
    private final AdminRoleService adminRoleService;

    public AdminService(TurmsClusterManager turmsClusterManager, TurmsPasswordUtil turmsPasswordUtil, ReactiveMongoTemplate mongoTemplate, AdminRoleService adminRoleService) {
        this.turmsClusterManager = turmsClusterManager;
        this.turmsPasswordUtil = turmsPasswordUtil;
        this.mongoTemplate = mongoTemplate;
        this.adminRoleService = adminRoleService;
    }

    @PostHazelcastInitialized
    public Function<TurmsClusterManager, Void> initAdminsCache() {
        return clusterManager -> {
            admins = clusterManager.getHazelcastInstance().getReplicatedMap(HAZELCAST_ADMINS_MAP);
            if (admins.size() == 0) {
                loadAllAdmins();
            }
            countRootAdmins().subscribe(number -> {
                if (number == 0) {
                    String account = RandomStringUtils.randomAlphabetic(16);
                    String rawPassword = RandomStringUtils.randomAlphanumeric(32);
                    Map<String, String> map = new HashMap<>(2);
                    map.put(DESC_ACCOUNT, account);
                    map.put(DESC_RAW_PASSWORD, rawPassword);
                    addAdmin(account,
                            rawPassword,
                            ADMIN_ROLE_ROOT_ID,
                            RandomStringUtils.randomAlphabetic(8),
                            new Date(),
                            false)
                            .doOnSuccess(admin -> {
                                ROOT_ADMIN_ACCOUNT = account;
                                TurmsLogger.logJson("Root admin", map);
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
        return adminRoleService.isAdminHigherThanRole(requester, roleId)
                .flatMap(isHigher -> {
                    if (isHigher) {
                        return addAdmin(account, rawPassword, roleId, name, registrationDate, upsert);
                    } else {
                        return Mono.error(new ResponseStatusException(HttpStatus.UNAUTHORIZED));
                    }
                })
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND)));
    }

    public Mono<Admin> addAdmin(
            @Nullable String account,
            @Nullable String rawPassword,
            @NotNull Long roleId,
            @Nullable String name,
            @Nullable Date registrationDate,
            boolean upsert) {
        Admin admin = new Admin();
        admin.setAccount(account != null ? account : RandomStringUtils.randomAlphabetic(16));
        admin.setName(name != null && !name.isBlank() ? name : RandomStringUtils.randomAlphabetic(8));
        String password = rawPassword != null && !rawPassword.isBlank() ?
                turmsPasswordUtil.encodeAdminPassword(rawPassword) :
                turmsPasswordUtil.encodeAdminPassword(RandomStringUtils.randomAlphabetic(10));
        admin.setPassword(password);
        admin.setRegistrationDate(registrationDate != null ? registrationDate : new Date());
        admin.setRoleId(roleId);
        if (upsert) {
            return mongoTemplate.save(admin).doOnSuccess(result -> admins.put(account, admin));
        } else {
            return mongoTemplate.insert(admin).doOnSuccess(result -> admins.put(account, admin));
        }
    }

    public Mono<Long> queryRoleId(@NotNull String account) {
        Admin admin = admins.get(account);
        if (admin != null) {
            return Mono.just(admin.getRoleId());
        } else {
            Query query = new Query().addCriteria(Criteria.where(ID).is(account));
            query.fields().include(Admin.Fields.roleId);
            return mongoTemplate.findOne(query, Admin.class)
                    .map(administrator -> {
                        admins.put(account, administrator);
                        return administrator.getRoleId();
                    });
        }
    }

    public Flux<Long> queryRolesIds(@NotEmpty Set<String> accounts) {
        List<Admin> cacheAdmins = new ArrayList<>(accounts.size());
        for (String account : accounts) {
            Admin admin = admins.get(account);
            cacheAdmins.add(admin);
        }
        if (cacheAdmins.size() == accounts.size()) {
            Set<Long> rolesIds = cacheAdmins.stream()
                    .map(Admin::getRoleId)
                    .mapToLong(value -> value)
                    .boxed()
                    .collect(Collectors.toSet());
            return Flux.fromIterable(rolesIds);
        } else {
            Query query = new Query().addCriteria(Criteria.where(ID).in(accounts));
            query.fields().include(Admin.Fields.roleId);
            return mongoTemplate.find(query, Admin.class)
                    .map(administrator -> {
                        admins.put(administrator.getAccount(), administrator);
                        return administrator.getRoleId();
                    });
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
        Query query = new Query()
                .addCriteria(Criteria.where(ID).is(account));
        query.fields().include(Admin.Fields.password);
        return mongoTemplate.findOne(query, Admin.class)
                .map(admin -> turmsPasswordUtil.matchesAdminPassword(rawPassword, admin.getPassword()))
                .defaultIfEmpty(false);
    }

    public Mono<Boolean> deleteAdmin(@NotNull String account) {
        Query query = new Query().addCriteria(Criteria.where(ID).is(account));
        return mongoTemplate.remove(query, Admin.class).map(result -> {
            admins.remove(account);
            return result.wasAcknowledged();
        });
    }

    public Mono<Boolean> authAndDeleteAdmin(@NotNull String requesterAccount, @NotNull String deleteAccount) {
        return isAdminAuthorized(requesterAccount, AdminPermission.ADMIN_DELETE)
                .flatMap(authorized -> {
                    if (authorized != null && authorized) {
                        Query query = new Query().addCriteria(Criteria.where(ID).is(deleteAccount));
                        return mongoTemplate.remove(query, Admin.class).map(DeleteResult::wasAcknowledged);
                    } else {
                        return Mono.error(TurmsBusinessException.get(TurmsStatusCode.UNAUTHORIZED));
                    }
                });
    }

    public Mono<Admin> queryAdmin(@NotNull String account, boolean withPassword) {
        Query query = new Query().addCriteria(Criteria.where(ID).is(account));
        if (!withPassword) {
            query.fields().exclude(Admin.Fields.password);
        }
        return mongoTemplate.findById(account, Admin.class);
    }

    public void loadAllAdmins() {
        mongoTemplate.find(new Query(), Admin.class)
                .doOnNext(admin -> admins.put(admin.getAccount(), admin))
                .subscribe();
    }

    public Flux<Admin> queryAdmins(
            @Nullable Set<String> accounts,
            @Nullable Long roleId,
            boolean withPassword,
            int page,
            int size) {
        Query query = QueryBuilder.newBuilder()
                .addInIfNotNull(ID, accounts)
                .addIsIfNotNull(Admin.Fields.roleId, roleId)
                .paginateIfNotNull(page, size);
        if (!withPassword) {
            query.fields().exclude(Admin.Fields.password);
        }
        return mongoTemplate.find(query, Admin.class);
    }


    public Mono<Boolean> authAndDeleteAdmins(
            @NotNull String requester,
            @NotEmpty Set<String> accounts) {
        return adminRoleService.isAdminHigherThanAdmins(requester, accounts)
                .flatMap(triple -> {
                    if (triple.getLeft()) {
                        return deleteAdmins(accounts);
                    } else {
                        return Mono.error(new ResponseStatusException(HttpStatus.UNAUTHORIZED));
                    }
                })
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND)));
    }

    public Mono<Boolean> deleteAdmins(@NotEmpty Set<String> accounts) {
        Query query = new Query()
                .addCriteria(Criteria.where(ID).in(accounts));
        Set<String> rootAdminsAccounts = getRootAdminsAccounts();
        rootAdminsAccounts.removeAll(accounts);
        if (!rootAdminsAccounts.isEmpty()) {
            return mongoTemplate.remove(query, Admin.class)
                    .map(result -> {
                        for (String account : accounts) {
                            admins.remove(account);
                        }
                        return result.wasAcknowledged();
                    });
        } else {
            return Mono.just(false);
        }
    }

    public Set<String> getRootAdminsAccounts() {
        return admins.values()
                .stream()
                .map(Admin::getAccount)
                .collect(Collectors.toSet());
    }

    public Mono<Boolean> authAndUpdateAdmins(
            @NotNull String requester,
            @NotEmpty Set<String> targetAccounts,
            @Nullable String password,
            @Nullable String name,
            @Nullable Long roleId) {
        if (targetAccounts.size() == 1 && targetAccounts.iterator().next().equals(requester)) {
            if (roleId == null) {
                return updateAdmins(targetAccounts, password, name, null);
            } else {
                return Mono.error(new ResponseStatusException(HttpStatus.UNAUTHORIZED));
            }
        } else {
            return adminRoleService.isAdminHigherThanAdmins(requester, targetAccounts)
                    .flatMap(triple -> {
                        if (triple.getLeft()) {
                            return adminRoleService.queryRankByRole(roleId)
                                    .flatMap(rank -> {
                                        if (triple.getMiddle() > rank) {
                                            return updateAdmins(targetAccounts, password, name, roleId);
                                        } else {
                                            return Mono.error(new ResponseStatusException(HttpStatus.UNAUTHORIZED));
                                        }
                                    });
                        } else {
                            return Mono.error(new ResponseStatusException(HttpStatus.UNAUTHORIZED));
                        }
                    })
                    .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND)));
        }
    }

    public Mono<Boolean> updateAdmins(
            @NotEmpty Set<String> targetAccounts,
            @Nullable String password,
            @Nullable String name,
            @Nullable Long roleId) {
        Query query = new Query();
        query.addCriteria(Criteria.where(ID).in(targetAccounts));
        Update update = UpdateBuilder
                .newBuilder()
                .setIfNotNull(Admin.Fields.password, password)
                .setIfNotNull(Admin.Fields.name, name)
                .setIfNotNull(Admin.Fields.roleId, roleId)
                .build();
        return mongoTemplate.updateMulti(query, update, Admin.class).map(UpdateResult::wasAcknowledged);
    }

    public Mono<Long> countAdmins(@Nullable Set<String> accounts, @Nullable Long roleId) {
        Query query = QueryBuilder.newBuilder()
                .addInIfNotNull(ID, accounts)
                .addIsIfNotNull(Admin.Fields.roleId, roleId)
                .buildQuery();
        return mongoTemplate.count(query, Admin.class);
    }
}
