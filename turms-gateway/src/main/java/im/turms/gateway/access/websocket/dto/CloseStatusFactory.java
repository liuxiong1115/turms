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

package im.turms.gateway.access.websocket.dto;

import im.turms.common.constant.statuscode.SessionCloseStatus;
import org.springframework.web.reactive.socket.CloseStatus;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;
import java.util.EnumMap;

/**
 * CloseStatus consists of status code (int) and reason (String)
 * SessionCloseStatus consists of only status code (int)
 *
 * @author James Chen
 */
public class CloseStatusFactory {

    private static final EnumMap<SessionCloseStatus, CloseStatus> STATUS_POOL = new EnumMap<>(SessionCloseStatus.class);

    static {
        for (SessionCloseStatus status : SessionCloseStatus.values()) {
            STATUS_POOL.put(status, new CloseStatus(status.getCode()));
        }
    }

    private CloseStatusFactory() {
    }

    public static CloseStatus get(SessionCloseStatus status) {
        return STATUS_POOL.get(status);
    }

    public static CloseStatus get(@NotNull SessionCloseStatus status, @Nullable String reason) {
        return reason != null
                ? new CloseStatus(status.getCode(), reason)
                : get(status);
    }

}