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

package im.turms.client.constant;

/**
 * @author James Chen
 */
public class TurmsStatusCode {

    private TurmsStatusCode() {
    }

    //**********************************************************
    //* Defined by client
    //**********************************************************

    //**********************************************************
    //* For application error
    //**********************************************************

    // Client - Request
    public static final int INVALID_REQUEST = 100;
    public static final int CLIENT_REQUESTS_TOO_FREQUENT = 101;
    public static final int REQUEST_TIMEOUT = 102;
    public static final int ILLEGAL_ARGUMENTS = 103;

    // Server - Notification
    public static final int INVALID_NOTIFICATION = 200;

    //**********************************************************
    //* For business error
    //**********************************************************

    // User - Session
    public static final int CLIENT_SESSION_ALREADY_ESTABLISHED = 300;
    public static final int CLIENT_SESSION_HAS_BEEN_CLOSED = 300;

    // Message
    public static final int MESSAGE_IS_REJECTED = 400;

    // Storage
    public static final int QUERY_PROFILE_URL_TO_UPDATE_BEFORE_LOGIN = 500;

    //**********************************************************
    //* Defined by server
    //**********************************************************

    // Successful responses
    public static final int OK = 1000;
    public static final int NO_CONTENT = 1001;
    public static final int ALREADY_UP_TO_DATE = 1002;

    //**********************************************************
    //* For application error
    //**********************************************************

    // Client
    public static final int INVALID_REQUEST_FROM_SERVER = 1100;
    public static final int CLIENT_REQUESTS_TOO_FREQUENT_FROM_SERVER = 1101;
    public static final int ILLEGAL_ARGUMENTS_FROM_SERVER = 1102;
    public static final int RECORD_CONTAINS_DUPLICATE_KEY = 1103;
    public static final int REQUESTED_RECORDS_TOO_MANY = 1104;

    // Server
    public static final int SERVER_INTERNAL_ERROR = 1200;
    public static final int SERVER_UNAVAILABLE = 1201;
    public static final int DISABLED_FUNCTION = 1202;

    //**********************************************************
    //* For error about administrator actions
    //**********************************************************

    // Admin
    public static final int UNAUTHORIZED = 1300;

    //**********************************************************
    //* For business error
    //**********************************************************

    // User

    // User - Login
    public static final int LOGIN_USER_ID_IS_NULL = 2000;
    public static final int LOGIN_AUTHENTICATION_FAILED = 2001;
    public static final int LOGGING_IN_USER_NOT_ACTIVE = 2002;
    public static final int LOGIN_FROM_FORBIDDEN_DEVICE_TYPE = 2003;
    public static final int FORBIDDEN_DEVICE_TYPE_FOR_LOGIN_FAILURE_REASON = 2004;
    public static final int CACHING_FOR_LOGIN_FAILURE_REASON_IS_DISABLED = 2005;

    // User - Session
    public static final int SESSION_SIMULTANEOUS_CONFLICTS_DECLINE = 2100;
    public static final int SESSION_SIMULTANEOUS_CONFLICTS_NOTIFY = 2101;
    public static final int SESSION_SIMULTANEOUS_CONFLICTS_OFFLINE = 2102;
    public static final int SESSION_NOT_EXISTS = 2103;
    public static final int CREATE_EXISTING_SESSION = 2104;
    public static final int FORBIDDEN_DEVICE_TYPE_FOR_SESSION_DISCONNECTION_REASON = 2105;
    public static final int CACHING_FOR_SESSION_DISCONNECTION_REASON_IS_DISABLED = 2106;

    // User - Location
    public static final int USER_LOCATION_RELATED_FEATURES_ARE_DISABLED = 2200;
    public static final int QUERYING_NEAREST_USERS_BY_SESSION_ID_IS_DISABLED = 2201;

    // User - Info
    public static final int UPDATE_INFO_OF_NON_EXISTING_USER = 2300;
    public static final int USER_PROFILE_NOT_FOUND = 2301;
    public static final int PROFILE_REQUESTER_NOT_IN_CONTACTS_OR_BLOCKED = 2302;
    public static final int PROFILE_REQUESTER_HAS_BEEN_BLOCKED = 2303;

    // User - Permission
    public static final int QUERY_PERMISSION_OF_NON_EXISTING_USER = 2400;

    // User - Relationship
    public static final int ADD_NOT_RELATED_USER_TO_GROUP = 2500;
    public static final int CREATE_EXISTING_RELATIONSHIP = 2501;

    // User - Friend Request
    public static final int REQUESTER_NOT_FRIEND_REQUEST_RECIPIENT = 2600;
    public static final int CREATE_EXISTING_FRIEND_REQUEST = 2601;
    public static final int FRIEND_REQUEST_SENDER_HAS_BEEN_BLOCKED = 2602;

    // Group

    // Group - Info
    public static final int UPDATE_INFO_OF_NON_EXISTING_GROUP = 3000;
    public static final int NO_PERMISSION_TO_UPDATE_GROUP_INFO = 3001;
    public static final int GROUP_HAS_BEEN_MUTED = 3002;

