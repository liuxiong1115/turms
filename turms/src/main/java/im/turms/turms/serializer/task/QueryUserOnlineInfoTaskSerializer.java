package im.turms.turms.serializer.task;

import com.hazelcast.nio.ObjectDataInput;
import com.hazelcast.nio.ObjectDataOutput;
import com.hazelcast.nio.serialization.StreamSerializer;
import im.turms.turms.config.hazelcast.IdentifiedDataFactory;
import im.turms.turms.task.QueryUserOnlineInfoTask;

import java.io.IOException;

public class QueryUserOnlineInfoTaskSerializer implements StreamSerializer<QueryUserOnlineInfoTask> {

    @Override
    public void write(ObjectDataOutput out, QueryUserOnlineInfoTask object) throws IOException {
        out.writeLong(object.getUserId());
    }

    @Override
    public QueryUserOnlineInfoTask read(ObjectDataInput in) throws IOException {
        return new QueryUserOnlineInfoTask(in.readLong());
    }

    @Override
    public int getTypeId() {
        return IdentifiedDataFactory.Type.TASK_QUERY_USER_ONLINE_INFO.getValue();
    }

}
