package im.turms.client.incubator.driver;

import im.turms.turms.constant.DeviceType;
import im.turms.turms.constant.UserStatus;
import im.turms.turms.pojo.bo.user.UserLocation;
import im.turms.turms.pojo.notification.TurmsNotification;
import im.turms.turms.pojo.request.TurmsRequest;
import im.turms.turms.pojo.request.user.QueryUserProfileRequest;
import org.junit.jupiter.api.*;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static helper.Constants.WS_URL;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class TurmsDriverIT {
    private static TurmsDriver turmsDriver;

    @BeforeAll
    static void setup() {
        turmsDriver = new TurmsDriver(WS_URL, null, null, null);
    }

    @AfterAll
    static void tearDown() {
        if (turmsDriver.connected()) {
            turmsDriver.disconnect();
        }
    }

    @Test
    @Order(1)
    public void constructor_shouldReturnNotNullDriverInstance() {
        assertNotNull(turmsDriver);
    }

    @Test
    @Order(2)
    public void connect_shouldSucceed() throws ExecutionException, InterruptedException, TimeoutException {
        UserLocation location = UserLocation
                .newBuilder()
                .setLongitude(1.0f)
                .setLatitude(1.0f)
                .build();
        CompletableFuture<Void> future = turmsDriver.connect(1, "123", 10, location, UserStatus.BUSY, DeviceType.ANDROID);
        Void result = future.get(5, TimeUnit.SECONDS);
        assertNull(result);
    }

    @Test
    @Order(3)
    public void sendHeartbeat_shouldSucceed() throws ExecutionException, InterruptedException, TimeoutException {
        Void result = turmsDriver.sendHeartbeat().get(5, TimeUnit.SECONDS);
        assertNull(result);
    }

    @Test
    @Order(4)
    public void sendTurmsRequest_shouldSucceed() throws ExecutionException, InterruptedException, TimeoutException {
        QueryUserProfileRequest profileRequest = QueryUserProfileRequest.newBuilder()
                .setUserId(1)
                .build();
        TurmsRequest.Builder builder = TurmsRequest.newBuilder()
                .setQueryUserProfileRequest(profileRequest);
        TurmsNotification result = turmsDriver.send(builder).get(5, TimeUnit.SECONDS);
        assertNotNull(result);
    }

    @Test
    @Order(Integer.MAX_VALUE)
    public void disconnect_shouldSucceed() throws ExecutionException, InterruptedException, TimeoutException {
        Void result = turmsDriver.disconnect().get(5, TimeUnit.SECONDS);
        assertNull(result);
    }
}
