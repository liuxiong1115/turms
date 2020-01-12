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

@Data
public class Session implements IdentifiedDataSerializable {
    /**
     * If the Turms server don't receive any request (including heartbeat request) from the client during requestTimeout,
     * the session will be closed.
     */
    @JsonView(MutablePropertiesView.class)
    private int requestHeartbeatTimeoutSeconds = 50;
    @JsonView(MutablePropertiesView.class)
    private int minHeartbeatRefreshIntervalSeconds = 3;
    @JsonView(MutablePropertiesView.class)
    private boolean enableQueryingLoginFailedReason = true;
    @JsonView(MutablePropertiesView.class)
    private boolean enableQueryingDisconnectionReason = true;
    @JsonView(MutablePropertiesView.class)
    private boolean notifyClientsOfSessionInfoAfterConnected = true;
    /**
     * If the Turms server only receives heartbeat requests from the client during maxIdleTime,
     * the session will be closed when the Session Cleaner detects it.
     */
//    @JsonView(MutablePropertiesView.class)
//    private int idleHeartbeatTimeoutSeconds = 0;

    @JsonIgnore
    @Override
    public int getFactoryId() {
        return IdentifiedDataFactory.FACTORY_ID;
    }

    @JsonIgnore
    @Override
    public int getClassId() {
        return IdentifiedDataFactory.Type.PROPERTY_SESSION.getValue();
    }

    @Override
    public void writeData(ObjectDataOutput out) throws IOException {
        out.writeInt(requestHeartbeatTimeoutSeconds);
        out.writeInt(minHeartbeatRefreshIntervalSeconds);
        out.writeBoolean(enableQueryingLoginFailedReason);
        out.writeBoolean(enableQueryingDisconnectionReason);
        out.writeBoolean(notifyClientsOfSessionInfoAfterConnected);
//        out.writeInt(idleHeartbeatTimeoutSeconds);
    }

    @Override
    public void readData(ObjectDataInput in) throws IOException {
        requestHeartbeatTimeoutSeconds = in.readInt();
        minHeartbeatRefreshIntervalSeconds = in.readInt();
        enableQueryingLoginFailedReason = in.readBoolean();
        enableQueryingDisconnectionReason = in.readBoolean();
        notifyClientsOfSessionInfoAfterConnected = in.readBoolean();
//        idleHeartbeatTimeoutSeconds = in.readInt();
    }
}