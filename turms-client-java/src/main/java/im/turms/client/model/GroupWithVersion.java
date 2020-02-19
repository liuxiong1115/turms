package im.turms.client.model;

import com.google.protobuf.Int64Value;
import im.turms.common.model.bo.group.Group;
import im.turms.common.model.bo.group.GroupsWithVersion;
import im.turms.common.model.dto.notification.TurmsNotification;

public class GroupWithVersion {
    private Group group;
    private long lastUpdatedDate;

    public static GroupWithVersion from(TurmsNotification notification) {
        if (notification != null) {
            TurmsNotification.Data data = notification.getData();
            if (notification.hasData()) {
                GroupsWithVersion groupsWithVersion = data.getGroupsWithVersion();
                if (data.hasGroupsWithVersion()) {
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
