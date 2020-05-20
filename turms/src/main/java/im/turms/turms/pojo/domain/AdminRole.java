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
import im.turms.turms.constant.AdminPermission;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.io.IOException;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Note that there is always a built-in root role {
 * id: 0,
 * name: "ROOT",
 * permissions: AdminPermission.ALL,
 * rank: Integer.MAX_VALUE
 * }
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Document
public final class AdminRole implements IdentifiedDataSerializable {

    @Id
    private Long id;

    @Field(Fields.NAME)
    private String name;

    @Field(Fields.PERMISSIONS)
    @OptionalIndexedForDifferentAmount
    private Set<AdminPermission> permissions;

    /**
     * Only the higher-ranking admins can add/delete/update lower-ranking admins' information.
     */
    @Field(Fields.RANK)
    @OptionalIndexedForDifferentAmount
    private Integer rank;

    @JsonIgnore
    @Override
    public int getFactoryId() {
        return IdentifiedDataFactory.FACTORY_ID;
    }

    @JsonIgnore
    @Override
    public int getClassId() {
        return IdentifiedDataFactory.Type.DOMAIN_ADMIN_ROLE.getValue();
    }

    @Override
    public void writeData(ObjectDataOutput out) throws IOException {
        out.writeLong(id);
        out.writeUTF(name);
        out.writeIntArray(permissions
                .stream()
                .mapToInt(Enum::ordinal)
                .toArray());
        out.writeInt(rank);
    }

    @Override
    public void readData(ObjectDataInput in) throws IOException {
        id = in.readLong();
        name = in.readUTF();
        permissions = Arrays.stream(in.readIntArray())
                .mapToObj(value -> AdminPermission.values()[value])
                .collect(Collectors.toSet());
        rank = in.readInt();
    }

    public static class Fields {
        public static final String NAME = "n";
        public static final String PERMISSIONS = "perm";
        public static final String RANK = "rank";

        private Fields() {
        }
    }
}