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
import im.turms.common.TurmsCloseStatus;
import im.turms.common.TurmsStatusCode;
import im.turms.common.constant.DeviceType;
import im.turms.common.constant.UserStatus;
import im.turms.common.exception.TurmsBusinessException;
import im.turms.common.model.bo.signal.Session;
import im.turms.common.model.dto.notification.TurmsNotification;
import im.turms.turms.access.websocket.dispatcher.InboundMessageDispatcher;
import im.turms.turms.constant.CloseStatusFactory;
import im.turms.turms.manager.TurmsClusterManager;
import im.turms.turms.service.user.onlineuser.OnlineUserService;
import im.turms.turms.util.SessionUtil;
import im.turms.turms.util.UserAgentUtil;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.validation.constraints.NotNull;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Component
@Log4j2
public class TurmsWebSocketHandler implements WebSocketHandler {
    private static final String HAS_LOGGED_IN = "hasLoggedIn";
    private static final List<TurmsStatusCode> LOGIN_CONFLICT_STATUS_CODES = List.of(
            TurmsStatusCode.SESSION_SIMULTANEOUS_CONFLICTS_DECLINE,
            TurmsStatusCode.SESSION_SIMULTANEOUS_CONFLICTS_OFFLINE,
            TurmsStatusCode.SESSION_SIMULTANEOUS_CONFLICTS_NOTIFY);
    private final TurmsClusterManager turmsClusterManager;
    private final InboundMessageDispatcher inboundMessageDispatcher;
    private final OnlineUserService onlineUserService;
    private final boolean locationEnabled;

    public TurmsWebSocketHandler(
            InboundMessageDispatcher inboundMessageDispatcher,
            OnlineUserService onlineUserService,
            TurmsClusterManager turmsClusterManager) {
        this.inboundMessageDispatcher = inboundMessageDispatcher;
        this.onlineUserService = onlineUserService;
        this.turmsClusterManager = turmsClusterManager;
        this.locationEnabled = turmsClusterManager.getTurmsProperties().getUser().getLocation().isEnabled();
    }

    @Override
    public Mono<Void> handle(WebSocketSession session) {
        Map<String, String> cookies = SessionUtil.getCookiesFromSession(session);
        HttpHeaders headers = session.getHandshakeInfo().getHeaders();
        Long userId = SessionUtil.getUserIdFromCookiesOrHeaders(cookies, headers);
        UserStatus userStatus = SessionUtil.getUserStatusFromCookiesAndHeaders(cookies, headers);
        PointFloat userLocation = locationEnabled ? SessionUtil.getLocationFromCookiesAndHeaders(cookies, headers) : null;
        String agent = session.getHandshakeInfo().getHeaders().getFirst(HttpHeaders.USER_AGENT);
        Map<String, String> deviceDetails = UserAgentUtil.parse(agent);
        DeviceType deviceType = UserAgentUtil.detectDeviceTypeIfUnset(
                SessionUtil.getDeviceTypeFromCookiesAndHeaders(cookies, headers),
                deviceDetails,
                turmsClusterManager.getTurmsProperties().getUser().isUseOsAsDefaultDeviceType());
        if (userId != null) {
            Integer ip = SessionUtil.parseIp(session);
            if (ip != null) {
                SessionUtil.putIp(session.getAttributes(), ip);
            }
            SessionUtil.putOnlineUserInfoToSession(session, userId, userStatus, deviceType, userLocation);
            Flux<WebSocketMessage> notificationOutput = Flux.create(notificationSink ->
                    // Note that executing addOnlineUser synchronously
                    // so that the user is in online or offline status after addOnlineUser
                    onlineUserService.addOnlineUser(
                            userId,
                            userStatus,
                            deviceType,
                            deviceDetails,
                            ip,
                            userLocation,
                            session,
                            notificationSink)
                            .doOnError(notificationSink::error) // This should never happen
                            .doOnSuccess(code -> {
                                if (code == TurmsStatusCode.OK) {
                                    session.getAttributes().put(HAS_LOGGED_IN, true);
                                    if (turmsClusterManager.getTurmsProperties().getSession()
                                            .isNotifyClientsOfSessionInfoAfterConnected()) {
                                        String address = turmsClusterManager.getLocalServerAddress();
                                        WebSocketMessage message = generateSessionNotification(session, address);
                                        notificationSink.next(message);
                                    }
                                } else {
                                    notificationSink.error(TurmsBusinessException.get(code));
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
            responseOutput = responseOutput.flatMap(inboundMessage -> inboundMessageDispatcher.dispatch(session, inboundMessage));
            return session.send(notificationOutput
                    .doOnComplete(() -> onlineUserService.setLocalUserDeviceOffline(userId, deviceType, CloseStatusFactory.get(TurmsCloseStatus.DISCONNECTED_BY_ADMIN)))
                    .mergeWith(responseOutput.doOnComplete(() -> onlineUserService.setLocalUserDeviceOffline(userId, deviceType, CloseStatusFactory.get(TurmsCloseStatus.DISCONNECTED_BY_CLIENT))))
                    .doOnError(throwable -> {
                        boolean hasLoggedIn = (boolean) session.getAttributes().getOrDefault(HAS_LOGGED_IN, false);
                        if (hasLoggedIn) {
                            TurmsCloseStatus closeStatus;
                            String reason;
                            if (throwable instanceof TurmsBusinessException) {
                                TurmsBusinessException exception = (TurmsBusinessException) throwable;
                                TurmsStatusCode code = exception.getCode();
                                if (LOGIN_CONFLICT_STATUS_CODES.contains(code)) {
                                    closeStatus = TurmsCloseStatus.LOGIN_CONFLICT;
                                    reason = null;
                                } else if (code == TurmsStatusCode.NOT_RESPONSIBLE) {
                                    closeStatus = TurmsCloseStatus.REDIRECT;
                                    reason = turmsClusterManager.getAddressIfCurrentNodeIrresponsibleByUserId(userId);
                                } else {
                                    closeStatus = TurmsCloseStatus.SERVER_ERROR;
                                    reason = throwable.getMessage();
                                }
                            } else {
                                closeStatus = TurmsCloseStatus.SERVER_ERROR;
                                reason = throwable.getMessage();
                            }
                            onlineUserService.setLocalUserDevicesOffline(
                                    userId,
                                    Collections.singleton(deviceType),
                                    CloseStatusFactory.get(closeStatus, reason));
                        }
                        log.error(throwable);
                    }));
        } else {
            return session.close(CloseStatusFactory.get(TurmsCloseStatus.SERVER_ERROR, "The user ID or IP is missing"));
        }
    }

    private WebSocketMessage generateSessionNotification(
            @NotNull WebSocketSession session,
            @NotNull String serverAddress) {
        return session.binaryMessage(factory -> {
            Session result = Session.newBuilder()
                    .setSessionId(session.getId())
                    .setAddress(serverAddress)
                    .build();
            TurmsNotification response = TurmsNotification.newBuilder()
                    .setData(TurmsNotification.Data.newBuilder().setSession(result))
                    .buildPartial();
            return factory.wrap(response.toByteArray());
        });
    }
}
