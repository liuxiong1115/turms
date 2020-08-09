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

package unit.im.turms.gateway.access;

import im.turms.common.constant.statuscode.SessionCloseStatus;
import im.turms.gateway.access.websocket.dto.CloseStatusFactory;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.socket.CloseStatus;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author James Chen
 */
class CloseStatusFactoryTests {

    @Test
    void get_shouldBeTheSameInstance_forTheSameCloseStatusWithoutReason() {
        CloseStatus closeStatus1 = CloseStatusFactory.get(SessionCloseStatus.SERVER_ERROR);
        CloseStatus closeStatus2 = CloseStatusFactory.get(SessionCloseStatus.SERVER_ERROR);
        CloseStatus closeStatus3 = CloseStatusFactory.get(SessionCloseStatus.SERVER_ERROR, null);
        
        assertEquals(closeStatus1, closeStatus2);
        assertEquals(closeStatus2, closeStatus3);
    }

}
