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

package im.turms.turms.service.user;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.SetMultimap;
import im.turms.turms.cluster.TurmsClusterManager;
import im.turms.turms.constant.DeviceType;
import im.turms.turms.pojo.bo.UserOnlineInfo;
import im.turms.turms.property.business.User;
import im.turms.turms.service.user.onlineuser.OnlineUserManager;
import im.turms.turms.service.user.onlineuser.OnlineUserService;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.socket.CloseStatus;
import reactor.core.publisher.Mono;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.HashSet;
import java.util.Set;

@Service
public class UserSimultaneousLoginService {
    private final TurmsClusterManager turmsClusterManager;
    private final OnlineUserService onlineUserService;
    // device type -> exclusive device types
    private SetMultimap<DeviceType, DeviceType> exclusiveDeviceTypes;
    private Set<DeviceType> forbiddenDeviceTypes;

    public UserSimultaneousLoginService(
            @Lazy OnlineUserService onlineUserService,
            TurmsClusterManager turmsClusterManager) {
        this.onlineUserService = onlineUserService;
        this.turmsClusterManager = turmsClusterManager;
        exclusiveDeviceTypes = HashMultimap.create();
        forbiddenDeviceTypes = new HashSet<>();
        applyStrategy(this.turmsClusterManager.getTurmsProperties()
                .getUser()
                .getSimultaneousLogin()
                .getStrategy());
    }

    public Mono<Boolean> setConflictedDevicesOffline(
            @NotNull Long userId,
            @NotNull DeviceType deviceType) {
        return onlineUserService.setUserDevicesOffline(
                userId,
                getConflictedDeviceTypes(deviceType),
                CloseStatus.POLICY_VIOLATION);
    }

    public void applyStrategy(@NotNull User.SimultaneousLogin.SimultaneousLoginStrategy strategy) {
        forbiddenDeviceTypes.clear();
        exclusiveDeviceTypes.clear();
        boolean allowUnknownDevice = turmsClusterManager.getTurmsProperties()
                .getUser()
                .getSimultaneousLogin()
                .isAllowUnknownDeviceCoexistsWithKnownDevice();
        // Every device type conflicts with itself
        for (DeviceType deviceType : DeviceType.values()) {
            exclusiveDeviceTypes.put(deviceType, deviceType);
            if (!allowUnknownDevice) {
                addConflictedDeviceTypes(DeviceType.UNKNOWN, deviceType);
                addConflictedDeviceTypes(DeviceType.OTHERS, deviceType);
            }
        }
        switch (strategy) {
            case ALLOW_ONE_DEVICE_OF_ONE_DEVICE_TYPE_ONLINE:
                for (DeviceType type : DeviceType.values()) {
                    addDeviceTypeConflictedWithAllTypes(type);
                }
                break;
            case ALLOW_ONE_DEVICE_OF_EVERY_DEVICE_TYPE_ONLINE:
                break;
            case ALLOW_ONE_DEVICE_OF_DESKTOP_AND_ONE_DEVICE_OF_MOBILE_ONLINE:
                forbiddenDeviceTypes.add(DeviceType.BROWSER);
                addConflictedDeviceTypes(DeviceType.ANDROID, DeviceType.IOS);
                break;
            case ALLOW_ONE_DEVICE_OF_DESKTOP_OR_WEB_AND_ONE_DEVICE_OF_MOBILE_ONLINE:
                addConflictedDeviceTypes(DeviceType.DESKTOP, DeviceType.BROWSER);
                addConflictedDeviceTypes(DeviceType.ANDROID, DeviceType.IOS);
                break;
            case ALLOW_ONE_DEVICE_OF_DESKTOP_AND_ONE_DEVICE_OF_WEB_AND_ONE_DEVICE_OF_MOBILE_ONLINE:
                addConflictedDeviceTypes(DeviceType.ANDROID, DeviceType.IOS);
                break;
            case ALLOW_ONE_DEVICE_OF_DESKTOP_OR_MOBILE_ONLINE:
                forbiddenDeviceTypes.add(DeviceType.BROWSER);
                addConflictedDeviceTypes(DeviceType.DESKTOP, DeviceType.ANDROID);
                addConflictedDeviceTypes(DeviceType.DESKTOP, DeviceType.IOS);
                addConflictedDeviceTypes(DeviceType.ANDROID, DeviceType.IOS);
                break;
            case ALLOW_ONE_DEVICE_OF_DESKTOP_OR_WEB_OR_MOBILE_ONLINE:
                addConflictedDeviceTypes(DeviceType.DESKTOP, DeviceType.BROWSER);
                addConflictedDeviceTypes(DeviceType.DESKTOP, DeviceType.ANDROID);
                addConflictedDeviceTypes(DeviceType.DESKTOP, DeviceType.IOS);
                addConflictedDeviceTypes(DeviceType.BROWSER, DeviceType.ANDROID);
                addConflictedDeviceTypes(DeviceType.BROWSER, DeviceType.IOS);
                addConflictedDeviceTypes(DeviceType.ANDROID, DeviceType.IOS);
                break;
            default:
                break;
        }
    }

    private void addDeviceTypeConflictedWithAllTypes(@NotNull DeviceType deviceType) {
        for (DeviceType type : DeviceType.values()) {
            addConflictedDeviceTypes(deviceType, type);
        }
    }

    private void addConflictedDeviceTypes(
            @NotNull DeviceType deviceTypeOne,
            @NotNull DeviceType deviceTypeTwo) {
        exclusiveDeviceTypes.put(deviceTypeOne, deviceTypeTwo);
        exclusiveDeviceTypes.put(deviceTypeTwo, deviceTypeOne);
    }

    private Set<DeviceType> getConflictedDeviceTypes(@NotNull DeviceType deviceType) {
        return exclusiveDeviceTypes.get(deviceType);
    }

    private boolean isConflicted(
            @NotEmpty Set<DeviceType> loggedDeviceTypes,
            @NotNull DeviceType loggingDeviceType) {
        Set<DeviceType> conflictedDeviceTypes = getConflictedDeviceTypes(loggingDeviceType);
        for (DeviceType loggedDeviceType : loggedDeviceTypes) {
            if (conflictedDeviceTypes.contains(loggedDeviceType)) {
                return true;
            }
        }
        return false;
    }

    public boolean isDeviceTypeAllowedToLogin(@NotNull Long userId, @NotNull DeviceType loggingDeviceType) {
        OnlineUserManager onlineUserManager = onlineUserService.getLocalOnlineUserManager(userId);
        if (onlineUserManager == null) {
            return true;
        } else if (forbiddenDeviceTypes.contains(loggingDeviceType)) {
            return false;
        }
        UserOnlineInfo onlineUserInfo = onlineUserManager.getOnlineUserInfo();
        Set<DeviceType> usingDeviceTypes = onlineUserInfo.getSessionMap().keySet();
        User.SimultaneousLogin.ConflictStrategy strategy = turmsClusterManager
                .getTurmsProperties()
                .getUser()
                .getSimultaneousLogin()
                .getConflictStrategy();
        return !isConflicted(usingDeviceTypes, loggingDeviceType) ||
                strategy != User.SimultaneousLogin.ConflictStrategy.LOGGING_DEVICE_OFFLINE;
    }
}
