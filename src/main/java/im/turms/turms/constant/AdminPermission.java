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

    GROUP_CREATE,
    GROUP_DELETE,
    GROUP_UPDATE,
    GROUP_QUERY,

    GROUP_TYPE_CREATE,
    GROUP_TYPE_DELETE,
    GROUP_TYPE_UPDATE,
    GROUP_TYPE_QUERY,

    MESSAGE_CREATE,
    MESSAGE_DELETE,
    MESSAGE_UPDATE,
    MESSAGE_QUERY,

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
        return permissions;
    }

    public static Set<AdminPermission> allContentGroup() {
        Set<AdminPermission> permissions = new HashSet<>();
        permissions.add(GROUP_CREATE);
        permissions.add(GROUP_DELETE);
        permissions.add(GROUP_UPDATE);
        permissions.add(GROUP_QUERY);
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
        permissions.add(CLUSTER_CONFIG_UPDATE);
        return permissions;
    }
}