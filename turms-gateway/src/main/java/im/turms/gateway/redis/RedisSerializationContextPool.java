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

package im.turms.gateway.redis;

import im.turms.common.constant.statuscode.TurmsStatusCode;
import im.turms.gateway.pojo.bo.login.LoginFailureReasonKey;
import im.turms.gateway.pojo.bo.session.SessionDisconnectionReasonKey;
import im.turms.gateway.redis.serializer.LoginFailureReasonKeySerializer;
import im.turms.gateway.redis.serializer.LoginFailureReasonSerializer;
import im.turms.gateway.redis.serializer.SessionDisconnectionReasonKeySerializer;
import im.turms.gateway.redis.serializer.SessionDisconnectionReasonSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;

/**
 * @author James Chen
 */
public class RedisSerializationContextPool {

    private RedisSerializationContextPool() {
    }

    public static final RedisSerializationContext<LoginFailureReasonKey, TurmsStatusCode> LOGIN_FAILURE_REASON_SERIALIZATION_CONTEXT;
    public static final RedisSerializationContext<SessionDisconnectionReasonKey, Integer> SESSION_DISCONNECTION_REASON_SERIALIZATION_CONTEXT;

    static {
        LoginFailureReasonKeySerializer loginFailureReasonKeySerializer = new LoginFailureReasonKeySerializer();
        LoginFailureReasonSerializer loginFailureReasonSerializer = new LoginFailureReasonSerializer();
        LOGIN_FAILURE_REASON_SERIALIZATION_CONTEXT = RedisSerializationContext
                .<LoginFailureReasonKey, TurmsStatusCode>newSerializationContext()
                .key(loginFailureReasonKeySerializer, loginFailureReasonKeySerializer)
                .value(loginFailureReasonSerializer, loginFailureReasonSerializer)
                .hashKey(loginFailureReasonKeySerializer, loginFailureReasonKeySerializer)
                .hashValue(loginFailureReasonSerializer, loginFailureReasonSerializer)
                .build();

        SessionDisconnectionReasonKeySerializer sessionDisconnectionReasonKeySerializer = new SessionDisconnectionReasonKeySerializer();
        SessionDisconnectionReasonSerializer sessionDisconnectionReasonSerializer = new SessionDisconnectionReasonSerializer();
        SESSION_DISCONNECTION_REASON_SERIALIZATION_CONTEXT = RedisSerializationContext
                .<SessionDisconnectionReasonKey, Integer>newSerializationContext()
                .key(sessionDisconnectionReasonKeySerializer, sessionDisconnectionReasonKeySerializer)
                .value(sessionDisconnectionReasonSerializer, sessionDisconnectionReasonSerializer)
                .hashKey(sessionDisconnectionReasonKeySerializer, sessionDisconnectionReasonKeySerializer)
                .hashValue(sessionDisconnectionReasonSerializer, sessionDisconnectionReasonSerializer)
                .build();
    }

}
