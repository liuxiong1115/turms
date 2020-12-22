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

package unit.im.turms.gateway.service.impl;

import im.turms.common.constant.DeviceType;
import im.turms.common.constant.statuscode.SessionCloseStatus;
import im.turms.gateway.service.impl.ReasonCacheService;
import im.turms.server.common.constant.TurmsStatusCode;
import im.turms.server.common.dto.CloseReason;
import im.turms.server.common.exception.TurmsBusinessException;
import im.turms.server.common.property.TurmsProperties;
import im.turms.server.common.property.TurmsPropertiesManager;
import im.turms.server.common.property.env.gateway.GatewayProperties;
import im.turms.server.common.property.env.gateway.SessionProperties;
import im.turms.server.common.redis.sharding.ConsistentHashingShardingAlgorithm;
import im.turms.server.common.redis.sharding.ShardingAlgorithm;
import im.turms.server.common.util.ExceptionUtil;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.ReactiveValueOperations;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author James Chen
 */
class ReasonCacheServiceTests {

    private final TurmsStatusCode loginFailureReason = TurmsStatusCode.SERVER_INTERNAL_ERROR;
    private final int disconnectionReason = SessionCloseStatus.DISCONNECTED_BY_ADMIN.getCode();

    @Test
    void constructor_shouldReturn() {
        ReasonCacheService reasonCacheService = newReasonCacheService(true, null, true);
        assertNotNull(reasonCacheService);
    }

    // Login Failure

    @Test
    void getLoginFailureReason_shouldThrow_ifDisabled() {
        ReasonCacheService reasonCacheService = newReasonCacheService(false, null, true);
        Mono<TurmsStatusCode> result = reasonCacheService.getLoginFailureReason(1L, DeviceType.ANDROID, 1L);

        StepVerifier.create(result)
                .expectErrorMatches(throwable -> throwable instanceof TurmsBusinessException && ((TurmsBusinessException) throwable).getCode().equals(TurmsStatusCode.LOGIN_FAILURE_REASON_CACHE_IS_DISABLED))
                .verify();
    }

    @Test
    void getLoginFailureReason_shouldThrow_forDegradedDeviceType() {
        ReasonCacheService reasonCacheService = newReasonCacheService(true, Collections.emptySet(), true);
        Mono<TurmsStatusCode> result = reasonCacheService.getLoginFailureReason(1L, DeviceType.ANDROID, 1L);

        StepVerifier.create(result)
                .expectErrorMatches(throwable -> throwable instanceof TurmsBusinessException && ((TurmsBusinessException) throwable).getCode().equals(TurmsStatusCode.FORBIDDEN_DEVICE_TYPE_FOR_LOGIN_FAILURE_REASON))
                .verify();
    }

    @Test
    void getLoginFailureReason_shouldComplete_ifReasonNotExists() {
        ReasonCacheService reasonCacheService = newReasonCacheService(true, Set.of(DeviceType.ANDROID), false);
        Mono<TurmsStatusCode> result = reasonCacheService.getLoginFailureReason(1L, DeviceType.ANDROID, 1L);

        StepVerifier.create(result)
                .verifyComplete();
    }

    @Test
    void getLoginFailureReason_shouldReturnReason_ifReasonExists() {
        ReasonCacheService reasonCacheService = newReasonCacheService(true, Set.of(DeviceType.ANDROID), true);
        Mono<TurmsStatusCode> result = reasonCacheService.getLoginFailureReason(1L, DeviceType.ANDROID, 1L);

        StepVerifier.create(result)
                .expectNext(loginFailureReason)
                .verifyComplete();
    }

    @Test
    void shouldCacheLoginFailureReason_shouldReturnTrue_ifAllArgsAreValid() {
        ReasonCacheService reasonCacheService = newReasonCacheService(true, Set.of(DeviceType.ANDROID), true);

        assertTrue(reasonCacheService.shouldCacheLoginFailureReason(1L, DeviceType.ANDROID, 1L));
    }

