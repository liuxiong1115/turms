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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.hazelcast.nio.ObjectDataInput;
import com.hazelcast.nio.ObjectDataOutput;
import com.hazelcast.nio.serialization.IdentifiedDataSerializable;
import im.turms.turms.config.hazelcast.IdentifiedDataFactory;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.IOException;

@Data
@JsonIgnoreProperties({"factoryId", "id"})
public class Database implements IdentifiedDataSerializable {

    private WriteConcern writeConcern = new WriteConcern();

    @Override
    public int getFactoryId() {
        return IdentifiedDataFactory.FACTORY_ID;
    }

    @Override
    public int getId() {
        return IdentifiedDataFactory.Type.PROPERTY_DATABASE.getValue();
    }

    @Override
    public void writeData(ObjectDataOutput out) throws IOException {
    }

    @Override
    public void readData(ObjectDataInput in) throws IOException {
    }

    @Data
    @NoArgsConstructor
    public static class WriteConcern {
        private com.mongodb.WriteConcern admin = com.mongodb.WriteConcern.MAJORITY;
        private com.mongodb.WriteConcern adminActionLog = com.mongodb.WriteConcern.ACKNOWLEDGED;
        private com.mongodb.WriteConcern adminRole = com.mongodb.WriteConcern.MAJORITY;
        private com.mongodb.WriteConcern group = com.mongodb.WriteConcern.ACKNOWLEDGED;
        private com.mongodb.WriteConcern groupBlacklistedUser = com.mongodb.WriteConcern.ACKNOWLEDGED;
        private com.mongodb.WriteConcern groupInvitation = com.mongodb.WriteConcern.ACKNOWLEDGED;
        private com.mongodb.WriteConcern groupJoinQuestion = com.mongodb.WriteConcern.ACKNOWLEDGED;
        private com.mongodb.WriteConcern groupJoinRequest = com.mongodb.WriteConcern.ACKNOWLEDGED;
        private com.mongodb.WriteConcern groupMember = com.mongodb.WriteConcern.ACKNOWLEDGED;
        private com.mongodb.WriteConcern groupType = com.mongodb.WriteConcern.MAJORITY;
        private com.mongodb.WriteConcern groupVersion = com.mongodb.WriteConcern.ACKNOWLEDGED;
        private com.mongodb.WriteConcern message = com.mongodb.WriteConcern.ACKNOWLEDGED;
        private com.mongodb.WriteConcern messageStatus = com.mongodb.WriteConcern.ACKNOWLEDGED;
        private com.mongodb.WriteConcern user = com.mongodb.WriteConcern.ACKNOWLEDGED;
        private com.mongodb.WriteConcern userFriendRequest = com.mongodb.WriteConcern.ACKNOWLEDGED;
        private com.mongodb.WriteConcern userLocation = com.mongodb.WriteConcern.ACKNOWLEDGED;
        private com.mongodb.WriteConcern userLoginLog = com.mongodb.WriteConcern.ACKNOWLEDGED;
        private com.mongodb.WriteConcern userMaxDailyOnlineUser = com.mongodb.WriteConcern.ACKNOWLEDGED;
        private com.mongodb.WriteConcern userOnlineUserNumber = com.mongodb.WriteConcern.ACKNOWLEDGED;
        private com.mongodb.WriteConcern userPermissionGroup = com.mongodb.WriteConcern.ACKNOWLEDGED;
        private com.mongodb.WriteConcern userPermissionGroupMember = com.mongodb.WriteConcern.ACKNOWLEDGED;
        private com.mongodb.WriteConcern userRelationship = com.mongodb.WriteConcern.ACKNOWLEDGED;
        private com.mongodb.WriteConcern userRelationshipGroup = com.mongodb.WriteConcern.ACKNOWLEDGED;
        private com.mongodb.WriteConcern userRelationshipGroupMember = com.mongodb.WriteConcern.ACKNOWLEDGED;
        private com.mongodb.WriteConcern userVersion = com.mongodb.WriteConcern.ACKNOWLEDGED;
    }
}