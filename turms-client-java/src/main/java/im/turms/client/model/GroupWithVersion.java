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
import im.turms.common.model.bo.group.Group;
import im.turms.common.model.bo.group.GroupsWithVersion;
import im.turms.common.model.dto.notification.TurmsNotification;

import javax.annotation.Nullable;

/**
 * @author James Chen
 */
public class GroupWithVersion {

    private Group group;
    private long lastUpdatedDate;

    public static GroupWithVersion from(@Nullable TurmsNotification notification) {
        if (notification != null) {
            TurmsNotification.Data data = notification.getData();
            if (notification.hasData() && data.hasGroupsWithVersion()) {
                GroupsWithVersion groupsWithVersion = data.getGroupsWithVersion();
                GroupWithVersion groupWithVersion = new GroupWithVersion();
                if (groupsWithVersion.getGroupsCount() > 0) {
                    groupWithVersion.setGroup(groupsWithVersion.getGroups(0));
                }
                Int64Value lastUpdatedDate = groupsWithVersion.getLastUpdatedDate();
                if (lastUpdatedDate != null) {
                    groupWithVersion.setLastUpdatedDate(lastUpdatedDate.getValue());
                }
                return groupWithVersion;
            }
        }
        return null;
    }

    public Group getGroup() {
        return group;
    }

    public void setGroup(Group group) {
        this.group = group;
    }

    public long getLastUpdatedDate() {
        return lastUpdatedDate;
    }

    public void setLastUpdatedDate(long lastUpdatedDate) {
        this.lastUpdatedDate = lastUpdatedDate;
    }

}
