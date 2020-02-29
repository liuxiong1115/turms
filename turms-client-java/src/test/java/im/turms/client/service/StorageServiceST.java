package im.turms.client.service;

import im.turms.client.TurmsClient;
import im.turms.common.constant.ChatType;
import im.turms.common.constant.DeviceType;
import im.turms.common.constant.UserStatus;
import org.junit.jupiter.api.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static helper.Constants.*;
import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class StorageServiceST {

    private static TurmsClient turmsClient;
    private static final long USER_ID = 1L;
    private static byte[] PROFILE_PICTURE;
    private static byte[] ATTACHMENT;
    private static long messageId;

    @BeforeAll
    static void setup() throws ExecutionException, InterruptedException, TimeoutException, IOException {
        turmsClient = new TurmsClient(WS_URL, null, null, STORAGE_SERVER_URL);
        turmsClient.getUserService()
                .login(USER_ID, "123", null, UserStatus.BUSY, DeviceType.ANDROID)
                .get(5, TimeUnit.SECONDS);
        PROFILE_PICTURE = Files.readAllBytes(Path.of("src", "test", "resources", "profile.webp"));
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
    public void uploadProfilePicture_should() throws InterruptedException, ExecutionException, TimeoutException {
        String url = turmsClient.getStorageService().uploadProfilePicture(PROFILE_PICTURE).get(10, TimeUnit.SECONDS);
        assertNotNull(url);
    }

    @Test
    @Order(ORDER_HIGH_PRIORITY)
    public void uploadAttachment_should() throws InterruptedException, ExecutionException, TimeoutException {
        messageId = turmsClient.getMessageService().sendMessage(ChatType.PRIVATE, 2L, null, "I've attached a picture", null, null)
                .get(5, TimeUnit.SECONDS);
        String url = turmsClient.getStorageService().uploadAttachment(messageId, ATTACHMENT).get(10, TimeUnit.SECONDS);
        assertNotNull(url);
    }

    // Query

    @Test
    @Order(ORDER_MIDDLE_PRIORITY)
    public void queryProfilePicture() throws InterruptedException, ExecutionException, TimeoutException {
        byte[] bytes = turmsClient.getStorageService().queryProfilePicture(USER_ID)
                .get(5, TimeUnit.SECONDS);
        assertArrayEquals(PROFILE_PICTURE, bytes);
    }

    @Test
    @Order(ORDER_MIDDLE_PRIORITY)
    public void queryAttachment() throws InterruptedException, ExecutionException, TimeoutException {
        byte[] bytes = turmsClient.getStorageService().queryAttachment(messageId, null)
                .get(5, TimeUnit.SECONDS);
        assertArrayEquals(PROFILE_PICTURE, bytes);
    }
}
