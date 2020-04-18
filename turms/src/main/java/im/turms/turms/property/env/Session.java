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
import im.turms.common.constant.DeviceType;
import im.turms.turms.config.hazelcast.IdentifiedDataFactory;
import im.turms.turms.property.MutablePropertiesView;
import jdk.jfr.Description;
import lombok.Data;

import javax.validation.constraints.Min;
import java.io.IOException;
import java.util.Set;

@Data
public class Session implements IdentifiedDataSerializable {
    private static final Set<DeviceType> BROWSER_SET = Set.of(DeviceType.BROWSER);

    @JsonView(MutablePropertiesView.class)
    @Description("A websocket connection will be closed if the turms server doesn't receive any request (including heartbeat request) from the client during heartbeatTimeoutSeconds")
    @Min(0)
    private int heartbeatTimeoutSeconds = 60;

    @Description("Whether to enable to query the login failed reason")
    private boolean enableQueryLoginFailedReason = true;

    @Description("Whether to enable to query the disconnection reason")
    private boolean enableQueryDisconnectionReason = true;

    @Description("The degraded device types that need to query the disconnection reason")
    private Set<DeviceType> degradedDeviceTypesForDisconnectionReason = BROWSER_SET;

    @Description("The degraded device types that need to query the login-failed reason")
    private Set<DeviceType> degradedDeviceTypesForLoginFailedReason = BROWSER_SET;

    @JsonView(MutablePropertiesView.class)
    @Description("Whether to notify clients of the session information after connected with a turms server")
    private boolean notifyClientsOfSessionInfoAfterConnected = true;

    @Description("Whether to allow users to connect to irresponsible servers.")
    private boolean allowIrresponsibleUsersWhenConnecting = true;

    @Description("Whether to allow online users to be still connecting to irresponsible servers after the servers' responsibility has changed." +
            "If false, when a server goes up or down, a part of users connected to other servers have to be disconnected to keep the consistency of users' status due to the consistent hashing")
    private boolean allowIrresponsibleUsersAfterResponsibilityChanged = true;

    @Description("The irresponsible users will be disconnected after [clearUpIrresponsibleUsersAfter - jitter, clearUpIrresponsibleUsersAfter + jitter] roughly." +
            "If the status of cluster members change again before the irresponsible users disconnect, the time to disconnect will be recalculated according to the new responsibility." +
            "When reaching the time for the irresponsible users to disconnect, the users connected to the irresponsible server will be disconnected and routed to other responsible servers.")
    @Min(0)
    private int clearUpIrresponsibleUsersAfter = 2 * 60;

    @Min(0)
    private int clearUpIrresponsibleUsersJitter = 30;

    @JsonView(MutablePropertiesView.class)
    @Description("Whether to enable to authenticate users when logging in." +
            "Note that user ID is always needed even if enableAuthentication is false")
    private boolean enableAuthentication = true;

    /**
     * If the turms server only receives heartbeat requests from the client during maxIdleTime,
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
        out.writeInt(heartbeatTimeoutSeconds);
        out.writeBoolean(enableQueryLoginFailedReason);
        out.writeBoolean(enableQueryDisconnectionReason);
        out.writeBoolean(notifyClientsOfSessionInfoAfterConnected);
        out.writeBoolean(enableAuthentication);
//        out.writeInt(idleHeartbeatTimeoutSeconds);
    }

    @Override
    public void readData(ObjectDataInput in) throws IOException {
        heartbeatTimeoutSeconds = in.readInt();
        enableQueryLoginFailedReason = in.readBoolean();
        enableQueryDisconnectionReason = in.readBoolean();
        notifyClientsOfSessionInfoAfterConnected = in.readBoolean();
        enableAuthentication = in.readBoolean();
//        idleHeartbeatTimeoutSeconds = in.readInt();
    }
}