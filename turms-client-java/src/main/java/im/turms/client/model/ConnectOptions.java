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

package im.turms.client.model;

import im.turms.common.constant.DeviceType;
import im.turms.common.constant.UserStatus;

/**
 * @author James Chen
 */
public final class ConnectOptions {

    private String wsUrl;
    private Integer connectTimeout;

    private Long userId;
    private String password;
    private DeviceType deviceType;
    private UserStatus userOnlineStatus;
    private UserLocation location;

    public String wsUrl() {
        return wsUrl;
    }

    public Integer connectTimeout() {
        return connectTimeout;
    }

    public Long userId() {
        return userId;
    }

    public String password() {
        return password;
    }

    public DeviceType deviceType() {
        return deviceType;
    }

    public UserStatus userOnlineStatus() {
        return userOnlineStatus;
    }

    public UserLocation location() {
        return location;
    }

    public ConnectOptions wsUrl(String wsUrl) {
        this.wsUrl = wsUrl;
        return this;
    }

    public ConnectOptions connectTimeout(Integer connectTimeout) {
        this.connectTimeout = connectTimeout;
        return this;
    }

    public ConnectOptions userId(Long userId) {
        this.userId = userId;
        return this;
    }

    public ConnectOptions password(String password) {
        this.password = password;
        return this;
    }

    public ConnectOptions deviceType(DeviceType deviceType) {
        this.deviceType = deviceType;
        return this;
    }

    public ConnectOptions userOnlineStatus(UserStatus userOnlineStatus) {
        this.userOnlineStatus = userOnlineStatus;
        return this;
    }

    public ConnectOptions location(UserLocation location) {
        this.location = location;
        return this;
    }

}
