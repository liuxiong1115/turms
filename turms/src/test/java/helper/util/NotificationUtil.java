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

package helper.util;

import im.turms.turms.pojo.notification.TurmsNotification;
import im.turms.turms.pojo.request.TurmsRequest;

import javax.validation.constraints.NotNull;

public class NotificationUtil {
    private NotificationUtil() {

    }

    public static boolean isSessionOrSpecificRequest(
            @NotNull TurmsNotification notification,
            @NotNull TurmsRequest.KindCase kindCase) {
        if (notification.hasData()) {
            return notification.getData().getKindCase() == TurmsNotification.Data.KindCase.SESSION;
        } else {
            return notification.getRelayedRequest().getKindCase() == kindCase;
        }
    }
}
