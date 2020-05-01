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

import com.hazelcast.cluster.Member;
import im.turms.common.constant.DeviceType;
import im.turms.turms.manager.ReasonCacheManager;
import im.turms.turms.manager.TurmsClusterManager;
import im.turms.turms.manager.TurmsPluginManager;
import im.turms.turms.plugin.UserAuthenticator;
import im.turms.turms.pojo.bo.UserLoginInfo;
import im.turms.turms.service.user.UserService;
import im.turms.turms.service.user.UserSimultaneousLoginService;
import im.turms.turms.service.user.onlineuser.IrresponsibleUserService;
import im.turms.turms.util.SessionUtil;
import org.apache.commons.lang3.tuple.Pair;
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
import java.util.List;

@Component
@Validated
public class TurmsHandshakeWebSocketService extends HandshakeWebSocketService {
    private static final String RESPONSE_HEADER_REASON = "reason";

    private final TurmsClusterManager turmsClusterManager;
    private final UserService userService;
    private final UserSimultaneousLoginService userSimultaneousLoginService;
    private final IrresponsibleUserService irresponsibleUserService;
    private final ReasonCacheManager reasonCacheManager;
    private final TurmsPluginManager turmsPluginManager;
    private final boolean pluginEnabled;

    @Autowired
    public TurmsHandshakeWebSocketService(
            UserService userService,
            TurmsClusterManager turmsClusterManager,
            UserSimultaneousLoginService userSimultaneousLoginService,
            TurmsPluginManager turmsPluginManager,
            IrresponsibleUserService irresponsibleUserService,
            ReasonCacheManager reasonCacheManager) {
        this.userService = userService;
        this.turmsClusterManager = turmsClusterManager;
        this.userSimultaneousLoginService = userSimultaneousLoginService;
        this.turmsPluginManager = turmsPluginManager;
        pluginEnabled = turmsClusterManager.getTurmsProperties().getPlugin().isEnabled();
        this.irresponsibleUserService = irresponsibleUserService;
        this.reasonCacheManager = reasonCacheManager;
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
        if (!turmsClusterManager.isActive()) {
            return tryCacheReasonAndReturnError(exchange, HttpStatus.GONE, deviceType, userId, requestId);
        }
        if (userId == null) {
            return tryCacheReasonAndReturnError(exchange, HttpStatus.UNAUTHORIZED, deviceType, userId, requestId);
        }
        Member redirectMember = turmsClusterManager.getMemberIfCurrentNodeIrresponsibleByUserId(userId);
        if (redirectMember != null && !irresponsibleUserService.isAllowIrresponsibleUsersWhenConnecting()) {
            return tryCacheRedirectReasonAndReturnError(exchange, turmsClusterManager.getAddress(redirectMember), deviceType, userId, requestId);
        } else {
            return userService.isActiveAndNotDeleted(userId)
                    .flatMap(isActiveAndNotDeleted -> {
                        if (isActiveAndNotDeleted == null || !isActiveAndNotDeleted) {
                            return tryCacheReasonAndReturnError(exchange, HttpStatus.UNAUTHORIZED, deviceType, userId, requestId);
                        }
                        if (!userSimultaneousLoginService.isDeviceTypeAllowedToLogin(userId, deviceType)) {
                            return tryCacheReasonAndReturnError(exchange, HttpStatus.CONFLICT, deviceType, userId, requestId);
                        } else {
                            boolean enableAuthentication = turmsClusterManager.getTurmsProperties().getSession().isEnableAuthentication();
                            String password = enableAuthentication ? SessionUtil.getPasswordFromRequest(request) : null;
                            Mono<Boolean> authenticate = enableAuthentication ? Mono.empty() : Mono.just(true);
                            if (enableAuthentication && pluginEnabled) {
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
                                                    .flatMap(wasOnline -> {
                                                        if (redirectMember != null) {
                                                            irresponsibleUserService.put(userId, turmsClusterManager.getLocalMember().getUuid());
                                                        }
                                                        return super.handleRequest(exchange, handler);
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
        if (reasonCacheManager.shouldCacheLoginFailedReason(userId, deviceType, requestId)) {
            reasonCacheManager.cacheLoginFailedReason(userId, deviceType, requestId, value);
        }
        return Mono.error(new ResponseStatusException(httpStatus));
    }

    private Mono<Void> tryCacheRedirectReasonAndReturnError(
            @NotNull ServerWebExchange exchange,
            @NotNull String address,
            @Nullable DeviceType deviceType,
            @Nullable Long userId,
            @Nullable Long requestId) {
        if (reasonCacheManager.shouldCacheLoginFailedReason(userId, deviceType, requestId)) {
            reasonCacheManager.cacheLoginFailedReason(userId, deviceType, requestId, address);
        }
        exchange.getResponse().getHeaders().put(RESPONSE_HEADER_REASON, List.of(address));
        return Mono.error(new ResponseStatusException(HttpStatus.TEMPORARY_REDIRECT));
    }
}
