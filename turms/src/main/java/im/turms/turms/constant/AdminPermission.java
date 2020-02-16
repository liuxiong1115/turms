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

package im.turms.turms.constant;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public enum AdminPermission {
    NONE,

    STATISTICS_USER_QUERY,
    STATISTICS_GROUP_QUERY,
    STATISTICS_MESSAGE_QUERY,

    USER_CREATE,
    USER_DELETE,
    USER_UPDATE,
    USER_QUERY,

    USER_RELATIONSHIP_CREATE,
    USER_RELATIONSHIP_DELETE,
    USER_RELATIONSHIP_UPDATE,
    USER_RELATIONSHIP_QUERY,

    USER_RELATIONSHIP_GROUP_CREATE,
    USER_RELATIONSHIP_GROUP_DELETE,
    USER_RELATIONSHIP_GROUP_UPDATE,
    USER_RELATIONSHIP_GROUP_QUERY,

    USER_FRIEND_REQUEST_CREATE,
    USER_FRIEND_REQUEST_DELETE,
    USER_FRIEND_REQUEST_UPDATE,
    USER_FRIEND_REQUEST_QUERY,

    USER_PERMISSION_GROUP_CREATE,
    USER_PERMISSION_GROUP_DELETE,
    USER_PERMISSION_GROUP_UPDATE,
    USER_PERMISSION_GROUP_QUERY,

    USER_ONLINE_INFO_UPDATE,
    USER_ONLINE_INFO_QUERY,

    GROUP_CREATE,
    GROUP_DELETE,
    GROUP_UPDATE,
    GROUP_QUERY,

    GROUP_BLACKLIST_CREATE,
    GROUP_BLACKLIST_DELETE,
    GROUP_BLACKLIST_UPDATE,
    GROUP_BLACKLIST_QUERY,

    GROUP_INVITATION_CREATE,
    GROUP_INVITATION_DELETE,
    GROUP_INVITATION_UPDATE,
    GROUP_INVITATION_QUERY,

    GROUP_QUESTION_CREATE,
    GROUP_QUESTION_DELETE,
    GROUP_QUESTION_UPDATE,
    GROUP_QUESTION_QUERY,

    GROUP_JOIN_REQUEST_CREATE,
    GROUP_JOIN_REQUEST_DELETE,
    GROUP_JOIN_REQUEST_UPDATE,
    GROUP_JOIN_REQUEST_QUERY,

    GROUP_MEMBER_UPDATE,
    GROUP_MEMBER_CREATE,
    GROUP_MEMBER_DELETE,
    GROUP_MEMBER_QUERY,

    GROUP_TYPE_CREATE,
    GROUP_TYPE_DELETE,
    GROUP_TYPE_UPDATE,
    GROUP_TYPE_QUERY,

    MESSAGE_CREATE,
    MESSAGE_DELETE,
    MESSAGE_UPDATE,
    MESSAGE_QUERY,

    MESSAGE_STATUS_QUERY,
    MESSAGE_STATUS_UPDATE,

    ADMIN_CREATE,
    ADMIN_DELETE,
    ADMIN_UPDATE,
    ADMIN_QUERY,

    ADMIN_ROLE_CREATE,
    ADMIN_ROLE_DELETE,
    ADMIN_ROLE_UPDATE,
    ADMIN_ROLE_QUERY,

    ADMIN_ACTION_LOG_DELETE,
    ADMIN_ACTION_LOG_QUERY,

    CLUSTER_CONFIG_UPDATE,
    CLUSTER_CONFIG_QUERY;

    public static Set<AdminPermission> all() {
        return new HashSet<>(Arrays.asList(AdminPermission.values()));
    }

    public static Set<AdminPermission> allStatistics() {
        Set<AdminPermission> permissions = new HashSet<>();
        permissions.add(STATISTICS_USER_QUERY);
        permissions.add(STATISTICS_GROUP_QUERY);
        permissions.add(STATISTICS_MESSAGE_QUERY);
        return permissions;
    }

    public static Set<AdminPermission> allContentUser() {
        Set<AdminPermission> permissions = new HashSet<>();
        permissions.add(USER_CREATE);
        permissions.add(USER_DELETE);
        permissions.add(USER_UPDATE);
        permissions.add(USER_QUERY);
        permissions.add(USER_RELATIONSHIP_CREATE);
        permissions.add(USER_RELATIONSHIP_DELETE);
        permissions.add(USER_RELATIONSHIP_UPDATE);
        permissions.add(USER_RELATIONSHIP_QUERY);
        permissions.add(USER_RELATIONSHIP_GROUP_CREATE);
        permissions.add(USER_RELATIONSHIP_GROUP_DELETE);
        permissions.add(USER_RELATIONSHIP_GROUP_UPDATE);
        permissions.add(USER_RELATIONSHIP_GROUP_QUERY);
        permissions.add(USER_FRIEND_REQUEST_CREATE);
        permissions.add(USER_FRIEND_REQUEST_DELETE);
        permissions.add(USER_FRIEND_REQUEST_UPDATE);
        permissions.add(USER_FRIEND_REQUEST_QUERY);
        permissions.add(USER_PERMISSION_GROUP_CREATE);
        permissions.add(USER_PERMISSION_GROUP_DELETE);
        permissions.add(USER_PERMISSION_GROUP_UPDATE);
        permissions.add(USER_PERMISSION_GROUP_QUERY);
        permissions.add(USER_ONLINE_INFO_UPDATE);
        permissions.add(USER_ONLINE_INFO_QUERY);
        return permissions;
    }

    public static Set<AdminPermission> allContentGroup() {
        Set<AdminPermission> permissions = new HashSet<>();
        permissions.add(GROUP_CREATE);
        permissions.add(GROUP_DELETE);
        permissions.add(GROUP_UPDATE);
        permissions.add(GROUP_QUERY);
        permissions.add(GROUP_BLACKLIST_CREATE);
        permissions.add(GROUP_BLACKLIST_DELETE);
        permissions.add(GROUP_BLACKLIST_UPDATE);
        permissions.add(GROUP_BLACKLIST_QUERY);
        permissions.add(GROUP_INVITATION_CREATE);
        permissions.add(GROUP_INVITATION_DELETE);
        permissions.add(GROUP_INVITATION_UPDATE);
        permissions.add(GROUP_INVITATION_QUERY);
        permissions.add(GROUP_QUESTION_CREATE);
        permissions.add(GROUP_QUESTION_DELETE);
        permissions.add(GROUP_QUESTION_UPDATE);
        permissions.add(GROUP_QUESTION_QUERY);
        permissions.add(GROUP_JOIN_REQUEST_CREATE);
        permissions.add(GROUP_JOIN_REQUEST_DELETE);
        permissions.add(GROUP_JOIN_REQUEST_UPDATE);
        permissions.add(GROUP_JOIN_REQUEST_QUERY);
        permissions.add(GROUP_MEMBER_CREATE);
        permissions.add(GROUP_MEMBER_DELETE);
        permissions.add(GROUP_MEMBER_UPDATE);
        permissions.add(GROUP_MEMBER_QUERY);
        permissions.add(GROUP_TYPE_CREATE);
        permissions.add(GROUP_TYPE_DELETE);
        permissions.add(GROUP_TYPE_UPDATE);
        permissions.add(GROUP_TYPE_QUERY);
        return permissions;
    }

    public static Set<AdminPermission> allContentMessage() {
        Set<AdminPermission> permissions = new HashSet<>();
        permissions.add(MESSAGE_CREATE);
        permissions.add(MESSAGE_DELETE);
        permissions.add(MESSAGE_UPDATE);
        permissions.add(MESSAGE_QUERY);
        permissions.add(MESSAGE_STATUS_QUERY);
        permissions.add(MESSAGE_STATUS_UPDATE);
        return permissions;
    }

    public static Set<AdminPermission> allContentAdmin() {
        Set<AdminPermission> permissions = new HashSet<>();
        permissions.add(ADMIN_CREATE);
        permissions.add(ADMIN_DELETE);
        permissions.add(ADMIN_UPDATE);
        permissions.add(ADMIN_QUERY);
        permissions.add(ADMIN_ROLE_CREATE);
        permissions.add(ADMIN_ROLE_DELETE);
        permissions.add(ADMIN_ROLE_UPDATE);
        permissions.add(ADMIN_ROLE_QUERY);
        permissions.add(ADMIN_ACTION_LOG_QUERY);
        permissions.add(ADMIN_ACTION_LOG_DELETE);
        return permissions;
    }

    public static Set<AdminPermission> allCluster() {
        Set<AdminPermission> permissions = new HashSet<>();
        permissions.add(CLUSTER_CONFIG_QUERY);
        permissions.add(CLUSTER_CONFIG_UPDATE);
        return permissions;
    }

    public static Set<AdminPermission> allCreate() {
        Set<AdminPermission> permissions = new HashSet<>();
        permissions.add(USER_CREATE);
        permissions.add(USER_RELATIONSHIP_CREATE);
        permissions.add(USER_RELATIONSHIP_GROUP_CREATE);
        permissions.add(USER_FRIEND_REQUEST_CREATE);
        permissions.add(USER_PERMISSION_GROUP_CREATE);
        permissions.add(GROUP_CREATE);
        permissions.add(GROUP_BLACKLIST_CREATE);
        permissions.add(GROUP_INVITATION_CREATE);
        permissions.add(GROUP_QUESTION_CREATE);
        permissions.add(GROUP_JOIN_REQUEST_CREATE);
        permissions.add(GROUP_MEMBER_CREATE);
        permissions.add(GROUP_TYPE_CREATE);
        permissions.add(MESSAGE_CREATE);
        permissions.add(ADMIN_CREATE);
        permissions.add(ADMIN_ROLE_CREATE);
        return permissions;
    }

    public static Set<AdminPermission> allQuery() {
        Set<AdminPermission> permissions = new HashSet<>();
        permissions.add(STATISTICS_USER_QUERY);
        permissions.add(STATISTICS_GROUP_QUERY);
        permissions.add(STATISTICS_MESSAGE_QUERY);
        permissions.add(USER_QUERY);
        permissions.add(USER_RELATIONSHIP_QUERY);
        permissions.add(USER_RELATIONSHIP_GROUP_QUERY);
        permissions.add(USER_FRIEND_REQUEST_QUERY);
        permissions.add(USER_PERMISSION_GROUP_QUERY);
        permissions.add(USER_ONLINE_INFO_QUERY);
        permissions.add(GROUP_QUERY);
        permissions.add(GROUP_BLACKLIST_QUERY);
        permissions.add(GROUP_INVITATION_QUERY);
        permissions.add(GROUP_QUESTION_QUERY);
        permissions.add(GROUP_JOIN_REQUEST_QUERY);
        permissions.add(GROUP_MEMBER_QUERY);
        permissions.add(GROUP_TYPE_QUERY);
        permissions.add(MESSAGE_QUERY);
        permissions.add(MESSAGE_STATUS_QUERY);
        permissions.add(ADMIN_QUERY);
        permissions.add(ADMIN_ROLE_QUERY);
        permissions.add(ADMIN_ACTION_LOG_QUERY);
        permissions.add(CLUSTER_CONFIG_QUERY);
        return permissions;
    }
}