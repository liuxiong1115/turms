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

package im.turms.turms.common;

import org.springframework.web.reactive.socket.CloseStatus;

/**
 * The status code can be used for both WebSocket or HTTP
 */
public enum TurmsStatusCode {
    // For general use
    OK(2000, "ok", 200),
    NOT_RESPONSIBLE(3000, "The server isn't responsible for the user", 307),
    FAILED(4000, "failed", 400),
    SERVER_INTERNAL_ERROR(5000, "Internal server error", 500),

    NO_CONTENT(2001, "No content", 204),
    ALREADY_UP_TO_DATE(2002, "Already up-to-date", 204),
    RECIPIENTS_OFFLINE(2003, "The recipients are offline", 200),

    DISABLE_FUNCTION(4001, "The function has been disabled in servers", 405),
    EXPIRY_DATE_BEFORE_NOW(4002, "Expiration date must be greater than now", 400),
    EXPIRY_RESOURCE(4003, "The target resource has expired", 400),
    ID_DUPLICATED(4004, "ID must be unique", 400),
    ILLEGAL_ARGUMENTS(4005, "Illegal arguments", 400),
    ILLEGAL_DATE_FORMAT(4006, "Illegal date format", 400),
    NOT_ACTIVE(4007, "Not active", 400),
    OWNED_RESOURCE_LIMIT_REACHED(4008, "The resource limit is reached", 400),
    REQUESTED_RECORDS_TOO_MANY(4009, "Too many records are requested", 400),
    RESOURCES_HAVE_BEEN_HANDLED(4010, "The resources have been handled", 400),
    RESOURCES_HAVE_CHANGED(4011, "The resources have been changed", 400),
    SESSION_SIMULTANEOUS_CONFLICTS_DECLINE(4012, "A different device has logged into your account", 409),
    SESSION_SIMULTANEOUS_CONFLICTS_NOTIFY(4013, "Someone attempted to log into your account", 409),
    SESSION_SIMULTANEOUS_CONFLICTS_OFFLINE(4014, "A different device has logged into your account", 409),
    SUCCESSOR_NOT_GROUP_MEMBER(4015, "The successor is not the group member", 400),
    TARGET_USERS_UNAUTHORIZED(4016, "The target users are unauthorized", 400),
    UNAUTHORIZED(4017, "Unauthorized", 401),

    LOGGED_DEVICES_CANNOT_OFFLINE(5001, "Cannot set logged in devices offline", 500);

    private int businessCode;
    private String reason;
    private int httpCode;

    TurmsStatusCode(int businessCode, String reason, int httpCode) {
        this.businessCode = businessCode;
        this.reason = reason;
        this.httpCode = httpCode;
    }

    public CloseStatus getWebSocketCloseStatus() {
        return new CloseStatus(businessCode, reason);
    }

    public int getBusinessCode() {
        return businessCode;
    }

    public String getReason() {
        return reason;
    }

    public int getHttpStatusCode() {
        return httpCode;
    }
}