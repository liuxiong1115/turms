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

package im.turms.turms.util;

import com.github.davidmoten.rtree2.geometry.internal.PointFloat;
import com.google.common.base.Enums;
import com.google.common.net.InetAddresses;
import im.turms.common.constant.DeviceType;
import im.turms.common.constant.UserStatus;
import org.apache.commons.lang3.EnumUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.socket.WebSocketSession;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import static com.google.common.net.HttpHeaders.X_FORWARDED_FOR;

public class SessionUtil {
    private SessionUtil() {
    }

    public static final String REQUEST_ID_FIELD = "rid";
    public static final String USER_ID_FIELD = "uid";
    public static final String PASSWORD_FIELD = "pwd";
    public static final String DEVICE_TYPE_FIELD = "dt";
    public static final String USER_ONLINE_STATUS_FIELD = "us";
    public static final String USER_LOCATION_FIELD = "loc";
    public static final String USER_DEVICE_DETAILS = "dd";
    public static final String IP_FIELD = "ip";
    private static final String LOCATION_SPLIT = ":";

    private static String getFirstValue(ServerHttpRequest request, String key) {
        String value = request.getHeaders().getFirst(key);
        if (value == null) {
            value = getFirstCookieValue(request.getCookies(), key);
        }
        return value;
    }

    private static String getFirstValue(@Nullable Map<String, String> cookies, @Nullable HttpHeaders headers, @NotNull String key) {
        String value = null;
        if (cookies != null) {
            value = cookies.get(key);
        }
        if (value == null && headers != null) {
            value = headers.getFirst(key);
        }
        return value;
    }

    private static String getFirstCookieValue(MultiValueMap<String, HttpCookie> cookies, String key) {
        if (cookies != null && !cookies.isEmpty() && key != null && !key.isBlank()) {
            HttpCookie cookie = cookies.getFirst(key);
            if (cookie != null) {
                return cookie.getValue();
            }
        }
        return null;
    }

    public static Map<String, String> getCookiesFromSession(WebSocketSession session) {
        String cookie = session.getHandshakeInfo().getHeaders().getFirst("Cookie");
        if (cookie == null) {
            return null;
        }
        String[] pairs = cookie.split(";\\s*");
        Map<String, String> keyAndValueMap = new HashMap<>(pairs.length);
        for (String pair : pairs) {
            String[] entry = pair.split("=");
            if (entry.length == 2) {
                keyAndValueMap.put(entry[0], entry[1]);
            }
        }
        return keyAndValueMap;
    }

    public static Long getUserIdFromSession(WebSocketSession session) {
        Object uid = session.getAttributes().get(USER_ID_FIELD);
        if (uid instanceof Long) {
            return (Long) uid;
        } else {
            return null;
        }
    }

    public static DeviceType getDeviceTypeFromSession(WebSocketSession session) {
        Object deviceType = session.getAttributes().get(DEVICE_TYPE_FIELD);
        deviceType = deviceType != null ?
                deviceType :
                session.getHandshakeInfo().getHeaders().getFirst(DEVICE_TYPE_FIELD);
        if (deviceType != null) {
            if (deviceType instanceof DeviceType) {
                return (DeviceType) deviceType;
            } else {
                return EnumUtils.getEnum(DeviceType.class, String.valueOf(deviceType));
            }
        } else {
            return null;
        }
    }

    public static UserStatus getUserOnlineStatusFromSession(WebSocketSession session) {
        Object userOnlineStatus = session.getAttributes().get(USER_ONLINE_STATUS_FIELD);
        userOnlineStatus = userOnlineStatus != null ? userOnlineStatus :
                session.getHandshakeInfo().getHeaders().get(USER_ONLINE_STATUS_FIELD);
        if (userOnlineStatus == null) {
            return null;
        }
        return UserStatus.forNumber(Integer.parseInt((String) userOnlineStatus));
    }

    public static DeviceType getDeviceTypeFromRequest(ServerHttpRequest request) {
        String deviceType = getFirstValue(request, DEVICE_TYPE_FIELD);
        if (deviceType != null) {
            switch (deviceType.toUpperCase()) {
                case "DESKTOP":
                    return DeviceType.DESKTOP;
                case "BROWSER":
                    return DeviceType.BROWSER;
                case "IOS":
                    return DeviceType.IOS;
                case "ANDROID":
                    return DeviceType.ANDROID;
                case "OTHERS":
                    return DeviceType.OTHERS;
                default:
                    return DeviceType.UNKNOWN;
            }
        } else {
            return DeviceType.UNKNOWN;
        }
    }

    public static Pair<String, DeviceType> parseDeviceTypeFromRequest(
            @NotNull ServerHttpRequest request,
            @NotNull Boolean isUseOsAsDefaultDeviceType) {
        DeviceType deviceType = getDeviceTypeFromRequest(request);
        if (deviceType == null || deviceType == DeviceType.UNKNOWN) {
            String agent = request.getHeaders().getFirst("User-Agent");
            if (agent != null && !agent.isEmpty()) {
                Map<String, String> deviceDetails = UserAgentUtil.parse(agent);
                deviceType = UserAgentUtil.detectDeviceTypeIfUnset(
                        deviceType,
                        deviceDetails,
                        isUseOsAsDefaultDeviceType);
            } else {
                deviceType = DeviceType.UNKNOWN;
            }
            return Pair.of(agent, deviceType);
        } else {
            return Pair.of(null, deviceType);
        }
    }

    public static String getUserStatusFromRequest(ServerHttpRequest request) {
        return getFirstValue(request, USER_ONLINE_STATUS_FIELD);
    }

    public static void putUserId(Map<String, Object> attributes, Long userId) {
        attributes.put(USER_ID_FIELD, userId);
    }

