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

package unit.im.turms.gateway.pojo.bo.session;

import im.turms.common.constant.DeviceType;
import im.turms.gateway.pojo.bo.session.SessionDisconnectionReasonKey;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * @author James Chen
 */
class SessionDisconnectionReasonKeyTests {

    private final long userId = 1L;
    private final DeviceType deviceType = DeviceType.ANDROID;
    private final int sessionId = 2;

    @Test
    void constructor_shouldReturnInstance() {
        SessionDisconnectionReasonKey key = new SessionDisconnectionReasonKey(userId, deviceType, sessionId);
        assertNotNull(key);
    }

    @Test
    void getters_shouldGetValues() {
        SessionDisconnectionReasonKey key = new SessionDisconnectionReasonKey(userId, deviceType, sessionId);
        assertEquals(userId, key.getUserId());
        assertEquals(deviceType, key.getDeviceType());
        assertEquals(sessionId, key.getSessionId());
    }

    @Test
    void equals_hashCode_shouldGetValues() {
        SessionDisconnectionReasonKey keyA1 = new SessionDisconnectionReasonKey(userId, deviceType, sessionId);
        SessionDisconnectionReasonKey keyA2 = new SessionDisconnectionReasonKey(userId, deviceType, sessionId);

        SessionDisconnectionReasonKey keyB1 = new SessionDisconnectionReasonKey(null, deviceType, sessionId);
        SessionDisconnectionReasonKey keyB2 = new SessionDisconnectionReasonKey(null, deviceType, sessionId);
        SessionDisconnectionReasonKey keyC1 = new SessionDisconnectionReasonKey(userId, null, sessionId);
        SessionDisconnectionReasonKey keyC2 = new SessionDisconnectionReasonKey(userId, null, sessionId);
        SessionDisconnectionReasonKey keyD1 = new SessionDisconnectionReasonKey(userId, deviceType, null);
        SessionDisconnectionReasonKey keyD2 = new SessionDisconnectionReasonKey(userId, deviceType, null);

        SessionDisconnectionReasonKey keyE1 = new SessionDisconnectionReasonKey(userId, null, null);
        SessionDisconnectionReasonKey keyE2 = new SessionDisconnectionReasonKey(userId, null, null);
        SessionDisconnectionReasonKey keyF1 = new SessionDisconnectionReasonKey(null, deviceType, null);
        SessionDisconnectionReasonKey keyF2 = new SessionDisconnectionReasonKey(null, deviceType, null);
        SessionDisconnectionReasonKey keyG1 = new SessionDisconnectionReasonKey(null, null, sessionId);
        SessionDisconnectionReasonKey keyG2 = new SessionDisconnectionReasonKey(null, null, sessionId);

        assertEquals(keyA1, keyA2);
        assertEquals(keyB1, keyB2);
        assertEquals(keyC1, keyC2);
        assertEquals(keyD1, keyD2);
        assertEquals(keyE1, keyE2);
        assertEquals(keyF1, keyF2);
        assertEquals(keyG1, keyG2);
    }

}
