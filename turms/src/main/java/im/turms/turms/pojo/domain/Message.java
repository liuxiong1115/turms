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

import im.turms.common.constant.ChatType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.experimental.FieldNameConstants;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;
import java.util.List;

@Data
@AllArgsConstructor
@Document
@FieldNameConstants
public final class Message {
    @Id
    private final Long id;

    @Indexed
    private final ChatType chatType;

    @Indexed
    private final Boolean isSystemMessage;

    @Indexed
    private final Date deliveryDate;

    @Indexed
    private final Date deletionDate;

    private final String text;

    @Indexed
    private final Long senderId;

    /**
     * Use "target" rather than "recipient" because the target may be a recipient or a group.
     */
    @Indexed
    private final Long targetId;

    /**
     * Use list to keep order
     */
    private final List<byte[]> records;

    private final Integer burnAfter;

    @Indexed
    private final Long referenceId;

    public Long groupId() {
        return chatType == ChatType.GROUP ? targetId : null;
    }
}