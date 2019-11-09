/*
 * Copyright (C) 2019 The Turms Project
 * https://github.com/turms-im/turms
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package im.turms.turms.pojo.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.hazelcast.nio.ObjectDataInput;
import com.hazelcast.nio.ObjectDataOutput;
import com.hazelcast.nio.serialization.IdentifiedDataSerializable;
import im.turms.turms.config.hazelcast.IdentifiedDataFactory;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;
import org.hibernate.validator.constraints.Length;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.io.IOException;
import java.util.Date;

@Data
@Document
@FieldNameConstants
@AllArgsConstructor
@NoArgsConstructor
public class Admin implements IdentifiedDataSerializable {
    @Id
    @Length(min = 1, max = 32)
    private String account;

    @Length(min = 1, max = 32)
    private String password;

    @Length(min = 1, max = 50)
    private String name;

    @Indexed
    private Long roleId;

    @Indexed
    private Date registrationDate;

    @JsonIgnore
    @Override
    public int getFactoryId() {
        return IdentifiedDataFactory.FACTORY_ID;
    }

    @JsonIgnore
    @Override
    public int getClassId() {
        return IdentifiedDataFactory.Type.DOMAIN_ADMIN.getValue();
    }

    @Override
    public void writeData(ObjectDataOutput out) throws IOException {
        out.writeUTF(account);
        out.writeUTF(password);
        out.writeUTF(name);
        out.writeLong(roleId);
        out.writeLong(registrationDate.getTime());
    }

    @Override
    public void readData(ObjectDataInput in) throws IOException {
        account = in.readUTF();
        password = in.readUTF();
        name = in.readUTF();
        roleId = in.readLong();
        registrationDate = new Date(in.readLong());
    }
}