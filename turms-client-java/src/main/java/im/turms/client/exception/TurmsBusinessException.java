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

package im.turms.client.exception;

import im.turms.common.exception.NoStackTraceException;
import im.turms.common.model.dto.notification.TurmsNotification;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * @author James Chen
 */
public class TurmsBusinessException extends NoStackTraceException {

    private final int code;
    private final String reason;

    private TurmsBusinessException(int code, @Nullable String reason, @Nullable Throwable cause) {
        super(formatMessage(code, reason), cause);
        this.code = code;
        this.reason = reason;
    }

    public static TurmsBusinessException get(int code) {
        return new TurmsBusinessException(code, null, null);
    }

    public static TurmsBusinessException get(int code, @Nullable String reason) {
        boolean reasonExists = reason != null && !reason.isEmpty();
        if (reasonExists) {
            return new TurmsBusinessException(code, reason, null);
        } else {
            return new TurmsBusinessException(code, null, null);
        }
    }

    public static TurmsBusinessException get(int code, @Nullable Throwable cause) {
        return new TurmsBusinessException(code, null, cause);
    }

    public static TurmsBusinessException get(TurmsNotification notification) {
        int code = notification.getCode().getValue();
        return notification.hasReason()
                ? TurmsBusinessException.get(code, notification.getReason().getValue())
                : TurmsBusinessException.get(code);
    }

    private static String formatMessage(int code, @Nullable String reason) {
        if (reason != null) {
            return "code: " + code + ", reason: " + reason;
        } else {
            return "code: " + code;
        }
    }

    public int getCode() {
        return code;
    }

    public String getReason() {
        return reason;
    }

    @Override
    public String getMessage() {
        return reason;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        TurmsBusinessException that = (TurmsBusinessException) o;
        return code == that.code && Objects.equals(reason, that.reason);
    }

    @Override
    public int hashCode() {
        return Objects.hash(code, reason);
    }

}