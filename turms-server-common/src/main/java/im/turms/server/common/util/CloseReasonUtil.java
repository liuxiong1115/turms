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

package im.turms.server.common.util;

import com.google.protobuf.Int32Value;
import com.google.protobuf.StringValue;
import im.turms.common.constant.statuscode.SessionCloseStatus;
import im.turms.common.constant.statuscode.TurmsStatusCode;
import im.turms.common.model.dto.notification.TurmsNotification;
import im.turms.server.common.dto.CloseReason;
import im.turms.server.common.pojo.ThrowableInfo;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;

/**
 * @author James Chen
 */
@Log4j2
public class CloseReasonUtil {

    @Getter
    @Setter
    private static boolean returnReasonForServerError;

    private CloseReasonUtil() {
    }

    public static CloseReason parse(Throwable throwable) {
        ThrowableInfo info = ThrowableInfo.get(throwable);
        return CloseReason.get(info.getCode(), info.getReason());
    }

    public static SessionCloseStatus statusCodeToCloseStatus(TurmsStatusCode code) {
        if (code.isServerError()) {
            return SessionCloseStatus.SERVER_ERROR;
        }
        SessionCloseStatus closeStatus;
        switch (code) {
            case SESSION_SIMULTANEOUS_CONFLICTS_DECLINE:
            case SESSION_SIMULTANEOUS_CONFLICTS_NOTIFY:
            case SESSION_SIMULTANEOUS_CONFLICTS_OFFLINE:
                closeStatus = SessionCloseStatus.DISCONNECTED_BY_OTHER_DEVICE;
                break;
            case UNAVAILABLE:
                closeStatus = SessionCloseStatus.SERVER_UNAVAILABLE;
                break;
            case ILLEGAL_ARGUMENTS:
            case FORBIDDEN_DEVICE_TYPE:
                closeStatus = SessionCloseStatus.ILLEGAL_REQUEST;
                break;
            default:
                closeStatus = SessionCloseStatus.UNKNOWN_ERROR;
                break;
        }
        return closeStatus;
    }

    public static TurmsNotification toNotification(CloseReason closeReason) {
        TurmsStatusCode statusCode;
        SessionCloseStatus closeStatus;
        if (closeReason.isTurmsStatusCode()) {
            statusCode = TurmsStatusCode.from(closeReason.getCode());
            closeStatus = statusCodeToCloseStatus(statusCode);
        } else {
            closeStatus = SessionCloseStatus.get(closeReason.getCode());
            if (closeStatus == null) {
                log.warn("Failed to convert the code {} to the session close status", closeReason.getCode());
                closeStatus = SessionCloseStatus.UNKNOWN_ERROR;
            }
        }
        String reason = closeReason.getReason();
        TurmsNotification.Builder builder = TurmsNotification
                .newBuilder()
                .setCloseStatus(Int32Value.newBuilder().setValue(closeStatus.getCode()).build())
                .setCode(Int32Value.newBuilder().setValue(closeReason.getCode()).build());
        if (reason != null) {
            if (closeStatus.isServerError()) {
                if (returnReasonForServerError) {
                    builder.setReason(StringValue.newBuilder().setValue(reason).build());
                }
            } else {
                builder.setReason(StringValue.newBuilder().setValue(reason).build());
            }
        }
        return builder.build();
    }

}