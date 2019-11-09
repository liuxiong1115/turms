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

package im.turms.turms.property.env;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonView;
import com.hazelcast.nio.ObjectDataInput;
import com.hazelcast.nio.ObjectDataOutput;
import com.hazelcast.nio.serialization.IdentifiedDataSerializable;
import im.turms.turms.config.hazelcast.IdentifiedDataFactory;
import im.turms.turms.property.MutablePropertiesView;
import lombok.Data;

import java.io.IOException;

/**
 * To keep flexible, Turms itself do not provide redundant configs because Hazelcast has exposed
 * a lot of configs and developers can use their own configs by assigning your own config file path
 * to spring.hazelcast.config.
 * The most important
 */
@Data
public class Cluster implements IdentifiedDataSerializable {
    /**
     * Note: The minimumQuorumToRun is different from the quorum of Hazelcast
     *
     * The value is used to avoid the split brain problem and keep consistent between turms servers.
     * The recommended value should be evaluated through the formula "floor(totalTurmsNodesNumber / 2) + 1".
     * For example, you need to set the value to 5 if you decide to deploy 9 turms nodes,
     * Note: The total number of turms nodes deployed should better be a odd number, or there will be a split brain problem.
     * And the value is also used to enable a node when the number of connected nodes reaching the value if allowSplitBrain is false.
     */
    @JsonView(MutablePropertiesView.class)
    private int minimumQuorumToRun = 1;

    @JsonIgnore
    @Override
    public int getFactoryId() {
        return IdentifiedDataFactory.FACTORY_ID;
    }

    @JsonIgnore
    @Override
    public int getClassId() {
        return IdentifiedDataFactory.Type.PROPERTY_CLUSTER.getValue();
    }

    @Override
    public void writeData(ObjectDataOutput out) throws IOException {
        out.writeInt(minimumQuorumToRun);
    }

    @Override
    public void readData(ObjectDataInput in) throws IOException {
        minimumQuorumToRun = in.readInt();
    }
}