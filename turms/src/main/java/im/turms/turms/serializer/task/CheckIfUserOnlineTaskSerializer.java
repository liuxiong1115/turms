package im.turms.turms.serializer.task;

import com.hazelcast.nio.ObjectDataInput;
import com.hazelcast.nio.ObjectDataOutput;
import com.hazelcast.nio.serialization.StreamSerializer;
import im.turms.turms.config.hazelcast.IdentifiedDataFactory;
import im.turms.turms.task.CheckIfUserOnlineTask;

import java.io.IOException;

/**
 * Size = com.hazelcast.internal.serialization.impl.HeapData#HEAP_DATA_OVERHEAD +
 */
public class CheckIfUserOnlineTaskSerializer implements StreamSerializer<CheckIfUserOnlineTask> {

    @Override
    public void write(ObjectDataOutput out, CheckIfUserOnlineTask object) throws IOException {
        out.writeLong(object.getUserId());
    }

    @Override
    public CheckIfUserOnlineTask read(ObjectDataInput in) throws IOException {
        return new CheckIfUserOnlineTask(in.readLong());
    }

    @Override
    public int getTypeId() {
        return IdentifiedDataFactory.Type.TASK_CHECK_IF_USER_ONLINE.getValue();
    }

}
