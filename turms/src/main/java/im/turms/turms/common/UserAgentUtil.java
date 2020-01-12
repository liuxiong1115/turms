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

package im.turms.turms.common;

import im.turms.turms.constant.DeviceType;
import nl.basjes.parse.useragent.UserAgent;
import nl.basjes.parse.useragent.UserAgentAnalyzer;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class UserAgentUtil {
    private static final Set<String> REQUIRED_FIELDS = Set.of(
            UserAgent.DEVICE_NAME,
            UserAgent.DEVICE_BRAND,
            UserAgent.OPERATING_SYSTEM_CLASS,
            UserAgent.OPERATING_SYSTEM_NAME,
            UserAgent.OPERATING_SYSTEM_VERSION_MAJOR,
            UserAgent.AGENT_CLASS,
            UserAgent.AGENT_NAME,
            UserAgent.AGENT_VERSION_MAJOR);

    private static final Map<String, String> KEY_MAPPER = Map.of(
            UserAgent.DEVICE_NAME, "DN",
            UserAgent.DEVICE_BRAND, "DB",
            UserAgent.OPERATING_SYSTEM_CLASS, "OC",
            UserAgent.OPERATING_SYSTEM_NAME, "ON",
            UserAgent.OPERATING_SYSTEM_VERSION_MAJOR, "OV",
            UserAgent.AGENT_CLASS, "AC",
            UserAgent.AGENT_NAME, "AN",
            UserAgent.AGENT_VERSION_MAJOR, "AV");

    private static final UserAgentAnalyzer agentAnalyzer = UserAgentAnalyzer
            .newBuilder()
            .hideMatcherLoadStats()
            .withFields(REQUIRED_FIELDS)
            .build();

    public static Map<String, String> parse(@NotNull String agent) {
        Map<String, String> map = new HashMap<>(REQUIRED_FIELDS.size());
        UserAgent userAgent = agentAnalyzer.parse(agent);
        for (String field : REQUIRED_FIELDS) {
            putIfNotDefault(map, userAgent, field);
        }
        return map;
    }

    public static DeviceType detectDeviceTypeIfUnset(
            @Nullable DeviceType deviceType,
            @NotNull Map<String, String> deviceDetails,
            boolean useOsAsDefaultDeviceType) {
        if (deviceType == null || deviceType == DeviceType.UNKNOWN) {
            if (useOsAsDefaultDeviceType) {
                String osClass = deviceDetails.get(KEY_MAPPER.get(UserAgent.OPERATING_SYSTEM_CLASS));
                if (osClass != null) {
                    switch (osClass) {
                        case "Mobile":
                            if (deviceDetails.get(KEY_MAPPER.get(UserAgent.OPERATING_SYSTEM_NAME)).equals("Android")) {
                                deviceType = DeviceType.ANDROID;
                            } else {
                                deviceType = DeviceType.IOS;
                            }
                            break;
                        case "Desktop":
                            deviceType = DeviceType.DESKTOP;
                            break;
                        default:
                            deviceType = DeviceType.OTHERS;
                            break;
                    }
                } else {
                    deviceType = DeviceType.OTHERS;
                }
            } else {
                deviceType = DeviceType.BROWSER;
            }
        }
        return deviceType;
    }

    private static Map<String, String> putIfNotDefault(
            Map<String, String> map,
            UserAgent userAgent,
            String fieldName) {
        if (userAgent.getConfidence(fieldName) >= 0) {
            String value = userAgent.getValue(fieldName);
            map.put(KEY_MAPPER.get(fieldName), value);
        }
        return map;
    }
}
