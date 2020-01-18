package im.turms.client.incubator.service;

import im.turms.client.incubator.TurmsClient;
import im.turms.turms.constant.DeviceType;
import im.turms.turms.constant.UserStatus;
import org.junit.jupiter.api.*;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static helper.Constants.*;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class GroupServiceIT {
    private static TurmsClient turmsClient;
    private static GroupService groupService;
    private static Long newGroupId;

    @BeforeAll
    static void setup() throws ExecutionException, InterruptedException, TimeoutException {
        turmsClient = new TurmsClient(WS_URL, null, null, null);
        groupService = new GroupService(turmsClient);
        CompletableFuture<Void> future = turmsClient.getDriver().connect(1, "123", 10, null, UserStatus.BUSY, DeviceType.ANDROID);
        future.get(5, TimeUnit.SECONDS);
    }

    @AfterAll
    static void tearDown() {
        if (turmsClient.getDriver().connected()) {
            turmsClient.getDriver().disconnect();
        }
    }

    @Test
    @Order(ORDER_FIRST)
    public void constructor_shouldReturnNotNullGroupServiceInstance() {
        assertNotNull(groupService);
    }

    @Test
    @Order(ORDER_MIDDLE_PRIORITY)
    public void createAndDeleteGroup_shouldSucceed() throws ExecutionException, InterruptedException, TimeoutException {
        newGroupId = groupService.createGroup("name", "intro", "announcement", null, 10, null, null)
                .get(5, TimeUnit.SECONDS);
        assertNotNull(newGroupId);
        Void deleteGroupResult = groupService.deleteGroup(newGroupId)
                .get(5, TimeUnit.SECONDS);
        assertNull(deleteGroupResult);
    }
}
