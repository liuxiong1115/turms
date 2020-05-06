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

package im.turms.turms.service.user.onlineuser.manager;

import im.turms.common.TurmsStatusCode;
import im.turms.common.constant.DeviceType;
import im.turms.common.constant.UserStatus;
import im.turms.common.exception.TurmsBusinessException;
import im.turms.turms.pojo.bo.UserOnlineInfo;
import im.turms.turms.pojo.domain.UserLocation;
import io.netty.util.Timeout;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.web.reactive.socket.CloseStatus;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.FluxSink;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class OnlineUserManager {
    private final UserOnlineInfo userOnlineInfo;

    public OnlineUserManager(
            @NotNull Long userId,
            @NotNull UserStatus userStatus,
            @NotNull DeviceType usingDeviceType,
            @Nullable UserLocation userLocation,
            @NotNull WebSocketSession webSocketSession,
            @NotNull FluxSink<WebSocketMessage> outputSink,
            @Nullable Timeout heartbeatTimeout,
            @Nullable Long logId) {
        Session session = new Session(
                usingDeviceType,
                new Date(),
                userLocation,
                webSocketSession,
                outputSink,
                heartbeatTimeout,
                logId,
                System.currentTimeMillis());
        ConcurrentHashMap<DeviceType, Session> sessionMap = new ConcurrentHashMap<>(DeviceType.values().length);
        sessionMap.putIfAbsent(usingDeviceType, session);
        this.userOnlineInfo = new UserOnlineInfo(userId, userStatus, sessionMap);
    }

    public void addOnlineDeviceType(
            @NotNull DeviceType deviceType,
            @Nullable UserLocation userLocation,
            @NotNull WebSocketSession webSocketSession,
            @NotNull FluxSink<WebSocketMessage> notificationSink,
            @Nullable Timeout heartbeatTimeout,
            @Nullable Long logId) {
        if (!userOnlineInfo.getSessionMap().containsKey(deviceType)) {
            Session session = new Session(
                    deviceType,
                    new Date(),
                    userLocation,
                    webSocketSession,
                    notificationSink,
                    heartbeatTimeout,
                    logId,
                    System.currentTimeMillis());
            if (userOnlineInfo.getSessionMap().putIfAbsent(deviceType, session) != null) {
                throw TurmsBusinessException.get(TurmsStatusCode.SESSION_SIMULTANEOUS_CONFLICTS_OFFLINE);
            }
        } else {
            throw TurmsBusinessException.get(TurmsStatusCode.SESSION_SIMULTANEOUS_CONFLICTS_OFFLINE);
        }
    }

    public void setDeviceOffline(
            @NotNull DeviceType deviceType,
            @NotNull CloseStatus closeStatus) {
        Session session = userOnlineInfo.getSessionMap().get(deviceType);
        if (session != null) {
            session.getNotificationSink().complete();
            Timeout timeout = session.getHeartbeatTimeout();
            if (timeout != null) {
                timeout.cancel();
            }
            session.getWebSocketSession().close(closeStatus).subscribe();
            userOnlineInfo.getSessionMap().remove(deviceType);
        }
    }

    public boolean setUserOnlineStatus(@NotNull UserStatus userStatus) {
        if (userStatus != UserStatus.OFFLINE && userStatus != UserStatus.UNRECOGNIZED) {
            userOnlineInfo.setUserStatus(userStatus);
            return true;
        } else {
            return false;
        }
    }

    public UserOnlineInfo getOnlineUserInfo() {
        return userOnlineInfo;
    }

    public Session getSession(@NotNull DeviceType deviceType) {
        return userOnlineInfo.getSessionMap().get(deviceType);
    }

    public int getSessionsNumber() {
        return userOnlineInfo.getSessionMap().size();
    }

    public Set<DeviceType> getUsingDeviceTypes() {
        return userOnlineInfo.getUsingDeviceTypes();
    }

    public List<WebSocketSession> getWebSocketSessions() {
        Collection<Session> values = userOnlineInfo.getSessionMap().values();
        List<WebSocketSession> sessions = new ArrayList<>(values.size());
        for (Session session : values) {
            sessions.add(session.webSocketSession);
        }
        return sessions;
    }

    public List<FluxSink<WebSocketMessage>> getOutputSinks() {
        Collection<Session> sessions = userOnlineInfo.getSessionMap().values();
        List<FluxSink<WebSocketMessage>> sinks = new ArrayList<>(sessions.size());
        for (Session session : sessions) {
            sinks.add(session.getNotificationSink());
        }
        return sinks;
    }

    @Data
    @AllArgsConstructor
    public static class Session {
        private final DeviceType deviceType;
        private final Date loginDate;
        private UserLocation location;

        private final transient WebSocketSession webSocketSession;
        private final transient FluxSink<WebSocketMessage> notificationSink;
        private transient Timeout heartbeatTimeout;
        private transient Long logId;
        private transient volatile long lastHeartbeatTimestamp;
    }
}