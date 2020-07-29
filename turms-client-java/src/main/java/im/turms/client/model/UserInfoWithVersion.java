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

import com.google.protobuf.Int64Value;
import im.turms.common.model.bo.user.UserInfo;
import im.turms.common.model.bo.user.UsersInfosWithVersion;
import im.turms.common.model.dto.notification.TurmsNotification;

public class UserInfoWithVersion {
    private UserInfo userInfo;
    private long lastUpdatedDate;

    public static UserInfoWithVersion from(TurmsNotification notification) {
        if (notification != null && notification.hasData()) {
            TurmsNotification.Data data = notification.getData();
            if (data.hasUsersInfosWithVersion()) {
                UsersInfosWithVersion usersInfosWithVersion = data.getUsersInfosWithVersion();
                UserInfoWithVersion userInfoWithVersion = new UserInfoWithVersion();
                if (usersInfosWithVersion.getUserInfosCount() > 0) {
                    userInfoWithVersion.setUserInfo(usersInfosWithVersion.getUserInfos(0));
                }
                Int64Value lastUpdatedDate = usersInfosWithVersion.getLastUpdatedDate();
                if (lastUpdatedDate != null) {
                    userInfoWithVersion.setLastUpdatedDate(lastUpdatedDate.getValue());
                }
                return userInfoWithVersion;
            }
        }
        return null;
    }

    public UserInfo getUserInfo() {
        return userInfo;
    }

    public void setUserInfo(UserInfo userInfo) {
        this.userInfo = userInfo;
    }

    public long getLastUpdatedDate() {
        return lastUpdatedDate;
    }

    public void setLastUpdatedDate(long lastUpdatedDate) {
        this.lastUpdatedDate = lastUpdatedDate;
    }
}
