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

package im.turms.client.driver;

import im.turms.client.TurmsClient;
import im.turms.common.constant.DeviceType;
import im.turms.common.constant.UserStatus;
import im.turms.common.model.bo.user.UserLocation;
import im.turms.common.model.dto.notification.TurmsNotification;
import im.turms.common.model.dto.request.TurmsRequest;
import im.turms.common.model.dto.request.user.QueryUserProfileRequest;
import org.junit.jupiter.api.*;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static helper.Constants.*;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class TurmsDriverIT {
    private static TurmsDriver turmsDriver;

    @BeforeAll
    static void setup() {
        turmsDriver = new TurmsClient(WS_URL).getDriver();
    }

    @AfterAll
    static void tearDown() {
        if (turmsDriver.isConnected()) {
            turmsDriver.disconnect();
        }
    }

    @Test
    @Order(ORDER_FIRST)
    void constructor_shouldReturnNotNullDriverInstance() {
        assertNotNull(turmsDriver);
    }

    @Test
    @Order(ORDER_HIGHEST_PRIORITY)
    void connect_shouldSucceed() throws ExecutionException, InterruptedException, TimeoutException {
        UserLocation location = UserLocation
                .newBuilder()
                .setLongitude(1.0f)
                .setLatitude(1.0f)
                .build();
        CompletableFuture<Void> future = turmsDriver.connect(1, "123", DeviceType.ANDROID, UserStatus.BUSY, location);
        Void result = future.get(5, TimeUnit.SECONDS);
        assertNull(result);
    }

    @Test
    @Order(ORDER_MIDDLE_PRIORITY)
    void sendHeartbeat_shouldSucceed() throws ExecutionException, InterruptedException, TimeoutException {
        Void result = turmsDriver.sendHeartbeat().get(5, TimeUnit.SECONDS);
        assertNull(result);
    }

    @Test
    @Order(ORDER_MIDDLE_PRIORITY)
    void sendTurmsRequest_shouldSucceed() throws ExecutionException, InterruptedException, TimeoutException {
        QueryUserProfileRequest profileRequest = QueryUserProfileRequest.newBuilder()
                .setUserId(1)
                .build();
        TurmsRequest.Builder builder = TurmsRequest.newBuilder()
                .setQueryUserProfileRequest(profileRequest);
        TurmsNotification result = turmsDriver.send(builder).get(5, TimeUnit.SECONDS);
        assertNotNull(result);
    }

    @Test
    @Order(ORDER_LOWEST_PRIORITY)
    void disconnect_shouldSucceed() throws ExecutionException, InterruptedException, TimeoutException {
        Void result = turmsDriver.disconnect().get(5, TimeUnit.SECONDS);
        assertNull(result);
    }

}
