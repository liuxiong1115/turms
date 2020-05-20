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
import im.turms.turms.annotation.domain.OptionalGeoSpatialIndexedForBasicFeature;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.Sharded;

import java.io.Serializable;
import java.util.Date;

/**
 * Pre-split
 */
@Data
@AllArgsConstructor
@Document
@CompoundIndex(
        name = UserLocation.Fields.TIMESTAMP + "_" + UserLocation.Fields.USER_ID + "_idx",
        def = "{'" + UserLocation.Fields.TIMESTAMP + "': 1, '" + UserLocation.Fields.USER_ID + "': 1}")
@Sharded(shardKey = {UserLocation.Fields.TIMESTAMP, UserLocation.Fields.USER_ID}, immutableKey = true)
public final class UserLocation implements Serializable {
    @Id
    private final Long id;

    @Field(Fields.USER_ID)
    private final Long userId;

    @Field(Fields.DEVICE_TYPE)
    private final DeviceType deviceType;

    /**
     * Use the legacy coordinate pairs to calculate distances
     * on a Euclidean plane instead of an earth-like sphere
     */
    @Field(Fields.COORDINATES)
    @OptionalGeoSpatialIndexedForBasicFeature
    private final float[] coordinates;

    @Field(Fields.NAME)
    private final String name;

    @Field(Fields.ADDRESS)
    private final String address;

    @Field(Fields.TIMESTAMP)
    @Indexed
    private final Date timestamp;

    public static class Fields {
        public static final String USER_ID = "uid";
        public static final String DEVICE_TYPE = "dt";
        public static final String COORDINATES = "coord";
        public static final String NAME = "n";
        public static final String ADDRESS = "adr";
        public static final String TIMESTAMP = "tim";

        private Fields() {
        }
    }
}