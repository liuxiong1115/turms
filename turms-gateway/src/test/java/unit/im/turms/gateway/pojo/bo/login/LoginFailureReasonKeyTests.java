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

package unit.im.turms.gateway.pojo.bo.login;

import im.turms.common.constant.DeviceType;
import im.turms.gateway.pojo.bo.login.LoginFailureReasonKey;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * @author James Chen
 */
class LoginFailureReasonKeyTests {

    private final long userId = 1L;
    private final DeviceType deviceType = DeviceType.ANDROID;
    private final long loginRequestId = 2L;

    @Test
    void constructor_shouldReturnInstance() {
        LoginFailureReasonKey key = new LoginFailureReasonKey(userId, deviceType, loginRequestId);
        assertNotNull(key);
    }

    @Test
    void getters_shouldGetValues() {
        LoginFailureReasonKey key = new LoginFailureReasonKey(userId, deviceType, loginRequestId);
        assertEquals(userId, key.getUserId());
        assertEquals(deviceType, key.getDeviceType());
        assertEquals(loginRequestId, key.getLoginRequestId());
    }

    @Test
    void equals_hashCode_shouldGetValues() {
        LoginFailureReasonKey keyA1 = new LoginFailureReasonKey(userId, deviceType, loginRequestId);
        LoginFailureReasonKey keyA2 = new LoginFailureReasonKey(userId, deviceType, loginRequestId);

        LoginFailureReasonKey keyB1 = new LoginFailureReasonKey(null, deviceType, loginRequestId);
        LoginFailureReasonKey keyB2 = new LoginFailureReasonKey(null, deviceType, loginRequestId);
        LoginFailureReasonKey keyC1 = new LoginFailureReasonKey(userId, null, loginRequestId);
        LoginFailureReasonKey keyC2 = new LoginFailureReasonKey(userId, null, loginRequestId);
        LoginFailureReasonKey keyD1 = new LoginFailureReasonKey(userId, deviceType, null);
        LoginFailureReasonKey keyD2 = new LoginFailureReasonKey(userId, deviceType, null);

        LoginFailureReasonKey keyE1 = new LoginFailureReasonKey(userId, null, null);
        LoginFailureReasonKey keyE2 = new LoginFailureReasonKey(userId, null, null);
        LoginFailureReasonKey keyF1 = new LoginFailureReasonKey(null, deviceType, null);
        LoginFailureReasonKey keyF2 = new LoginFailureReasonKey(null, deviceType, null);
        LoginFailureReasonKey keyG1 = new LoginFailureReasonKey(null, null, loginRequestId);
        LoginFailureReasonKey keyG2 = new LoginFailureReasonKey(null, null, loginRequestId);

        assertEquals(keyA1, keyA2);
        assertEquals(keyB1, keyB2);
        assertEquals(keyC1, keyC2);
        assertEquals(keyD1, keyD2);
        assertEquals(keyE1, keyE2);
        assertEquals(keyF1, keyF2);
        assertEquals(keyG1, keyG2);
    }

}
