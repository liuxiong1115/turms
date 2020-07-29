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

package im.turms.server.common.property.env.service.env;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.autoconfigure.mongo.MongoProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.data.annotation.Transient;

/**
 * @author James Chen
 */
@Data
public class DatabaseProperties {

    @NestedConfigurationProperty
    private Properties mongoProperties = new Properties();

    @JsonIgnore
    @Transient
    private WriteConcern writeConcern = new WriteConcern();

    @Data
    @NoArgsConstructor
    public static class Properties {

        @JsonIgnore
        @Transient
        @NestedConfigurationProperty
        private MongoProperties defaultProperties = new MongoProperties();

        @JsonIgnore
        @Transient
        @NestedConfigurationProperty
        private MongoProperties admin = new MongoProperties();

        @JsonIgnore
        @Transient
        @NestedConfigurationProperty
        private MongoProperties user = new MongoProperties();

        @JsonIgnore
        @Transient
        @NestedConfigurationProperty
        private MongoProperties group = new MongoProperties();

        @JsonIgnore
        @Transient
        @NestedConfigurationProperty
        private MongoProperties message = new MongoProperties();

    }

    @Data
    @NoArgsConstructor
    public static class WriteConcern {
        private com.mongodb.WriteConcern admin = com.mongodb.WriteConcern.MAJORITY;
        private com.mongodb.WriteConcern adminRole = com.mongodb.WriteConcern.MAJORITY;

        private com.mongodb.WriteConcern group = com.mongodb.WriteConcern.ACKNOWLEDGED;
        private com.mongodb.WriteConcern groupBlacklistedUser = com.mongodb.WriteConcern.ACKNOWLEDGED;
        private com.mongodb.WriteConcern groupInvitation = com.mongodb.WriteConcern.ACKNOWLEDGED;
        private com.mongodb.WriteConcern groupJoinQuestion = com.mongodb.WriteConcern.ACKNOWLEDGED;
        private com.mongodb.WriteConcern groupJoinRequest = com.mongodb.WriteConcern.ACKNOWLEDGED;
        private com.mongodb.WriteConcern groupMember = com.mongodb.WriteConcern.ACKNOWLEDGED;
        private com.mongodb.WriteConcern groupType = com.mongodb.WriteConcern.MAJORITY;
        private com.mongodb.WriteConcern groupVersion = com.mongodb.WriteConcern.ACKNOWLEDGED;

        private com.mongodb.WriteConcern user = com.mongodb.WriteConcern.ACKNOWLEDGED;
        private com.mongodb.WriteConcern userFriendRequest = com.mongodb.WriteConcern.ACKNOWLEDGED;
        private com.mongodb.WriteConcern userLocation = com.mongodb.WriteConcern.ACKNOWLEDGED;
        private com.mongodb.WriteConcern userMaxDailyOnlineUser = com.mongodb.WriteConcern.ACKNOWLEDGED;
        private com.mongodb.WriteConcern userPermissionGroup = com.mongodb.WriteConcern.ACKNOWLEDGED;
        private com.mongodb.WriteConcern userRelationship = com.mongodb.WriteConcern.ACKNOWLEDGED;
        private com.mongodb.WriteConcern userRelationshipGroup = com.mongodb.WriteConcern.ACKNOWLEDGED;
        private com.mongodb.WriteConcern userRelationshipGroupMember = com.mongodb.WriteConcern.ACKNOWLEDGED;
        private com.mongodb.WriteConcern userVersion = com.mongodb.WriteConcern.ACKNOWLEDGED;

        private com.mongodb.WriteConcern message = com.mongodb.WriteConcern.ACKNOWLEDGED;
        private com.mongodb.WriteConcern messageStatus = com.mongodb.WriteConcern.ACKNOWLEDGED;
    }

}