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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonView;
import com.hazelcast.nio.ObjectDataInput;
import com.hazelcast.nio.ObjectDataOutput;
import com.hazelcast.nio.serialization.IdentifiedDataSerializable;
import im.turms.turms.config.hazelcast.IdentifiedDataFactory;
import im.turms.turms.property.MutablePropertiesView;
import jdk.jfr.Description;
import lombok.Data;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

import javax.validation.Valid;
import javax.validation.constraints.Min;
import java.io.IOException;

@Data
public class Message implements IdentifiedDataSerializable {
    @Description("The time type for the delivery time of message")
    private TimeType timeType = TimeType.LOCAL_SERVER_TIME;

    @JsonView(MutablePropertiesView.class)
    @Description("Whether to check if the target (recipient or group) of a message is active and not deleted")
    private boolean checkIfTargetActiveAndNotDeleted = true;

    @JsonView(MutablePropertiesView.class)
    @Description("The maximum allowed length for the text of a message")
    @Min(0)
    private int maxTextLimit = 500;

    @JsonView(MutablePropertiesView.class)
    @Description("The maximum allowed size for the records of a message")
    @Min(0)
    private int maxRecordsSizeBytes = 15 * 1024 * 1024;

    @Description("The maximum message size to relay directly." +
            "If the size of a message is below the specified threshold," +
            "the server will check the recipient' status first and send the message if the recipient is online")
    @Min(0)
    private int maxMessageSizeToRelayDirectly = 5 * 1024;

    @JsonView(MutablePropertiesView.class)
    @Description("Whether to persist messages in databases.\n" +
            "Note: If false, senders will not get the message ID after the message has sent and cannot edit it")
    private boolean messagePersistent = true;

    @JsonView(MutablePropertiesView.class)
    @Description("Whether to persist the records of messages in databases")
    private boolean recordsPersistent = false;

    @JsonView(MutablePropertiesView.class)
    @Description("Whether to persist the status of messages.\n" +
            "If false, users will not receive the messages sent to them when they were offline.\n" +
            "NOTE: This is a major factor that affects performance")
    private boolean messageStatusPersistent = true;

    @JsonView(MutablePropertiesView.class)
    @Description("A message will become expired after the TTL has elapsed. 0 means infinite")
    @Min(0)
    private int messageTimeToLiveHours = 0;

    @JsonView(MutablePropertiesView.class)
    @Description("Whether to delete messages logically by default")
    private boolean deleteMessageLogicallyByDefault = true;

    @JsonView(MutablePropertiesView.class)
    @Description("Whether to allow users to send messages to a stranger")
    private boolean allowSendingMessagesToStranger = false;

    @JsonView(MutablePropertiesView.class)
    @Description("Whether to allow users to send messages to themselves")
    private boolean allowSendingMessagesToOneself = false;

    @JsonView(MutablePropertiesView.class)
    @Description("Whether to delete private messages after acknowledged by the recipient")
    private boolean deletePrivateMessageAfterAcknowledged = false;

    @JsonView(MutablePropertiesView.class)
    @Description("Whether to allow users to recall messages.\n" +
            "Note: To recall messages, more system resources are needed")
    private boolean allowRecallingMessage = true;

    @JsonView(MutablePropertiesView.class)
    @Description("Whether to allow the sender of a message to edit the message")
    private boolean allowEditingMessageBySender = true;

    @JsonView(MutablePropertiesView.class)
    @Description("The available recall duration for the sender of a message")
    @Min(0)
    private int availableRecallDurationSeconds = 60 * 5;

    @JsonView(MutablePropertiesView.class)
    @Description("The default available messages number with the \"total\" field that users request")
    @Min(0)
    private int defaultAvailableMessagesNumberWithTotal = 1;

    @JsonView(MutablePropertiesView.class)
    @Description("Whether to update the read date when users querying messages")
    private boolean updateReadDateWhenUserQueryingMessage = true;

    @JsonView(MutablePropertiesView.class)
    @Description("Whether to send the message to the other sender's online devices when sending a message")
    private boolean sendMessageToOtherSenderOnlineDevices = true;

