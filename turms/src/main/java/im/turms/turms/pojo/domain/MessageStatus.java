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

import im.turms.common.constant.MessageDeliveryStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.PersistenceConstructor;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;
import java.util.List;

@Data
@Document
@FieldNameConstants
@AllArgsConstructor(onConstructor = @__(@PersistenceConstructor))
public final class MessageStatus {
    @Id
    private final Key key;

    @Indexed
    private final Long groupId;

    private final Boolean isSystemMessage;

    @Indexed
    private final Long senderId;

    private final MessageDeliveryStatus deliveryStatus;

    @Indexed
    private final Date receptionDate;

    @Indexed
    private final Date readDate;

    @Indexed
    private final Date recallDate;

    public MessageStatus(
            Long id,
            Long groupId,
            Boolean isSystemMessage,
            Long senderId,
            Long recipientId,
            MessageDeliveryStatus status,
            Date receptionDate,
            Date readDate,
            Date recallDate) {
        this.key = new Key(id, recipientId);
        this.groupId = groupId;
        this.isSystemMessage = isSystemMessage;
        this.senderId = senderId;
        this.deliveryStatus = status;
        this.receptionDate = receptionDate;
        this.readDate = readDate;
        this.recallDate = recallDate;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor // Make sure spring can initiate the key and use setters
    @EqualsAndHashCode
    public static final class Key {
        private Long messageId;

        @Indexed
        private Long recipientId;
    }

    @Data
    @AllArgsConstructor
    public static final class KeyList {
        private List<Key> keys;
    }
}