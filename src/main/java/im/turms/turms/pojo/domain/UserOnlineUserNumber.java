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

import lombok.Data;
import lombok.experimental.FieldNameConstants;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Data
@Document
@FieldNameConstants
public class UserOnlineUserNumber {
    /**
     * Use Instant rather than Date to fix the bug of Spring Data Mongo itself.
     * If persisting Date as ID, the timestamp will be persisted as ObjectID rather than Date in MongoDB
     * while Instant can be persisted as Date in MongoDB.
     * As a result, use Instant until Spring Data Mongo fixes this bug.
     */
    //
    @Id
    private Instant timestamp;

    @Indexed
    private Integer number;
}