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

package im.turms.client.model;

import im.turms.common.constant.statuscode.SessionCloseStatus;

import java.util.Objects;

/**
 * @author James Chen
 */
public class SessionDisconnectInfo {

    private final boolean wasConnected;
    private final boolean isClosedByClient;
    private final boolean isReconnecting;
    private final SessionCloseStatus closeStatus;
    private final Integer webSocketStatus;
    private final String webSocketReason;
    private final Throwable error;

    public SessionDisconnectInfo(boolean wasConnected,
                                 boolean isClosedByClient,
                                 boolean isReconnecting,
                                 SessionCloseStatus closeStatus,
                                 Integer webSocketStatus,
                                 String webSocketReason,
                                 Throwable error) {
        this.wasConnected = wasConnected;
        this.isClosedByClient = isClosedByClient;
        this.isReconnecting = isReconnecting;
        this.closeStatus = closeStatus;
        this.webSocketStatus = webSocketStatus;
        this.webSocketReason = webSocketReason;
        this.error = error;
    }

    public boolean isWasConnected() {
        return wasConnected;
    }

    public boolean isClosedByClient() {
        return isClosedByClient;
    }

    public boolean isReconnecting() {
        return isReconnecting;
    }

    public SessionCloseStatus getCloseStatus() {
        return closeStatus;
    }

    public Integer getWebSocketStatus() {
        return webSocketStatus;
    }

    public String getWebSocketReason() {
        return webSocketReason;
    }

    public Throwable getError() {
        return error;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        SessionDisconnectInfo that = (SessionDisconnectInfo) o;
        return wasConnected == that.wasConnected &&
                isClosedByClient == that.isClosedByClient &&
                isReconnecting == that.isReconnecting &&
                closeStatus == that.closeStatus &&
                Objects.equals(webSocketStatus, that.webSocketStatus) &&
                Objects.equals(webSocketReason, that.webSocketReason) &&
                Objects.equals(error, that.error);
    }

    @Override
    public int hashCode() {
        return Objects.hash(wasConnected, isClosedByClient, isReconnecting, closeStatus, webSocketStatus, webSocketReason, error);
    }

}