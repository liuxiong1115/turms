package im.turms.turms.pojo.bo.admin;

import com.hazelcast.nio.ObjectDataInput;
import com.hazelcast.nio.ObjectDataOutput;
import com.hazelcast.nio.serialization.IdentifiedDataSerializable;
import im.turms.turms.config.hazelcast.IdentifiedDataFactory;
import im.turms.turms.pojo.domain.Admin;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.IOException;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminInfo implements IdentifiedDataSerializable {
    private Admin admin;
    private String rawPassword;

    @Override
    public int getFactoryId() {
        return IdentifiedDataFactory.FACTORY_ID;
    }

    @Override
    public int getClassId() {
        return IdentifiedDataFactory.Type.BO_ADMIN_INFO.getValue();
    }

    @Override
    public void writeData(ObjectDataOutput out) throws IOException {
        admin.writeData(out);
        out.writeUTF(rawPassword);
    }

    @Override
    public void readData(ObjectDataInput in) throws IOException {
        admin.readData(in);
        rawPassword = in.readUTF();
    }
}