    @Valid
    @NestedConfigurationProperty
    private ReadReceipt readReceipt = new ReadReceipt();

    @Valid
    @NestedConfigurationProperty
    private TypingStatus typingStatus = new TypingStatus();

    @JsonIgnore
    @Override
    public int getFactoryId() {
        return IdentifiedDataFactory.FACTORY_ID;
    }

    @JsonIgnore
    @Override
    public int getClassId() {
        return IdentifiedDataFactory.Type.PROPERTY_MESSAGE.getValue();
    }

    @Override
    public void writeData(ObjectDataOutput out) throws IOException {
        out.writeBoolean(checkIfTargetActiveAndNotDeleted);
        out.writeInt(maxTextLimit);
        out.writeInt(maxRecordsSizeBytes);
        out.writeBoolean(messagePersistent);
        out.writeBoolean(recordsPersistent);
        out.writeBoolean(messageStatusPersistent);
        out.writeInt(messageTimeToLiveHours);
        out.writeBoolean(deleteMessageLogicallyByDefault);
        readReceipt.writeData(out);
        out.writeBoolean(allowSendingMessagesToStranger);
        out.writeBoolean(allowSendingMessagesToOneself);
        out.writeBoolean(deletePrivateMessageAfterAcknowledged);
        out.writeBoolean(allowRecallingMessage);
        out.writeBoolean(allowEditingMessageBySender);
        out.writeInt(availableRecallDurationSeconds);
        typingStatus.writeData(out);
        out.writeInt(defaultAvailableMessagesNumberWithTotal);
        out.writeBoolean(updateReadDateWhenUserQueryingMessage);
        out.writeBoolean(sendMessageToOtherSenderOnlineDevices);
    }

    @Override
    public void readData(ObjectDataInput in) throws IOException {
        checkIfTargetActiveAndNotDeleted = in.readBoolean();
        maxTextLimit = in.readInt();
        maxRecordsSizeBytes = in.readInt();
        messagePersistent = in.readBoolean();
        recordsPersistent = in.readBoolean();
        messageStatusPersistent = in.readBoolean();
        messageTimeToLiveHours = in.readInt();
        deleteMessageLogicallyByDefault = in.readBoolean();
        readReceipt.readData(in);
        allowSendingMessagesToStranger = in.readBoolean();
        allowSendingMessagesToOneself = in.readBoolean();
        deletePrivateMessageAfterAcknowledged = in.readBoolean();
        allowRecallingMessage = in.readBoolean();
        allowEditingMessageBySender = in.readBoolean();
        availableRecallDurationSeconds = in.readInt();
        typingStatus.readData(in);
        defaultAvailableMessagesNumberWithTotal = in.readInt();
        updateReadDateWhenUserQueryingMessage = in.readBoolean();
        sendMessageToOtherSenderOnlineDevices = in.readBoolean();
    }

    public enum TimeType {
        CLIENT_TIME,
        LOCAL_SERVER_TIME
    }

    @Data
    public static class TypingStatus implements IdentifiedDataSerializable {
        @JsonView(MutablePropertiesView.class)
        @Description("Whether to notify users of typing statuses sent by other users")
        boolean enabled = true;

        @JsonIgnore
        @Override
        public int getFactoryId() {
            return IdentifiedDataFactory.FACTORY_ID;
        }

        @JsonIgnore
        @Override
        public int getClassId() {
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
    public static class ReadReceipt implements IdentifiedDataSerializable {
        @JsonView(MutablePropertiesView.class)
        @Description("Whether to allow to update the read date of messages")
        private boolean enabled = true;

        @JsonView(MutablePropertiesView.class)
        @Description("Whether to use server time to set the read date of messages")
        private boolean useServerTime = true;

        @JsonIgnore
        @Override
        public int getFactoryId() {
            return IdentifiedDataFactory.FACTORY_ID;
        }

        @JsonIgnore
        @Override
        public int getClassId() {
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