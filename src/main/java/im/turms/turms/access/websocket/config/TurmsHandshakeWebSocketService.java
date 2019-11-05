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
import im.turms.turms.cluster.TurmsClusterManager;
import im.turms.turms.common.SessionUtil;
import im.turms.turms.constant.DeviceType;
import im.turms.turms.plugin.TurmsPluginManager;
import im.turms.turms.plugin.UserAuthenticator;
import im.turms.turms.pojo.bo.UserLoginInfo;
import im.turms.turms.service.user.UserService;
import im.turms.turms.service.user.UserSimultaneousLoginService;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.server.support.HandshakeWebSocketService;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;
import java.time.Duration;
import java.util.List;

@Component
public class TurmsHandshakeWebSocketService extends HandshakeWebSocketService {
    private final TurmsClusterManager turmsClusterManager;
    private final UserService userService;
    private final UserSimultaneousLoginService userSimultaneousLoginService;
    private final TurmsPluginManager turmsPluginManager;
    /**
     * Pair<user id, login request id> ->
     * 1. Integer: http status code
     * 2. String: redirect address
     * <p>
     * Note:
     * 1. The reason to cache both user ID and request ID is to
     * prevent others from being able to query others' login failed reason.
     * 2. To keep it simple, don't define/use a new model
     */
    private Cache<Pair<Long, Long>, Object> loginFailedReasonCache;

    @Autowired
    public TurmsHandshakeWebSocketService(UserService userService, TurmsClusterManager turmsClusterManager, UserSimultaneousLoginService userSimultaneousLoginService, TurmsPluginManager turmsPluginManager) {
        this.userService = userService;
        this.turmsClusterManager = turmsClusterManager;
        this.userSimultaneousLoginService = userSimultaneousLoginService;
        this.turmsPluginManager = turmsPluginManager;
        this.loginFailedReasonCache = Caffeine
                .newBuilder()
                .expireAfterWrite(Duration.ofMinutes(10L))
                .build();
    }

    /**
     * Authenticate during the handshake to avoid wasting resources.
     */
    @Override
    public Mono<Void> handleRequest(ServerWebExchange exchange, WebSocketHandler handler) {
        if (!turmsClusterManager.isWorkable()) {
            return Mono.error(new ResponseStatusException(HttpStatus.GONE));
        }
        ServerHttpRequest request = exchange.getRequest();
        Long userId = SessionUtil.getUserIdFromRequest(request);
        Long requestId = SessionUtil.getRequestIdFromRequest(request);
        if (userId == null) {
            return cacheAndReturnError(HttpStatus.UNAUTHORIZED, userId, requestId);
        } else if (!turmsClusterManager.isCurrentNodeResponsibleByUserId(userId)) {
            return cacheAndReturnError(HttpStatus.TEMPORARY_REDIRECT, userId, requestId);
        } else {
            Pair<String, DeviceType> loggingDeviceType = SessionUtil.parseDeviceTypeFromRequest(
                    request,
                    turmsClusterManager.getTurmsProperties().getUser().isUseOsAsDefaultDeviceType());
            if (!userSimultaneousLoginService.isDeviceTypeAllowedToLogin(userId, loggingDeviceType.getRight())) {
                return cacheAndReturnError(HttpStatus.CONFLICT, userId, requestId);
            } else {
                String password = SessionUtil.getPasswordFromRequest(request);
                Mono<Boolean> finalMono = Mono.empty();
                if (turmsClusterManager.getTurmsProperties().getPlugin().isEnabled()) {
                    List<UserAuthenticator> authenticatorList = turmsPluginManager.getUserAuthenticatorList();
                    if (!authenticatorList.isEmpty()) {
                        UserLoginInfo userLoginInfo = new UserLoginInfo(
                                userId,
                                password,
                                loggingDeviceType.getRight(),
                                loggingDeviceType.getLeft());
                        for (UserAuthenticator authenticator : authenticatorList) {
                            Mono<Boolean> authenticateMono = authenticator.authenticate(userLoginInfo);
                            finalMono = finalMono.switchIfEmpty(authenticateMono);
                        }
                    }
                }
                return finalMono.switchIfEmpty(userService.authenticate(userId, password))
                        .flatMap(authenticated -> {
                            if (authenticated != null && authenticated) {
                                return userSimultaneousLoginService.setConflictedDevicesOffline(userId, loggingDeviceType.getRight())
                                        .flatMap(success -> {
                                            if (success) {
                                                if (password != null && !password.isBlank()) {
                                                    return super.handleRequest(exchange, handler);
                                                } else {
                                                    return cacheAndReturnError(HttpStatus.UNAUTHORIZED, userId, requestId);
                                                }
                                            } else {
                                                return cacheAndReturnError(HttpStatus.INTERNAL_SERVER_ERROR, userId, requestId);
                                            }
                                        });
                            } else {
                                return cacheAndReturnError(HttpStatus.UNAUTHORIZED, userId, requestId);
                            }
                        });
            }
        }
    }

    private Mono<Void> cacheAndReturnError(
            @NotNull HttpStatus httpStatus,
            @Nullable Long userId,
            @Nullable Long requestId) {
        if (userId != null && requestId != null) {
            if (httpStatus != HttpStatus.TEMPORARY_REDIRECT) {
                loginFailedReasonCache.put(Pair.of(userId, requestId), httpStatus.value());
            } else {
                return turmsClusterManager.getResponsibleTurmsServerAddress(userId)
                        .doOnSuccess(address -> loginFailedReasonCache.put(Pair.of(userId, requestId), address))
                        .then(Mono.error(new ResponseStatusException(httpStatus)));
            }
        }
        return Mono.error(new ResponseStatusException(httpStatus));
    }

    public Object getFailedReason(@NotNull Long userId, @NotNull Long requestId) {
        if (turmsClusterManager.getTurmsProperties().getSession().isEnableQueryingLoginFailedReason()) {
            return loginFailedReasonCache.getIfPresent(Pair.of(userId, requestId));
        } else {
            return null;
        }
    }
}
