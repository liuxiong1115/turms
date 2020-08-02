/*
 * Copyright (C) 2019 The Turms Project
 * https://github.com/turms-im/turms
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package im.turms.client.util;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * @author James Chen
 */
public class MapUtil {

    private MapUtil() {
    }

    public static int getCapability(int expectedSize) {
        return (int) ((float) expectedSize / 0.75F + 1.0F);
    }

    public static Map<String, Object> of(Object... array) {
        if (array != null && array.length % 2 == 0) {
            HashMap<String, Object> map = new HashMap<>(getCapability(array.length / 2));
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
