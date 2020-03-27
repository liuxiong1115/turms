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

package im.turms.turms.access.websocket.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import im.turms.common.TurmsStatusCode;
import im.turms.common.constant.DeviceType;
import im.turms.common.exception.TurmsBusinessException;
import im.turms.turms.cluster.TurmsClusterManager;
import im.turms.turms.common.SessionUtil;
import im.turms.turms.plugin.TurmsPluginManager;
import im.turms.turms.plugin.UserAuthenticator;
import im.turms.turms.pojo.bo.UserLoginInfo;
import im.turms.turms.property.TurmsProperties;
import im.turms.turms.service.user.UserService;
import im.turms.turms.service.user.UserSimultaneousLoginService;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.server.support.HandshakeWebSocketService;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;
import java.time.Duration;
import java.util.List;
import java.util.Set;

@Component
@Validated
public class TurmsHandshakeWebSocketService extends HandshakeWebSocketService {
    private static final String RESPONSE_HEADER_REASON = "reason";

    private final TurmsClusterManager turmsClusterManager;
    private final UserService userService;
    private final UserSimultaneousLoginService userSimultaneousLoginService;
    private final TurmsPluginManager turmsPluginManager;
    private final Set<DeviceType> degradedDeviceTypes;
    private final boolean enableQueryLoginFailedReason;
    private final boolean pluginEnabled;
    /**
     * Triple<user ID, device type, login request ID> -> reason
     * 1. Integer: http status code
     * 2. String: redirect address
     * <p>
     * Note:
     * 1. The reason to cache the request ID (as a token) is to
     * prevent others from querying others' login failed reason.
     * 2. To keep it simple, use Object to avoid defining/using a new model
     */
    private final Cache<Triple<Long, DeviceType, Long>, Object> loginFailedReasonCache;

    @Autowired
    public TurmsHandshakeWebSocketService(UserService userService, TurmsClusterManager turmsClusterManager, UserSimultaneousLoginService userSimultaneousLoginService, TurmsPluginManager turmsPluginManager, TurmsProperties turmsProperties) {
        this.userService = userService;
        this.turmsClusterManager = turmsClusterManager;
        this.userSimultaneousLoginService = userSimultaneousLoginService;
        this.turmsPluginManager = turmsPluginManager;
        this.loginFailedReasonCache = Caffeine
                .newBuilder()
                .maximumSize(turmsProperties.getCache().getLoginFailedReasonCacheMaxSize())
                .expireAfterWrite(Duration.ofSeconds(turmsProperties.getCache().getLoginFailedReasonExpireAfter()))
                .build();
        degradedDeviceTypes = turmsProperties.getSession().getDegradedDeviceTypesForLoginFailedReason();
        enableQueryLoginFailedReason = turmsProperties.getSession().isEnableQueryLoginFailedReason();
        pluginEnabled = turmsClusterManager.getTurmsProperties().getPlugin().isEnabled();
    }

