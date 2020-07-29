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

package im.turms.gateway.access.websocket.util;

import com.google.common.net.InetAddresses;
import im.turms.common.constant.DeviceType;
import im.turms.common.constant.UserStatus;
import org.apache.commons.lang3.EnumUtils;
import org.springframework.data.geo.Point;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * @author James Chen
 */
public class HandshakeRequestUtil {

    public static final String REQUEST_ID_FIELD = "rid";
    public static final String USER_ID_FIELD = "uid";
    public static final String PASSWORD_FIELD = "pwd";
    public static final String DEVICE_TYPE_FIELD = "dt";
    public static final String USER_ONLINE_STATUS_FIELD = "us";
    public static final String USER_LOCATION_FIELD = "loc";
    private static final String LOCATION_DELIMITER = ":";
    private static final int LOCATION_FIELDS_NUMBER = 2;
    private static final String X_FORWARDED_FOR = "X-Forwarded-For";

    private HandshakeRequestUtil() {
    }

    // Business

    public static DeviceType parseDeviceTypeFromHeadersOrCookies(ServerHttpRequest request) {
        String deviceType = parseFirstValueFromHeadersOrCookies(request, DEVICE_TYPE_FIELD);
        return deviceType != null
                ? EnumUtils.getEnum(DeviceType.class, deviceType.toUpperCase(), DeviceType.UNKNOWN)
                : DeviceType.UNKNOWN;
    }

    public static Map<String, String> parseDeviceDetailsFromHeaders(@NotNull ServerHttpRequest request) {
        String agent = request.getHeaders().getFirst(HttpHeaders.USER_AGENT);
        return UserAgentUtil.parse(agent);
    }

    /**
     * @return User Agent -> Device Type
     */
    public static DeviceType parseDeviceTypeFromHeadersOrCookies(
            @NotNull ServerHttpRequest request,
            @Nullable Map<String, String> deviceDetails,
            @NotNull Boolean isUseOperatingSystemClassAsDefaultDeviceType) {
        DeviceType deviceType = parseDeviceTypeFromHeadersOrCookies(request);
        if (deviceType == null || deviceType == DeviceType.UNKNOWN) {
            deviceType = deviceDetails != null ? UserAgentUtil.detectDeviceTypeIfUnset(
                    deviceType,
                    deviceDetails,
                    isUseOperatingSystemClassAsDefaultDeviceType) : DeviceType.UNKNOWN;
        }
        return deviceType;
    }

    public static Long parseRequestIdFromHeadersOrCookies(ServerHttpRequest request) {
        return parseLongFromHeadersOrCookies(request, REQUEST_ID_FIELD);
    }

    public static Long parseUserIdFromHeadersOrCookies(ServerHttpRequest request) {
        return parseLongFromHeadersOrCookies(request, USER_ID_FIELD);
    }

    public static String parsePasswordFromHeadersOrCookies(ServerHttpRequest request) {
        String password = parseFirstValueFromHeadersOrCookies(request, PASSWORD_FIELD);
        if (password != null) {
            password = URLDecoder.decode(password, StandardCharsets.UTF_8);
        }
        return password;
    }

    public static UserStatus parseUserStatusFromHeadersOrCookies(ServerHttpRequest request) {
        String userStatus = parseFirstValueFromHeadersOrCookies(request, USER_ONLINE_STATUS_FIELD);
        if (userStatus != null) {
            return EnumUtils.getEnum(UserStatus.class, userStatus, UserStatus.AVAILABLE);
        } else {
            return null;
        }
    }

    public static Point parseLocationFromHeadersOrCookies(ServerHttpRequest request) {
        String location = parseFirstValueFromHeadersOrCookies(request, USER_LOCATION_FIELD);
        if (location != null) {
            String[] split = StringUtils.split(location, LOCATION_DELIMITER);
            if (split != null && split.length == LOCATION_FIELDS_NUMBER) {
                try {
                    return new Point(Float.parseFloat(split[0]), Float.parseFloat(split[1]));
                } catch (Exception e) {
                    return null;
                }
            }
        }
        return null;
    }

    public static String parseIp(ServerHttpRequest request) {
        String ip = request.getHeaders().getFirst(X_FORWARDED_FOR);
        if (ip == null) {
            InetSocketAddress address = request.getRemoteAddress();
            if (address != null) {
                ip = address.getHostString();
            }
        }
        return ip != null && InetAddresses.isInetAddress(ip) ? ip : null;
    }

    // Base

    private static String parseFirstValueFromHeadersOrCookies(ServerHttpRequest request, String key) {
        String value = request.getHeaders().getFirst(key);
        if (value == null) {
            value = parseFirstCookieValue(request.getCookies(), key);
        }
        return value;
    }

    private static String parseFirstCookieValue(MultiValueMap<String, HttpCookie> cookies, String key) {
        if (cookies != null && !cookies.isEmpty() && key != null && !key.isBlank()) {
            HttpCookie cookie = cookies.getFirst(key);
            if (cookie != null) {
                return cookie.getValue();
            }
        }
        return null;
    }

    private static Long parseLongFromHeadersOrCookies(ServerHttpRequest request, String fieldName) {
        String longValue = parseFirstValueFromHeadersOrCookies(request, fieldName);
        try {
            if (longValue != null && !longValue.isBlank()) {
                return Long.parseLong(longValue);
            }
            return null;
        } catch (NumberFormatException exception) {
            return null;
        }
    }

}
