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

import com.github.davidmoten.rtree2.geometry.internal.PointFloat;
import com.google.common.base.Enums;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import im.turms.turms.constant.DeviceType;
import im.turms.turms.constant.UserStatus;
import org.apache.commons.lang3.EnumUtils;
import org.springframework.http.HttpCookie;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.socket.WebSocketSession;

import javax.validation.constraints.NotNull;
import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkNotNull;

@Component
public class SessionUtil {
    private static final String USER_ID_FIELD = "uid";
    private static final String PASSWORD_FIELD = "pwd";
    private static final String DEVICE_TYPE_FIELD = "dt";
    private static final String USER_ONLINE_STATUS_FIELD = "us";
    private static final String USER_LOCATION_FIELD = "loc";
    private static final String LOCATION_SPLIT = ":";
    private static final String USER_DEVICE_DETAILS = "dd";

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
        return Arrays.stream(cookie.split(";\\s*"))
                .map(s -> s.split("="))
                .collect(Collectors.toMap(s -> s[0], s -> s[1]));
    }

    public static Long getUserIdFromSession(WebSocketSession session) {
        Object uid = session.getAttributes().get(USER_ID_FIELD);
        if (uid != null) {
            return Long.parseLong(String.valueOf(uid));
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
            return EnumUtils.getEnum(DeviceType.class, String.valueOf(deviceType));
        } else {
            return null;
        }
    }

    public static UserStatus getUserOnlineStatusFromSession(WebSocketSession session) throws IOException {
        Object userOnlineStatus = session.getAttributes().get(USER_ONLINE_STATUS_FIELD);
        userOnlineStatus = userOnlineStatus != null ? userOnlineStatus :
                session.getHandshakeInfo().getHeaders().get(USER_ONLINE_STATUS_FIELD);
        if (userOnlineStatus == null) {
            return null;
        }
        return UserStatus.forNumber(Integer.parseInt((String) userOnlineStatus));

    }

    public static DeviceType getDeviceTypeFromRequest(ServerHttpRequest request) {
        String deviceType = getFirstCookieValue(request.getCookies(), DEVICE_TYPE_FIELD);
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

    public static String getUserStatusFromRequest(ServerHttpRequest request) {
        return getFirstCookieValue(request.getCookies(), USER_ONLINE_STATUS_FIELD);
    }

    public static void putUserId(Map<String, Object> attributes, Long userId) {
        attributes.put(USER_ID_FIELD, userId);
    }

    public static void putUserOnlineStatus(Map<String, Object> attributes, UserStatus userOnlineStatus) {
        attributes.put(USER_ONLINE_STATUS_FIELD, userOnlineStatus);
    }

    public static void putDeviceType(@NotNull Map<String, Object> attributes, DeviceType deviceType) {
        checkNotNull(attributes);
        attributes.put(DEVICE_TYPE_FIELD, deviceType);
    }

    public static Long getUserIdFromRequest(ServerHttpRequest request) {
        String uid = getFirstCookieValue(request.getCookies(), USER_ID_FIELD);
        try {
            if (uid != null && !uid.isBlank()) {
                return Long.parseLong(uid);
            } else {
                uid = request.getHeaders().getFirst("uid");
                if (uid != null && !uid.isBlank()) {
                    return Long.parseLong(uid);
                }
            }
            return null;
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    public static String getPasswordFromRequest(ServerHttpRequest request) {
        String password = getFirstCookieValue(request.getCookies(), PASSWORD_FIELD);
        if (password == null || password.isBlank()) {
            password = request.getHeaders().getFirst(PASSWORD_FIELD);
        }
        return password;
    }

    public static String getLocationFromRequest(ServerHttpRequest request) {
        return getFirstCookieValue(request.getCookies(), USER_LOCATION_FIELD);
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

    public static Long getUserIdFromCookies(Map<String, String> cookies) {
        String userId = cookies.get(USER_ID_FIELD);
        if (userId != null) {
            return Long.parseLong(userId);
        } else {
            return null;
        }
    }

    public static DeviceType getDeviceTypeFromCookies(Map<String, String> cookies) {
        String deviceType = cookies.get(DEVICE_TYPE_FIELD);
        if (deviceType != null) {
            return Enums.getIfPresent(DeviceType.class, deviceType).or(DeviceType.UNKNOWN);
        } else {
            return DeviceType.UNKNOWN;
        }
    }

    public static UserStatus getUserStatusFromCookies(Map<String, String> cookies) {
        String userStatus = cookies.get(USER_ONLINE_STATUS_FIELD);
        if (userStatus != null) {
            return Enums.getIfPresent(UserStatus.class, userStatus).or(UserStatus.AVAILABLE);
        } else {
            return UserStatus.AVAILABLE;
        }
    }

    public static PointFloat getLocationFromCookies(Map<String, String> cookies) {
        String location = cookies.get(USER_LOCATION_FIELD);
        if (location != null) {
            String[] split = location.split(LOCATION_SPLIT);
            if (split.length == 2) {
                return PointFloat.create(Float.parseFloat(split[0]), Float.parseFloat(split[1]));
            }
        }
        return null;
    }

    public static DBObject getDeviceDetailsFromCookies(Map<String, String> cookies) {
        String deviceDetails = cookies.get(USER_DEVICE_DETAILS);
        if (deviceDetails != null) {
            try {
                return BasicDBObject.parse(URLDecoder.decode(deviceDetails, StandardCharsets.UTF_8));
            } catch (Exception e) {
                return new BasicDBObject();
            }
        } else {
            return new BasicDBObject();
        }
    }
}
