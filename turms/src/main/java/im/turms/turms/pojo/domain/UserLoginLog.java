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
import im.turms.turms.annotation.domain.OptionalIndexedForCustomFeature;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.Sharded;

import java.util.Date;
import java.util.Map;

/**
 * TODO: pre-split
 */
@Data
@Document
@Sharded(shardKey = UserLoginLog.Fields.LOGIN_DATE, immutableKey = true)
public final class UserLoginLog {
    @Id
    private final Long id;

    @Field(Fields.USER_ID)
    @OptionalIndexedForCustomFeature
    private final Long userId;

    /**
     * (TODO:https://github.com/turms-im/turms/issues/346) Use the custom TTL cron instead of the TTL index.
     */
    @Field(Fields.LOGIN_DATE)
    private final Date loginDate;

    @Field(Fields.LOGOUT_DATE)
    @OptionalIndexedForCustomFeature
    private final Date logoutDate;

    @Field(Fields.LOCATION_ID)
    @Indexed
    private final Long locationId;

    @Field(Fields.IP)
    private final Integer ip;

    @Field(Fields.DEVICE_TYPE)
    private final DeviceType deviceType;

    @Field(Fields.DEVICE_DETAILS)
    private final Map<String, String> deviceDetails;

    public static class Fields {
        public static final String USER_ID = "uid";
        public static final String LOGIN_DATE = "ld";
        public static final String LOGOUT_DATE = "ltd";
        public static final String LOCATION_ID = "lid";
        public static final String IP = "ip";
        public static final String DEVICE_TYPE = "dt";
        public static final String DEVICE_DETAILS = "dd";

        private Fields() {
        }
    }
}