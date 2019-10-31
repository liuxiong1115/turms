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

package im.turms.turms.service.user.onlineuser;

import com.fasterxml.jackson.annotation.JsonIgnore;
import im.turms.turms.common.Constants;
import im.turms.turms.constant.DeviceType;
import im.turms.turms.constant.UserStatus;
import im.turms.turms.pojo.bo.UserOnlineInfo;
import im.turms.turms.pojo.domain.UserLocation;
import io.netty.util.Timeout;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.data.annotation.Transient;
import org.springframework.lang.NonNull;
import org.springframework.web.reactive.socket.CloseStatus;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.FluxSink;

import javax.annotation.Nullable;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

public class OnlineUserManager {
    private UserOnlineInfo userOnlineInfo;

    public OnlineUserManager(
            @NonNull Long userId,
            @NonNull UserStatus userStatus,
            @NonNull DeviceType usingDeviceType,
            @Nullable UserLocation userLocation,
            @NonNull WebSocketSession webSocketSession,
            @NotNull FluxSink<WebSocketMessage> outputSink,
            @NotNull Timeout timeout,
            @NotNull Long logId) {
        Session session = new Session(
                usingDeviceType,
                new Date(),
                userLocation,
                webSocketSession,
                outputSink,
                timeout,
                logId,
                System.currentTimeMillis());
        ConcurrentMap<DeviceType, Session> sessionMap = new ConcurrentHashMap<>(DeviceType.values().length);
        sessionMap.putIfAbsent(usingDeviceType, session);
        this.userOnlineInfo = new UserOnlineInfo(userId, userStatus, sessionMap);
    }

    public void setDeviceTypeOnline(
            @NotNull DeviceType deviceType,
            @Nullable UserLocation userLocation,
            @NotNull WebSocketSession webSocketSession,
            @NotNull FluxSink<WebSocketMessage> notificationSink,
            @NotNull Timeout heartbeatTimeout,
            @NotNull Long logId) {
        setOfflineByDeviceType(deviceType, CloseStatus.POLICY_VIOLATION);
        Session session = new Session(
                deviceType,
                new Date(),
                userLocation,
                webSocketSession,
                notificationSink,
                heartbeatTimeout,
                logId,
                System.currentTimeMillis());
        userOnlineInfo.getSessionMap().put(deviceType, session);
    }

    public void setAllDevicesOffline(@NotNull CloseStatus closeStatus) {
        setSpecificDevicesOffline(Constants.ALL_DEVICE_TYPES, closeStatus);
    }

    public void setOfflineByDeviceType(
            @NotNull DeviceType deviceType,
            @NotNull CloseStatus closeStatus) {
        setSpecificDevicesOffline(Collections.singleton(deviceType), closeStatus);
    }

    public void setSpecificDevicesOffline(
            @NotEmpty Set<DeviceType> deviceTypes,
            @NotNull CloseStatus closeStatus) {
        for (DeviceType deviceType : deviceTypes) {
            Session session = userOnlineInfo.getSessionMap().get(deviceType);
            if (session != null) {
                session.getNotificationSink().complete();
                session.getHeartbeatTimeout().cancel();
                session.getWebSocketSession().close(closeStatus).subscribe();
                userOnlineInfo.getSessionMap().remove(deviceType);
            }
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

    public Set<DeviceType> getUsingDeviceTypes() {
        return userOnlineInfo.getUsingDeviceTypes();
    }

    public List<WebSocketSession> getWebSocketSessions() {
        return userOnlineInfo.getSessionMap()
                .values()
                .stream()
                .map(session -> session.webSocketSession)
                .collect(Collectors.toList());
    }

    public List<FluxSink<WebSocketMessage>> getOutputSinks() {
        return userOnlineInfo.getSessionMap()
                .values()
                .stream()
                .map(OnlineUserManager.Session::getNotificationSink)
                .collect(Collectors.toList());
    }

    @Data
    @AllArgsConstructor
    public static class Session {
        private DeviceType deviceType;
        private Date loginDate;
        private UserLocation location;
        @JsonIgnore
        @Transient
        private WebSocketSession webSocketSession;
        @JsonIgnore
        @Transient
        private FluxSink<WebSocketMessage> notificationSink;
        @JsonIgnore
        @Transient
        private Timeout heartbeatTimeout;
        @JsonIgnore
        @Transient
        private Long logId;
        @JsonIgnore
        @Transient
        private Long lastHeartbeatTimestamp;
    }
}