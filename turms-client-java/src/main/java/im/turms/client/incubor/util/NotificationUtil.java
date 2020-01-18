package im.turms.client.incubor.util;

import im.turms.turms.pojo.bo.common.Int64Values;
import im.turms.turms.pojo.notification.TurmsNotification;

import java.util.Collections;
import java.util.List;

public class NotificationUtil {

    private NotificationUtil() {
    }

    public static Long getFirstIdFromIds(TurmsNotification notification) {
        TurmsNotification.Data data = notification.getData();
        if (data != null) {
            Int64Values ids = data.getIds();
            if (ids != null && ids.getValuesCount() > 0) {
                return ids.getValues(0);
            }
        }
        return null;
    }

    public static List<Long> getIds(TurmsNotification notification) {
        TurmsNotification.Data data = notification.getData();
        if (data != null) {
            Int64Values ids = data.getIds();
            if (ids != null) {
                return ids.getValuesList();
            }
        }
        return Collections.emptyList();
    }
}
