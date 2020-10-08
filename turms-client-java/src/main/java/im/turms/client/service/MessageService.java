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

package im.turms.client.service;

import com.google.protobuf.*;
import im.turms.client.TurmsClient;
import im.turms.client.driver.TurmsDriver;
import im.turms.client.model.MessageAddition;
import im.turms.client.util.MapUtil;
import im.turms.client.util.NotificationUtil;
import im.turms.client.util.TurmsBusinessExceptionUtil;
import im.turms.common.annotation.NotEmpty;
import im.turms.common.constant.MessageDeliveryStatus;
import im.turms.common.constant.statuscode.TurmsStatusCode;
import im.turms.common.model.bo.file.AudioFile;
import im.turms.common.model.bo.file.File;
import im.turms.common.model.bo.file.ImageFile;
import im.turms.common.model.bo.file.VideoFile;
import im.turms.common.model.bo.message.Message;
import im.turms.common.model.bo.message.MessageStatus;
import im.turms.common.model.bo.message.MessagesWithTotal;
import im.turms.common.model.bo.user.UserLocation;
import im.turms.common.model.dto.request.TurmsRequest;
import im.turms.common.model.dto.request.message.*;
import im.turms.common.model.dto.request.signal.AckRequest;
import im.turms.common.util.Validator;
import java8.util.concurrent.CompletableFuture;
import java8.util.function.BiConsumer;
import java8.util.function.Function;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author James Chen
 */
public class MessageService {

    /**
     * Format: "@{userId}"
     * <p>
     * Example: "@{123}", "I need to talk with @{123} and @{321}"
     */
    private static final Function<Message, Set<Long>> DEFAULT_MENTIONED_USER_IDS_PARSER = new Function<Message, Set<Long>>() {
        private final Pattern regex = Pattern.compile("@\\{(\\d+?)}");

        @Override
        public Set<Long> apply(Message message) {
            if (message != null && message.hasText()) {
                String text = message.getText().getValue();
                Matcher matcher = regex.matcher(text);
                Set<Long> userIds = new LinkedHashSet<>();
                while (matcher.find()) {
                    String group = matcher.group(1);
                    userIds.add(Long.parseLong(group));
                }
                return userIds;
            }
            return Collections.emptySet();
        }
    };

    private final TurmsClient turmsClient;
    private final ConcurrentLinkedQueue<Long> unacknowledgedMessageIds;

    private Function<Message, Set<Long>> mentionedUserIdsParser;

    private BiConsumer<Message, MessageAddition> onMessage;

    /**
     * @param ackMessageInterval null if don't want to acknowledge messages automatically;
     *                           <=0 if want to acknowledge messages once received
     *                           >0 if wan to acknowledge at specified interval
     */
    public MessageService(TurmsClient turmsClient, @Nullable Integer ackMessageInterval) {
        this.turmsClient = turmsClient;
        if (ackMessageInterval != null && ackMessageInterval > 0) {
            unacknowledgedMessageIds = new ConcurrentLinkedQueue<>();
            startAckMessagesTimer(ackMessageInterval);
        } else {
            unacknowledgedMessageIds = null;
        }
        this.turmsClient.getDriver()
                .addOnNotificationListener(notification -> {
                    if (onMessage != null && notification.hasRelayedRequest()) {
                        TurmsRequest relayedRequest = notification.getRelayedRequest();
                        if (relayedRequest.hasCreateMessageRequest()) {
                            CreateMessageRequest createMessageRequest = relayedRequest.getCreateMessageRequest();
                            if (ackMessageInterval != null) {
                                long messageId = createMessageRequest.getMessageId().getValue();
                                if (ackMessageInterval > 0) {
                                    unacknowledgedMessageIds.add(messageId);
                                } else {
                                    ackMessages(Collections.singletonList(messageId));
                                }
                            }

                            long requesterId = notification.getRequesterId().getValue();
                            Message message = createMessageRequest2Message(requesterId, createMessageRequest);
                            MessageAddition addition = parseMessageAddition(message);
                            onMessage.accept(message, addition);
                        }
                    }
                });
    }

    public void setOnMessage(BiConsumer<Message, MessageAddition> onMessage) {
        this.onMessage = onMessage;
    }

