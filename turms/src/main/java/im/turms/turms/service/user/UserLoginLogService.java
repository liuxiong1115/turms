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

import com.mongodb.client.result.UpdateResult;
import im.turms.common.constant.DeviceType;
import im.turms.turms.annotation.constraint.DeviceTypeConstraint;
import im.turms.turms.cluster.TurmsClusterManager;
import im.turms.turms.plugin.LogHandler;
import im.turms.turms.plugin.TurmsPluginManager;
import im.turms.turms.pojo.domain.UserLoginLog;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;
import reactor.core.publisher.Mono;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.PastOrPresent;
import java.util.Date;
import java.util.Map;

import static im.turms.turms.common.Constants.ID;

@Service
@Validated
public class UserLoginLogService {
    private final ReactiveMongoTemplate mongoTemplate;
    private final TurmsClusterManager turmsClusterManager;
    private final TurmsPluginManager turmsPluginManager;

    public UserLoginLogService(ReactiveMongoTemplate mongoTemplate, TurmsClusterManager turmsClusterManager, TurmsPluginManager turmsPluginManager) {
        this.mongoTemplate = mongoTemplate;
        this.turmsClusterManager = turmsClusterManager;
        this.turmsPluginManager = turmsPluginManager;
    }

    public Mono<UserLoginLog> save(
            @NotNull Long userId,
            @Nullable Integer ip,
            @NotNull @DeviceTypeConstraint DeviceType deviceType,
            @Nullable Map<String, String> deviceDetails,
            @Nullable Long locationId) {
        Long id = turmsClusterManager.generateRandomId();
        UserLoginLog userLoginLog = new UserLoginLog(id, userId, new Date(), null, locationId, ip, deviceType, deviceDetails);
        return mongoTemplate.save(userLoginLog);
    }

    public Mono<Boolean> updateLogoutDate(
            @NotNull Long id,
            @NotNull @PastOrPresent Date logoutDate) {
        Query query = new Query().addCriteria(Criteria.where(ID).is(id));
        Update update = new Update().set(UserLoginLog.Fields.logoutDate, logoutDate);
        return mongoTemplate.updateFirst(query, update, UserLoginLog.class)
                .map(UpdateResult::wasAcknowledged);
    }

    public void triggerLogHandlers(@NotNull UserLoginLog log) {
        for (LogHandler handler : turmsPluginManager.getLogHandlerList()) {
            handler.handleUserLoginLog(log);
        }
    }

    public void triggerLogHandlers(
            @Nullable Long userId,
            @Nullable Integer ip,
            @Nullable @DeviceTypeConstraint DeviceType loggingInDeviceType,
            @Nullable Map<String, String> deviceDetails,
            @Nullable Long locationId) {
        if (!turmsPluginManager.getLogHandlerList().isEmpty()) {
            UserLoginLog userLoginLog = new UserLoginLog(null, userId, new Date(), null,
                    locationId, ip, loggingInDeviceType, deviceDetails);
            triggerLogHandlers(userLoginLog);
        }
    }
}
