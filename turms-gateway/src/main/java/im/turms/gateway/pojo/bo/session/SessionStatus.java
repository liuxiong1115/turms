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

package im.turms.gateway.pojo.bo.session;

/**
 * @author James Chen
 */
public enum SessionStatus {
    /**
     * WebSocket connection is established
     */
    CONNECTED,
    /**
     * WebSocket connection is closed, the session is open because the clients still send heartbeat requests over UDP
     */
    DISCONNECTED,
    /**
     * WebSocket connection is closed, the server has notified clients over UDP to reconnect or the server will close the session
     */
    RECOVERING,
    /**
     * WebSocket connection is closed
     */
    CLOSED
}