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

package im.turms.turms.access.web.controller;

import im.turms.turms.access.websocket.config.TurmsHandshakeWebSocketService;
import im.turms.turms.annotation.web.RequiredPermission;
import im.turms.turms.constant.AdminPermission;
import im.turms.turms.service.user.onlineuser.OnlineUserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/reasons")
public class ReasonController {
    private final TurmsHandshakeWebSocketService handshakeService;
    private final OnlineUserService onlineUserService;

    public ReasonController(TurmsHandshakeWebSocketService handshakeService, OnlineUserService onlineUserService) {
        this.handshakeService = handshakeService;
        this.onlineUserService = onlineUserService;
    }

    @GetMapping("/login-failed")
    @RequiredPermission(AdminPermission.CUSTOM)
    public Object getLoginFailedReason(
            @RequestParam Long userId,
            @RequestParam Long requestId) {
        Object reason = handshakeService.getFailedReason(userId, requestId);
        if (reason instanceof String) {
            return ResponseEntity
                    .status(HttpStatus.TEMPORARY_REDIRECT)
                    .body(reason);
        } else {
            return reason;
        }
    }

    @GetMapping("/disconnection")
    @RequiredPermission(AdminPermission.CUSTOM)
    public Integer getDisconnectionReason(
            @RequestParam Long userId,
            @RequestParam String sessionId) {
        return onlineUserService.getDisconnectionReason(userId, sessionId);
    }
}
