package im.turms.client;

import org.junit.jupiter.api.Test;

import static helper.Constants.WS_URL;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class TurmsClientIT {

    @Test
    public void constructor_shouldReturnNotNullClientInstance() {
        TurmsClient turmsClient = new TurmsClient(WS_URL, null, null);
        assertNotNull(turmsClient);
    }
}
