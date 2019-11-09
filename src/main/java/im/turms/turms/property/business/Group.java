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

package im.turms.turms.property.business;

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
public class Group implements IdentifiedDataSerializable {
    @JsonView(MutablePropertiesView.class)
    private int userOwnedGroupLimit = 10;
    @JsonView(MutablePropertiesView.class)
    private int userOwnedLimitForEachGroupTypeByDefault = Integer.MAX_VALUE;
    @JsonView(MutablePropertiesView.class)
    private int groupInvitationContentLimit = 200;
    /**
     * 0 is infinite.
     */
    @JsonView(MutablePropertiesView.class)
    private int groupInvitationTimeToLiveHours = 0;
    @JsonView(MutablePropertiesView.class)
    private int groupJoinRequestContentLimit = 200;
    /**
     * 0 is infinite.
     */
    @JsonView(MutablePropertiesView.class)
    private int groupJoinRequestTimeToLiveHours = 0;
    @JsonView(MutablePropertiesView.class)
    private boolean allowRecallingJoinRequestSentByOneself = false;
    @JsonView(MutablePropertiesView.class)
    private boolean allowRecallingPendingGroupInvitationByOwnerAndManager = false;
    @JsonView(MutablePropertiesView.class)
    private boolean logicallyDeleteGroupByDefault = true;
    @JsonView(MutablePropertiesView.class)
    private boolean deleteExpiryGroupInvitations = false;
    @JsonView(MutablePropertiesView.class)
    private boolean deleteExpiryGroupJoinRequests = false;

    @JsonIgnore
    @Override
    public int getFactoryId() {
        return IdentifiedDataFactory.FACTORY_ID;
    }

    @JsonIgnore
    @Override
    public int getClassId() {
        return IdentifiedDataFactory.Type.PROPERTY_GROUP.getValue();
    }

    @Override
    public void writeData(ObjectDataOutput out) throws IOException {
        out.writeInt(userOwnedGroupLimit);
        out.writeInt(userOwnedLimitForEachGroupTypeByDefault);
        out.writeInt(groupInvitationContentLimit);
        out.writeInt(groupInvitationTimeToLiveHours);
        out.writeInt(groupJoinRequestContentLimit);
        out.writeInt(groupJoinRequestTimeToLiveHours);
        out.writeBoolean(allowRecallingJoinRequestSentByOneself);
        out.writeBoolean(allowRecallingPendingGroupInvitationByOwnerAndManager);
        out.writeBoolean(logicallyDeleteGroupByDefault);
        out.writeBoolean(deleteExpiryGroupInvitations);
        out.writeBoolean(deleteExpiryGroupJoinRequests);
    }

    @Override
    public void readData(ObjectDataInput in) throws IOException {
        userOwnedGroupLimit = in.readInt();
        userOwnedLimitForEachGroupTypeByDefault = in.readInt();
        groupInvitationContentLimit = in.readInt();
        groupInvitationTimeToLiveHours = in.readInt();
        groupJoinRequestContentLimit = in.readInt();
        groupJoinRequestTimeToLiveHours = in.readInt();
        allowRecallingJoinRequestSentByOneself = in.readBoolean();
        allowRecallingPendingGroupInvitationByOwnerAndManager = in.readBoolean();
        logicallyDeleteGroupByDefault = in.readBoolean();
        deleteExpiryGroupInvitations = in.readBoolean();
        deleteExpiryGroupJoinRequests = in.readBoolean();
    }
}