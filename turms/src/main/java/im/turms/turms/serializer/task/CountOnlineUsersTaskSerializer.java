package im.turms.turms.serializer.task;

import com.hazelcast.nio.ObjectDataInput;
import com.hazelcast.nio.ObjectDataOutput;
import com.hazelcast.nio.serialization.StreamSerializer;
import im.turms.turms.config.hazelcast.IdentifiedDataFactory;
import im.turms.turms.task.CountOnlineUsersTask;

public class CountOnlineUsersTaskSerializer implements StreamSerializer<CountOnlineUsersTask> {

    @Override
    public void write(ObjectDataOutput out, CountOnlineUsersTask object) {
    }

    @Override
    public CountOnlineUsersTask read(ObjectDataInput in) {
        return new CountOnlineUsersTask();
    }

    @Override
    public int getTypeId() {
        return IdentifiedDataFactory.Type.TASK_COUNT_ONLINE_USERS.getValue();
    }

}
