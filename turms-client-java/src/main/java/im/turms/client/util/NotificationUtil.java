package im.turms.client.util;

import im.turms.common.TurmsStatusCode;
import im.turms.common.exception.TurmsBusinessException;
import im.turms.common.model.bo.common.Int64Values;
import im.turms.common.model.dto.notification.TurmsNotification;

public class NotificationUtil {
    private NotificationUtil() {
    }

    public static Long getFirstId(TurmsNotification notification) {
        Int64Values ids = notification.getData().getIds();
        if (ids.getValuesCount() > 0) {
            return ids.getValues(0);
        } else {
            throw TurmsBusinessException.get(TurmsStatusCode.MISSING_DATA);
        }
    }
}
