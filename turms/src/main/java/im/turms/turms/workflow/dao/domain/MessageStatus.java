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

import im.turms.common.constant.MessageDeliveryStatus;
import im.turms.turms.workflow.dao.index.OptionalIndexedForExtendedFeature;
import im.turms.turms.workflow.dao.index.OptionalIndexedForStatistics;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.PersistenceConstructor;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.Sharded;

import java.util.Date;
import java.util.List;

/**
 * TODO: Remove
 *
 * @author James Chen
 */
@Data
@AllArgsConstructor(onConstructor = @__(@PersistenceConstructor))
@Document
@CompoundIndex(
        name = MessageStatus.Key.Fields.RECIPIENT_ID + "_" + MessageStatus.Key.Fields.MESSAGE_ID,
        def = "{'" + MessageStatus.Fields.ID_RECIPIENT_ID + "': 1, '" + MessageStatus.Fields.ID_MESSAGE_ID + "': 1}")
@Sharded(shardKey = {MessageStatus.Fields.ID_RECIPIENT_ID, MessageStatus.Fields.ID_MESSAGE_ID}, immutableKey = true)
public final class MessageStatus {

    public static final String COLLECTION_NAME = "messageStatus";

    @Id
    private final Key key;

    @Field(Fields.GROUP_ID)
    @OptionalIndexedForStatistics
    private final Long groupId;

    @Field(Fields.IS_SYSTEM_MESSAGE)
    private final Boolean isSystemMessage;

    @Field(Fields.SENDER_ID)
    @Indexed
    private final Long senderId;

    @Field(Fields.DELIVERY_STATUS)
    private final MessageDeliveryStatus deliveryStatus;

    @Field(Fields.RECEPTION_DATE)
    @OptionalIndexedForStatistics
    private final Date receptionDate;

    @Field(Fields.READ_DATE)
    @OptionalIndexedForStatistics
    private final Date readDate;

    @Field(Fields.RECALL_DATE)
    @OptionalIndexedForStatistics
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
    public static final class Key {

        @Field(Fields.MESSAGE_ID)
        private Long messageId;

        @Field(Fields.RECIPIENT_ID)
        @OptionalIndexedForExtendedFeature //?
        private Long recipientId;

        public static class Fields {
            public static final String MESSAGE_ID = "mid";
            public static final String RECIPIENT_ID = "rid";

            private Fields() {
            }
        }
    }

    public static class Fields {
        public static final String ID_MESSAGE_ID = "_id." + Key.Fields.MESSAGE_ID;
        public static final String ID_RECIPIENT_ID = "_id." + Key.Fields.RECIPIENT_ID;
        public static final String GROUP_ID = "gid";
        public static final String IS_SYSTEM_MESSAGE = "sm";
        public static final String SENDER_ID = "sid";
        public static final String DELIVERY_STATUS = "ds";
        public static final String RECEPTION_DATE = "rd";
        public static final String READ_DATE = "rdd";
        public static final String RECALL_DATE = "rcd";

        private Fields() {
        }
    }

    @Data
    @AllArgsConstructor
    public static final class KeyList {
        private List<Key> keys;
    }
}