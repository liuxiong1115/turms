package im.turms.client.incubator.util;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class MapUtil {
    private MapUtil() {
    }

    public static Map<String, ?> of(Object... array) {
        if (array != null && array.length % 2 == 0) {
            HashMap<String, Object> map = new HashMap<>(array.length / 2);
            for (int i = 0; i < array.length; i = i + 2) {
                String key = (String) array[i];
                Object value = array[i + 1];
                if (value != null) {
                    map.put(key, value);
                }
            }
            return map;
        }
        return Collections.emptyMap();
    }
}
