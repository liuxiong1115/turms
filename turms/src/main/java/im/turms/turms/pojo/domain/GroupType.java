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
import im.turms.common.constant.GroupInvitationStrategy;
import im.turms.common.constant.GroupJoinStrategy;
import im.turms.common.constant.GroupUpdateStrategy;
import im.turms.turms.config.hazelcast.IdentifiedDataFactory;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.io.IOException;

/**
 * No need to shard because there are only a few (or some) group types.
 * <p>
 * Note that a built-in group type {
 * id: 0,
 * name: "DEFAULT"
 * } always exists in Turms
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Document
public final class GroupType implements IdentifiedDataSerializable {

    @Id
    private Long id;

    @Field(Fields.NAME)
    private String name;

    @Field(Fields.GROUP_SIZE_LIMIT)
    private Integer groupSizeLimit;

    @Field(Fields.INVITATION_STRATEGY)
    private GroupInvitationStrategy invitationStrategy;

    @Field(Fields.JOIN_STRATEGY)
    private GroupJoinStrategy joinStrategy;

    @Field(Fields.GROUP_INFO_UPDATE_STRATEGY)
    private GroupUpdateStrategy groupInfoUpdateStrategy;

    @Field(Fields.MEMBER_INFO_UPDATE_STRATEGY)
    private GroupUpdateStrategy memberInfoUpdateStrategy;

    @Field(Fields.GUEST_SPEAKABLE)
    private Boolean guestSpeakable;

    @Field(Fields.SELF_INFO_UPDATABLE)
    private Boolean selfInfoUpdatable;

    @Field(Fields.ENABLE_READ_RECEIPT)
    private Boolean enableReadReceipt;

    @Field(Fields.MESSAGE_EDITABLE)
    private Boolean messageEditable;

    public static final class Fields {
        public static final String NAME = "n";
        public static final String GROUP_SIZE_LIMIT = "gsl";
        public static final String INVITATION_STRATEGY = "is";
        public static final String JOIN_STRATEGY = "js";
        public static final String GROUP_INFO_UPDATE_STRATEGY = "gius";
        public static final String MEMBER_INFO_UPDATE_STRATEGY = "mius";
        public static final String GUEST_SPEAKABLE = "gs";
        public static final String SELF_INFO_UPDATABLE = "siu";
        public static final String ENABLE_READ_RECEIPT = "err";
        public static final String MESSAGE_EDITABLE = "me";

        private Fields() {
        }
    }

    @JsonIgnore
    @Override
    public int getFactoryId() {
        return IdentifiedDataFactory.FACTORY_ID;
    }

    @JsonIgnore
    @Override
    public int getClassId() {
        return IdentifiedDataFactory.Type.DOMAIN_GROUP_TYPE.getValue();
    }

    @Override
    public void writeData(ObjectDataOutput out) throws IOException {
        out.writeLong(id);
        out.writeUTF(name);
        out.writeInt(groupSizeLimit);
        out.writeInt(invitationStrategy.ordinal());
        out.writeInt(joinStrategy.ordinal());
        out.writeInt(groupInfoUpdateStrategy.ordinal());
        out.writeInt(memberInfoUpdateStrategy.ordinal());
        out.writeBoolean(guestSpeakable);
        out.writeBoolean(selfInfoUpdatable);
        out.writeBoolean(enableReadReceipt);
        out.writeBoolean(messageEditable);
    }

    @Override
    public void readData(ObjectDataInput in) throws IOException {
        id = in.readLong();
        name = in.readUTF();
        groupSizeLimit = in.readInt();
        invitationStrategy = GroupInvitationStrategy.values()[in.readInt()];
        joinStrategy = GroupJoinStrategy.values()[in.readInt()];
        groupInfoUpdateStrategy = GroupUpdateStrategy.values()[in.readInt()];
        memberInfoUpdateStrategy = GroupUpdateStrategy.values()[in.readInt()];
        guestSpeakable = in.readBoolean();
        selfInfoUpdatable = in.readBoolean();
        enableReadReceipt = in.readBoolean();
        messageEditable = in.readBoolean();
    }
}