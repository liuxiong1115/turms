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

import im.turms.common.constant.DeviceType;

public class SystemUtil {
    private SystemUtil() {
    }

    private static Boolean isAndroid;
    private static final DeviceType deviceType;

    static {
        Boolean isAndroid = isAndroid();
        if (isAndroid != null) {
            deviceType = isAndroid ? DeviceType.ANDROID : DeviceType.DESKTOP;
        } else {
            deviceType = DeviceType.UNKNOWN;
        }
    }

    public static DeviceType getDeviceType() {
        return deviceType;
    }

    public static Boolean isAndroid() {
        if (isAndroid == null) {
            try {
                isAndroid = System.getProperty("java.vendor").toLowerCase().contains("android");
                if (isAndroid) {
                    return true;
                }
                isAndroid = System.getProperty("java.vm.vendor").toLowerCase().contains("android");
                if (isAndroid) {
                    return true;
                }
                isAndroid = System.getProperty("java.vm.name").toLowerCase().equals("dalvik");
                if (isAndroid) {
                    return true;
                }
                isAndroid = System.getProperty("java.runtime.name").toLowerCase().equals("android runtime");
                if (isAndroid) {
                    return true;
                }
                try {
                    Class.forName("android.util.Log");
                    isAndroid = true;
                } catch (ClassNotFoundException e) {
                    isAndroid = false;
                }
                return isAndroid;
            } catch (SecurityException e) {
                return null;
            }
        }
        return isAndroid;
    }
}
