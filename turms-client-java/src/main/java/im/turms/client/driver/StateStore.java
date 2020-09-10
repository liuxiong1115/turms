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

package im.turms.client.driver;

import okhttp3.WebSocket;

/**
 * @author James Chen
 */
public class StateStore {

    private WebSocket webSocket;
    private volatile boolean isConnected;
    private Integer connectionRequestId;
    private String sessionId;

    private long lastRequestDate;

    public WebSocket getWebSocket() {
        return webSocket;
    }

    public void setWebSocket(WebSocket webSocket) {
        this.webSocket = webSocket;
    }

    public boolean isConnected() {
        return isConnected;
    }

    public void setConnected(boolean connected) {
        isConnected = connected;
    }

    public Integer getConnectionRequestId() {
        return connectionRequestId;
    }

    public void setConnectionRequestId(Integer connectionRequestId) {
        this.connectionRequestId = connectionRequestId;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public long getLastRequestDate() {
        return lastRequestDate;
    }

    public void setLastRequestDate(long lastRequestDate) {
        this.lastRequestDate = lastRequestDate;
    }

}