    /**
     * Authenticate during the handshake to avoid wasting resources.
     */
    @Override
    public Mono<Void> handleRequest(ServerWebExchange exchange, WebSocketHandler handler) {
        ServerHttpRequest request = exchange.getRequest();
        Long userId = SessionUtil.getUserIdFromRequest(request);
        Long requestId = SessionUtil.getRequestIdFromRequest(request);
        Pair<String, DeviceType> loggingInDeviceType = SessionUtil.parseDeviceTypeFromRequest(
                request,
                turmsClusterManager.getTurmsProperties().getUser().isUseOsAsDefaultDeviceType());
        DeviceType deviceType = loggingInDeviceType.getRight();
        if (!turmsClusterManager.isServing()) {
            return tryCacheReasonAndReturnError(exchange, HttpStatus.GONE, deviceType, userId, requestId);
        }
        if (userId == null) {
            return tryCacheReasonAndReturnError(exchange, HttpStatus.UNAUTHORIZED, deviceType, userId, requestId);
        } else if (!turmsClusterManager.isCurrentNodeResponsibleByUserId(userId)) {
            return tryCacheReasonAndReturnError(exchange, HttpStatus.TEMPORARY_REDIRECT, deviceType, userId, requestId);
        } else {
            return userService.isActiveAndNotDeleted(userId)
                    .flatMap(isActiveAndNotDeleted -> {
                        if (isActiveAndNotDeleted == null || !isActiveAndNotDeleted) {
                            return tryCacheReasonAndReturnError(exchange, HttpStatus.UNAUTHORIZED, deviceType, userId, requestId);
                        }
                        if (!userSimultaneousLoginService.isDeviceTypeAllowedToLogin(userId, deviceType)) {
                            return tryCacheReasonAndReturnError(exchange, HttpStatus.CONFLICT, deviceType, userId, requestId);
                        } else {
                            String password = SessionUtil.getPasswordFromRequest(request);
                            Mono<Boolean> authenticate = Mono.empty();
                            if (pluginEnabled) {
                                List<UserAuthenticator> authenticatorList = turmsPluginManager.getUserAuthenticatorList();
                                if (!authenticatorList.isEmpty()) {
                                    UserLoginInfo userLoginInfo = new UserLoginInfo(
                                            userId,
                                            password,
                                            deviceType,
                                            loggingInDeviceType.getLeft());
                                    for (UserAuthenticator authenticator : authenticatorList) {
                                        Mono<Boolean> authenticateMono = authenticator.authenticate(userLoginInfo);
                                        authenticate = authenticate.switchIfEmpty(authenticateMono);
                                    }
                                }
                            }
                            return authenticate.switchIfEmpty(userService.authenticate(userId, password))
                                    .flatMap(authenticated -> {
                                        if (authenticated != null && authenticated) {
                                            return userSimultaneousLoginService.setConflictedDevicesOffline(userId, deviceType)
                                                    .flatMap(success -> {
                                                        if (success) {
                                                            if (password != null && !password.isBlank()) {
                                                                return super.handleRequest(exchange, handler);
                                                            } else {
                                                                return tryCacheReasonAndReturnError(exchange, HttpStatus.UNAUTHORIZED, deviceType, userId, requestId);
                                                            }
                                                        } else {
                                                            return tryCacheReasonAndReturnError(exchange, HttpStatus.INTERNAL_SERVER_ERROR, deviceType, userId, requestId);
                                                        }
                                                    });
                                        } else {
                                            return tryCacheReasonAndReturnError(exchange, HttpStatus.UNAUTHORIZED, deviceType, userId, requestId);
                                        }
                                    });
                        }
                    });
        }
    }

    private Mono<Void> tryCacheReasonAndReturnError(
            @NotNull ServerWebExchange exchange,
            @NotNull HttpStatus httpStatus,
            @Nullable DeviceType deviceType,
            @Nullable Long userId,
            @Nullable Long requestId) {
        boolean shouldCache = enableQueryLoginFailedReason &&
                deviceType != null &&
                degradedDeviceTypes.contains(deviceType) &&
                userId != null &&
                requestId != null;
        Object value = null;
        if (httpStatus == HttpStatus.TEMPORARY_REDIRECT) {
            if (userId != null) {
                String address = turmsClusterManager.getResponsibleTurmsServerAddress(userId);
                            exchange.getResponse().getHeaders().put(RESPONSE_HEADER_REASON, List.of(address));
                value = address;
                            }
            } else {
            value = httpStatus.value();
            }
            if (shouldCache) {
            loginFailedReasonCache.put(Triple.of(userId, deviceType, requestId), value);
            }
            return Mono.error(new ResponseStatusException(httpStatus));
        }

    public Object getLoginFailedReason(@NotNull Long userId, @NotNull DeviceType deviceType, @NotNull Long requestId) {
        if (!enableQueryLoginFailedReason) {
            throw TurmsBusinessException.get(TurmsStatusCode.DISABLED_FUNCTION);
        } else if (!degradedDeviceTypes.contains(deviceType)) {
            String reason = String.format("The device type %s is not allowed to query login-failed reason", deviceType);
            throw TurmsBusinessException.get(TurmsStatusCode.ILLEGAL_ARGUMENTS, reason);
        } else {
            return loginFailedReasonCache.getIfPresent(Triple.of(userId, deviceType, requestId));
        }
    }
}
