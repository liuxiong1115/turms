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

package im.turms.client.service;

import im.turms.client.TurmsClient;
import im.turms.common.model.dto.request.TurmsRequest;

import java.util.function.Consumer;

/**
 * @author James Chen
 */
public class NotificationService {

    private Consumer<TurmsRequest> onNotification;

    public NotificationService(TurmsClient turmsClient) {
        turmsClient.getDriver()
                .getOnNotificationListeners()
                .add(notification -> {
                    if (onNotification != null && notification.hasRelayedRequest()) {
                        TurmsRequest request = notification.getRelayedRequest();
                        if (!request.hasCreateMessageRequest()) {
                            onNotification.accept(request);
                        }
                    }
                });
    }

    public void setOnNotification(Consumer<TurmsRequest> onNotification) {
        this.onNotification = onNotification;
    }

}
