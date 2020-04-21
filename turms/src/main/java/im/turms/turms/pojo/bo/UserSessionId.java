package im.turms.turms.pojo.bo;

import im.turms.common.constant.DeviceType;
import lombok.AllArgsConstructor;
import lombok.Data;

@AllArgsConstructor
@Data
public final class UserSessionId {
    private final long userId;
    private final DeviceType deviceType;
}
