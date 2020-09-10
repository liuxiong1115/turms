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

package im.turms.client.driver.service;

import im.turms.client.driver.StateStore;
import im.turms.client.model.SessionDisconnectInfo;
import im.turms.client.model.SessionStatus;

import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;

/**
 * @author James Chen
 */
public class SessionService {

    private final StateStore stateStore;
    private volatile SessionStatus currentStatus = SessionStatus.CLOSED;

    private final List<Consumer<Void>> onSessionConnectedListeners = new LinkedList<>();
    private final List<Consumer<SessionDisconnectInfo>> onSessionDisconnectedListeners = new LinkedList<>();
    private final List<Consumer<SessionDisconnectInfo>> onSessionClosedListeners = new LinkedList<>();

    public SessionService(StateStore stateStore) {
        this.stateStore = stateStore;
    }

    public void setSessionId(String sessionId) {
        stateStore.setSessionId(sessionId);
    }

    // Status

    public SessionStatus getStatus() {
        return currentStatus;
    }

    public boolean isConnected() {
        return currentStatus == SessionStatus.CONNECTED;
    }

    public boolean isClosed() {
        return currentStatus == SessionStatus.CLOSED;
    }

    // Listeners

    public void addOnSessionConnectedListeners(Consumer<Void> listener) {
        onSessionConnectedListeners.add(listener);
    }

    public void addOnSessionDisconnectedListeners(Consumer<SessionDisconnectInfo> listener) {
        onSessionDisconnectedListeners.add(listener);
    }

    public void addOnSessionClosedListeners(Consumer<SessionDisconnectInfo> listener) {
        onSessionClosedListeners.add(listener);
    }

    public synchronized void notifyOnSessionConnectedListeners() {
        if (currentStatus == SessionStatus.DISCONNECTED || currentStatus == SessionStatus.CLOSED) {
            for (Consumer<Void> listener : onSessionConnectedListeners) {
                listener.accept(null);
            }
            currentStatus = SessionStatus.CONNECTED;
        }
    }

    public synchronized void notifyOnSessionDisconnectedListeners(SessionDisconnectInfo info) {
        if (currentStatus == SessionStatus.CONNECTED) {
            for (Consumer<SessionDisconnectInfo> listener : onSessionDisconnectedListeners) {
                listener.accept(info);
            }
            currentStatus = SessionStatus.DISCONNECTED;
        }
    }

    public synchronized void notifyOnSessionClosedListeners(SessionDisconnectInfo info) {
        switch (currentStatus) {
            case CONNECTED:
                this.notifyOnSessionDisconnectedListeners(info);
                break;
            case DISCONNECTED:
                currentStatus = SessionStatus.DISCONNECTED;
                break;
            case CLOSED:
                return;
            default:
                throw new IllegalStateException("Unexpected value: " + currentStatus);
        }
        for (Consumer<SessionDisconnectInfo> listener : onSessionClosedListeners) {
            listener.accept(info);
        }
        currentStatus = SessionStatus.CLOSED;
    }

}
