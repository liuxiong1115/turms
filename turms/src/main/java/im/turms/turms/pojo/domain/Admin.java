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
import im.turms.turms.annotation.domain.OptionalIndexedForDifferentAmount;
import im.turms.turms.config.hazelcast.IdentifiedDataFactory;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.io.IOException;
import java.util.Date;

/**
 * No need to shard because there are only a few (or some) admins for most groups.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Document
public final class Admin implements IdentifiedDataSerializable, Cloneable {

    @Id
    private String account;

    @Field(Fields.PASSWORD)
    private String password;

    @Field(Fields.NAME)
    private String name;

    @Field(Fields.ROLE_ID)
    @OptionalIndexedForDifferentAmount
    private Long roleId;

    @Field(Fields.REGISTRATION_DATE)
    @OptionalIndexedForDifferentAmount
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
    public Admin clone() {
        try {
            return (Admin) super.clone();
        } catch (CloneNotSupportedException e) {
            // This should never happen
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public void readData(ObjectDataInput in) throws IOException {
        account = in.readUTF();
        password = in.readUTF();
        name = in.readUTF();
        roleId = in.readLong();
        registrationDate = new Date(in.readLong());
    }

    public static class Fields {
        public static final String PASSWORD = "pw";
        public static final String NAME = "n";
        public static final String ROLE_ID = "rid";
        public static final String REGISTRATION_DATE = "rd";

        private Fields() {
        }
    }
}