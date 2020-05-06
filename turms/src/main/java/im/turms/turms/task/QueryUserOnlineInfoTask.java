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
import im.turms.common.constant.UserStatus;
import im.turms.turms.pojo.bo.UserOnlineInfo;
import im.turms.turms.service.user.onlineuser.OnlineUserService;
import im.turms.turms.service.user.onlineuser.manager.OnlineUserManager;
import lombok.Getter;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import javax.validation.constraints.NotNull;
import java.util.concurrent.Callable;

@SpringAware
public class QueryUserOnlineInfoTask implements Callable<UserOnlineInfo>, ApplicationContextAware {

    @Getter
    private final Long userId;

    private transient ApplicationContext context;
    private transient OnlineUserService onlineUserService;

    public QueryUserOnlineInfoTask(@NotNull Long userId) {
        this.userId = userId;
    }

    @Override
    public UserOnlineInfo call() {
        OnlineUserManager userManager = onlineUserService.getLocalOnlineUserManager(userId);
        if (userManager != null) {
            return userManager.getOnlineUserInfo();
        } else {
            return UserOnlineInfo.builder()
                    .userId(userId)
                    .userStatus(UserStatus.OFFLINE)
                    .build();
        }
    }

    @Override
    public void setApplicationContext(final ApplicationContext applicationContext) throws BeansException {
        context = applicationContext;
    }

    @Autowired
    public void setOnlineUserService(final OnlineUserService onlineUserService) {
        this.onlineUserService = onlineUserService;
    }
}
