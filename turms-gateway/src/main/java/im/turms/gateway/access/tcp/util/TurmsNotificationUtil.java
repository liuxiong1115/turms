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

package im.turms.gateway.access.tcp.util;

import com.google.protobuf.Int32Value;
import com.google.protobuf.Int64Value;
import im.turms.server.common.constant.TurmsStatusCode;
import im.turms.common.model.dto.notification.TurmsNotification;

/**
 * @author James Chen
 */
public class TurmsNotificationUtil {

    private TurmsNotificationUtil() {
    }

    public static TurmsNotification sessionClosed(long requestId) {
        return TurmsNotification
                .newBuilder()
                .setRequestId(Int64Value.newBuilder().setValue(requestId).build())
                .setCode(Int32Value.newBuilder().setValue(TurmsStatusCode.SERVER_INTERNAL_ERROR.getBusinessCode()).build())
                .build();
    }

}