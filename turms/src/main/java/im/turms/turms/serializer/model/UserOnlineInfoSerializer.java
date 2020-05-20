package im.turms.turms.serializer.model;

import com.hazelcast.internal.nio.Bits;
import com.hazelcast.nio.ObjectDataInput;
import com.hazelcast.nio.ObjectDataOutput;
import com.hazelcast.nio.serialization.StreamSerializer;
import im.turms.common.constant.DeviceType;
import im.turms.common.constant.UserStatus;
import im.turms.turms.config.hazelcast.IdentifiedDataFactory;
import im.turms.turms.pojo.bo.UserOnlineInfo;
import im.turms.turms.pojo.domain.UserLocation;
import im.turms.turms.service.user.onlineuser.manager.OnlineUserManager;

import java.io.IOException;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;


public class UserOnlineInfoSerializer implements StreamSerializer<UserOnlineInfo> {

    @Override
    public void write(ObjectDataOutput out, UserOnlineInfo object) throws IOException {
        // User ID
        out.writeLong(object.getUserId());

        // Schema + the existence of UserLocation
        Map<DeviceType, OnlineUserManager.Session> sessionMap = object.getSessionMap();
        // The most significant bit for determining whether there is any UserLocation
        // The next 3 bits for determining the size of sessions
        // The last 4 bits for determining the user status
        byte schema = (byte) object.getUserStatus().getNumber();
        schema |= object.getSessionMap().size() << 4;

        byte userLocationExistenceBits = 0;
        Collection<OnlineUserManager.Session> sessions = sessionMap.values();
        int index = 0;
        for (OnlineUserManager.Session session : sessions) {
            if (session.getLocation() != null) {
                userLocationExistenceBits = Bits.setBit(userLocationExistenceBits, index);
            }
            index++;
        }

        if (userLocationExistenceBits > 0) {
            schema |= 1 << 7;
        }
        out.writeByte(schema);
        out.writeByte(userLocationExistenceBits);

        // Session
        for (OnlineUserManager.Session session : sessions) {
            out.writeByte(session.getDeviceType().getNumber());
            out.writeLong(session.getLoginDate().getTime());
            if (session.getLocation() != null) {
                float[] coordinates = session.getLocation().getCoordinates();
                out.writeFloat(coordinates[0]);
                out.writeFloat(coordinates[1]);
                out.writeLong(session.getLocation().getTimestamp().getTime());
            }
        }
    }

    @Override
    public UserOnlineInfo read(ObjectDataInput in) throws IOException {
        long userId = in.readLong();
        byte schema = in.readByte();
        boolean hasAnyUserLocation = (schema & 0b1000_0000) > 0;
        int sessionSize = (schema & 0b0111_0000) >> 4;
        UserStatus userStatus = UserStatus.forNumber(schema & 0b0000_1111);

        byte userLocationExistenceBits = 0;
        if (hasAnyUserLocation) {
            userLocationExistenceBits = in.readByte();
        }

        Map<DeviceType, OnlineUserManager.Session> sessionMap = new HashMap<>(sessionSize);
        for (int i = 0; i < sessionSize; i++) {
            DeviceType deviceType = DeviceType.forNumber(in.readByte() & 0xFF);
            Date loginDate = new Date(in.readLong());
            float[] coordinates = null;
            Date timestamp = null;
            boolean hasUserLocation = Bits.isBitSet(userLocationExistenceBits, i);
            if (hasUserLocation) {
                coordinates = new float[]{in.readFloat(), in.readFloat()};
                timestamp = new Date(in.readLong());
            }
            UserLocation userLocation = new UserLocation(null, null, null, coordinates, null, null, timestamp);
            sessionMap.put(deviceType, new OnlineUserManager.Session(deviceType, loginDate, userLocation, null, null, null, null, 0));
        }
        return new UserOnlineInfo(userId, userStatus, sessionMap);
    }

    @Override
    public int getTypeId() {
        return IdentifiedDataFactory.Type.BO_USER_ONLINE_INFO.getValue();
    }

}
