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
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.Sharded;
import org.springframework.data.mongodb.core.mapping.ShardingStrategy;

import java.util.Date;
import java.util.List;

/**
 * Don't consolidate the two sided relationships as one model to eliminate transactions
 * (unless MongoDB supports multiple shard keys though it seems impossible https://jira.mongodb.org/browse/SERVER-2949).
 * Otherwise, it's impossible to use targeted queries to query the data
 * according to either the owner ID or the related user ID because MongoDB can have only one shard key.
 * https://github.com/turms-im/turms/issues/343
 *
 * @author James Chen
 */
@Data
@AllArgsConstructor(onConstructor = @__(@PersistenceConstructor))
@Document
@Sharded(shardKey = UserRelationship.Fields.ID_OWNER_ID, shardingStrategy = ShardingStrategy.HASH, immutableKey = true)
public final class UserRelationship {

    public static final String COLLECTION_NAME = "userRelationship";

    @Id
    private final Key key;

    @Field(Fields.IS_BLOCKED)
    private final Boolean isBlocked;

    @Field(Fields.ESTABLISHMENT_DATE)
    @OptionalIndexedForExtendedFeature
    private final Date establishmentDate;

    public UserRelationship(Long ownerId, Long relatedUserId, Boolean isBlocked, Date establishmentDate) {
        this.key = new Key(ownerId, relatedUserId);
        this.isBlocked = isBlocked;
        this.establishmentDate = establishmentDate;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor // Make sure spring can initiate the key and use setters
    public static final class Key {

        @Field(Fields.OWNER_ID)
        @Indexed
        private Long ownerId;

        @Field(Fields.RELATED_USER_ID)
        private Long relatedUserId;

        public static class Fields {
            public static final String OWNER_ID = "oid";
            public static final String RELATED_USER_ID = "rid";

            private Fields() {
            }
        }
    }

    public static class Fields {
        public static final String ID_OWNER_ID = "_id." + Key.Fields.OWNER_ID;
        public static final String ID_RELATED_USER_ID = "_id." + Key.Fields.RELATED_USER_ID;
        public static final String IS_BLOCKED = "bld";
        public static final String ESTABLISHMENT_DATE = "ed";

        private Fields() {
        }
    }

    @Data
    @AllArgsConstructor
    public static final class KeyList {
        private List<Key> keys;
    }
}