    // Group - Type
    public static final int NO_PERMISSION_TO_CREATE_GROUP_WITH_GROUP_TYPE = 3100;
    public static final int CREATE_GROUP_WITH_NON_EXISTING_GROUP_TYPE = 3101;

    // Group - Ownership
    public static final int CREATE_GROUP_REQUESTER_NOT_ACTIVE = 3200;
    public static final int NO_PERMISSION_TO_TRANSFER_GROUP = 3201;
    public static final int NO_PERMISSION_TO_DELETE_GROUP = 3202;
    public static final int SUCCESSOR_NOT_GROUP_MEMBER = 3203;
    public static final int MAX_OWNED_GROUPS_REACHED = 3204;

    // Group - Question
    public static final int NO_PERMISSION_TO_ACCESS_GROUP_QUESTION = 3300;
    public static final int GROUP_QUESTION_ANSWERER_HAS_BEEN_BLOCKED = 3301;
    public static final int MEMBER_CANNOT_ANSWER_GROUP_QUESTION = 3302;
    public static final int ANSWER_QUESTION_OF_INACTIVE_GROUP = 3303;

    // Group - Member
    public static final int NO_PERMISSION_TO_REMOVE_GROUP_MEMBER_INFO = 3400;
    public static final int NO_PERMISSION_TO_UPDATE_GROUP_MEMBER_INFO = 3401;
    public static final int USER_NOT_GROUP_MEMBER = 3402;
    public static final int MEMBER_HAS_BEEN_MUTED = 3403;
    public static final int GUESTS_HAVE_BEEN_MUTED = 3404;
    public static final int ADD_INACTIVE_BLOCKED_USER_TO_GROUP = 3405;
    public static final int ADD_USER_TO_INACTIVE_GROUP = 3406;

    // Group - Blocklist
    public static final int NOT_OWNER_OR_MANAGER_TO_BLOCK_GROUP_USER = 3500;

    // Group - Join Request
    public static final int GROUP_JOIN_REQUEST_SENDER_HAS_BEEN_BLOCKED = 3600;
    public static final int REQUESTER_NOT_JOIN_REQUEST_SENDER = 3601;
    public static final int NO_PERMISSION_TO_ACCESS_GROUP_REQUEST = 3602;
    public static final int GROUP_JOIN_REQUEST_NOT_PENDING = 3603;
    public static final int SEND_JOIN_REQUEST_TO_INACTIVE_GROUP = 3604;
    public static final int RECALLING_GROUP_JOIN_REQUEST_IS_DISABLED = 3605;

    // Group - Invitation
    public static final int GROUP_INVITER_NOT_MEMBER = 3701;
    public static final int GROUP_INVITEE_ALREADY_GROUP_MEMBER = 3702;
    public static final int NO_PERMISSION_TO_ACCESS_INVITATION = 3703;
    public static final int INVITEE_HAS_BEEN_BLOCKED = 3704;
    public static final int RECALLING_GROUP_INVITATION_IS_DISABLED = 3705;
    public static final int REDUNDANT_GROUP_INVITATION = 3706;
    public static final int GROUP_INVITATION_NOT_PENDING = 3707;

    // Conversation
    public static final int UPDATING_TYPING_STATUS_IS_DISABLED = 4000;

    // Message
    public static final int MESSAGE_RECALL_TIMEOUT = 5000;

    // Message - Send
    public static final int MESSAGE_RECIPIENT_NOT_ACTIVE = 5100;
    public static final int MESSAGE_SENDER_NOT_IN_CONTACTS_OR_BLOCKED = 5101;
    public static final int PRIVATE_MESSAGE_SENDER_HAS_BEEN_BLOCKED = 5102;
    public static final int GROUP_MESSAGE_SENDER_HAS_BEEN_BLOCKED = 5103;
    public static final int SEND_MESSAGE_TO_INACTIVE_GROUP = 5104;
    public static final int SENDING_MESSAGES_TO_ONESELF_IS_DISABLED = 5105;

    // Message - Update
    public static final int UPDATING_MESSAGE_BY_SENDER_IS_DISABLED = 5200;
    public static final int UPDATE_MESSAGE_REQUESTER_NOT_SENDER = 5201;
    public static final int UPDATE_MESSAGE_REQUESTER_NOT_MESSAGE_RECIPIENT = 5202;

    // Message - Recall
    public static final int RECALL_NON_EXISTING_MESSAGE = 5300;
    public static final int RECALLING_MESSAGE_IS_DISABLED = 5301;

    // Storage
    public static final int STORAGE_NOT_IMPLEMENTED = 6000;
    public static final int FILE_TOO_LARGE = 6001;

    public static boolean isSuccessCode(int businessCode) {
        return 1000 <= businessCode && businessCode < 2000;
    }

    public static boolean isServerError(int businessCode) {
        return 5000 <= businessCode && businessCode < 6000;
    }

}