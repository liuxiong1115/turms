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

import im.turms.turms.constant.DeviceType;
import im.turms.turms.service.user.onlineuser.OnlineUserManager;
import org.pf4j.ExtensionPoint;
import org.springframework.web.reactive.socket.CloseStatus;
import reactor.core.publisher.Mono;

import javax.validation.constraints.NotNull;

/**
 * UserOnlineStatusChangeHandler notifies when user goes online or offline.
 */
public interface UserOnlineStatusChangeHandler extends ExtensionPoint {
    Mono<Void> goOnline(@NotNull OnlineUserManager onlineUserManager, @NotNull DeviceType loggingInDeviceType);
    Mono<Void> goOffline(@NotNull OnlineUserManager onlineUserManager, @NotNull CloseStatus closeStatus);
}