    public CompletableFuture<Long> sendMessage(
            boolean isGroupMessage,
            long targetId,
            @Nullable Date deliveryDate,
            @Nullable String text,
            @Nullable List<ByteBuffer> records,
            @Nullable Integer burnAfter) {
        if (text == null && records == null) {
            return TurmsBusinessExceptionUtil.getFuture(TurmsStatusCode.ILLEGAL_ARGUMENTS, "text and records must not all be null");
        }
        if (deliveryDate == null) {
            deliveryDate = new Date();
        }
        return turmsClient.getDriver()
                .send(CreateMessageRequest.newBuilder(), MapUtil.of(
                        "group_id", isGroupMessage ? targetId : null,
                        "recipient_id", !isGroupMessage ? targetId : null,
                        "delivery_date", deliveryDate,
                        "text", text,
                        "records", records,
                        "burn_after", burnAfter))
                .thenApply(NotificationUtil::getFirstId);
    }

    public CompletableFuture<Void> ackMessages(@NotEmpty List<Long> messageIds) {
        if (messageIds == null || messageIds.isEmpty()) {
            return TurmsBusinessExceptionUtil.getFuture(TurmsStatusCode.ILLEGAL_ARGUMENTS, "messageIds must not be null or empty");
        }
        return turmsClient.getDriver()
                .send(AckRequest.newBuilder(), MapUtil.of("messages_ids", messageIds))
                .thenApply(notification -> null);
    }

    public CompletableFuture<Long> forwardMessage(
            long messageId,
            boolean isGroupMessage,
            long targetId) {
        return turmsClient.getDriver()
                .send(CreateMessageRequest.newBuilder(), MapUtil.of(
                        "message_id", messageId,
                        "group_id", isGroupMessage ? targetId : null,
                        "recipient_id", !isGroupMessage ? targetId : null))
                .thenApply(NotificationUtil::getFirstId);
    }

    public CompletableFuture<Void> updateSentMessage(
            long messageId,
            @Nullable String text,
            @Nullable ByteBuffer records) {
        if (Validator.areAllFalsy(text, records)) {
            return CompletableFuture.completedFuture(null);
        }
        return turmsClient.getDriver()
                .send(UpdateMessageRequest.newBuilder(), MapUtil.of(
                        "message_id", messageId,
                        "text", text,
                        "records", records))
                .thenApply(notification -> null);
    }

    public CompletableFuture<List<Message>> queryMessages(
            @Nullable List<Long> ids,
            @Nullable Boolean areGroupMessages,
            @Nullable Boolean areSystemMessages,
            @Nullable Long senderId,
            @Nullable Date deliveryDateStart,
            @Nullable Date deliveryDateEnd,
            @Nullable MessageDeliveryStatus deliveryStatus,
            Integer size) {
        if (size == null) {
            size = 50;
        }
        return turmsClient.getDriver()
                .send(QueryMessagesRequest.newBuilder(), MapUtil.of(
                        "ids", ids,
                        "are_group_messages", areGroupMessages,
                        "are_system_messages", areSystemMessages,
                        "from_id", senderId,
                        "delivery_date_after", deliveryDateStart,
                        "delivery_date_before", deliveryDateEnd,
                        "size", size,
                        "delivery_status", deliveryStatus))
                .thenApply(notification -> notification.getData().getMessages().getMessagesList());
    }

    public CompletableFuture<List<MessagesWithTotal>> queryPendingMessagesWithTotal(@Nullable Integer size) {
        if (size == null) {
            size = 1;
        }
        return turmsClient.getDriver()
                .send(QueryPendingMessagesWithTotalRequest.newBuilder(), MapUtil.of(
                        "size", size))
                .thenApply(notification -> notification.getData().getMessagesWithTotalList().getMessagesWithTotalListList());
    }

    public CompletableFuture<List<MessageStatus>> queryMessageStatus(long messageId) {
        return turmsClient.getDriver()
                .send(QueryMessageStatusesRequest.newBuilder(), MapUtil.of(
                        "message_id", messageId))
                .thenApply(notification -> notification.getData().getMessageStatuses().getMessageStatusesList());
    }

    public CompletableFuture<Void> recallMessage(long messageId, @Nullable Date recallDate) {
        if (recallDate == null) {
            recallDate = new Date();
        }
        return turmsClient.getDriver()
                .send(UpdateMessageRequest.newBuilder(), MapUtil.of(
                        "message_id", messageId,
                        "recall_date", recallDate))
                .thenApply(notification -> null);
    }

