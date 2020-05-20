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

import im.turms.common.constant.DeviceType;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.Sharded;

import java.util.Date;

@Data
@AllArgsConstructor
@Document
@Sharded(shardKey = {UserActionLog.Fields.LOG_DATE, UserActionLog.Fields.USER_ID}, immutableKey = true)
public final class UserActionLog {
    @Id
    private final Long id;

    @Field(Fields.USER_ID)
    @Indexed
    private final Long userId;

    @Field(Fields.DEVICE_TYPE)
    private final DeviceType deviceType;

    /**
     * (TODO:https://github.com/turms-im/turms/issues/346) Use the custom TTL cron instead of the TTL index.
     */
    @Field(Fields.LOG_DATE)
    private final Date logDate;

    @Field(Fields.IP)
    @Indexed
    private final Integer ip;

    @Field(Fields.ACTION)
    private final String action;

    @Field(Fields.DETAILS)
    private final String details;

    public static final class Fields {
        public static final String USER_ID = "uid";
        public static final String DEVICE_TYPE = "dt";
        public static final String LOG_DATE = "ld";
        public static final String IP = "ip";
        public static final String ACTION = "act";
        public static final String DETAILS = "dets";

        private Fields() {
        }
    }
}