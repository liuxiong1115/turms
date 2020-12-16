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

package im.turms.turms.workflow.dao.domain;

import im.turms.turms.workflow.dao.index.OptionalIndexedForExtendedFeature;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.PersistenceConstructor;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.Sharded;
import org.springframework.data.mongodb.core.mapping.ShardingStrategy;

import java.util.Date;
import java.util.List;

/**
 * @author James Chen
 */
@Data
@AllArgsConstructor(onConstructor = @__(@PersistenceConstructor))
@Document
@CompoundIndex(
        name = GroupBlacklistedUser.Key.Fields.GROUP_ID + "_" + GroupBlacklistedUser.Key.Fields.USER_ID,
        def = "{'" + GroupBlacklistedUser.Fields.ID_GROUP_ID + "': 1, '" + GroupBlacklistedUser.Fields.ID_USER_ID + "': 1}")
@Sharded(shardKey = GroupBlacklistedUser.Fields.ID_GROUP_ID, shardingStrategy = ShardingStrategy.HASH, immutableKey = true)
public final class GroupBlacklistedUser {

    public static final String COLLECTION_NAME = "groupBlacklistedUser";

    @Id
    private final Key key;

    @Field(Fields.BLOCK_DATE)
    @OptionalIndexedForExtendedFeature
    private final Date blockDate;

    @Field(Fields.REQUESTER_ID)
    @OptionalIndexedForExtendedFeature
    private final Long requesterId;

    public GroupBlacklistedUser(Long groupId, Long userId, Date blockDate, Long requesterId) {
        this.key = new Key(groupId, userId);
        this.blockDate = blockDate;
        this.requesterId = requesterId;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor // Make sure spring can initiate the key and use setters
    public static final class Key {

        @Field(Fields.GROUP_ID)
        private Long groupId;

        @Field(Fields.USER_ID)
        private Long userId;

        public static class Fields {
            public static final String GROUP_ID = "gid";
            public static final String USER_ID = "uid";

            private Fields() {
            }
        }
    }

    @Data
    @AllArgsConstructor
    public static final class KeyList {
        private List<Key> keys;
    }

    public static class Fields {
        public static final String ID_GROUP_ID = "_id." + Key.Fields.GROUP_ID;
        public static final String ID_USER_ID = "_id." + Key.Fields.USER_ID;
        public static final String BLOCK_DATE = "bd";
        public static final String REQUESTER_ID = "rid";

        private Fields() {
        }
    }
}