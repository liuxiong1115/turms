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

import com.github.davidmoten.rtree2.geometry.internal.PointFloat;
import com.google.common.net.InetAddresses;
import im.turms.turms.access.websocket.dispatcher.InboundMessageDispatcher;
import im.turms.turms.cluster.TurmsClusterManager;
import im.turms.turms.common.SessionUtil;
import im.turms.turms.common.TurmsStatusCode;
import im.turms.turms.common.UserAgentUtil;
import im.turms.turms.constant.DeviceType;
import im.turms.turms.constant.UserStatus;
import im.turms.turms.pojo.dto.Session;
import im.turms.turms.pojo.response.TurmsResponse;
import im.turms.turms.service.user.onlineuser.OnlineUserService;
import org.springframework.stereotype.Component;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.reactive.socket.CloseStatus;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.annotation.PostConstruct;
import javax.validation.constraints.NotNull;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.Collections;
import java.util.Map;

@Component
public class TurmsWebSocketHandler implements WebSocketHandler, CorsConfigurationSource {
    private final TurmsClusterManager turmsClusterManager;
    private final InboundMessageDispatcher inboundMessageDispatcher;
    private final OnlineUserService onlineUserService;

    public TurmsWebSocketHandler(InboundMessageDispatcher inboundMessageDispatcher, OnlineUserService onlineUserService, TurmsClusterManager turmsClusterManager) {
        this.inboundMessageDispatcher = inboundMessageDispatcher;
        this.onlineUserService = onlineUserService;
        this.turmsClusterManager = turmsClusterManager;
    }

    @PostConstruct
    public void warmUp() {
        UserAgentUtil.parse("User-Agent,Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/77.0.3865.90 Safari/537.36");
    }

    @Override
    public Mono<Void> handle(@NotNull WebSocketSession session) {
        Map<String, String> cookies = SessionUtil.getCookiesFromSession(session);
        if (cookies == null || cookies.isEmpty()) {
            return session.close();
        }
        Long userId = SessionUtil.getUserIdFromCookies(cookies);
        DeviceType deviceType = SessionUtil.getDeviceTypeFromCookies(cookies);
        UserStatus userStatus = SessionUtil.getUserStatusFromCookies(cookies);
        PointFloat userLocation = SessionUtil.getLocationFromCookies(cookies);
        String agent = session.getHandshakeInfo().getHeaders().getFirst("User-Agent");
        Map<String, String> deviceDetails = UserAgentUtil.parse(agent);
        deviceType = UserAgentUtil.detectDeviceTypeIfUnset(
                deviceType,
                deviceDetails,
                turmsClusterManager.getTurmsProperties().getUser().isUseOsAsDefaultDeviceType());
        InetSocketAddress ip = session.getHandshakeInfo().getRemoteAddress();
        if (userId != null && ip != null) {
            Integer ipInNumber;
            try {
                ipInNumber = InetAddresses.coerceToInteger(InetAddresses.forString(ip.getHostString()));
            } catch (Exception e) {
                ipInNumber = null;
            }
            SessionUtil.putOnlineUserInfoToSession(session, userId, userStatus, deviceType, userLocation);
            Integer finalIpInNumber = ipInNumber;
            DeviceType finalDeviceType = deviceType;
            Flux<WebSocketMessage> notificationOutput = Flux.create(notificationSink ->
                    onlineUserService.addOnlineUser(
                            userId,
                            userStatus,
                            finalDeviceType,
                            deviceDetails,
                            finalIpInNumber,
                            userLocation,
                            session,
                            notificationSink)
                            .doOnError(throwable -> onlineUserService.setLocalUserDevicesOffline(
                                    userId,
                                    Collections.singleton(finalDeviceType),
                                    CloseStatus.SERVER_ERROR))
                            .doOnSuccess(code -> {
                                if (code != TurmsStatusCode.OK) {
                                    onlineUserService.setLocalUserDevicesOffline(
                                            userId,
                                            Collections.singleton(finalDeviceType),
                                            CloseStatus.SERVER_ERROR);
                                } else if (turmsClusterManager.getTurmsProperties().getSession()
                                        .isNotifyClientsOfSessionInfoAfterConnected()) {
                                    String address = turmsClusterManager.getLocalTurmsServerAddress();
                                    WebSocketMessage message = generateSessionNotification(session, address);
                                    notificationSink.next(message);
                                }
                            })
                            .subscribe());
            Flux<WebSocketMessage> responseOutput = session.receive();
            int requestInterval = turmsClusterManager.getTurmsProperties().getSecurity().getMinClientRequestsIntervalMillis();
            if (requestInterval != 0) {
                responseOutput = responseOutput
                        .doOnNext(WebSocketMessage::retain)
                        .sample(Duration.ofMillis(requestInterval));
            }
            responseOutput = responseOutput
                    .doFinally(signalType -> onlineUserService.setLocalUserDeviceOffline(userId, finalDeviceType, CloseStatus.NORMAL))
                    .flatMap(inboundMessage -> inboundMessageDispatcher.dispatch(session, inboundMessage));
            return session.send(notificationOutput.mergeWith(responseOutput));
        } else {
            return session.close(CloseStatus.SERVER_ERROR);
        }
    }

    //TODO: check whether this is necessary while there has been a WebConfig
    @Override
    public CorsConfiguration getCorsConfiguration(ServerWebExchange exchange) {
        CorsConfiguration corsConfiguration = new CorsConfiguration();
        corsConfiguration.addAllowedOrigin("*");
        corsConfiguration.addAllowedMethod("*");
        corsConfiguration.addAllowedHeader("*");
        corsConfiguration.setAllowCredentials(true);
        return corsConfiguration;
    }

    private WebSocketMessage generateSessionNotification(
            @NotNull WebSocketSession session,
            @NotNull String serverAddress) {
        return session.binaryMessage(factory -> {
            Session result = Session.newBuilder()
                    .setSessionId(session.getId())
                    .setAddress(serverAddress)
                    .build();
            TurmsResponse response = TurmsResponse.newBuilder()
                    .setData(TurmsResponse.Data.newBuilder().setSession(result))
                    .buildPartial();
            return factory.wrap(response.toByteArray());
        });
    }
}
