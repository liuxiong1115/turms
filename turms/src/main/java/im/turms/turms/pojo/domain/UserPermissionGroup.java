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
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;
import org.springframework.data.annotation.Id;

import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Data
@FieldNameConstants
@NoArgsConstructor
public class UserPermissionGroup implements IdentifiedDataSerializable {
    @Id
    private Long id;

    private Set<Long> creatableGroupTypeIds;

    private Integer ownedGroupLimit;

    private Integer ownedGroupLimitForEachGroupType;
    // group type id -> limit
    private Map<Long, Integer> groupTypeLimits;

    public UserPermissionGroup(Long id, Set<Long> creatableGroupTypeIds, Integer ownedGroupLimit, Integer ownedGroupLimitForEachGroupType, Map<Long, Integer> groupTypeLimits) {
        this.id = id;
        this.creatableGroupTypeIds = creatableGroupTypeIds;
        this.ownedGroupLimit = ownedGroupLimit;
        this.ownedGroupLimitForEachGroupType = ownedGroupLimitForEachGroupType;
        this.groupTypeLimits = groupTypeLimits;
    }

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
}
