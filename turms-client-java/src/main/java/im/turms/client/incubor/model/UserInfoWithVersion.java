package im.turms.client.incubor.model;

import com.google.protobuf.Int64Value;
import im.turms.turms.pojo.bo.user.UserInfo;
import im.turms.turms.pojo.bo.user.UsersInfosWithVersion;
import im.turms.turms.pojo.notification.TurmsNotification;
import lombok.Data;

@Data
public class UserInfoWithVersion {
    private UserInfo userInfo;
    private long lastUpdatedDate;

    public static UserInfoWithVersion from(TurmsNotification notification) {
        if (notification != null) {
            TurmsNotification.Data data = notification.getData();
            if (data != null) {
                UsersInfosWithVersion usersInfosWithVersion = data.getUsersInfosWithVersion();
                if (usersInfosWithVersion != null) {
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
        }
        return null;
    }
}
