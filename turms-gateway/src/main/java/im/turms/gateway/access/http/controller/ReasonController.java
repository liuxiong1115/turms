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

package im.turms.gateway.access.http.controller;

import im.turms.common.constant.DeviceType;
import im.turms.common.constant.statuscode.SessionCloseStatus;
import im.turms.gateway.access.http.dto.LoginFailureReasonDTO;
import im.turms.gateway.access.http.dto.SessionDisconnectionReasonDTO;
import im.turms.gateway.service.impl.ReasonCacheService;
import io.netty.handler.codec.http.websocketx.WebSocketCloseStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/**
 * A service degradation for turms-client-js in the browser to query
 * the reasons of login failure or session disconnection
 *
 * @author James Chen
 */
@RestController
@RequestMapping("/reasons")
public class ReasonController {

    /**
     * We don't use 404 because it's confusing and cannot be used to distinguish
     */
    private static final ResponseEntity NO_CONTENT_RESPONSE = ResponseEntity.noContent().build();
    private final ReasonCacheService reasonCacheService;

    public ReasonController(ReasonCacheService reasonCacheService) {
        this.reasonCacheService = reasonCacheService;
    }

    @GetMapping("/login-failure")
    public Mono<ResponseEntity<LoginFailureReasonDTO>> getLoginFailureReason(
            @RequestParam Long userId,
            @RequestParam DeviceType deviceType,
            @RequestParam Long requestId) {
        return reasonCacheService.getLoginFailureReason(userId, deviceType, requestId)
                .map(code -> {
                    LoginFailureReasonDTO reason = new LoginFailureReasonDTO(code.getBusinessCode(), code.name(), code.getReason());
                    return ResponseEntity.ok(reason);
                })
                .defaultIfEmpty(NO_CONTENT_RESPONSE);
    }

    @GetMapping("/disconnection")
    public Mono<ResponseEntity<SessionDisconnectionReasonDTO>> getSessionDisconnectionReason(
            @RequestParam Long userId,
            @RequestParam DeviceType deviceType,
            @RequestParam Integer sessionId) {
        return reasonCacheService.getDisconnectionReason(userId, deviceType, sessionId)
                .map(code -> {
                    SessionCloseStatus closeStatus = SessionCloseStatus.get(code);
                    String name = closeStatus == null
                            ? WebSocketCloseStatus.valueOf(code).reasonText()
                            : closeStatus.name();
                    return ResponseEntity.ok(new SessionDisconnectionReasonDTO(code, name, ""));
                })
                .defaultIfEmpty(NO_CONTENT_RESPONSE);
    }

}
