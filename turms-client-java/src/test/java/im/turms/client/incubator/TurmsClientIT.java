package im.turms.client.incubator;

import org.junit.jupiter.api.Test;

import static helper.Constants.WS_URL;
import static org.hibernate.validator.internal.util.Contracts.assertNotNull;

public class TurmsClientIT {

    @Test
    public void constructor_shouldReturnNotNullClientInstance() {
        TurmsClient turmsClient = new TurmsClient(WS_URL, null, null, null);
        assertNotNull(turmsClient);
    }
}
