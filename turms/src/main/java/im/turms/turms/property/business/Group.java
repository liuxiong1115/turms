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
import jdk.jfr.Description;
import lombok.Data;

import javax.validation.constraints.Min;
import java.io.IOException;

@Data
public class Group implements IdentifiedDataSerializable {
    @JsonView(MutablePropertiesView.class)
    @Deprecated
    @Min(0)
    private int userOwnedGroupLimit = 10;
    @JsonView(MutablePropertiesView.class)
    @Deprecated
    @Min(0)
    private int userOwnedLimitForEachGroupTypeByDefault = Integer.MAX_VALUE;
    @JsonView(MutablePropertiesView.class)
    @Description("The maximum allowed length for the text of a group invitation")
    @Min(0)
    private int groupInvitationContentLimit = 200;
    @JsonView(MutablePropertiesView.class)
    @Description("A group invitation will become expired after the TTL has elapsed. 0 means infinite")
    @Min(0)
    private int groupInvitationTimeToLiveHours = 0;
    @JsonView(MutablePropertiesView.class)
    @Description("The maximum allowed length for the text of a group join request")
    @Min(0)
    private int groupJoinRequestContentLimit = 200;
    @JsonView(MutablePropertiesView.class)
    @Description("A group join request will become expired after the TTL has elapsed. 0 means infinite")
    @Min(0)
    private int groupJoinRequestTimeToLiveHours = 0;
    @JsonView(MutablePropertiesView.class)
    @Description("Whether to allow users to recall the join requests sent by themselves")
    private boolean allowRecallingJoinRequestSentByOneself = false;
    @JsonView(MutablePropertiesView.class)
    @Description("Whether to allow the owner and managers of a group to recall pending group invitations")
    private boolean allowRecallingPendingGroupInvitationByOwnerAndManager = false;
    @JsonView(MutablePropertiesView.class)
    @Description("Whether to delete groups logically by default")
    private boolean shouldDeleteGroupLogicallyByDefault = true;
    @JsonView(MutablePropertiesView.class)
    @Description("Whether to delete expired group invitations automatically")
    private boolean shouldDeleteExpiredGroupInvitationsAutomatically = false;
    @JsonView(MutablePropertiesView.class)
    @Description("Whether to delete expired group join requests automatically")
    private boolean shouldDeleteExpiredGroupJoinRequestsAutomatically = false;

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
        out.writeBoolean(shouldDeleteGroupLogicallyByDefault);
        out.writeBoolean(shouldDeleteExpiredGroupInvitationsAutomatically);
        out.writeBoolean(shouldDeleteExpiredGroupJoinRequestsAutomatically);
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
        shouldDeleteGroupLogicallyByDefault = in.readBoolean();
        shouldDeleteExpiredGroupInvitationsAutomatically = in.readBoolean();
        shouldDeleteExpiredGroupJoinRequestsAutomatically = in.readBoolean();
    }
}