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

import im.turms.turms.annotation.domain.OptionalIndexedForCustomFeature;
import lombok.Data;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;
import org.springframework.data.mongodb.core.mapping.MongoId;

import java.util.Date;

/**
 * No need to shard because the size of the collections is still small even after 10 years
 * and just keep it simple by default.
 */
@Data
@Document
public final class UserOnlineUserNumber {

    @MongoId(FieldType.DATE_TIME)
    private final Date timestamp;

    @Field(Fields.NUMBER)
    @OptionalIndexedForCustomFeature
    private final Integer number;

    public static class Fields {
        public static final String NUMBER = "no";

        private Fields() {
        }
    }
}