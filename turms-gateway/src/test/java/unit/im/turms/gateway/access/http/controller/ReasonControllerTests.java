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

package unit.im.turms.gateway.access.http.controller;

import im.turms.common.constant.DeviceType;
import im.turms.common.constant.statuscode.SessionCloseStatus;
import im.turms.common.constant.statuscode.TurmsStatusCode;
import im.turms.gateway.access.http.controller.ReasonController;
import im.turms.gateway.access.http.dto.LoginFailureReasonDTO;
import im.turms.gateway.access.http.dto.SessionDisconnectionReasonDTO;
import im.turms.gateway.service.impl.ReasonCacheService;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author James Chen
 */
class ReasonControllerTests {

    @Test
    void getLoginFailureReason_shouldReturnReason_ifReasonExists() {
        ReasonCacheService service = mock(ReasonCacheService.class);
        long userId = 1L;
        long requestId = 2L;
        DeviceType deviceType = DeviceType.DESKTOP;
        TurmsStatusCode expectedStatusCode = TurmsStatusCode.OK;
        when(service.getLoginFailureReason(userId, deviceType, requestId))
                .thenReturn(Mono.just(expectedStatusCode));
        ReasonController reasonController = new ReasonController(service);
        Mono<ResponseEntity<LoginFailureReasonDTO>> result = reasonController.getLoginFailureReason(userId, deviceType, requestId);

        StepVerifier
                .create(result)
                .expectNextMatches(entity -> expectedStatusCode.getBusinessCode() == entity.getBody().getStatusCode())
                .verifyComplete();
    }

    @Test
    void getLoginFailureReason_shouldReturn404_ifReasonNotExists() {
        ReasonCacheService service = mock(ReasonCacheService.class);
        long userId = 1L;
        long requestId = 2L;
        DeviceType deviceType = DeviceType.DESKTOP;
        when(service.getLoginFailureReason(userId, deviceType, requestId))
                .thenReturn(Mono.empty());
        ReasonController reasonController = new ReasonController(service);
        Mono<ResponseEntity<LoginFailureReasonDTO>> result = reasonController.getLoginFailureReason(userId, deviceType, requestId);

        StepVerifier
                .create(result)
                .expectNextMatches(entity -> entity.getStatusCode().equals(HttpStatus.NOT_FOUND))
                .verifyComplete();
    }

    @Test
    void getSessionDisconnectionReason_shouldReturnReason_ifReasonExists() {
        ReasonCacheService service = mock(ReasonCacheService.class);
        long userId = 1L;
        int sessionId = 2;
        DeviceType deviceType = DeviceType.DESKTOP;
        int code = SessionCloseStatus.SERVER_ERROR.getCode();
        when(service.getDisconnectionReason(userId, deviceType, sessionId))
                .thenReturn(Mono.just(code));
        ReasonController reasonController = new ReasonController(service);
        Mono<ResponseEntity<SessionDisconnectionReasonDTO>> result = reasonController.getSessionDisconnectionReason(userId, deviceType, sessionId);

        StepVerifier
                .create(result)
                .expectNextMatches(entity -> code == entity.getBody().getCloseCode())
                .verifyComplete();
    }

    @Test
    void getSessionDisconnectionReason_shouldReturn404_ifReasonNotExists() {
        ReasonCacheService service = mock(ReasonCacheService.class);
        long userId = 1L;
        int sessionId = 2;
        DeviceType deviceType = DeviceType.DESKTOP;
        when(service.getDisconnectionReason(userId, deviceType, sessionId))
                .thenReturn(Mono.empty());
        ReasonController reasonController = new ReasonController(service);
        Mono<ResponseEntity<SessionDisconnectionReasonDTO>> result = reasonController.getSessionDisconnectionReason(userId, deviceType, sessionId);

        StepVerifier
                .create(result)
                .expectNextMatches(entity -> entity.getStatusCode().equals(HttpStatus.NOT_FOUND))
                .verifyComplete();
    }

}
