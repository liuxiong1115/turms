package im.turms.turms.serializer.task;

import com.hazelcast.nio.ObjectDataInput;
import com.hazelcast.nio.ObjectDataOutput;
import com.hazelcast.nio.serialization.StreamSerializer;
import im.turms.turms.config.hazelcast.IdentifiedDataFactory;
import im.turms.turms.task.QueryNearestUserSessionsIdsTask;

import java.io.IOException;

public class QueryNearestUserSessionsIdsTaskSerializer implements StreamSerializer<QueryNearestUserSessionsIdsTask> {

    @Override
    public void write(ObjectDataOutput out, QueryNearestUserSessionsIdsTask object) throws IOException {
        out.writeFloat(object.getLongitude());
        out.writeFloat(object.getLatitude());
        out.writeDouble(object.getMaxDistance());
        out.writeShort(object.getMaxNumber());
    }

    @Override
    public QueryNearestUserSessionsIdsTask read(ObjectDataInput in) throws IOException {
        float longitude = in.readFloat();
        float latitude = in.readFloat();
        double maxDistance = in.readDouble();
        short maxNumber = in.readShort();
        return new QueryNearestUserSessionsIdsTask(longitude, latitude, maxDistance, maxNumber);
    }

    @Override
    public int getTypeId() {
        return IdentifiedDataFactory.Type.TASK_QUERY_NEAREST_USER_SESSIONS_IDS.getValue();
    }

}
