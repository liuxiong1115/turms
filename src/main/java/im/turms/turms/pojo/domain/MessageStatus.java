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

import im.turms.turms.constant.MessageDeliveryStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Document
@FieldNameConstants
public class MessageStatus {
    @Id
    private Key key;

    @Indexed
    private Long groupId;

    @Indexed
    private Long senderId;

    @Indexed
    private MessageDeliveryStatus deliveryStatus;

    @Indexed
    private Date receptionDate;

    @Indexed
    private Date readDate;

    @Indexed
    private Date recallDate;

    public MessageStatus(
            Long id,
            Long groupId,
            Long senderId,
            Long recipientId,
            MessageDeliveryStatus status) {
        this.key = new Key(id, recipientId);
        this.groupId = groupId;
        this.senderId = senderId;
        this.deliveryStatus = status;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Key {
        @Indexed
        private Long messageId;

        @Indexed
        private Long recipientId;
    }
}