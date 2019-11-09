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
import im.turms.turms.config.hazelcast.IdentifiedDataFactory;
import im.turms.turms.constant.GroupInvitationStrategy;
import im.turms.turms.constant.GroupJoinStrategy;
import im.turms.turms.constant.GroupUpdateStrategy;
import lombok.*;
import lombok.experimental.FieldNameConstants;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.io.IOException;

@Data
@Document
@FieldNameConstants
@AllArgsConstructor
@NoArgsConstructor
/**
 * Note: There is an group type {
 *     id: 0,
 *     name: "DEFAULT"
 * } hardcoded in Turms
 */
public class GroupType implements IdentifiedDataSerializable {
    @Id
    @Setter(AccessLevel.NONE)
    private Long id;

    private String name;

    private Integer groupSizeLimit;

    private GroupInvitationStrategy invitationStrategy;

    private GroupJoinStrategy joinStrategy;

    private GroupUpdateStrategy groupInfoUpdateStrategy;

    private GroupUpdateStrategy memberInfoUpdateStrategy;

    private Boolean guestSpeakable;

    private Boolean selfInfoUpdatable;

    private Boolean enableReadReceipt;

    private Boolean messageEditable;

    public Long getTypeId() {
        return this.id;
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