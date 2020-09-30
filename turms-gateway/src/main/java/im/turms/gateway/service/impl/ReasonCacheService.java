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

package im.turms.gateway.service.impl;

import im.turms.common.constant.DeviceType;
import im.turms.common.constant.statuscode.SessionCloseStatus;
import im.turms.common.constant.statuscode.TurmsStatusCode;
import im.turms.common.exception.TurmsBusinessException;
import im.turms.gateway.pojo.bo.login.LoginFailureReasonKey;
import im.turms.gateway.pojo.bo.session.SessionDisconnectionReasonKey;
import im.turms.server.common.property.TurmsPropertiesManager;
import im.turms.server.common.property.env.gateway.SessionProperties;
import im.turms.server.common.util.AssertUtil;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.ReactiveValueOperations;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.socket.CloseStatus;
import reactor.core.publisher.Mono;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;
import java.time.Duration;
import java.util.Set;

/**
 * @author James Chen
 */
@Service
@Log4j2
public class ReasonCacheService {

    private final ReactiveValueOperations<LoginFailureReasonKey, TurmsStatusCode> loginFailureReasonCache;
    private final ReactiveValueOperations<SessionDisconnectionReasonKey, SessionCloseStatus> disconnectionReasonCache;
    private final Set<DeviceType> degradedDeviceTypes;
    private final boolean enableQueryLoginFailureReason;
    private final boolean enableQueryDisconnectionReason;
    private final Duration loginFailureReasonExpireAfter;
    private final Duration disconnectionReasonExpireAfter;

    @Autowired
    public ReasonCacheService(
            ReactiveRedisTemplate<LoginFailureReasonKey, TurmsStatusCode> loginFailureRedisTemplate,
            ReactiveRedisTemplate<SessionDisconnectionReasonKey, SessionCloseStatus> sessionDisconnectionRedisTemplate,
            TurmsPropertiesManager turmsPropertiesManager) {
        SessionProperties sessionProperties = turmsPropertiesManager.getLocalProperties().getGateway().getSession();
        loginFailureReasonCache = loginFailureRedisTemplate.opsForValue();
        disconnectionReasonCache = sessionDisconnectionRedisTemplate.opsForValue();
        enableQueryLoginFailureReason = sessionProperties.isEnableQueryLoginFailureReason();
        enableQueryDisconnectionReason = sessionProperties.isEnableQueryDisconnectionReason();
        degradedDeviceTypes = sessionProperties.getDegradedDeviceTypesForLoginFailureReason();
        loginFailureReasonExpireAfter = Duration.ofSeconds(sessionProperties.getLoginFailureReasonExpireAfter());
        disconnectionReasonExpireAfter = Duration.ofSeconds(sessionProperties.getDisconnectionReasonExpireAfter());
    }

    public Mono<TurmsStatusCode> getLoginFailureReason(@NotNull Long userId,
                                                       @NotNull DeviceType deviceType,
                                                       @NotNull Long requestId) {
        if (!enableQueryLoginFailureReason) {
            return Mono.error(TurmsBusinessException.get(TurmsStatusCode.DISABLED_FUNCTION));
        } else if (!degradedDeviceTypes.contains(deviceType)) {
            String reason = "The device type " + deviceType.name() + " is forbidden to query the reason for login failure";
            return Mono.error(TurmsBusinessException.get(TurmsStatusCode.FORBIDDEN_DEVICE_TYPE, reason));
        } else {
            try {
                AssertUtil.notNull(userId, "userId");
                AssertUtil.notNull(deviceType, "deviceType");
                AssertUtil.notNull(requestId, "requestId");
            } catch (TurmsBusinessException e) {
                return Mono.error(e);
            }
            LoginFailureReasonKey key = new LoginFailureReasonKey(userId, deviceType, requestId);
            return loginFailureReasonCache.get(key);
        }
    }

    public boolean shouldCacheLoginFailureReason(@Nullable Long userId,
                                                 @Nullable DeviceType deviceType,
                                                 @Nullable Long requestId) {
        return enableQueryLoginFailureReason
                && deviceType != null
                && degradedDeviceTypes.contains(deviceType)
                && userId != null
                && requestId != null;
    }

    public Mono<Boolean> cacheLoginFailureReason(
            @NotNull Long userId,
            @NotNull DeviceType deviceType,
            @NotNull Long requestId,
            @NotNull TurmsStatusCode status) {
        try {
            AssertUtil.notNull(userId, "userId");
            AssertUtil.notNull(deviceType, "deviceType");
            AssertUtil.notNull(requestId, "requestId");
            AssertUtil.notNull(status, "status");
        } catch (TurmsBusinessException e) {
            return Mono.error(e);
        }
        return loginFailureReasonCache.set(
                new LoginFailureReasonKey(userId, deviceType, requestId),
                status,
                loginFailureReasonExpireAfter);
    }

    public boolean shouldCacheDisconnectionReason(@Nullable Long userId, @Nullable DeviceType deviceType) {
        return enableQueryDisconnectionReason
                && deviceType != null
                && degradedDeviceTypes.contains(deviceType)
                && userId != null;
    }

    public Mono<Boolean> cacheDisconnectionReason(@NotNull Long userId,
                                                  @NotNull DeviceType deviceType,
                                                  @NotNull Integer sessionId,
                                                  @NotNull CloseStatus closeStatus) {
        try {
            AssertUtil.notNull(userId, "userId");
            AssertUtil.notNull(deviceType, "deviceType");
            AssertUtil.notNull(sessionId, "sessionId");
            AssertUtil.notNull(closeStatus, "closeStatus");
        } catch (TurmsBusinessException e) {
            return Mono.error(e);
        }
        SessionCloseStatus status = SessionCloseStatus.get(closeStatus.getCode());
        if (status != null) {
            return disconnectionReasonCache.set(
                    new SessionDisconnectionReasonKey(userId, deviceType, sessionId),
                    status,
                    disconnectionReasonExpireAfter);
        } else {
            String reason = "Cannot find a mapping SessionCloseStatus for the close status " + closeStatus.getCode();
            return Mono.error(TurmsBusinessException.get(TurmsStatusCode.SERVER_INTERNAL_ERROR, reason));
        }
    }

    public Mono<SessionCloseStatus> getDisconnectionReason(@NotNull Long userId,
                                                           @NotNull DeviceType deviceType,
                                                           @NotNull Integer sessionId) {
        if (!enableQueryDisconnectionReason) {
            return Mono.error(TurmsBusinessException.get(TurmsStatusCode.DISABLED_FUNCTION));
        } else if (!degradedDeviceTypes.contains(deviceType)) {
            String reason = "The device type " + deviceType + " is forbidden to query the reason for session disconnection";
            return Mono.error(TurmsBusinessException.get(TurmsStatusCode.FORBIDDEN_DEVICE_TYPE, reason));
        } else {
            try {
                AssertUtil.notNull(userId, "userId");
                AssertUtil.notNull(deviceType, "deviceType");
                AssertUtil.notNull(sessionId, "sessionId");
            } catch (TurmsBusinessException e) {
                return Mono.error(e);
            }
            SessionDisconnectionReasonKey key = new SessionDisconnectionReasonKey(userId, deviceType, sessionId);
            return disconnectionReasonCache.get(key);
        }
    }

}
