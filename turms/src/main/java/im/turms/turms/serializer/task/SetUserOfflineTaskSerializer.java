package im.turms.turms.serializer.task;

import com.hazelcast.internal.nio.Bits;
import com.hazelcast.nio.ObjectDataInput;
import com.hazelcast.nio.ObjectDataOutput;
import com.hazelcast.nio.serialization.StreamSerializer;
import im.turms.common.constant.DeviceType;
import im.turms.turms.config.hazelcast.IdentifiedDataFactory;
import im.turms.turms.task.SetUserOfflineTask;
import org.springframework.web.reactive.socket.CloseStatus;

import java.io.EOFException;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

public class SetUserOfflineTaskSerializer implements StreamSerializer<SetUserOfflineTask> {

    @Override
    public void write(ObjectDataOutput out, SetUserOfflineTask object) throws IOException {
        out.writeLong(object.getUserId());
        out.writeShort(object.getCloseStatus().getCode());
        Set<DeviceType> deviceTypes = object.getDeviceTypes();
        if (deviceTypes != null && !deviceTypes.isEmpty()) {
            byte deviceTypesMask = 0;
            for (DeviceType deviceType : deviceTypes) {
                // e.g.
                // The first device type (0) -> 0000 0001
                // The last device type (5) -> 0001 0000
                deviceTypesMask |= 1 << deviceType.getNumber();
            }
            out.writeByte(deviceTypesMask);
        }
    }

    @Override
    public SetUserOfflineTask read(ObjectDataInput in) throws IOException {
        long userId = in.readLong();
        CloseStatus closeStatus = new CloseStatus(in.readShort());
        Set<DeviceType> deviceTypes;
        try {
            byte deviceTypesMask = in.readByte();
            DeviceType[] allDeviceTypes = DeviceType.values();
            int length = allDeviceTypes.length;
            deviceTypes = new HashSet<>(length);
            for (int i = 0; i < length; i++) {
                if (Bits.isBitSet(deviceTypesMask, i)) {
                    deviceTypes.add(allDeviceTypes[i]);
                }
            }
        } catch (EOFException ignored) {
            deviceTypes = null;
        }
        return new SetUserOfflineTask(userId, deviceTypes, closeStatus);
    }

    @Override
    public int getTypeId() {
        return IdentifiedDataFactory.Type.TASK_SET_USER_OFFLINE.getValue();
    }

}
