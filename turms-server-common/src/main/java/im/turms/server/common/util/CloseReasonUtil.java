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

import im.turms.common.constant.statuscode.SessionCloseStatus;
import im.turms.common.constant.statuscode.TurmsStatusCode;
import im.turms.common.exception.TurmsBusinessException;
import im.turms.server.common.dto.CloseReason;

/**
 * @author James Chen
 */
public class CloseReasonUtil {

    private CloseReasonUtil() {
    }

    public static CloseReason parse(Throwable throwable) {
        TurmsStatusCode code;
        String reason;
        if (throwable instanceof TurmsBusinessException) {
            TurmsBusinessException exception = (TurmsBusinessException) throwable;
            code = exception.getCode();
            reason = exception.getReason();
        } else {
            code = TurmsStatusCode.SERVER_INTERNAL_ERROR;
            reason = throwable.getMessage();
        }
        return CloseReason.get(code, reason);
    }

    public static SessionCloseStatus statusCodeToCloseStatus(TurmsStatusCode code) {
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
                closeStatus = code.isServerError()
                        ? SessionCloseStatus.SERVER_ERROR
                        : SessionCloseStatus.UNKNOWN_ERROR;
                break;
        }
        return closeStatus;
    }

}