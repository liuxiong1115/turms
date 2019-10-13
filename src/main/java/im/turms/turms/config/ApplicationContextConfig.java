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

package im.turms.turms.config;

import im.turms.turms.common.Constants;
import im.turms.turms.common.TurmsLogger;
import im.turms.turms.service.user.onlineuser.OnlineUserService;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.CloseStatus;

@Component
public class ApplicationContextConfig {
    private final OnlineUserService onlineUserService;

    public ApplicationContextConfig(OnlineUserService onlineUserService) {
        this.onlineUserService = onlineUserService;
    }

    @EventListener(classes = ContextRefreshedEvent.class)
    public void handleContextRefreshedEvent() {
        if (Constants.DEV_MODE) {
            TurmsLogger.getLogger().warn("Turms is running in dev mode. Turn it off in im/turms/turms/common/Constants.java");
        }
    }

    @EventListener(classes = ContextClosedEvent.class)
    public void handleContextClosedEvent() {
        onlineUserService.setAllLocalUsersOffline(CloseStatus.SERVICE_RESTARTED);
    }
}
