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

import com.mongodb.DBObject;
import im.turms.turms.annotation.domain.AdminActionLogShardKey;
import im.turms.turms.annotation.domain.OptionalIndexedForDifferentAmount;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.Date;

/**
 * There is no need to shard for most groups. Thus, sharding is disabled by default.
 * <p>
 * Don't use the capped collection because:
 * 1. Turms allows admins to delete logs by default while the capped collection forbids deletion operations.
 * By the way, both Turms and the capped collection don't allow the update operations.
 * 2. (TODO:https://github.com/turms-im/turms/issues/346) The custom TTL cron is enough and more flexible.
 * <p>
 * Compound index: Except querying by key in the non-sharded collections, the log date should be always specified to acquire a better performance.
 * If your group has a lot of accounts you can add the account field as a part of the compound index
 * <p>
 * Note that:
 * 1. If you need to use the shard key, make sure that you has split the collection because the log date increases monotonically.
 * <p>
 * 2. You can disable the persistence of AdminActionLog data if you have used the load balancing servers to log.
 */
@Data
@AllArgsConstructor
@Document
@CompoundIndex(
        name = AdminActionLog.Fields.LOG_DATE + "_" + AdminActionLog.Fields.ACTION + "_idx",
        def = "{'" + AdminActionLog.Fields.LOG_DATE + "': 1, '" + AdminActionLog.Fields.ACTION + "': 1}")
@AdminActionLogShardKey
public final class AdminActionLog {

    @Id
    private final Long id;

    @Field(Fields.ACCOUNT)
    @OptionalIndexedForDifferentAmount
    private final String account;

    /**
     * TODO:https://github.com/turms-im/turms/issues/346) Use the custom TTL cron instead of the TTL index.
     */
    @Field(Fields.LOG_DATE)
    private final Date logDate;

    @Field(Fields.IP)
    @OptionalIndexedForDifferentAmount
    private final Integer ip;

    /**
     * Indexed as a part of the compound index because of its medium index selectivity.
     * No need to change it
     */
    @Field(Fields.ACTION)
    private final String action;

    @Field(Fields.PARAMS)
    private final DBObject params;

    @Field(Fields.BODY)
    private final DBObject body;

    public static class Fields {
        public static final String ACCOUNT = "acct";
        public static final String LOG_DATE = "ld";
        public static final String IP = "ip";
        public static final String ACTION = "act";
        public static final String PARAMS = "param";
        public static final String BODY = "body";

        private Fields() {
        }
    }
}