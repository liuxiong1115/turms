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

import com.hazelcast.nio.ObjectDataInput;
import com.hazelcast.nio.ObjectDataOutput;
import com.hazelcast.nio.serialization.IdentifiedDataSerializable;
import im.turms.turms.config.hazelcast.IdentifiedDataFactory;
import im.turms.turms.constant.AdminPermission;
import lombok.*;
import lombok.experimental.FieldNameConstants;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.io.IOException;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

@Data
@Document
@FieldNameConstants
@AllArgsConstructor
@NoArgsConstructor
/**
 * Note: There is an admin role {
 *     id: 0,
 *     name: "ROOT",
 *     permissions: all permissions
 * } hardcoded in Turms
 */
public class AdminRole implements IdentifiedDataSerializable {
    @Id
    @Setter(AccessLevel.NONE)
    private Long id;
    private String name;
    @Indexed
    private Set<AdminPermission> permissions;

    @Override
    public int getFactoryId() {
        return IdentifiedDataFactory.FACTORY_ID;
    }

    @Override
    public int getId() {
        return IdentifiedDataFactory.Type.DOMAIN_ADMIN_ROLE.getValue();
    }

    public Long getRoleId() {
        return this.id;
    }

    @Override
    public void writeData(ObjectDataOutput out) throws IOException {
        out.writeLong(id);
        out.writeUTF(name);
        out.writeIntArray(permissions
                .stream()
                .mapToInt(Enum::ordinal)
                .toArray());
    }

    @Override
    public void readData(ObjectDataInput in) throws IOException {
        id = in.readLong();
        name = in.readUTF();
        permissions = Arrays.stream(in.readIntArray())
                .mapToObj(value -> AdminPermission.values()[value])
                .collect(Collectors.toSet());
    }
}