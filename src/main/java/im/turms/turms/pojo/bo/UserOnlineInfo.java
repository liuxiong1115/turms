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

package im.turms.turms.pojo.bo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import im.turms.turms.constant.DeviceType;
import im.turms.turms.constant.UserStatus;
import im.turms.turms.service.user.onlineuser.OnlineUserManager;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
// Warning: Always to add Serializable to DeviceType & UserStatus after recompiling proto files
public class UserOnlineInfo implements Serializable {
    private Long userId;
    private UserStatus userStatus;
    private ConcurrentMap<DeviceType, OnlineUserManager.Session> sessionMap;

    @JsonIgnore
    public UserStatus getUserStatusForClients() {
        if (userStatus == UserStatus.INVISIBLE) {
            return UserStatus.OFFLINE;
        } else {
            return userStatus;
        }
    }

    @JsonIgnore
    public Set<DeviceType> getUsingDeviceTypes() {
        if (sessionMap != null) {
            return sessionMap.keySet();
        } else {
            return Collections.emptySet();
        }
    }
}