    public static void putUserOnlineStatus(Map<String, Object> attributes, UserStatus userOnlineStatus) {
        attributes.put(USER_ONLINE_STATUS_FIELD, userOnlineStatus);
    }

    public static void putDeviceType(@NotNull Map<String, Object> attributes, DeviceType deviceType) {
        attributes.put(DEVICE_TYPE_FIELD, deviceType);
    }

    public static void putIp(@NotNull Map<String, Object> attributes, Integer ip) {
        attributes.put(IP_FIELD, ip);
    }

    public static Integer getIp(@NotNull WebSocketSession session) {
        Object ip = session.getAttributes().get(IP_FIELD);
        if (ip != null) {
            return (Integer) ip;
        } else {
            return null;
        }
    }

    public static Long getRequestIdFromRequest(ServerHttpRequest request) {
        return getLongFromRequest(request, REQUEST_ID_FIELD);
    }

    public static Long getUserIdFromRequest(ServerHttpRequest request) {
        return getLongFromRequest(request, USER_ID_FIELD);
    }

    private static Long getLongFromRequest(ServerHttpRequest request, String fieldName) {
        String longValue = getFirstValue(request, fieldName);
        try {
            if (longValue != null && !longValue.isBlank()) {
                return Long.parseLong(longValue);
            }
            return null;
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    public static String getPasswordFromRequest(ServerHttpRequest request) {
        String password = getFirstValue(request, PASSWORD_FIELD);
        if (password != null) {
            password = URLDecoder.decode(password, StandardCharsets.UTF_8);
        }
        return password;
    }

    public static String getLocationFromRequest(ServerHttpRequest request) {
        return getFirstValue(request, USER_LOCATION_FIELD);
    }

    public static PointFloat getLocationFromSession(WebSocketSession session) {
        Object location = session.getAttributes().get(USER_LOCATION_FIELD);
        if (location != null) {
            return (PointFloat) location;
        } else {
            location = session.getHandshakeInfo().getHeaders().getFirst(USER_LOCATION_FIELD);
            if (location != null) {
                String[] split = location.toString().split(":");
                if (split.length == 2) {
                    return PointFloat.create(Float.parseFloat(split[0]), Float.parseFloat(split[1]));
                } else {
                    return null;
                }
            } else {
                return null;
            }
        }
    }

    public static UserStatus getUserStatusFromSession(WebSocketSession session) {
        Object userStatus = session.getAttributes().get(USER_ONLINE_STATUS_FIELD);
        if (userStatus != null) {
            return (UserStatus) userStatus;
        } else {
            userStatus = session.getHandshakeInfo().getHeaders().getFirst(USER_ONLINE_STATUS_FIELD);
            if (userStatus != null) {
                return EnumUtils.getEnum(UserStatus.class, userStatus.toString());
            } else {
                return null;
            }
        }
    }

    public static void putOnlineUserInfoToSession(WebSocketSession session, Long userId, UserStatus userStatus, DeviceType deviceType, PointFloat userLocation) {
        if (userId != null) {
            session.getAttributes().put(USER_ID_FIELD, userId);
        }
        if (userStatus != null) {
            session.getAttributes().put(USER_ONLINE_STATUS_FIELD, userStatus);
        }
        if (deviceType != null) {
            session.getAttributes().put(DEVICE_TYPE_FIELD, deviceType);
        }
        if (userLocation != null) {
            session.getAttributes().put(USER_LOCATION_FIELD, userLocation);
        }
    }

    public static Long getUserIdFromCookiesOrHeaders(@Nullable Map<String, String> cookies, @Nullable HttpHeaders headers) {
        String userId = getFirstValue(cookies, headers, USER_ID_FIELD);
        if (userId != null) {
            return Long.parseLong(userId);
        } else {
            return null;
        }
    }

    public static DeviceType getDeviceTypeFromCookiesAndHeaders(@Nullable Map<String, String> cookies, @Nullable HttpHeaders headers) {
        String deviceType = getFirstValue(cookies, headers, DEVICE_TYPE_FIELD);
        if (deviceType != null) {
            return Enums.getIfPresent(DeviceType.class, deviceType).or(DeviceType.UNKNOWN);
        } else {
            return DeviceType.UNKNOWN;
        }
    }

    public static UserStatus getUserStatusFromCookiesAndHeaders(@Nullable Map<String, String> cookies, @Nullable HttpHeaders headers) {
        String userStatus = getFirstValue(cookies, headers, USER_ONLINE_STATUS_FIELD);
        if (userStatus != null) {
            return Enums.getIfPresent(UserStatus.class, userStatus).or(UserStatus.AVAILABLE);
        } else {
            return UserStatus.AVAILABLE;
        }
    }

    public static PointFloat getLocationFromCookiesAndHeaders(@Nullable Map<String, String> cookies, @Nullable HttpHeaders headers) {
        String location = getFirstValue(cookies, headers, USER_LOCATION_FIELD);
        if (location != null) {
            String[] split = location.split(LOCATION_SPLIT);
            if (split.length == 2) {
                try {
                    return PointFloat.create(Float.parseFloat(split[0]), Float.parseFloat(split[1]));
                } catch (Exception e) {
                    return null;
                }
            }
        }
        return null;
    }

    public static Integer parseIp(WebSocketSession session) {
        InetSocketAddress ip = session.getHandshakeInfo().getRemoteAddress();
        String ipStr = ip != null ?
                ip.getHostString() :
                session.getHandshakeInfo().getHeaders().getFirst(X_FORWARDED_FOR);
        if (ipStr != null) {
            try {
                return InetAddresses.coerceToInteger(InetAddresses.forString(ipStr));
            } catch (Exception e) {
                return null;
            }
        } else {
            return null;
        }
    }
}
