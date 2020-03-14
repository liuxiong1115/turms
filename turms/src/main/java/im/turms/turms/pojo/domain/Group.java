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

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.experimental.FieldNameConstants;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;

@Data
@Document
@FieldNameConstants
@AllArgsConstructor
public final class Group {
    @Id
    private final Long id;

    private final Long typeId;

    @Indexed
    private final Long creatorId;

    @Indexed
    private final Long ownerId;

    private final String name;

    private final String intro;

    private final String announcement;

    private final Integer minimumScore;

    @Indexed
    private final Date creationDate;

    @Indexed
    private final Date deletionDate;

    @Indexed
    private final Date muteEndDate;

    private final Boolean active;
}