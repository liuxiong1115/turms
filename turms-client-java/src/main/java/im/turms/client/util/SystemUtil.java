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

/**
 * @author James Chen
 */
public class SystemUtil {

    private SystemUtil() {
    }

    private static Boolean isAndroid;
    private static final DeviceType DEVICE_TYPE;

    static {
        Boolean isAndroid = isAndroid();
        if (isAndroid != null) {
            DEVICE_TYPE = isAndroid ? DeviceType.ANDROID : DeviceType.DESKTOP;
        } else {
            DEVICE_TYPE = DeviceType.UNKNOWN;
        }
    }

    public static DeviceType getDeviceType() {
        return DEVICE_TYPE;
    }

    public static Boolean isAndroid() {
        if (isAndroid == null) {
            try {
                isAndroid = isAndroid0();
            } catch (SecurityException e) {
                return null;
            }
        }
        return isAndroid;
    }

    private static Boolean isAndroid0() {
        if (System.getProperty("java.vendor").toLowerCase().contains("android")
                || System.getProperty("java.vm.vendor").toLowerCase().contains("android")
                || "dalvik".equalsIgnoreCase(System.getProperty("java.vm.name"))
                || "android runtime".equalsIgnoreCase(System.getProperty("java.runtime.name"))) {
            return true;
        }
        try {
            Class.forName("android.util.Log");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

}
