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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import im.turms.common.constant.DeviceType;
import im.turms.common.exception.TurmsBusinessException;
import im.turms.turms.pojo.domain.UserLocation;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.dao.DuplicateKeyException;
import reactor.core.publisher.Mono;
import reactor.retry.Retry;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

public class Common {

    private Common() {
    }

    public static final String ID = "_id";

    public static final String HAZELCAST_ADMINS_MAP = "admins";
    public static final String HAZELCAST_ROLES_MAP = "adminRoles";
    public static final String HAZELCAST_USER_PERMISSION_GROUPS_MAP = "userPermissionGroups";
    public static final String HAZELCAST_IRRESPONSIBLE_USERS_MAP = "users";
    public static final String HAZELCAST_GROUP_TYPES_MAP = "groupTypes";

    public static final String EXPIRED_USER_FRIEND_REQUESTS_CLEANER_CRON = "0 0 2 * * ?";
    public static final String EXPIRED_GROUP_INVITATIONS_CLEANER_CRON = "0 15 2 * * ?";
    public static final String EXPIRED_GROUP_JOIN_REQUESTS_CLEANER_CRON = "0 30 2 * * ?";
    public static final String EXPIRED_MESSAGES_CLEANER_CRON = "0 45 2 * * ?";

    public static final Long RESERVED_ID = 0L;
    public static final Long ADMIN_ROLE_ROOT_ID = RESERVED_ID;
    public static final Long ADMIN_REQUESTER_ID = RESERVED_ID;
    public static final String ADMIN_ROLE_ROOT_NAME = "ROOT";
    public static final Long DEFAULT_USER_PERMISSION_GROUP_ID = RESERVED_ID;
    public static final Long DEFAULT_GROUP_TYPE_ID = RESERVED_ID;
    public static final String DEFAULT_GROUP_TYPE_NAME = "DEFAULT";
    public static final Integer DEFAULT_RELATIONSHIP_GROUP_INDEX = Math.toIntExact(RESERVED_ID);
    public static final Object[] EMPTY_ARRAY = new Object[0];
    public static final Object EMPTY_OBJECT = new Object();
    public static final Pair EMPTY_PAIR = Pair.of(null, null);
    public static final UserLocation EMPTY_USER_LOCATION = new UserLocation(null, null, null, null, null, null, null);
    public static final Mono EMPTY_SET_MONO = Mono.just(Collections.emptySet());
    public static final Date EPOCH = new Date(0);
    public static final Set<DeviceType> ALL_DEVICE_TYPES = Arrays.stream(DeviceType.values())
            .filter(deviceType -> deviceType != DeviceType.UNRECOGNIZED)
            .collect(Collectors.toSet());

    public static final int MONGO_TRANSACTION_RETRIES_NUMBER = 3;
    public static final Duration MONGO_TRANSACTION_BACKOFF = Duration.ofMillis(1500);
    public static final Retry<Object> INSERT_RETRY = Retry.allBut(DuplicateKeyException.class)
            .retryMax(MONGO_TRANSACTION_RETRIES_NUMBER)
            .fixedBackoff(MONGO_TRANSACTION_BACKOFF);
    public static final Retry<Object> TRANSACTION_RETRY = Retry.allBut(DuplicateKeyException.class, TurmsBusinessException.class)
            .retryMax(MONGO_TRANSACTION_RETRIES_NUMBER)
            .fixedBackoff(MONGO_TRANSACTION_BACKOFF);

    public static final ObjectMapper MAPPER = new ObjectMapper()
            .setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
    public static final TypeReference<HashMap<String, Object>> TYPE_REF_MAP = new TypeReference<>() {
    };

    public static <T, R> Pair<T, R> emptyPair() {
        return EMPTY_PAIR;
    }

    public static <T> Mono<Set<T>> emptySetMono() {
        return EMPTY_SET_MONO;
    }
}
