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

package im.turms.client.service;

import im.turms.client.TurmsClient;
import org.junit.jupiter.api.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static helper.Constants.*;
import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class StorageServiceST {

    private static TurmsClient turmsClient;
    private static final long USER_ID = 1L;
    private static final long GROUP_ID = 1;
    private static byte[] PROFILE_PICTURE;
    private static byte[] ATTACHMENT;
    private static long messageId;

    @BeforeAll
    static void setup() throws ExecutionException, InterruptedException, TimeoutException, IOException {
        turmsClient = new TurmsClient(WS_URL, null, null, STORAGE_SERVER_URL);
        turmsClient.getUserService()
                .login(USER_ID, "123")
                .get(5, TimeUnit.SECONDS);
        PROFILE_PICTURE = Files.readAllBytes(Paths.get("src", "test", "resources", "profile.webp"));
        ATTACHMENT = PROFILE_PICTURE;
    }

    @AfterAll
    static void tearDown() {
        if (turmsClient.getDriver().connected()) {
            turmsClient.getDriver().disconnect();
        }
    }

    // Create

    @Test
    @Order(ORDER_HIGH_PRIORITY)
    void uploadProfilePicture_shouldReturnUrl() throws InterruptedException, ExecutionException, TimeoutException {
        String url = turmsClient.getStorageService().uploadProfilePicture(PROFILE_PICTURE)
                .get(5, TimeUnit.SECONDS);
        assertNotNull(url);
    }

    @Test
    @Order(ORDER_HIGH_PRIORITY)
    void uploadGroupProfilePicture_shouldReturnUrl() throws InterruptedException, ExecutionException, TimeoutException {
        String url = turmsClient.getStorageService().uploadGroupProfilePicture(PROFILE_PICTURE, GROUP_ID)
                .get(5, TimeUnit.SECONDS);
        assertNotNull(url);
    }

    @Test
    @Order(ORDER_HIGH_PRIORITY)
    void uploadAttachment_shouldReturnUrl() throws InterruptedException, ExecutionException, TimeoutException {
        messageId = turmsClient.getMessageService().sendMessage(false, 2L, null, "I've attached a picture", null, null)
                .get(5, TimeUnit.SECONDS);
        String url = turmsClient.getStorageService().uploadAttachment(messageId, ATTACHMENT)
                .get(5, TimeUnit.SECONDS);
        assertNotNull(url);
    }

    // Query

    @Test
    @Order(ORDER_MIDDLE_PRIORITY)
    void queryProfilePicture_shouldEqualUploadedPicture() throws InterruptedException, ExecutionException, TimeoutException {
        byte[] bytes = turmsClient.getStorageService().queryProfilePicture(USER_ID)
                .get(5, TimeUnit.SECONDS);
        assertArrayEquals(PROFILE_PICTURE, bytes);
    }

    @Test
    @Order(ORDER_MIDDLE_PRIORITY)
    void queryGroupProfilePicture_shouldEqualUploadedPicture() throws InterruptedException, ExecutionException, TimeoutException {
        byte[] bytes = turmsClient.getStorageService().queryGroupProfilePicture(GROUP_ID)
                .get(5, TimeUnit.SECONDS);
        assertArrayEquals(PROFILE_PICTURE, bytes);
    }

    @Test
    @Order(ORDER_MIDDLE_PRIORITY)
    void queryAttachment_shouldEqualUploadedAttachment() throws InterruptedException, ExecutionException, TimeoutException {
        byte[] bytes = turmsClient.getStorageService().queryAttachment(messageId, null)
                .get(5, TimeUnit.SECONDS);
        assertArrayEquals(PROFILE_PICTURE, bytes);
    }

    // Delete

    @Test
    @Order(ORDER_LOW_PRIORITY)
    void deleteProfile_shouldSucceed() throws InterruptedException, ExecutionException, TimeoutException {
        turmsClient.getStorageService().deleteProfile()
                .get(5, TimeUnit.SECONDS);
        assertTrue(true);
    }

    @Test
    @Order(ORDER_LOW_PRIORITY)
    void deleteGroupProfile_shouldSucceed() throws InterruptedException, ExecutionException, TimeoutException {
        turmsClient.getStorageService().deleteGroupProfile(GROUP_ID)
                .get(5, TimeUnit.SECONDS);
        assertTrue(true);
    }
}
