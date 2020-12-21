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

package im.turms.server.common.constant;

import java.util.HashMap;
import java.util.Map;

/**
 * @author James Chen
 */
public enum TurmsStatusCode {

    // Successful responses
    OK(1000, "ok", 200),
    NO_CONTENT(1001, "No content", 204),
    ALREADY_UP_TO_DATE(1002, "Already up-to-date", 204),

    //**********************************************************
    //* For application error
    //**********************************************************

    // Client
    INVALID_REQUEST(1100, "The client request is invalid", 400),
    CLIENT_REQUESTS_TOO_FREQUENT(1101, "The client requests are too frequent to handle", 429),
    ILLEGAL_ARGUMENTS(1102, "Illegal arguments", 400),
    RECORD_CONTAINS_DUPLICATE_KEY(1103, "The record to add contains a duplicate key", 409),
    REQUESTED_RECORDS_TOO_MANY(1104, "Too many records are requested", 429),

    // Server
    SERVER_INTERNAL_ERROR(1200, "Internal server error", 500),
    SERVER_UNAVAILABLE(1201, "The server is unavailable", 503),
    // TODO: Remove after MessageStatusService is removed
    DISABLED_FUNCTION(1202, "The function has been disabled on the server side", 405),

    //**********************************************************
    //* For error about administrator actions
    //**********************************************************

    // Admin
    UNAUTHORIZED(1300, "Unauthorized", 401),

    //**********************************************************
    //* For business error
    //**********************************************************

    // User

    // User - Login
    LOGIN_USER_ID_IS_NULL(2000, "", 0),
    LOGIN_AUTHENTICATION_FAILED(2001, "The user information is wrong", 401),
    LOGGING_IN_USER_NOT_ACTIVE(2002, "The logging in user is inactive or deleted", 401),
    LOGIN_FROM_FORBIDDEN_DEVICE_TYPE(2003, "The device type is forbidden for the request", 403),
    FORBIDDEN_DEVICE_TYPE_FOR_LOGIN_FAILURE_REASON(2004, "The device type is forbidden for the request", 403),
    CACHING_FOR_LOGIN_FAILURE_REASON_IS_DISABLED(2005, "", 0),

    // User - Session
    SESSION_SIMULTANEOUS_CONFLICTS_DECLINE(2100, "A different device has logged into your account", 409),
    SESSION_SIMULTANEOUS_CONFLICTS_NOTIFY(2101, "Someone attempted to log into your account", 409),
    SESSION_SIMULTANEOUS_CONFLICTS_OFFLINE(2102, "A different device has logged into your account", 409),
    SESSION_NOT_EXISTS(2103, "The session doesn't exist", 503),
    CREATE_EXISTING_SESSION(2104, "The session doesn't exist", 503),
    FORBIDDEN_DEVICE_TYPE_FOR_SESSION_DISCONNECTION_REASON(2105, "The device type is forbidden for the request", 403),
    CACHING_FOR_SESSION_DISCONNECTION_REASON_IS_DISABLED(2106, "", 0),

    // User - Location
    USER_LOCATION_RELATED_FEATURES_ARE_DISABLED(2200, "", 0),
    QUERYING_NEAREST_USERS_BY_SESSION_ID_IS_DISABLED(2201, "", 0),

    // User - Info
    UPDATE_INFO_OF_NON_EXISTING_USER(2300, "", 0),
    USER_PROFILE_NOT_FOUND(2301, "", 0),
    PROFILE_REQUESTER_NOT_IN_CONTACTS_OR_BLOCKED(2302, "", 0),
    PROFILE_REQUESTER_HAS_BEEN_BLOCKED(2303, "The user has been blacklisted", 406),

    // User - Permission
    QUERY_PERMISSION_OF_NON_EXISTING_USER(2400, "", 0),

    // User - Relationship
    ADD_NOT_RELATED_USER_TO_GROUP(2500, "", 0),
    CREATE_EXISTING_RELATIONSHIP(2501, "Cannot create existing relationship", 406),

    // User - Friend Request
    REQUESTER_NOT_FRIEND_REQUEST_RECIPIENT(2600, "Only the recipient of the friend request can handle the friend request", 0),
    CREATE_EXISTING_FRIEND_REQUEST(2601, "A friend request has already existed", 406),
    FRIEND_REQUEST_SENDER_HAS_BEEN_BLOCKED(2602, "The friend request sender has been blocked by the requestee", 406),

    // Group

    // Group - Info
    UPDATE_INFO_OF_NON_EXISTING_GROUP(3000, "", 0),
    NO_PERMISSION_TO_UPDATE_GROUP_INFO(3001, "", 0),
    GROUP_HAS_BEEN_MUTED(3002, "The group has been muted", 406),

    // Group - Type
    NO_PERMISSION_TO_CREATE_GROUP_WITH_GROUP_TYPE(3100, "", 0),
    CREATE_GROUP_WITH_NON_EXISTING_GROUP_TYPE(3101, "", 0),

    // Group - Ownership
    CREATE_GROUP_REQUESTER_NOT_ACTIVE(3200, "The user is inactive or deleted", 503),
    NO_PERMISSION_TO_TRANSFER_GROUP(3201, "", 0),
    NO_PERMISSION_TO_DELETE_GROUP(3202, "", 0),
    SUCCESSOR_NOT_GROUP_MEMBER(3203, "The successor is not a member of the group", 406),
    MAX_OWNED_GROUPS_REACHED(3204, "The user has reached the maximum allowed owned groups", 406),
    TRANSFER_NON_EXISTING_GROUP(3205, "", 0),

