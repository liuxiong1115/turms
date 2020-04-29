package im.turms.turms.constant;

import im.turms.common.TurmsCloseStatus;
import org.springframework.web.reactive.socket.CloseStatus;

import java.util.EnumMap;

public class CloseStatusFactory {
    private static final EnumMap<TurmsCloseStatus, CloseStatus> STATUS_POOL = new EnumMap<>(TurmsCloseStatus.class);

    static {
        for (TurmsCloseStatus status : TurmsCloseStatus.values()) {
            STATUS_POOL.put(status, new CloseStatus(status.getCode()));
        }
    }

    private CloseStatusFactory() {
    }

    public static CloseStatus get(TurmsCloseStatus status) {
        return STATUS_POOL.get(status);
    }

    public static CloseStatus get(TurmsCloseStatus status, String reason) {
        if (reason != null) {
            return new CloseStatus(status.getCode(), reason);
        } else {
            return get(status);
        }
    }
}
