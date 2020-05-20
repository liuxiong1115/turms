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

import com.google.common.net.InetAddresses;
import com.mongodb.DBObject;
import com.mongodb.client.result.DeleteResult;
import im.turms.turms.annotation.constraint.IpAddressConstraint;
import im.turms.turms.annotation.constraint.NoWhitespaceConstraint;
import im.turms.turms.builder.QueryBuilder;
import im.turms.turms.manager.TurmsClusterManager;
import im.turms.turms.manager.TurmsPluginManager;
import im.turms.turms.plugin.LogHandler;
import im.turms.turms.pojo.bo.DateRange;
import im.turms.turms.pojo.domain.AdminActionLog;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.PastOrPresent;
import java.net.InetAddress;
import java.util.Date;
import java.util.Set;

import static im.turms.turms.constant.Common.ID;

@Service
@Validated
public class AdminActionLogService {
    private final TurmsClusterManager turmsClusterManager;
    private final ReactiveMongoTemplate mongoTemplate;
    private final TurmsPluginManager turmsPluginManager;

    public AdminActionLogService(
            TurmsClusterManager turmsClusterManager,
            @Qualifier("logMongoTemplate") ReactiveMongoTemplate mongoTemplate,
            TurmsPluginManager turmsPluginManager) {
        this.turmsClusterManager = turmsClusterManager;
        this.mongoTemplate = mongoTemplate;
        this.turmsPluginManager = turmsPluginManager;
    }

    public Mono<AdminActionLog> saveAdminActionLog(
            @NotNull @NoWhitespaceConstraint String account,
            @NotNull @PastOrPresent Date timestamp,
            @NotNull @IpAddressConstraint String ip,
            @NotNull @NoWhitespaceConstraint String action,
            @Nullable DBObject params,
            @Nullable DBObject body) {
        InetAddress inetAddress = InetAddresses.forString(ip);
        AdminActionLog adminActionLog = new AdminActionLog(
                turmsClusterManager.generateRandomId(),
                account,
                timestamp,
                InetAddresses.coerceToInteger(inetAddress),
                action,
                params,
                body);
        return mongoTemplate.insert(adminActionLog);
    }

    public Mono<Boolean> deleteAdminActionLogs(
            @Nullable Set<Long> ids) {
        Query query = QueryBuilder.newBuilder()
                .addInIfNotNull(ID, ids)
                .buildQuery();
        return mongoTemplate.remove(query, AdminActionLog.class)
                .map(DeleteResult::wasAcknowledged);
    }

    public Flux<AdminActionLog> queryAdminActionLogs(
            @Nullable Set<Long> ids,
            @Nullable Set<String> accounts,
            @Nullable DateRange logDateRange,
            @Nullable Integer page,
            @Nullable Integer size) {
        Query query = QueryBuilder.newBuilder()
                .addInIfNotNull(ID, ids)
                .addInIfNotNull(AdminActionLog.Fields.ACCOUNT, accounts)
                .addBetweenIfNotNull(AdminActionLog.Fields.LOG_DATE, logDateRange)
                .paginateIfNotNull(page, size);
        return mongoTemplate.find(query, AdminActionLog.class);
    }

    public void triggerLogHandlers(
            @NotNull ServerWebExchange exchange,
            @Nullable Long id,
            @Nullable String account,
            @Nullable Date timestamp,
            @Nullable String host,
            @Nullable String action,
            @Nullable DBObject params,
            @Nullable DBObject body) {
        InetAddress inetAddress = InetAddresses.forString(host);
        AdminActionLog adminActionLog = new AdminActionLog(
                id,
                account,
                timestamp,
                InetAddresses.coerceToInteger(inetAddress),
                action,
                params,
                body);
        for (LogHandler logHandler : turmsPluginManager.getLogHandlerList()) {
            logHandler.handleAdminActionLog(exchange, adminActionLog);
        }
    }

    public void triggerLogHandlers(
            @NotNull ServerWebExchange exchange,
            @NotNull AdminActionLog log) {
        for (LogHandler logHandler : turmsPluginManager.getLogHandlerList()) {
            logHandler.handleAdminActionLog(exchange, log);
        }
    }

    public Mono<Long> countAdminActionLogs(
            @Nullable Set<Long> ids,
            @Nullable Set<String> accounts,
            @Nullable DateRange logDateRange) {
        Query query = QueryBuilder.newBuilder()
                .addInIfNotNull(ID, ids)
                .addInIfNotNull(AdminActionLog.Fields.ACCOUNT, accounts)
                .addBetweenIfNotNull(AdminActionLog.Fields.LOG_DATE, logDateRange)
                .buildQuery();
        return mongoTemplate.count(query, AdminActionLog.class);
    }
}
