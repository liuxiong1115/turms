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

package im.turms.turms.workflow.service.impl.log;

import im.turms.common.constant.DeviceType;
import im.turms.common.model.dto.request.TurmsRequest;
import im.turms.server.common.cluster.node.Node;
import im.turms.server.common.log4j.UserActivityLogging;
import im.turms.turms.bo.UserActionLog;
import im.turms.turms.plugin.extension.handler.UserActionLogHandler;
import im.turms.turms.plugin.manager.TurmsPluginManager;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @author James Chen
 */
@Service
public class UserActionLogService {

    private final Node node;
    private final TurmsPluginManager turmsPluginManager;

    public UserActionLogService(
            Node node,
            TurmsPluginManager turmsPluginManager) {
        this.node = node;
        this.turmsPluginManager = turmsPluginManager;
    }

    public void tryLogAndTriggerLogHandlers(@NotNull Long userId, @NotNull DeviceType deviceType, @NotNull TurmsRequest request) {
        boolean logUserAction = node.getSharedProperties().getService().getLog().isLogUserAction();
        List<UserActionLogHandler> handlerList = turmsPluginManager.getUserActionLogHandlerList();
        boolean triggerHandlers = turmsPluginManager.isEnabled() && !handlerList.isEmpty();
        if (logUserAction || triggerHandlers) {
            UserActionLog userActionLog;
            // Note that we use toString() instead of JSON for better performance
            String actionDetails = node.getSharedProperties().getService().getLog().isLogUserActionDetails()
                    ? request.toString()
                    : null;
            userActionLog = new UserActionLog(userId, deviceType, new Date(), request.getKindCase().name(), actionDetails);
            if (logUserAction) {
                UserActivityLogging.log(userActionLog);
            }
            if (triggerHandlers) {
                List<Mono<Void>> monos = new ArrayList<>(handlerList.size());
                for (UserActionLogHandler handler : handlerList) {
                    monos.add(handler.handleUserActionLog(userActionLog));
                }
                Mono.when(monos).subscribe();
            }
        }
    }

}