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

package im.turms.turms.task;

import com.hazelcast.spring.context.SpringAware;
import im.turms.common.constant.DeviceType;
import im.turms.turms.service.user.onlineuser.OnlineUserService;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.web.reactive.socket.CloseStatus;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;
import java.util.Set;
import java.util.concurrent.Callable;

@SpringAware
public class SetUserOfflineTask implements Callable<Boolean>, ApplicationContextAware {

    @Getter
    private final Long userId;

    @Getter
    private final Set<DeviceType> deviceTypes;

    @Getter
    private final CloseStatus closeStatus;

    private transient ApplicationContext context;
    private transient OnlineUserService onlineUserService;

    public SetUserOfflineTask(
            @NotNull Long userId,
            @Nullable Set<DeviceType> deviceTypes,
            @NotNull CloseStatus closeStatus) {
        this.userId = userId;
        this.deviceTypes = deviceTypes;
        this.closeStatus = closeStatus;
    }

    @Override
    public Boolean call() {
        if (deviceTypes != null && !deviceTypes.isEmpty()) {
            return onlineUserService.setLocalUserDevicesOffline(userId, deviceTypes, closeStatus);
        } else {
            return onlineUserService.setLocalUserOffline(userId, closeStatus);
        }
    }

    @Override
    public void setApplicationContext(final ApplicationContext applicationContext) {
        context = applicationContext;
    }

    @Autowired
    public void setOnlineUserService(final OnlineUserService onlineUserService) {
        this.onlineUserService = onlineUserService;
    }
}
