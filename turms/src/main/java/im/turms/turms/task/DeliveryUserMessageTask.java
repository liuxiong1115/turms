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
import im.turms.turms.service.user.onlineuser.OnlineUserManager;
import im.turms.turms.service.user.onlineuser.OnlineUserService;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.FluxSink;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentMap;

/**
 * Note: UserMessage refers to WebSocketMessage, not the message in business.
 */
@SpringAware
public class DeliveryUserMessageTask implements Callable<Boolean>, Serializable, ApplicationContextAware {
    private static final long serialVersionUID = 4595269008081593689L;
    private final byte[] clientMessageBytes;
    private final Long recipientId;
    private transient ApplicationContext context;
    private transient OnlineUserService onlineUserService;

    public DeliveryUserMessageTask(@NotEmpty byte[] clientMessageBytes, @NotNull Long recipientId) {
        this.clientMessageBytes = clientMessageBytes;
        this.recipientId = recipientId;
    }

    @Override
    public Boolean call() {
        OnlineUserManager userManager = onlineUserService.getLocalOnlineUserManager(recipientId);
        if (userManager != null) {
            ConcurrentMap<DeviceType, OnlineUserManager.Session> sessionMap = userManager
                    .getOnlineUserInfo().getSessionMap();
            for (OnlineUserManager.Session session : sessionMap.values()) {
                WebSocketSession webSocketSession = session.getWebSocketSession();
                FluxSink<WebSocketMessage> outputSink = session.getNotificationSink();
                WebSocketMessage message = webSocketSession
                        .binaryMessage(factory -> factory.wrap(clientMessageBytes));
                outputSink.next(message);
            }
            return true;
        } else {
            return false;
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
