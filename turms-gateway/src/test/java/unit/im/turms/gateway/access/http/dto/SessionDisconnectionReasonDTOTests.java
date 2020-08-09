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

package unit.im.turms.gateway.access.http.dto;

import im.turms.common.constant.statuscode.SessionCloseStatus;
import im.turms.common.constant.statuscode.TurmsStatusCode;
import im.turms.gateway.access.http.dto.LoginFailureReasonDTO;
import im.turms.gateway.access.http.dto.SessionDisconnectionReasonDTO;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author James Chen
 */
class SessionDisconnectionReasonDTOTests {

    @Test
    void constructor_shouldSucceed() {
        SessionCloseStatus closeStatus = SessionCloseStatus.SERVER_ERROR;
        String errorReason = "error";
        SessionDisconnectionReasonDTO reason = new SessionDisconnectionReasonDTO(closeStatus.getCode(), closeStatus.name(), errorReason);

        assertEquals(closeStatus.getCode(), reason.getCloseCode());
        assertEquals(closeStatus.name(), reason.getCodeName());
        assertEquals(errorReason, reason.getReason());
    }

}