    public CompletableFuture<Void> readMessage(long messageId, @Nullable Date readDate) {
        if (readDate == null) {
            readDate = new Date();
        }
        return turmsClient.getDriver()
                .send(UpdateMessageRequest.newBuilder(), MapUtil.of(
                        "message_id", messageId,
                        "read_date", readDate))
                .thenApply(notification -> null);
    }

    public CompletableFuture<Void> markMessageUnread(long messageId) {
        return this.readMessage(messageId, new Date(0));
    }

    public CompletableFuture<Void> updateTypingStatusRequest(
            boolean isGroupMessage,
            long targetId) {
        return turmsClient.getDriver()
                .send(UpdateTypingStatusRequest.newBuilder(), MapUtil.of(
                        "is_group_message", isGroupMessage,
                        "to_id", targetId))
                .thenApply(notification -> null);
    }

    public boolean isMentionEnabled() {
        return mentionedUserIdsParser != null;
    }

    public void enableMention() {
        if (mentionedUserIdsParser == null) {
            mentionedUserIdsParser = DEFAULT_MENTIONED_USER_IDS_PARSER;
        }
    }

    public void enableMention(@NotNull Function<Message, Set<Long>> mentionedUserIdsParser) {
        if (mentionedUserIdsParser == null) {
            throw new IllegalArgumentException("mentionedUserIdsParser must not be null");
        }
        this.mentionedUserIdsParser = mentionedUserIdsParser;
    }

    public static ByteBuffer generateLocationRecord(
            float latitude,
            float longitude,
            @Nullable String locationName,
            @Nullable String address) {
        UserLocation.Builder builder = UserLocation.newBuilder()
                .setLatitude(latitude)
                .setLongitude(longitude);
        if (locationName != null) {
            builder.setName(StringValue.newBuilder().setValue(locationName).build());
        }
        if (address != null) {
            builder.setAddress(StringValue.newBuilder().setValue(address).build());
        }
        return builder.build().toByteString().asReadOnlyByteBuffer();
    }

    public static ByteBuffer generateAudioRecordByDescription(
            @NotNull String url,
            @Nullable Integer duration,
            @Nullable String format,
            @Nullable Integer size) {
        Validator.throwIfAnyFalsy(url);
        AudioFile.Description.Builder builder = AudioFile.Description.newBuilder();
        builder.setUrl(url);
        if (duration != null) {
            builder.setDuration(Int32Value.newBuilder().setValue(duration).build());
        }
        if (format != null) {
            builder.setFormat(StringValue.newBuilder().setValue(format).build());
        }
        if (size != null) {
            builder.setSize(Int32Value.newBuilder().setValue(size).build());
        }
        return AudioFile.newBuilder()
                .setDescription(builder)
                .build()
                .toByteString()
                .asReadOnlyByteBuffer();
    }

    public static ByteBuffer generateAudioRecordByData(byte[] data) {
        Validator.throwIfAnyFalsy((Object) data);
        BytesValue bytesValue = BytesValue.newBuilder()
                .setValue(ByteString.copyFrom(data))
                .build();
        return AudioFile.newBuilder()
                .setData(bytesValue)
                .build()
                .toByteString()
                .asReadOnlyByteBuffer();
    }

    public static ByteBuffer generateVideoRecordByDescription(
            @NotNull String url,
            @Nullable Integer duration,
            @Nullable String format,
            @Nullable Integer size) {
        Validator.throwIfAnyFalsy(url);
        VideoFile.Description.Builder builder = VideoFile.Description.newBuilder()
                .setUrl(url);
        if (duration != null) {
            builder.setDuration(Int32Value.newBuilder().setValue(duration).build());
        }
        if (format != null) {
            builder.setFormat(StringValue.newBuilder().setValue(format).build());
        }
        if (size != null) {
            builder.setSize(Int32Value.newBuilder().setValue(size).build());
        }
        return VideoFile.newBuilder()
                .setDescription(builder)
                .build()
                .toByteString()
                .asReadOnlyByteBuffer();
    }

    public static ByteBuffer generateVideoRecordByData(byte[] data) {
        Validator.throwIfAnyFalsy((Object) data);
        BytesValue bytesValue = BytesValue.newBuilder()
                .setValue(ByteString.copyFrom(data))
                .build();
        return VideoFile.newBuilder()
                .setData(bytesValue)
                .build()
                .toByteString()
                .asReadOnlyByteBuffer();
    }

