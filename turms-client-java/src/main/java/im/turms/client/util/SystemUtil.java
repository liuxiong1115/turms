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
