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

import java.util.List;

@Component
public class TurmsHandshakeWebSocketService extends HandshakeWebSocketService {
    private final TurmsClusterManager turmsClusterManager;
    private final UserService userService;
    private final UserSimultaneousLoginService userSimultaneousLoginService;
    private final TurmsPluginManager turmsPluginManager;

    @Autowired
    public TurmsHandshakeWebSocketService(UserService userService, TurmsClusterManager turmsClusterManager, UserSimultaneousLoginService userSimultaneousLoginService, TurmsPluginManager turmsPluginManager) {
        this.userService = userService;
        this.turmsClusterManager = turmsClusterManager;
        this.userSimultaneousLoginService = userSimultaneousLoginService;
        this.turmsPluginManager = turmsPluginManager;
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
        if (userId == null) {
            return Mono.error(new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        } else if (!turmsClusterManager.isCurrentNodeResponsibleByUserId(userId)) {
            return Mono.error(new ResponseStatusException(HttpStatus.TEMPORARY_REDIRECT));
        } else {
            Pair<String, DeviceType> loggingDeviceType = SessionUtil.parseDeviceTypeFromRequest(
                    request,
                    turmsClusterManager.getTurmsProperties().getUser().isUseOsAsDefaultDeviceType());
            if (!userSimultaneousLoginService.isDeviceTypeAllowedToLogin(userId, loggingDeviceType.getRight())) {
                return Mono.error(new ResponseStatusException(HttpStatus.CONFLICT));
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
                                                    return Mono.error(new ResponseStatusException(HttpStatus.UNAUTHORIZED));
                                                }
                                            } else {
                                                return Mono.error(new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR));
                                            }
                                        });
                            } else {
                                return Mono.error(new ResponseStatusException(HttpStatus.UNAUTHORIZED));
                            }
                        });
            }
        }
    }
}
