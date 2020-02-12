package im.turms.client.incubator.service;

import com.google.protobuf.*;
import im.turms.client.incubator.TurmsClient;
import im.turms.client.incubator.util.MapUtil;
import im.turms.turms.common.Validator;
import im.turms.turms.constant.ChatType;
import im.turms.turms.constant.MessageDeliveryStatus;
import im.turms.turms.pojo.bo.file.AudioFile;
import im.turms.turms.pojo.bo.file.File;
import im.turms.turms.pojo.bo.file.ImageFile;
import im.turms.turms.pojo.bo.file.VideoFile;
import im.turms.turms.pojo.bo.message.Message;
import im.turms.turms.pojo.bo.message.MessageStatus;
import im.turms.turms.pojo.bo.message.MessagesWithTotal;
import im.turms.turms.pojo.bo.user.UserLocation;
import im.turms.turms.pojo.notification.TurmsNotification;
import im.turms.turms.pojo.request.message.*;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

public class MessageService {
    private TurmsClient turmsClient;
    public Function<Message, Void> onMessage;

    public MessageService(TurmsClient turmsClient) {
        this.turmsClient = turmsClient;
        this.turmsClient.getDriver()
                .getOnNotificationListeners()
                .add(notification -> {
                    if (onMessage != null && notification.hasData()) {
                        TurmsNotification.Data data = notification.getData();
                        if (data.hasMessages()) {
                            for (Message message : data.getMessages().getMessagesList()) {
                                onMessage.apply(message);
                            }
                        }
                    }
                    return null;
                });
    }

    public CompletableFuture<Long> sendMessage(
            @NotNull ChatType chatType,
            long toId,
            @NotNull Date deliveryDate,
            @Nullable String text,
            @Nullable byte[] records,
            @Nullable Integer burnAfter) {
        Validator.throwIfAnyFalsy(chatType, deliveryDate);
        Validator.throwIfAllFalsy(text, records);
        return turmsClient.getDriver()
                .send(CreateMessageRequest.newBuilder(), MapUtil.of(
                        "chat_type", chatType,
                        "to_id", toId,
                        "delivery_date", deliveryDate,
                        "text", text,
                        "records", records,
                        "burn_after", burnAfter))
                .thenApply(notification -> notification.getData().getIds().getValuesList().get(0));
    }

    public CompletableFuture<Long> forwardMessage(
            long messageId,
            @NotNull ChatType chatType,
            long targetId) {
        Validator.throwIfAnyFalsy(chatType);
        return turmsClient.getDriver()
                .send(CreateMessageRequest.newBuilder(), MapUtil.of(
                        "message_id", messageId,
                        "chat_type", chatType,
                        "to_id", targetId))
                .thenApply(notification -> notification.getData().getIds().getValuesList().get(0));
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
            @Nullable ChatType chatType,
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
                        "chat_type", chatType,
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
            @NotNull ChatType chatType,
            long targetId) {
        Validator.throwIfAnyFalsy(chatType);
        return turmsClient.getDriver()
                .send(UpdateTypingStatusRequest.newBuilder(), MapUtil.of(
                        "chat_type", chatType,
                        "to_id", targetId))
                .thenApply(notification -> null);
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
}