    @Test
    void shouldCacheLoginFailureReason_shouldReturnFalse_forAnyInvalidArgs() {
        ReasonCacheService reasonCacheService = newReasonCacheService(true, Set.of(DeviceType.IOS), true);

        assertFalse(reasonCacheService.shouldCacheLoginFailureReason(null, DeviceType.IOS, 1L));
        assertFalse(reasonCacheService.shouldCacheLoginFailureReason(1L, null, 1L));
        assertFalse(reasonCacheService.shouldCacheLoginFailureReason(1L, DeviceType.IOS, null));
        assertFalse(reasonCacheService.shouldCacheLoginFailureReason(1L, DeviceType.ANDROID, 1L));
    }

    @Test
    void cacheLoginFailureReason_shouldReturnTrue_forValidArgs() {
        ReasonCacheService reasonCacheService = newReasonCacheService(true, Set.of(DeviceType.ANDROID), true);
        Mono<Boolean> result = reasonCacheService.cacheLoginFailureReason(1L, DeviceType.ANDROID, 1L, TurmsStatusCode.SESSION_SIMULTANEOUS_CONFLICTS_DECLINE);

        StepVerifier.create(result)
                .expectNext(true)
                .verifyComplete();
    }

    // Session Disconnection

    @Test
    void getDisconnectionReason_shouldThrow_ifDisabled() {
        ReasonCacheService reasonCacheService = newReasonCacheService(false, null, true);
        Mono<Integer> result = reasonCacheService.getDisconnectionReason(1L, DeviceType.ANDROID, 1);

        StepVerifier.create(result)
                .expectErrorMatches(throwable -> ExceptionUtil.isStatusCode(throwable, TurmsStatusCode.CACHING_FOR_SESSION_DISCONNECTION_REASON_IS_DISABLED))
                .verify();
    }

    @Test
    void getDisconnectionReason_shouldThrow_forDegradedDeviceType() {
        ReasonCacheService reasonCacheService = newReasonCacheService(true, Collections.emptySet(), true);
        Mono<Integer> result = reasonCacheService.getDisconnectionReason(1L, DeviceType.ANDROID, 1);

        StepVerifier.create(result)
                .expectErrorMatches(throwable -> ExceptionUtil.isStatusCode(throwable, TurmsStatusCode.FORBIDDEN_DEVICE_TYPE_FOR_SESSION_DISCONNECTION_REASON))
                .verify();
    }

    @Test
    void getDisconnectionReason_shouldComplete_ifReasonNotExists() {
        ReasonCacheService reasonCacheService = newReasonCacheService(true, Set.of(DeviceType.ANDROID), false);
        Mono<Integer> result = reasonCacheService.getDisconnectionReason(1L, DeviceType.ANDROID, 1);

        StepVerifier.create(result)
                .verifyComplete();
    }

    @Test
    void getDisconnectionReason_shouldReturnReason_ifReasonExists() {
        ReasonCacheService reasonCacheService = newReasonCacheService(true, Set.of(DeviceType.ANDROID), true);
        Mono<Integer> result = reasonCacheService.getDisconnectionReason(1L, DeviceType.ANDROID, 1);

        StepVerifier.create(result)
                .expectNext(disconnectionReason)
                .verifyComplete();
    }

    @Test
    void shouldCacheDisconnectionReason_shouldReturnTrue_ifAllArgsAreValid() {
        ReasonCacheService reasonCacheService = newReasonCacheService(true, Set.of(DeviceType.ANDROID), true);

        assertTrue(reasonCacheService.shouldCacheDisconnectionReason(1L, DeviceType.ANDROID, CloseReason.get(SessionCloseStatus.DISCONNECTED_BY_ADMIN)));
    }

