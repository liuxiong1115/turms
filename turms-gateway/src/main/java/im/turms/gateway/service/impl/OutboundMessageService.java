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

package im.turms.gateway.service.impl;

import im.turms.common.model.dto.notification.TurmsNotification;
import im.turms.gateway.manager.UserSessionsManager;
import im.turms.gateway.plugin.extension.NotificationHandler;
import im.turms.gateway.plugin.manager.TurmsPluginManager;
import im.turms.gateway.pojo.bo.session.UserSession;
import im.turms.server.common.cluster.node.Node;
import im.turms.server.common.rpc.service.IOutboundMessageService;
import im.turms.server.common.util.AssertUtil;
import io.netty.buffer.ByteBuf;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.*;

/**
 * @author James Chen
 */
@Component
@Log4j2
public class OutboundMessageService implements IOutboundMessageService {

    private final Node node;
    private final SessionService sessionService;
    private final TurmsPluginManager turmsPluginManager;

    public OutboundMessageService(
            Node node,
            SessionService sessionService,
            TurmsPluginManager turmsPluginManager) {
        this.node = node;
        this.sessionService = sessionService;
        this.turmsPluginManager = turmsPluginManager;
    }

    /**
     * @param notificationData should be a data serialized from TurmsNotification
     * @return true if the notification has forwarded to one recipient at least
     */
    @Override
    public boolean sendNotificationToLocalClients(
            @NotNull ByteBuf notificationData,
            @NotEmpty Set<Long> recipientIds) {
        AssertUtil.notNull(notificationData, "notificationData");
        AssertUtil.notEmpty(recipientIds, "recipientIds");
        // Prepare data
        boolean hasForwardedMessageToOneRecipient = false;
        boolean triggerHandlers = node.getSharedProperties().getPlugin().isEnabled()
                && !turmsPluginManager.getNotificationHandlerList().isEmpty();
        Set<Long> offlineRecipientIds = triggerHandlers
                ? new HashSet<>(Math.max(1, recipientIds.size() / 2))
                : Collections.emptySet();

        // Send notification
        for (Long recipientId : recipientIds) {
            UserSessionsManager userSessionsManager = sessionService.getUserSessionsManager(recipientId);
            if (userSessionsManager != null) {
                for (UserSession userSession : userSessionsManager.getSessionMap().values()) {
                    notificationData.retain();
                    // This will decrease the reference count of the message by 1 if the message is sent
                    boolean sent = userSession.tryEmitNextNotification(notificationData);
                    if (sent) {
                        hasForwardedMessageToOneRecipient = true;
                    }
                    userSession.getConnection().tryNotifyClientToRecover();
                }
            } else {
                if (triggerHandlers) {
                    offlineRecipientIds.add(recipientId);
                }
            }
        }

        // Trigger plugins
        if (triggerHandlers) {
            triggerPlugins(notificationData, recipientIds, offlineRecipientIds);
        }

        // Release
        boolean isReleased = notificationData.release();
        if (!isReleased) {
            // This may happen if any notification failed to emit to the client.
            while (!notificationData.release()) {
            }
        }

        return hasForwardedMessageToOneRecipient;
    }

    private void triggerPlugins(@NotNull ByteBuf notificationData,
                                @NotNull Set<Long> recipientIds,
                                @NotNull Set<Long> offlineRecipientIds) {
        TurmsNotification notification = null;
        try {
            notification = TurmsNotification.parseFrom(notificationData.nioBuffer());
        } catch (Exception e) {
            log.error("Failed to parse TurmsNotification", e);
        }
        if (notification != null) {
            List<NotificationHandler> handlerList = turmsPluginManager.getNotificationHandlerList();
            int size = handlerList.size();
            if (size > 0) {
                if (size == 1) {
                    handlerList.iterator().next().handle(notification, recipientIds, offlineRecipientIds)
                            .doOnError(log::error)
                            .subscribe();
                } else {
                    List<Mono<Void>> monos = new ArrayList<>(size);
                    for (NotificationHandler handler : handlerList) {
                        monos.add(handler.handle(notification, recipientIds, offlineRecipientIds));
                    }
                    Mono.when(monos)
                            .doOnError(log::error)
                            .subscribe();
                }
            }
        }
    }

}