    // Group - Question
    NO_PERMISSION_TO_ACCESS_GROUP_QUESTION(3300, "", 0),
    GROUP_QUESTION_ANSWERER_HAS_BEEN_BLOCKED(3301, "The user has been blacklisted", 406),
    MEMBER_CANNOT_ANSWER_GROUP_QUESTION(3302, "The user is already a member of the group", 406),
    ANSWER_QUESTION_OF_INACTIVE_GROUP(3303, "", 0),

    // Group - Member
    NO_PERMISSION_TO_REMOVE_GROUP_MEMBER_INFO(3400, "", 0),
    NO_PERMISSION_TO_UPDATE_GROUP_MEMBER_INFO(3401, "", 0),
    USER_NOT_GROUP_MEMBER(3402, "The user is not a member of the group", 406),
    MEMBER_HAS_BEEN_MUTED(3403, "The group member has been muted", 406),
    GUESTS_HAVE_BEEN_MUTED(3404, "The guests of the group have been muted", 406),
    ADD_INACTIVE_BLOCKED_USER_TO_GROUP(3405, "", 0),
    ADD_USER_TO_INACTIVE_GROUP(3406, "", 0),

    // Group - Blocklist
    NOT_OWNER_OR_MANAGER_TO_BLOCK_GROUP_USER(3500, "", 0),

    // Group - Join Request
    GROUP_JOIN_REQUEST_SENDER_HAS_BEEN_BLOCKED(3600, "The group join request sender has been blocked", 406),
    REQUESTER_NOT_JOIN_REQUEST_SENDER(3601, "", 0),
    NO_PERMISSION_TO_ACCESS_GROUP_REQUEST(3602, "", 0),
    GROUP_JOIN_REQUEST_NOT_PENDING(3603, "", 0),
    SEND_JOIN_REQUEST_TO_INACTIVE_GROUP(3604, "", 0),
    RECALLING_GROUP_JOIN_REQUEST_IS_DISABLED(3605, "It's not allow to recall the join request sent by oneself", 0),

    // Group - Invitation
    GROUP_INVITER_NOT_MEMBER(3701, "", 0),
    GROUP_INVITEE_ALREADY_GROUP_MEMBER(3702, "The user is already a member of the group", 406),
    NO_PERMISSION_TO_ACCESS_INVITATION(3703, "", 0),
    INVITEE_HAS_BEEN_BLOCKED(3704, "The invitee has been blacklisted", 406),
    RECALLING_GROUP_INVITATION_IS_DISABLED(3705, "The invitee has been blacklisted", 406),
    REDUNDANT_GROUP_INVITATION(3706, "The group invitation is redundant", 406),
    GROUP_INVITATION_NOT_PENDING(3707, "", 0),

    // Conversation
    UPDATING_TYPING_STATUS_IS_DISABLED(4000, "The function has been disabled in servers", 405),

    // Message
    MESSAGE_RECALL_TIMEOUT(5000, "", 0),

    // Message - Send
    MESSAGE_RECIPIENT_NOT_ACTIVE(5100, "", 0),
    MESSAGE_SENDER_NOT_IN_CONTACTS_OR_BLOCKED(5101, "", 0),
    PRIVATE_MESSAGE_SENDER_HAS_BEEN_BLOCKED(5102, "The user has been blacklisted", 406),
    GROUP_MESSAGE_SENDER_HAS_BEEN_BLOCKED(5103, "The user has been blacklisted", 406),
    SEND_MESSAGE_TO_INACTIVE_GROUP(5104, "The user has been blacklisted", 406),
    SENDING_MESSAGES_TO_ONESELF_IS_DISABLED(5105, "It's not allowed to send messages to oneself", 0),

    // Message - Update
    UPDATING_MESSAGE_BY_SENDER_IS_DISABLED(5200, "", 0),
    UPDATE_MESSAGE_REQUESTER_NOT_SENDER(5201, "", 0),
    UPDATE_MESSAGE_REQUESTER_NOT_MESSAGE_RECIPIENT(5202, "Only the message recipient can update the read date", 0),

    // Message - Recall
    RECALL_NON_EXISTING_MESSAGE(5300, "", 0),
    RECALLING_MESSAGE_IS_DISABLED(5301, "It's not allowed to recall message", 0),

    // Storage
    STORAGE_NOT_IMPLEMENTED(6000, "The function is enabled but not implemented yet", 501),
    FILE_TOO_LARGE(6001, "The file is too large to upload", 413),

    // Storage - Extension
    REDUNDANT_REQUEST_FOR_PRESIGNED_PROFILE_URL(6500, "The file is too large to upload", 413);

    public static final int STATUS_CODE_LENGTH = 4;
    private static final Map<Integer, TurmsStatusCode> CODE_POOL = new HashMap<>((int) (TurmsStatusCode.values().length / 0.5));

    static {
        for (TurmsStatusCode value : TurmsStatusCode.values()) {
            TurmsStatusCode code = CODE_POOL.put(value.businessCode, value);
            if (code != null) {
                throw new IllegalStateException("Found duplicate business code " + code.businessCode);
            }
        }
    }

    private final int businessCode;
    private final String reason;
    private final int httpCode;

    TurmsStatusCode(int businessCode, String reason, int httpCode) {
        this.businessCode = businessCode;
        this.reason = reason;
        this.httpCode = httpCode;
    }

    public static TurmsStatusCode from(int businessCode) {
        return CODE_POOL.get(businessCode);
    }

    public static boolean isSuccessCode(int businessCode) {
        return 1100 <= businessCode && businessCode < 1200;
    }

    public static boolean isServerError(int businessCode) {
        return 1200 <= businessCode && businessCode < 1300;
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

    public boolean isSuccessCode() {
        return isSuccessCode(businessCode);
    }

    public boolean isServerError() {
        return isServerError(businessCode);
    }

}