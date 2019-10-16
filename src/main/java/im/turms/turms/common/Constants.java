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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import im.turms.turms.constant.DeviceType;
import im.turms.turms.constant.UserStatus;
import im.turms.turms.pojo.bo.UserOnlineInfo;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.DefaultManagedTaskScheduler;

import java.util.Arrays;
import java.util.Date;
import java.util.Set;
import java.util.stream.Collectors;

public class Constants {

    private Constants() {
    }

    public static final String ID = "_id";
    public static final String ID_GROUP_ID = "_id.groupId";
    public static final String ID_USER_ID = "_id.userId";
    public static final String ID_OWNER_ID = "_id.ownerId";
    public static final String ID_RELATED_USER_ID = "_id.relatedUserId";
    public static final String ID_GROUP_INDEX = "_id.groupIndex";
    public static final String ID_GROUP_TYPE_ID = "_id.groupTypeId";
    public static final String ID_INDEX = "_id.index";
    public static final String ID_MESSAGE_ID = "_id.messageId";
    public static final String ID_RECIPIENT_ID = "_id.recipientId";
    public static final String ID_TARGET_ID = "_id.targetId";
    public static final String ID_CHAT_TYPE = "_id.chatType";

    public static final String HAZELCAST_ADMINS_MAP = "admins";
    public static final String HAZELCAST_ROLES_MAP = "adminRoles";
    public static final String HAZELCAST_GROUP_TYPES_MAP = "groupTypes";
    public static final String HAZELCAST_EXPIRY_USER_FRIEND_REQUESTS_CLEANER_LOCK = "ufrcl";
    public static final String HAZELCAST_EXPIRY_GROUP_INVITATIONS_CLEANER_LOCK = "gicl";
    public static final String HAZELCAST_EXPIRY_GROUP_JOIN_REQUESTS_CLEANER_LOCK = "gjrcl";

    public static final String EXPIRY_USER_FRIEND_REQUESTS_CLEANER_CRON = "0 0 2 * * ?";
    public static final String EXPIRY_GROUP_INVITATIONS_CLEANER_CRON = "0 15 2 * * ?";
    public static final String EXPIRY_GROUP_JOIN_REQUESTS_CLEANER_CRON = "0 30 2 * * ?";
    public static final String EXPIRY_MESSAGES_CLEANER_CRON = "0 45 2 * * ?";
    public static final String ONLINE_USERS_NUMBER_PERSISTER_CRON = "0 0/5 * * * ?";

    public static final String ACCOUNT = "account";
    public static final String PASSWORD = "password";
    public static final String ACKNOWLEDGED = "acknowledged";
    public static final String AUTHENTICATED = "authenticated";
    public static final String STATUS = "status";

    public static final long ADMIN_ROLE_ROOT_ID = 0L;
    public static final String ADMIN_ROLE_ROOT_NAME = "ROOT";
    public static final long DEFAULT_GROUP_TYPE_ID = 0L;
    public static final String DEFAULT_GROUP_TYPE_NAME = "DEFAULT";
    public static final int DEFAULT_RELATIONSHIP_GROUP_INDEX = 0;
    public static final Object[] EMPTY_ARRAY = new Object[0];
    public static final Object EMPTY_OBJECT = new Object();
    public static final Date EPOCH = new Date(0);
    public static final Date MAX_DATE = new Date(Long.MAX_VALUE);
    public static final Set<DeviceType> ALL_DEVICE_TYPES = Arrays.stream(DeviceType.values()).collect(Collectors.toSet());
    public static final UserOnlineInfo OFFLINE_USER_ONLINE_INFO = UserOnlineInfo.builder()
            .userStatus(UserStatus.OFFLINE)
            .build();

    public static final TaskScheduler TASK_SCHEDULER = new DefaultManagedTaskScheduler();
    public static final ObjectMapper MAPPER = new ObjectMapper()
            .setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
    /**
     * Note: This is a precompile directive that java compiler can use it to optimize
     * the source code in at compile time.
     * So, DO NOT remove "static final"
     */
    public static final boolean DEV_MODE = true;
}
