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
import com.google.common.primitives.Longs;
import com.hazelcast.nio.ObjectDataInput;
import com.hazelcast.nio.ObjectDataOutput;
import com.hazelcast.nio.serialization.IdentifiedDataSerializable;
import im.turms.turms.config.hazelcast.IdentifiedDataFactory;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * No need to shard because there are only a few (or some) user permission groups.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Document
public final class UserPermissionGroup implements IdentifiedDataSerializable {

    @Id
    private Long id;

    @Field(Fields.CREATABLE_GROUP_TYPE_IDS)
    private Set<Long> creatableGroupTypeIds;

    @Field(Fields.OWNED_GROUP_LIMIT)
    private Integer ownedGroupLimit;

    @Field(Fields.OWNED_GROUP_LIMIT_FOR_EACH_GROUP_TYPE)
    private Integer ownedGroupLimitForEachGroupType;

    @Field(Fields.GROUP_TYPE_LIMITS)
    // group type id -> limit
    private Map<Long, Integer> groupTypeLimits;

    @JsonIgnore
    @Override
    public int getFactoryId() {
        return IdentifiedDataFactory.FACTORY_ID;
    }

    @JsonIgnore
    @Override
    public int getClassId() {
        return IdentifiedDataFactory.Type.DOMAIN_USER_PERMISSION_GROUP.getValue();
    }

    @Override
    public void writeData(ObjectDataOutput out) throws IOException {
        out.writeLong(id);
        out.writeLongArray(Longs.toArray(creatableGroupTypeIds));
        out.writeInt(ownedGroupLimit);
        out.writeInt(ownedGroupLimitForEachGroupType);
        IdentifiedDataFactory.writeMap(groupTypeLimits, out);
    }

    @Override
    public void readData(ObjectDataInput in) throws IOException {
        id = in.readLong();
        creatableGroupTypeIds = Arrays.stream(in.readLongArray())
                .boxed()
                .collect(Collectors.toSet());
        ownedGroupLimit = in.readInt();
        ownedGroupLimitForEachGroupType = in.readInt();
        groupTypeLimits = IdentifiedDataFactory.readMaps(in);
    }


    public static class Fields {
        public static final String CREATABLE_GROUP_TYPE_IDS = "cgtid";
        public static final String OWNED_GROUP_LIMIT = "ogl";
        public static final String OWNED_GROUP_LIMIT_FOR_EACH_GROUP_TYPE = "oglegt";
        public static final String GROUP_TYPE_LIMITS = "gtl";

        private Fields() {
        }
    }

}
