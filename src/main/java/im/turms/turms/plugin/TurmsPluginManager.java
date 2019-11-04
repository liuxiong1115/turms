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

package im.turms.turms.plugin;

import lombok.Data;
import org.pf4j.DefaultPluginManager;
import org.pf4j.PluginManager;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;

@Component
@Data
public class TurmsPluginManager {
    private List<ClientRequestHandler> clientRequestHandlerList;
    private List<ExpiryMessageAutoDeletionNotificationHandler> expiryMessageAutoDeletionNotificationHandlerList;
    private List<UserAuthenticator> userAuthenticatorList;
    private List<UserOnlineStatusChangeHandler> userOnlineStatusChangeHandlerList;

    public TurmsPluginManager() {
        clientRequestHandlerList = Collections.emptyList();
        expiryMessageAutoDeletionNotificationHandlerList = Collections.emptyList();
        userAuthenticatorList = Collections.emptyList();
        userOnlineStatusChangeHandlerList = Collections.emptyList();
    }

    @PostConstruct
    public void init() {
        PluginManager pluginManager = new DefaultPluginManager(Paths.get("../plugins"));
        pluginManager.loadPlugins();
        pluginManager.startPlugins();
        clientRequestHandlerList = pluginManager.getExtensions(ClientRequestHandler.class);
        expiryMessageAutoDeletionNotificationHandlerList = pluginManager.getExtensions(ExpiryMessageAutoDeletionNotificationHandler.class);
        userAuthenticatorList = pluginManager.getExtensions(UserAuthenticator.class);
        userOnlineStatusChangeHandlerList = pluginManager.getExtensions(UserOnlineStatusChangeHandler.class);
    }
}