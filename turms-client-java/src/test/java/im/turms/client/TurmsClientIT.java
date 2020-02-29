package im.turms.client;

import org.junit.jupiter.api.Test;

import static helper.Constants.STORAGE_SERVER_URL;
import static helper.Constants.WS_URL;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class TurmsClientIT {

    @Test
    public void constructor_shouldReturnNotNullClientInstance() {
        TurmsClient turmsClient = new TurmsClient(WS_URL, null, null, STORAGE_SERVER_URL);
        assertNotNull(turmsClient);
    }
}
