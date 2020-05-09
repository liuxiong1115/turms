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

import im.turms.turms.manager.TurmsPluginManager;
import im.turms.turms.plugin.LogHandler;
import im.turms.turms.pojo.domain.UserActionLog;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;
import reactor.core.publisher.Mono;

import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;

@Service
@Validated
public class UserActionLogService {
    private final ReactiveMongoTemplate mongoTemplate;
    private final TurmsPluginManager turmsPluginManager;

    public UserActionLogService(
            @Qualifier("logMongoTemplate") ReactiveMongoTemplate mongoTemplate,
            TurmsPluginManager turmsPluginManager) {
        this.mongoTemplate = mongoTemplate;
        this.turmsPluginManager = turmsPluginManager;
    }

    public Mono<UserActionLog> save(UserActionLog log) {
        return mongoTemplate.save(log);
    }

    public Mono<Void> triggerLogHandlers(@NotNull UserActionLog log) {
        List<LogHandler> logHandlerList = turmsPluginManager.getLogHandlerList();
        if (!logHandlerList.isEmpty()) {
            List<Mono<Void>> monos = new ArrayList<>(logHandlerList.size());
            for (LogHandler handler : logHandlerList) {
                monos.add(handler.handleUserActionLog(log));
            }
            return Mono.when(monos);
        } else {
            return Mono.empty();
        }
    }
}