    @Test
    void shouldCacheDisconnectionReason_shouldReturnFalse_forAnyInvalidArgs() {
        ReasonCacheService reasonCacheService = newReasonCacheService(true, Set.of(DeviceType.IOS), true);
        CloseReason validReason = CloseReason.get(SessionCloseStatus.DISCONNECTED_BY_ADMIN);
        CloseReason invalidReason = CloseReason.get(TurmsStatusCode.ILLEGAL_ARGUMENT);

        assertFalse(reasonCacheService.shouldCacheDisconnectionReason(null, DeviceType.IOS, validReason));
        assertFalse(reasonCacheService.shouldCacheDisconnectionReason(1L, null, validReason));
        assertFalse(reasonCacheService.shouldCacheDisconnectionReason(1L, DeviceType.ANDROID, validReason));
        assertFalse(reasonCacheService.shouldCacheDisconnectionReason(1L, DeviceType.BROWSER, invalidReason));
    }

    @Test
    void cacheDisconnectionReason_shouldReturnTrue_forValidArgs() {
        ReasonCacheService reasonCacheService = newReasonCacheService(true, Set.of(DeviceType.ANDROID), true);
        CloseReason closeReason = CloseReason.get(SessionCloseStatus.DISCONNECTED_BY_ADMIN);
        Mono<Boolean> result = reasonCacheService.cacheDisconnectionReason(1L, DeviceType.ANDROID, 1, closeReason);

        StepVerifier.create(result)
                .expectNext(true)
                .verifyComplete();
    }

    private ReasonCacheService newReasonCacheService(boolean enableCache, Set<DeviceType> degradedDeviceTypes, boolean reasonExists) {
        ShardingAlgorithm shardingAlgorithm = new ConsistentHashingShardingAlgorithm();
        ReactiveValueOperations loginFailureOperations = mock(ReactiveValueOperations.class);
        ReactiveValueOperations disconnectionOperations = mock(ReactiveValueOperations.class);
        ReactiveRedisTemplate loginFailureRedisTemplate = mock(ReactiveRedisTemplate.class);
        ReactiveRedisTemplate disconnectionRedisTemplate = mock(ReactiveRedisTemplate.class);
        if (reasonExists) {
            when(loginFailureOperations.get(any()))
                    .thenReturn(Mono.just(loginFailureReason));
            when(disconnectionOperations.get(any()))
                    .thenReturn(Mono.just(disconnectionReason));
        } else {
            when(loginFailureOperations.get(any()))
                    .thenReturn(Mono.empty());
            when(disconnectionOperations.get(any()))
                    .thenReturn(Mono.empty());
        }
        when(loginFailureOperations.set(any(), any(), any()))
                .thenReturn(Mono.just(true));
        when(disconnectionOperations.set(any(), any(), any()))
                .thenReturn(Mono.just(true));
        when(loginFailureRedisTemplate.opsForValue())
                .thenReturn(loginFailureOperations);
        when(disconnectionRedisTemplate.opsForValue())
                .thenReturn(disconnectionOperations);

        TurmsPropertiesManager propertiesManager = mock(TurmsPropertiesManager.class);
        TurmsProperties turmsProperties = new TurmsProperties();
        GatewayProperties gateway = new GatewayProperties();
        SessionProperties session = new SessionProperties();
        session.setEnableQueryLoginFailureReason(enableCache);
        session.setEnableQueryDisconnectionReason(enableCache);
        session.setLoginFailureReasonExpireAfter(Integer.MAX_VALUE);
        session.setDisconnectionReasonExpireAfter(Integer.MAX_VALUE);
        session.setDegradedDeviceTypesForLoginFailureReason(degradedDeviceTypes);
        session.setDegradedDeviceTypesForDisconnectionReason(degradedDeviceTypes);
        gateway.setSession(session);
        turmsProperties.setGateway(gateway);
        when(propertiesManager.getLocalProperties())
                .thenReturn(turmsProperties);
        return new ReasonCacheService(shardingAlgorithm, shardingAlgorithm, List.of(loginFailureRedisTemplate), List.of(disconnectionRedisTemplate), propertiesManager);
    }

}
