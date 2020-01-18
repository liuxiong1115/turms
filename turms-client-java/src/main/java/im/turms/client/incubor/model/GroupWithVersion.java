package im.turms.client.incubor.model;

import com.google.protobuf.Int64Value;
import im.turms.turms.pojo.bo.group.Group;
import im.turms.turms.pojo.bo.group.GroupsWithVersion;
import im.turms.turms.pojo.notification.TurmsNotification;
import lombok.Data;

@Data
public class GroupWithVersion {
    private Group group;
    private long lastUpdatedDate;

    public static GroupWithVersion from(TurmsNotification notification) {
        if (notification != null) {
            TurmsNotification.Data data = notification.getData();
            if (data != null) {
                GroupsWithVersion groupsWithVersion = data.getGroupsWithVersion();
                if (groupsWithVersion != null) {
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
}
