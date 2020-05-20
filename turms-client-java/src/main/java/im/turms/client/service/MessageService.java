package im.turms.client.service;

import com.google.protobuf.*;
import im.turms.client.TurmsClient;
import im.turms.client.model.MessageAddition;
import im.turms.client.util.MapUtil;
import im.turms.client.util.NotificationUtil;
import im.turms.common.TurmsStatusCode;
import im.turms.common.constant.MessageDeliveryStatus;
import im.turms.common.exception.TurmsBusinessException;
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
import im.turms.common.util.Validator;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MessageService {
    /**
     * Format: "@{userId}"
     * Example: "@{123}", "I need to talk with @{123} and @{321}"
     */
    private static final Function<Message, Set<Long>> DEFAULT_MENTIONED_USER_IDS_PARSER = new Function<>() {
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
    private Function<Message, Set<Long>> mentionedUserIdsParser;

    private BiFunction<Message, MessageAddition, Void> onMessage;

    public MessageService(TurmsClient turmsClient) {
        this.turmsClient = turmsClient;
        this.turmsClient.getDriver()
                .getOnNotificationListeners()
                .add(notification -> {
                    if (onMessage != null && notification.hasRelayedRequest()) {
                        TurmsRequest relayedRequest = notification.getRelayedRequest();
                        if (relayedRequest.hasCreateMessageRequest()) {
                            CreateMessageRequest createMessageRequest = relayedRequest.getCreateMessageRequest();
                            Message message = createMessageRequest2Message(notification.getRequestId().getValue(), createMessageRequest);
                            MessageAddition addition = parseMessageAddition(message);
                            onMessage.apply(message, addition);
                        }
                    }
                    return null;
                });
    }

    public void setOnMessage(BiFunction<Message, MessageAddition, Void> onMessage) {
        this.onMessage = onMessage;
    }

    public CompletableFuture<Long> sendMessage(
            boolean isGroupMessage,
            long targetId,
            @Nullable Date deliveryDate,
            @Nullable String text,
            @Nullable byte[] records,
            @Nullable Integer burnAfter) {
        if (text == null && records == null) {
            return CompletableFuture.failedFuture(TurmsBusinessException.get(TurmsStatusCode.ILLEGAL_ARGUMENTS, "text and records must not all be null"));
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
            @Nullable byte[] records) {
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

    public static byte[] generateLocationRecord(
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
        return builder.build().toByteArray();
    }

    public static byte[] generateAudioRecordByDescription(
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
                .build().toByteArray();
    }

    public static byte[] generateAudioRecordByData(byte[] data) {
        Validator.throwIfAnyFalsy((Object) data);
        BytesValue bytesValue = BytesValue.newBuilder()
                .setValue(ByteString.copyFrom(data))
                .build();
        return AudioFile.newBuilder()
                .setData(bytesValue)
                .build()
                .toByteArray();
    }

    public static byte[] generateVideoRecordByDescription(
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
                .toByteArray();
    }

    public static byte[] generateVideoRecordByData(byte[] data) {
        Validator.throwIfAnyFalsy((Object) data);
        BytesValue bytesValue = BytesValue.newBuilder()
                .setValue(ByteString.copyFrom(data))
                .build();
        return VideoFile.newBuilder()
                .setData(bytesValue)
                .build()
                .toByteArray();
    }

    public static byte[] generateImageRecordByData(byte[] data) {
        Validator.throwIfAnyFalsy((Object) data);
        BytesValue bytesValue = BytesValue.newBuilder()
                .setValue(ByteString.copyFrom(data))
                .build();
        return ImageFile.newBuilder()
                .setData(bytesValue)
                .build()
                .toByteArray();
    }

    public static byte[] generateImageRecordByDescription(
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
                .toByteArray();
    }

    public static byte[] generateFileRecordByDate(byte[] data) {
        Validator.throwIfAnyFalsy((Object) data);
        BytesValue bytesValue = BytesValue.newBuilder()
                .setValue(ByteString.copyFrom(data))
                .build();
        return File.newBuilder()
                .setData(bytesValue)
                .build()
                .toByteArray();
    }

    public static byte[] generateFileRecordByDescription(
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
                .toByteArray();
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