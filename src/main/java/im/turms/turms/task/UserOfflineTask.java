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
import im.turms.turms.constant.DeviceType;
import im.turms.turms.service.user.onlineuser.OnlineUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.web.reactive.socket.CloseStatus;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

@SpringAware
public class UserOfflineTask implements Callable<Boolean>, Serializable, ApplicationContextAware {
    private static final long serialVersionUID = -1625876227422489616L;
    private final Long userId;
    private final Set<Integer> deviceTypes;
    private final Integer closeStatus;
    private transient ApplicationContext context;
    private transient OnlineUserService onlineUserService;

    public UserOfflineTask(
            @NotNull Long userId,
            @NotEmpty Set<Integer> deviceTypes,
            @NotNull Integer closeStatus) {
        this.userId = userId;
        this.deviceTypes = deviceTypes;
        this.closeStatus = closeStatus;
    }

    @Override
    public Boolean call() {
        Set<DeviceType> types = deviceTypes
                .stream()
                .map(integer -> DeviceType.values()[integer])
                .collect(Collectors.toSet());
        return onlineUserService.setLocalUserDevicesOffline(
                userId,
                types,
                new CloseStatus(closeStatus));
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
