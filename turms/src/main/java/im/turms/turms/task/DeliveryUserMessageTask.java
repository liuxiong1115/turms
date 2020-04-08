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
import im.turms.turms.service.message.OutboundMessageService;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import javax.validation.constraints.NotEmpty;
import java.io.Serializable;
import java.util.Set;
import java.util.concurrent.Callable;

/**
 * Note: UserMessage refers to WebSocketMessage, not the message model in business.
 */
@SpringAware
public class DeliveryUserMessageTask implements Callable<Boolean>, Serializable, ApplicationContextAware {
    private static final long serialVersionUID = 4595269008081593689L;
    private final byte[] clientMessageBytes;
    private final Set<Long> recipientIds;
    private transient ApplicationContext context;
    private transient OutboundMessageService outboundMessageService;

    public DeliveryUserMessageTask(@NotEmpty byte[] clientMessageBytes, @NotEmpty Set<Long> recipientIds) {
        this.clientMessageBytes = clientMessageBytes;
        this.recipientIds = recipientIds;
    }

    @Override
    public Boolean call() {
        return outboundMessageService.relayClientMessageToLocalClients(clientMessageBytes, recipientIds);
    }

    @Override
    public void setApplicationContext(final ApplicationContext applicationContext) throws BeansException {
        context = applicationContext;
    }

    @Autowired
    public void setOutboundMessageService(final OutboundMessageService outboundMessageService) {
        this.outboundMessageService = outboundMessageService;
    }
}
