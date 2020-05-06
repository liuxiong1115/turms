package im.turms.turms.serializer.task;

import com.hazelcast.nio.ObjectDataInput;
import com.hazelcast.nio.ObjectDataOutput;
import com.hazelcast.nio.serialization.StreamSerializer;
import im.turms.common.constant.UserStatus;
import im.turms.turms.config.hazelcast.IdentifiedDataFactory;
import im.turms.turms.task.UpdateOnlineUserStatusTask;

import java.io.IOException;

public class UpdateOnlineUserStatusTaskSerializer implements StreamSerializer<UpdateOnlineUserStatusTask> {

    @Override
    public void write(ObjectDataOutput out, UpdateOnlineUserStatusTask object) throws IOException {
        out.writeLong(object.getUserId());
        out.writeByte(object.getUserStatus().getNumber());
    }

    @Override
    public UpdateOnlineUserStatusTask read(ObjectDataInput in) throws IOException {
        long userId = in.readLong();
        UserStatus userStatus = UserStatus.forNumber(in.readByte() & 0xFF);
        return new UpdateOnlineUserStatusTask(userId, userStatus);
    }

    @Override
    public int getTypeId() {
        return IdentifiedDataFactory.Type.TASK_UPDATE_ONLINE_USER_STATUS.getValue();
    }

}
