package im.turms.client.incubator.model;

import com.google.protobuf.Int64Value;
import im.turms.turms.pojo.bo.user.UserInfo;
import im.turms.turms.pojo.bo.user.UsersInfosWithVersion;
import im.turms.turms.pojo.notification.TurmsNotification;

public class UserInfoWithVersion {
    private UserInfo userInfo;
    private long lastUpdatedDate;

    public static UserInfoWithVersion from(TurmsNotification notification) {
        if (notification != null) {
            TurmsNotification.Data data = notification.getData();
            if (notification.hasData()) {
                UsersInfosWithVersion usersInfosWithVersion = data.getUsersInfosWithVersion();
                if (data.hasUsersInfosWithVersion()) {
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
