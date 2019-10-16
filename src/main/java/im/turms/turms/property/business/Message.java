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

package im.turms.turms.property.business;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonView;
import com.hazelcast.nio.ObjectDataInput;
import com.hazelcast.nio.ObjectDataOutput;
import com.hazelcast.nio.serialization.IdentifiedDataSerializable;
import im.turms.turms.config.hazelcast.IdentifiedDataFactory;
import im.turms.turms.property.MutablePropertiesView;
import lombok.Data;

import java.io.IOException;

@Data
@JsonIgnoreProperties({"factoryId", "id"})
public class Message implements IdentifiedDataSerializable {
    @JsonView(MutablePropertiesView.class)
    private TimeType timeType = TimeType.LOCAL_SERVER_TIME;
    @JsonView(MutablePropertiesView.class)
    private boolean checkIfTargetExists = false;

    /**
     * The maximum length of a message allowed to send.
     */
    @JsonView(MutablePropertiesView.class)
    private int maxTextLimit = 500;
    @JsonView(MutablePropertiesView.class)
    private int maxRecordsSizeBytes = 15 * 1024 * 1024;
    /**
     * Whether to persist messages in databases.
     * Note: If false, senders will not get the message id after sending a message and also cannot edit a message
     */
    @JsonView(MutablePropertiesView.class)
    private boolean messagePersistent = true;
    @JsonView(MutablePropertiesView.class)
    private boolean recordsPersistent = false;
    /**
     * Whether to persist the status of messages.
     * If messageStatusPersistent is false, users will not receive the messages sent to them when they were offline.
     * NOTE: This is a major factor that influences performance.
     * Scenario: If a group have 500 members, 500 MessageStatuses will be persisted
     * when a user send a message to the group
     */
    @JsonView(MutablePropertiesView.class)
    private boolean messageStatusPersistent = true;
    @JsonView(MutablePropertiesView.class)
    private boolean logicallyDeleteMessageByDefault = true;
    private ReadReceipt readReceipt = new ReadReceipt();
    @JsonView(MutablePropertiesView.class)
    private boolean allowSendingMessagesToStranger = false;
    @JsonView(MutablePropertiesView.class)
    private boolean allowSendingMessagesToOneself = false;
    @JsonView(MutablePropertiesView.class)
    private boolean deletePrivateMessageAfterAcknowledged = false;
    /**
     * NOTE: To recall messages, more system resources are needed. So, turn it off if you don't need it.
     */
    @JsonView(MutablePropertiesView.class)
    private boolean allowRecallingMessage = true;
    @JsonView(MutablePropertiesView.class)
    private boolean allowEditingMessageBySender = true;
    @JsonView(MutablePropertiesView.class)
    private int allowableRecallDurationSeconds = 60 * 5;
    private TypingStatus typingStatus = new TypingStatus();
    @JsonView(MutablePropertiesView.class)
    private int defaultMessagesNumberWithTotal = 1;
    @JsonView(MutablePropertiesView.class)
    private boolean updateReadDateAutomaticallyAfterUserQueryingMessage = true;

    @Override
    public int getFactoryId() {
        return IdentifiedDataFactory.FACTORY_ID;
    }

    @Override
    public int getId() {
        return IdentifiedDataFactory.Type.PROPERTY_MESSAGE.getValue();
    }

    @Override
    public void writeData(ObjectDataOutput out) throws IOException {
        out.writeInt(timeType.ordinal());
        out.writeBoolean(checkIfTargetExists);
        out.writeInt(maxTextLimit);
        out.writeInt(maxRecordsSizeBytes);
        out.writeBoolean(messagePersistent);
        out.writeBoolean(recordsPersistent);
        out.writeBoolean(messageStatusPersistent);
        out.writeBoolean(logicallyDeleteMessageByDefault);
        readReceipt.writeData(out);
        out.writeBoolean(allowSendingMessagesToStranger);
        out.writeBoolean(allowSendingMessagesToOneself);
        out.writeBoolean(deletePrivateMessageAfterAcknowledged);
        out.writeBoolean(allowRecallingMessage);
        out.writeBoolean(allowEditingMessageBySender);
        out.writeInt(allowableRecallDurationSeconds);
        typingStatus.writeData(out);
        out.writeInt(defaultMessagesNumberWithTotal);
        out.writeBoolean(updateReadDateAutomaticallyAfterUserQueryingMessage);
    }

    @Override
    public void readData(ObjectDataInput in) throws IOException {
        timeType = TimeType.values()[in.readInt()];
        checkIfTargetExists = in.readBoolean();
        maxTextLimit = in.readInt();
        maxRecordsSizeBytes = in.readInt();
        messagePersistent = in.readBoolean();
        recordsPersistent = in.readBoolean();
        messageStatusPersistent = in.readBoolean();
        logicallyDeleteMessageByDefault = in.readBoolean();
        readReceipt.readData(in);
        allowSendingMessagesToStranger = in.readBoolean();
        allowSendingMessagesToOneself = in.readBoolean();
        deletePrivateMessageAfterAcknowledged = in.readBoolean();
        allowRecallingMessage = in.readBoolean();
        allowEditingMessageBySender = in.readBoolean();
        allowableRecallDurationSeconds = in.readInt();
        typingStatus.readData(in);
        defaultMessagesNumberWithTotal = in.readInt();
        updateReadDateAutomaticallyAfterUserQueryingMessage = in.readBoolean();
    }

    public enum TimeType {
        CLIENT_TIME,
        LOCAL_SERVER_TIME
    }

    @Data
    @JsonIgnoreProperties({"factoryId", "id"})
    public static class TypingStatus implements IdentifiedDataSerializable {
        @JsonView(MutablePropertiesView.class)
        boolean enabled = true;

        @Override
        public int getFactoryId() {
            return IdentifiedDataFactory.FACTORY_ID;
        }

        @Override
        public int getId() {
            return IdentifiedDataFactory.Type.PROPERTY_MESSAGE_TYPING_STATUS.getValue();
        }

        @Override
        public void writeData(ObjectDataOutput out) throws IOException {
            out.writeBoolean(enabled);
        }

        @Override
        public void readData(ObjectDataInput in) throws IOException {
            enabled = in.readBoolean();
        }
    }

    @Data
    @JsonIgnoreProperties({"factoryId", "id"})
    public static class ReadReceipt implements IdentifiedDataSerializable {
        @JsonView(MutablePropertiesView.class)
        private boolean enabled = true;
        @JsonView(MutablePropertiesView.class)
        private boolean useServerTime = true;

        @Override
        public int getFactoryId() {
            return IdentifiedDataFactory.FACTORY_ID;
        }

        @Override
        public int getId() {
            return IdentifiedDataFactory.Type.PROPERTY_MESSAGE_READ_RECEIPT.getValue();
        }

        @Override
        public void writeData(ObjectDataOutput out) throws IOException {
            out.writeBoolean(enabled);
            out.writeBoolean(useServerTime);
        }

        @Override
        public void readData(ObjectDataInput in) throws IOException {
            enabled = in.readBoolean();
            useServerTime = in.readBoolean();
        }
    }
}