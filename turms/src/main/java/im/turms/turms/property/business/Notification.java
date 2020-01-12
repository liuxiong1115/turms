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
public class Notification implements IdentifiedDataSerializable {
    // User
    @JsonView(MutablePropertiesView.class)
    private boolean notifyRelatedUsersAfterUserInfoUpdated = false;
    @JsonView(MutablePropertiesView.class)
    private boolean notifyRelatedUsersAfterOtherRelatedUsersOnlineStatusUpdated = false;
    @JsonView(MutablePropertiesView.class)
    private boolean notifyRecipientAfterReceivedFriendRequest = true;
    @JsonView(MutablePropertiesView.class)
    private boolean notifyRequesterAfterFriendRequestUpdated = true;
    @JsonView(MutablePropertiesView.class)
    private boolean notifyRelatedUserAfterAddedToOneSidedRelationshipGroupByOthers = false;
    @JsonView(MutablePropertiesView.class)
    private boolean notifyRelatedUserAfterOneSidedRelationshipGroupUpdatedByOthers = false;
    @JsonView(MutablePropertiesView.class)
    private boolean notifyRelatedUserAfterRemoveFromRelationshipGroupByOthers = false;
    @JsonView(MutablePropertiesView.class)
    private boolean notifyRelatedUserAfterOneSidedRelationshipUpdatedByOthers = false;

    // Group
    @JsonView(MutablePropertiesView.class)
    private boolean notifyMembersAfterGroupDeleted = true;
    @JsonView(MutablePropertiesView.class)
    private boolean notifyMembersAfterGroupUpdate = true;
    @JsonView(MutablePropertiesView.class)
    private boolean notifyMembersAfterOtherMemberOnlineStatusUpdated = false;
    @JsonView(MutablePropertiesView.class)
    private boolean notifyMembersAfterOtherMemberInfoUpdated = false;
    @JsonView(MutablePropertiesView.class)
    private boolean notifyUserAfterBlacklistedByGroup = false;
    @JsonView(MutablePropertiesView.class)
    private boolean notifyUserAfterUnblacklistedByGroup = false;
    @JsonView(MutablePropertiesView.class)
    private boolean notifyUserAfterInvitedByGroup = true;
    @JsonView(MutablePropertiesView.class)
    private boolean notifyAdminsAndOwnerAfterReceiveJoinRequest = true;
    @JsonView(MutablePropertiesView.class)
    private boolean notifyInviteeAfterGroupInvitationRecalled = true;
    @JsonView(MutablePropertiesView.class)
    private boolean notifyManagersAndOwnerAfterGroupJoinRequestRecalled = true;
    @JsonView(MutablePropertiesView.class)
    private boolean notifyUserAfterAddedToGroupByOthers = true;
    @JsonView(MutablePropertiesView.class)
    private boolean notifyUserAfterRemovedFromGroupByOthers = true;
    @JsonView(MutablePropertiesView.class)
    private boolean notifyMemberAfterUpdatedByOthers = true;

    @JsonIgnore
    @Override
    public int getFactoryId() {
        return IdentifiedDataFactory.FACTORY_ID;
    }

    @JsonIgnore
    @Override
    public int getClassId() {
        return IdentifiedDataFactory.Type.PROPERTY_NOTIFICATION.getValue();
    }

    @Override
    public void writeData(ObjectDataOutput out) throws IOException {
        // User
        out.writeBoolean(notifyRelatedUsersAfterUserInfoUpdated);
        out.writeBoolean(notifyRelatedUsersAfterOtherRelatedUsersOnlineStatusUpdated);
        out.writeBoolean(notifyRecipientAfterReceivedFriendRequest);
        out.writeBoolean(notifyRequesterAfterFriendRequestUpdated);
        out.writeBoolean(notifyRelatedUserAfterAddedToOneSidedRelationshipGroupByOthers);
        out.writeBoolean(notifyRelatedUserAfterOneSidedRelationshipGroupUpdatedByOthers);
        out.writeBoolean(notifyRelatedUserAfterRemoveFromRelationshipGroupByOthers);
        out.writeBoolean(notifyRelatedUserAfterOneSidedRelationshipUpdatedByOthers);

        // Group
        out.writeBoolean(notifyMembersAfterGroupDeleted);
        out.writeBoolean(notifyMembersAfterGroupUpdate);
        out.writeBoolean(notifyMembersAfterOtherMemberOnlineStatusUpdated);
        out.writeBoolean(notifyMembersAfterOtherMemberInfoUpdated);
        out.writeBoolean(notifyUserAfterBlacklistedByGroup);
        out.writeBoolean(notifyUserAfterUnblacklistedByGroup);
        out.writeBoolean(notifyUserAfterInvitedByGroup);
        out.writeBoolean(notifyAdminsAndOwnerAfterReceiveJoinRequest);
        out.writeBoolean(notifyInviteeAfterGroupInvitationRecalled);
        out.writeBoolean(notifyManagersAndOwnerAfterGroupJoinRequestRecalled);
        out.writeBoolean(notifyUserAfterAddedToGroupByOthers);
        out.writeBoolean(notifyUserAfterRemovedFromGroupByOthers);
        out.writeBoolean(notifyMemberAfterUpdatedByOthers);
    }

    @Override
    public void readData(ObjectDataInput in) throws IOException {
        // User
        notifyRelatedUsersAfterUserInfoUpdated = in.readBoolean();
        notifyRelatedUsersAfterOtherRelatedUsersOnlineStatusUpdated = in.readBoolean();
        notifyRecipientAfterReceivedFriendRequest = in.readBoolean();
        notifyRequesterAfterFriendRequestUpdated = in.readBoolean();
        notifyRelatedUserAfterAddedToOneSidedRelationshipGroupByOthers = in.readBoolean();
        notifyRelatedUserAfterOneSidedRelationshipGroupUpdatedByOthers = in.readBoolean();
        notifyRelatedUserAfterRemoveFromRelationshipGroupByOthers = in.readBoolean();
        notifyRelatedUserAfterOneSidedRelationshipUpdatedByOthers = in.readBoolean();

        // Group
        notifyMembersAfterGroupDeleted = in.readBoolean();
        notifyMembersAfterGroupUpdate = in.readBoolean();
        notifyMembersAfterOtherMemberOnlineStatusUpdated = in.readBoolean();
        notifyMembersAfterOtherMemberInfoUpdated = in.readBoolean();
        notifyUserAfterBlacklistedByGroup = in.readBoolean();
        notifyUserAfterUnblacklistedByGroup = in.readBoolean();
        notifyUserAfterInvitedByGroup = in.readBoolean();
        notifyAdminsAndOwnerAfterReceiveJoinRequest = in.readBoolean();
        notifyInviteeAfterGroupInvitationRecalled = in.readBoolean();
        notifyManagersAndOwnerAfterGroupJoinRequestRecalled = in.readBoolean();
        notifyUserAfterAddedToGroupByOthers = in.readBoolean();
        notifyUserAfterRemovedFromGroupByOthers = in.readBoolean();
        notifyMemberAfterUpdatedByOthers = in.readBoolean();
    }
}