    public static ByteBuffer generateImageRecordByData(byte[] data) {
        Validator.throwIfAnyFalsy((Object) data);
        BytesValue bytesValue = BytesValue.newBuilder()
                .setValue(ByteString.copyFrom(data))
                .build();
        return ImageFile.newBuilder()
                .setData(bytesValue)
                .build()
                .toByteString()
                .asReadOnlyByteBuffer();
    }

    public static ByteBuffer generateImageRecordByDescription(
            @NotNull String url,
            @Nullable Integer fileSize,
            @Nullable Integer imageSize,
            @Nullable Boolean original) {
        Validator.throwIfAnyFalsy(url);
        ImageFile.Description.Builder builder = ImageFile.Description.newBuilder()
                .setUrl(url);
        if (fileSize != null) {
            builder.setFileSize(Int32Value.newBuilder().setValue(fileSize).build());
        }
        if (imageSize != null) {
            builder.setImageSize(Int32Value.newBuilder().setValue(imageSize).build());
        }
        if (original != null) {
            builder.setOriginal(BoolValue.newBuilder().setValue(original).build());
        }
        return ImageFile.newBuilder()
                .setDescription(builder)
                .build()
                .toByteString()
                .asReadOnlyByteBuffer();
    }

    public static ByteBuffer generateFileRecordByDate(byte[] data) {
        Validator.throwIfAnyFalsy((Object) data);
        BytesValue bytesValue = BytesValue.newBuilder()
                .setValue(ByteString.copyFrom(data))
                .build();
        return File.newBuilder()
                .setData(bytesValue)
                .build()
                .toByteString()
                .asReadOnlyByteBuffer();
    }

    public static ByteBuffer generateFileRecordByDescription(
            @NotNull String url,
            @Nullable String format,
            @Nullable Integer size) {
        Validator.throwIfAnyFalsy(url);
        File.Description.Builder builder = File.Description.newBuilder()
                .setUrl(url);
        if (format != null) {
            builder.setFormat(StringValue.newBuilder().setValue(format).build());
        }
        if (size != null) {
            builder.setSize(Int32Value.newBuilder().setValue(size).build());
        }
        return File.newBuilder()
                .setDescription(builder)
                .build()
                .toByteString()
                .asReadOnlyByteBuffer();
    }

    private void startAckMessagesTimer(int ackMessageInterval) {
        TurmsDriver.scheduledService.scheduleWithFixedDelay(() -> {
            if (!unacknowledgedMessageIds.isEmpty()) {
                List<Long> unacknowledgedMessageIdList = new LinkedList<>();
                while (!unacknowledgedMessageIds.isEmpty()) {
                    Long messageId = unacknowledgedMessageIds.poll();
                    unacknowledgedMessageIdList.add(messageId);
                }
                if (!unacknowledgedMessageIdList.isEmpty()) {
                    ackMessages(unacknowledgedMessageIdList)
                            .whenComplete((unused, throwable) -> {
                                if (throwable != null) {
                                    unacknowledgedMessageIds.addAll(unacknowledgedMessageIdList);
                                }
                            });
                }
            }
        }, ackMessageInterval, ackMessageInterval, TimeUnit.MILLISECONDS);
    }

    private MessageAddition parseMessageAddition(Message message) {
        Set<Long> mentionedUserIds;
        if (mentionedUserIdsParser != null) {
            mentionedUserIds = mentionedUserIdsParser.apply(message);
        } else {
            mentionedUserIds = Collections.emptySet();
        }
        boolean isMentioned = mentionedUserIds.contains(turmsClient.getUserService().getUserId());
        return new MessageAddition(isMentioned, mentionedUserIds);
    }

    private Message createMessageRequest2Message(long requesterId, CreateMessageRequest request) {
        Message.Builder builder = Message.newBuilder();
        if (request.hasMessageId()) {
            builder.setId(request.getMessageId());
        }
        builder.setDeliveryDate(Int64Value.newBuilder().setValue(request.getDeliveryDate()).build());

        if (request.hasText()) {
            builder.setText(request.getText());
        }
        if (request.getRecordsCount() > 0) {
            builder.addAllRecords(request.getRecordsList());
        }
        builder.setSenderId(Int64Value.newBuilder().setValue(requesterId).build());
        if (request.hasGroupId()) {
            builder.setGroupId(request.getGroupId());
        }
        if (request.hasRecipientId()) {
            builder.setRecipientId(request.getRecipientId());
        }
        return builder.build();
    }

}