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

package im.turms.common;

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

    DISABLED_FUNCTION(4001, "The function has been disabled in servers", 405),
    EXPIRY_DATE_BEFORE_NOW(4002, "Expiration date must be greater than now", 400),
    EXPIRED_RESOURCE(4003, "The target resource has expired", 406),
    DUPLICATE_KEY(4004, "The record being added contains a duplicate key", 406),
    ILLEGAL_ARGUMENTS(4005, "Illegal arguments", 400),
    ILLEGAL_DATE_FORMAT(4006, "Illegal date format", 400),
    NOT_ACTIVE(4007, "Not active", 406),
    OWNED_RESOURCE_LIMIT_REACHED(4008, "The resource limit is reached", 406),
    REQUESTED_RECORDS_TOO_MANY(4009, "Too many records are requested", 406),
    RESOURCES_HAVE_BEEN_HANDLED(4010, "The resources have been handled", 406),
    RESOURCES_HAVE_CHANGED(4011, "The resources have been changed", 406),
    SESSION_SIMULTANEOUS_CONFLICTS_DECLINE(4012, "A different device has logged into your account", 409),
    SESSION_SIMULTANEOUS_CONFLICTS_NOTIFY(4013, "Someone attempted to log into your account", 409),
    SESSION_SIMULTANEOUS_CONFLICTS_OFFLINE(4014, "A different device has logged into your account", 409),
    SUCCESSOR_NOT_GROUP_MEMBER(4015, "The successor is not a member of the group", 406),
    TARGET_USERS_UNAUTHORIZED(4016, "The target users are unauthorized", 406),
    TARGET_USERS_NOT_EXIST(4017, "The target users do not exist", 404),
    TYPE_NOT_EXISTS(4018, "The resource type does not exist", 404),
    UNAUTHORIZED(4019, "Unauthorized", 401),
    REDUNDANT_REQUEST(4020, "The request is redundant", 406),
    ALREADY_GROUP_MEMBER(4021, "The user is already a member of the group", 406),
    FRIEND_REQUEST_HAS_EXISTED(4022, "A friend request has already existed", 406),
    RELATIONSHIP_HAS_ESTABLISHED(4023, "The relationship has already established", 406),
    USER_NOT_GROUP_MEMBER(4024, "The user is not a member of the group", 406),
    USER_HAS_BEEN_BLACKLISTED(4025, "The user has been blacklisted", 406),
    GROUP_HAS_BEEN_MUTED(4026, "The group has been muted", 406),
    MEMBER_HAS_BEEN_MUTED(4027, "The group member has been muted", 406),
    GUESTS_HAVE_BEEN_MUTED(4028, "The guests of the group have been muted", 406),
    FILE_TOO_LARGE(4029, "The file is too large to upload", 413),
    REQUEST_TOO_LARGE(4030, "The request is too large", 413),

    LOGGED_DEVICES_CANNOT_OFFLINE(5001, "Cannot set logged in devices offline", 500),
    NOT_IMPLEMENTED(5002, "Not implemented", 501),
    UNAVAILABLE(5003, "The service is unavailable", 503),

    CLIENT_USER_ID_AND_PASSWORD_MUST_NOT_NULL(6000, "The user ID and password must not be null", 0),
    CLIENT_SESSION_HAS_BEEN_CLOSED(6001, "The session has been closed", 0),
    CLIENT_SESSION_ALREADY_ESTABLISHED(6002, "The session has been established", 0),
    CLIENT_REQUESTS_TOO_FREQUENT(6003, "Client requests are too frequent", 0),
    MISSING_DATA(6004, "The data is missing", 0),
    MESSAGE_IS_REJECTED(6050, "The message failed to be enqueued by the outgoing message buffer", 0);

    private final int businessCode;
    private final String reason;
    private final int httpCode;

    TurmsStatusCode(int businessCode, String reason, int httpCode) {
        this.businessCode = businessCode;
        this.reason = reason;
        this.httpCode = httpCode;
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

    public static TurmsStatusCode from(int businessCode) {
        for (TurmsStatusCode code : TurmsStatusCode.values()) {
            if (code.getHttpStatusCode() == businessCode) {
                return code;
            }
        }
        return null;
    }

    public boolean isSuccess() {
        return isSuccess(businessCode);
    }

    public static boolean isSuccess(int code) {
        return 2000 <= code && code < 3000;
    }
}