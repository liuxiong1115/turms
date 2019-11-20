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

package im.turms.turms.service.message;

import com.google.protobuf.Int64Value;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;
import im.turms.turms.cluster.TurmsClusterManager;
import im.turms.turms.common.*;
import im.turms.turms.constant.ChatType;
import im.turms.turms.constant.MessageDeliveryStatus;
import im.turms.turms.exception.TurmsBusinessException;
import im.turms.turms.plugin.ExpiryMessageAutoDeletionNotificationHandler;
import im.turms.turms.plugin.TurmsPluginManager;
import im.turms.turms.pojo.domain.Message;
import im.turms.turms.pojo.domain.MessageStatus;
import im.turms.turms.pojo.dto.CreateMessageDTO;
import im.turms.turms.pojo.notification.TurmsNotification;
import im.turms.turms.pojo.request.TurmsRequest;
import im.turms.turms.service.group.GroupMemberService;
import im.turms.turms.service.user.UserService;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.ReactiveMongoOperations;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

import static im.turms.turms.common.Constants.*;
import static im.turms.turms.common.TurmsStatusCode.*;

@Service
public class MessageService {
    private final ReactiveMongoTemplate mongoTemplate;
    private final TurmsClusterManager turmsClusterManager;
    private final MessageStatusService messageStatusService;
    private final OutboundMessageService outboundMessageService;
    private final GroupMemberService groupMemberService;
    private final UserService userService;
    private TurmsPluginManager turmsPluginManager;

    @Autowired
    public MessageService(ReactiveMongoTemplate mongoTemplate, TurmsClusterManager turmsClusterManager, MessageStatusService messageStatusService, GroupMemberService groupMemberService, UserService userService, OutboundMessageService outboundMessageService) {
        this.mongoTemplate = mongoTemplate;
        this.turmsClusterManager = turmsClusterManager;
        this.messageStatusService = messageStatusService;
        this.groupMemberService = groupMemberService;
        this.userService = userService;
        this.outboundMessageService = outboundMessageService;
    }

    @Scheduled(cron = EXPIRY_MESSAGES_CLEANER_CRON)
    public void expiryMessagesCleaner() {
        if (turmsClusterManager.isCurrentMemberMaster()) {
            int messagesTimeToLiveHours = turmsClusterManager.getTurmsProperties()
                    .getMessage().getMessagesTimeToLiveHours();
            if (messagesTimeToLiveHours != 0) {
                deleteExpiryMessagesAndStatuses(messagesTimeToLiveHours).subscribe();
            }
        }
    }

    public Mono<Boolean> isMessageSentByUser(@NotNull Long messageId, @NotNull Long senderId) {
        Query query = new Query()
                .addCriteria(Criteria.where(ID).is(messageId))
                .addCriteria(Criteria.where(Message.Fields.senderId).is(senderId));
        return mongoTemplate.exists(query, Message.class);
    }

    public Mono<Boolean> isMessageSentToUser(@NotNull Long messageId, @NotNull Long recipientId) {
        // Warning: Do not check whether a user is the recipient of a message
        // according to the Message collection because a message can be sent to user or group.
        Query query = new Query()
                .addCriteria(Criteria.where(ID_MESSAGE_ID).is(messageId))
                .addCriteria(Criteria.where(ID_RECIPIENT_ID).is(recipientId));
        return mongoTemplate.exists(query, MessageStatus.class);
    }

    public Mono<Boolean> isMessageRecallable(@NotNull Long messageId) {
        Query query = new Query().addCriteria(Criteria.where(ID).is(messageId));
        query.fields().include(Message.Fields.deliveryDate);
        return mongoTemplate.findOne(query, Message.class)
                .map(message -> {
                    Date deliveryDate = message.getDeliveryDate();
                    if (deliveryDate != null) {
                        long elapsedTime = (deliveryDate.getTime() - System.currentTimeMillis()) / 1000;
                        return elapsedTime < turmsClusterManager.getTurmsProperties()
                                .getMessage().getAllowableRecallDurationSeconds();
                    } else {
                        return false;
                    }
                })
                .defaultIfEmpty(false);
    }

    public Flux<Message> authAndQueryCompleteMessages(
            @NotNull boolean closeToDate,
            @Nullable ChatType chatType,
            @Nullable Boolean areSystemMessages,
            @Nullable Long senderId,
            @Nullable Long targetId,
            @Nullable Date startDate,
            @Nullable Date endDate,
            @Nullable MessageDeliveryStatus deliveryStatus,
            @Nullable Integer size) {
        if (deliveryStatus == MessageDeliveryStatus.READY
                || deliveryStatus == MessageDeliveryStatus.RECEIVED) {
            return queryCompleteMessages(closeToDate, chatType, areSystemMessages, senderId, targetId, startDate, endDate, deliveryStatus, size);
        } else {
            throw TurmsBusinessException.get(ILLEGAL_ARGUMENTS);
        }
    }

    public Mono<Message> queryMessage(@NotNull Long messageId) {
        Query query = new Query().addCriteria(Criteria.where(ID).is(messageId));
        return mongoTemplate.findOne(query, Message.class);
    }

    public Flux<Message> queryCompleteMessages(
            @NotNull boolean closeToDate,
            @Nullable ChatType chatType,
            @Nullable Boolean areSystemMessages,
            @Nullable Long senderId,
            @Nullable Long targetId,
            @Nullable Date startDate,
            @Nullable Date endDate,
            @Nullable MessageDeliveryStatus deliveryStatus,
            @Nullable Integer size) {
        QueryBuilder builder = QueryBuilder.newBuilder()
                .addIfNotNull(Criteria.where(Message.Fields.chatType).is(chatType), chatType)
                .addIfNotNull(Criteria.where(Message.Fields.isSystemMessage).is(areSystemMessages), areSystemMessages)
                .addIfNotNull(Criteria.where(Message.Fields.senderId).is(senderId), senderId)
                .addIfNotNull(Criteria.where(Message.Fields.targetId).is(targetId), targetId)
                .addBetweenIfNotNull(Message.Fields.deliveryDate, startDate, endDate);
        Sort.Direction direction = null;
        if (closeToDate) {
            direction = startDate != null ? Sort.Direction.ASC : Sort.Direction.DESC;
        }
        if (deliveryStatus != null) {
            Sort.Direction finalDirection = direction;
            return messageStatusService.getMessagesIdsByDeliveryStatusAndRecipientId(deliveryStatus, targetId)
                    .collect(Collectors.toSet())
                    .flatMapMany(ids -> {
                        if (ids.isEmpty()) {
                            throw TurmsBusinessException.get(TurmsStatusCode.NOT_FOUND);
                        }
                        Query query = builder.add(Criteria.where(ID).in(ids))
                                .paginateIfNotNull(0, size, finalDirection);
                        return mongoTemplate.find(query, Message.class);
                    });
        } else {
            Query query = builder.paginateIfNotNull(0, size, direction);
            return mongoTemplate.find(query, Message.class);
        }
    }

    public Mono<Message> saveMessage(
            @NotNull Message message,
            @Nullable Long referenceId,
            @Nullable ReactiveMongoOperations operations) {
        return saveMessage(
                message.getSenderId(),
                message.getTargetId(),
                message.getChatType(),
                message.getIsSystemMessage(),
                message.getText(),
                message.getRecords(),
                message.getBurnAfter(),
                message.getDeliveryDate(),
                referenceId,
                operations);
    }

    public Mono<Message> saveMessage(
            @NotNull Long senderId,
            @NotNull Long targetId,
            @NotNull ChatType chatType,
            @NotNull Boolean isSystemMessage,
            @Nullable String text,
            @Nullable List<byte[]> records,
            @Nullable Integer burnAfter,
            @Nullable Date deliveryDate,
            @Nullable Long referenceId,
            @Nullable ReactiveMongoOperations operations) {
        if (text != null && text.length() > turmsClusterManager.getTurmsProperties().getMessage().getMaxTextLimit()) {
            throw TurmsBusinessException.get(ILLEGAL_ARGUMENTS);
        }
        int maxRecordsSize = turmsClusterManager.getTurmsProperties()
                .getMessage().getMaxRecordsSizeBytes();
        if (records != null && maxRecordsSize != 0) {
            int count = 0;
            for (byte[] record : records) {
                count = record.length;
            }
            if (count > maxRecordsSize) {
                throw TurmsBusinessException.get(ILLEGAL_ARGUMENTS);
            }
        }
        if (turmsClusterManager.getTurmsProperties().getMessage().getTimeType()
                != im.turms.turms.property.business.Message.TimeType.CLIENT_TIME
                || deliveryDate == null) {
            deliveryDate = new Date();
        }
        if (!turmsClusterManager.getTurmsProperties().getMessage().isRecordsPersistent()) {
            records = null;
        }
        Message message = new Message(
                turmsClusterManager.generateRandomId(),
                chatType,
                isSystemMessage,
                deliveryDate,
                text,
                senderId,
                targetId,
                records,
                burnAfter,
                referenceId);
        ReactiveMongoOperations mongoOperations = operations != null ? operations : mongoTemplate;
        return mongoOperations.insert(message);
    }

    public Mono<Boolean> saveMessageStatuses(
            @NotNull Message message,
            @Nullable ReactiveMongoOperations operations) {
        ReactiveMongoOperations mongoOperations = operations != null ? operations : mongoTemplate;
        switch (message.getChatType()) {
            case PRIVATE:
                MessageStatus messageStatus = new MessageStatus(
                        message.getId(),
                        null,
                        message.getSenderId(),
                        message.getTargetId(),
                        MessageDeliveryStatus.READY);
                return mongoOperations.save(messageStatus).thenReturn(true);
            case GROUP:
                return groupMemberService
                        .queryGroupMembersIds(message.getTargetId())
                        .collect(Collectors.toSet())
                        .flatMap(membersIds -> {
                            //TODO: duplicated query for membersIds
                            if (membersIds.isEmpty()) {
                                return Mono.just(true);
                            }
                            List<MessageStatus> messageStatuses = new ArrayList<>(membersIds.size());
                            for (Long memberId : membersIds) {
                                messageStatuses.add(new MessageStatus(
                                        message.getId(),
                                        message.getTargetId(),
                                        message.getSenderId(),
                                        memberId,
                                        MessageDeliveryStatus.READY));
                            }
                            return mongoOperations.insertAll(messageStatuses).then(Mono.just(true));
                        });
            default:
                throw TurmsBusinessException.get(ILLEGAL_ARGUMENTS);
        }
    }

    public Mono<Message> saveMessageAndMessagesStatus(
            @NotNull Long senderId,
            @NotNull Long targetId,
            @NotNull ChatType chatType,
            @NotNull Boolean isSystemMessage,
            @NotNull String text,
            @Nullable List<byte[]> records,
            @Nullable Integer burnAfter,
            @Nullable Date deliveryDate,
            @Nullable Long referenceId) {
        if (turmsClusterManager.getTurmsProperties().getMessage().getTimeType()
                != im.turms.turms.property.business.Message.TimeType.CLIENT_TIME
                || deliveryDate == null) {
            deliveryDate = new Date();
        }
        Message message = new Message(
                turmsClusterManager.generateRandomId(),
                chatType,
                isSystemMessage,
                deliveryDate,
                text,
                senderId,
                targetId,
                records,
                burnAfter,
                referenceId);
        return saveMessageAndMessagesStatus(message, referenceId);
    }

    public Mono<Message> saveMessageAndMessagesStatus(@NotNull Message message, @Nullable Long referenceId) {
        return mongoTemplate.inTransaction()
                .execute(operations -> saveMessage(message, referenceId, operations)
                        .zipWith(saveMessageStatuses(message, operations))
                        .map(Tuple2::getT1))
                .retryBackoff(MONGO_TRANSACTION_RETRIES_NUMBER, MONGO_TRANSACTION_BACKOFF)
                .single();
    }

    public Flux<Long> queryExpiryMessagesIds(@NotNull Integer timeToLiveHours) {
        Date beforeDate = Date.from(Instant.now().minus(timeToLiveHours, ChronoUnit.HOURS));
        Query query = new Query()
                .addCriteria(Criteria.where(Message.Fields.deliveryDate).lt(beforeDate));
        query.fields().include(ID);
        return mongoTemplate.find(query, Message.class)
                .map(Message::getId);
    }

    public Mono<Boolean> deleteExpiryMessagesAndStatuses(@NotNull Integer timeToLiveHours) {
        return queryExpiryMessagesIds(timeToLiveHours)
                .collectList()
                .flatMap(messagesIds -> {
                    if (messagesIds.isEmpty()) {
                        return Mono.just(true);
                    } else {
                        Query messagesQuery = new Query().addCriteria(Criteria.where(ID).in(messagesIds));
                        Query messagesStatusesQuery = new Query().addCriteria(Criteria.where(ID_MESSAGE_ID).in(messagesIds));
                        Mono<Boolean> allowedMono = Mono.just(true);
                        if (turmsClusterManager.getTurmsProperties().getPlugin().isEnabled()) {
                            allowedMono = mongoTemplate.find(messagesQuery, Message.class)
                                    .collectList()
                                    .flatMap(messages -> {
                                        Mono<Boolean> mono = Mono.just(true);
                                        for (ExpiryMessageAutoDeletionNotificationHandler handler : turmsPluginManager
                                                .getExpiryMessageAutoDeletionNotificationHandlerList()) {
                                            mono = mono
                                                    .defaultIfEmpty(true)
                                                    .flatMap(allowed -> {
                                                        if (allowed) {
                                                            return handler.allowDeleting(messages);
                                                        } else {
                                                            return Mono.just(false);
                                                        }
                                                    });
                                        }
                                        return mono;
                                    });
                        }
                        return allowedMono.flatMap(allowed -> {
                            if (allowed) {
                                return mongoTemplate.inTransaction()
                                        .execute(operations -> Mono.zip(
                                                operations.remove(messagesQuery, Message.class),
                                                operations.remove(messagesStatusesQuery, MessageStatus.class))
                                                .thenReturn(true))
                                        .retryBackoff(MONGO_TRANSACTION_RETRIES_NUMBER, MONGO_TRANSACTION_BACKOFF)
                                        .single();
                            } else {
                                return Mono.just(false);
                            }
                        });
                    }
                });
    }

    public Mono<Boolean> deleteMessages(
            @Nullable Set<Long> messagesIds,
            boolean deleteMessageStatus,
            @Nullable Boolean logicalDelete) {
        Query queryMessage = new Query();
        Query queryMessageStatus = new Query();
        if (messagesIds != null && !messagesIds.isEmpty()) {
            queryMessage.addCriteria(Criteria.where(ID).in(messagesIds));
            queryMessageStatus.addCriteria(Criteria.where(ID_MESSAGE_ID).in(messagesIds));
        }
        if (logicalDelete == null) {
            logicalDelete = turmsClusterManager.getTurmsProperties()
                    .getMessage().isLogicallyDeleteMessageByDefault();
        }
        if (logicalDelete) {
            Update update = new Update().set(Message.Fields.deletionDate, new Date());
            if (deleteMessageStatus) {
                return mongoTemplate.inTransaction()
                        .execute(operations -> Mono.zip(
                                operations.updateMulti(queryMessage, update, Message.class),
                                operations.remove(queryMessageStatus, MessageStatus.class))
                                .thenReturn(true))
                        .retryBackoff(MONGO_TRANSACTION_RETRIES_NUMBER, MONGO_TRANSACTION_BACKOFF)
                        .single();
            } else {
                return mongoTemplate.updateMulti(queryMessage, update, Message.class)
                        .map(UpdateResult::wasAcknowledged);
            }
        } else {
            if (deleteMessageStatus) {
                return mongoTemplate.inTransaction()
                        .execute(operations -> Mono.zip(
                                operations.remove(queryMessage, Message.class),
                                operations.remove(queryMessageStatus, MessageStatus.class))
                                .thenReturn(true))
                        .retryBackoff(MONGO_TRANSACTION_RETRIES_NUMBER, MONGO_TRANSACTION_BACKOFF)
                        .single();
            }
            return mongoTemplate.remove(queryMessage, Message.class)
                    .map(DeleteResult::wasAcknowledged);
        }
    }

    public Mono<Boolean> deleteMessage(
            @NotNull Long messageId,
            boolean deleteMessageStatus,
            @Nullable Boolean logicalDelete) {
        return deleteMessages(Set.of(messageId), deleteMessageStatus, logicalDelete);
    }

    public Mono<Boolean> updateMessage(
            @NotNull Long messageId,
            @Nullable String text,
            @Nullable List<byte[]> records,
            @Nullable ReactiveMongoOperations operations) {
        if (text == null && records == null) {
            throw TurmsBusinessException.get(ILLEGAL_ARGUMENTS);
        }
        Query query = new Query().addCriteria(Criteria.where(ID).is(messageId));
        Update update = UpdateBuilder.newBuilder()
                .setIfNotNull(Message.Fields.text, text)
                .setIfNotNull(Message.Fields.records, records)
                .build();
        ReactiveMongoOperations mongoOperations = operations != null ? operations : mongoTemplate;
        return mongoOperations.updateFirst(query, update, Message.class)
                .map(UpdateResult::wasAcknowledged);
    }

    public Mono<Long> countUsersWhoSentMessage(
            @Nullable Date startDate,
            @Nullable Date endDate,
            @Nullable ChatType chatType,
            @Nullable Boolean areSystemMessages) {
        Criteria criteria = QueryBuilder.newBuilder()
                .addBetweenIfNotNull(Message.Fields.deliveryDate, startDate, endDate)
                .addIfNotNull(Criteria.where(Message.Fields.chatType).is(chatType), chatType)
                .addIfNotNull(Criteria.where(Message.Fields.isSystemMessage).is(areSystemMessages), areSystemMessages)
                .buildCriteria();
        return AggregationUtil.countDistinct(
                mongoTemplate,
                criteria,
                Message.Fields.senderId,
                Message.class);
    }

    public Mono<Long> countGroupsThatSentMessages(
            @NotNull Date startDate,
            @NotNull Date endDate) {
        Criteria criteria = QueryBuilder.newBuilder()
                .addBetweenIfNotNull(Message.Fields.deliveryDate, startDate, endDate)
                .add(Criteria.where(Message.Fields.chatType).is(ChatType.GROUP))
                .buildCriteria();
        return AggregationUtil.countDistinct(
                mongoTemplate,
                criteria,
                Message.Fields.targetId,
                Message.class);
    }

    public Mono<Long> countUsersWhoAcknowledgedMessage(
            @Nullable Date startDate,
            @Nullable Date endDate,
            @Nullable ChatType chatType) {
        Criteria criteria = QueryBuilder.newBuilder()
                .addBetweenIfNotNull(MessageStatus.Fields.receptionDate, startDate, endDate)
                .buildCriteria();
        if (chatType != null) {
            if (chatType == ChatType.GROUP) {
                criteria.and(MessageStatus.Fields.groupId).ne(null);
            } else if (chatType == ChatType.PRIVATE) {
                criteria.and(MessageStatus.Fields.groupId).is(null);
            }
        }
        return AggregationUtil.countDistinct(
                mongoTemplate,
                criteria,
                ID_RECIPIENT_ID,
                MessageStatus.class);
    }

    public Mono<Long> countSentMessages(
            @Nullable Date startDate,
            @Nullable Date endDate,
            @Nullable ChatType chatType,
            @Nullable Boolean areSystemMessages) {
        Query query = QueryBuilder.newBuilder()
                .addBetweenIfNotNull(Message.Fields.deliveryDate, startDate, endDate)
                .addIfNotNull(Criteria.where(Message.Fields.chatType).is(chatType), chatType)
                .addIfNotNull(Criteria.where(Message.Fields.isSystemMessage).is(areSystemMessages), areSystemMessages)
                .buildQuery();
        return mongoTemplate.count(query, Message.class);
    }

    public Mono<Long> countDeliveredMessagesOnAverage(
            @Nullable Date startDate,
            @Nullable Date endDate,
            @Nullable ChatType chatType,
            @Nullable Boolean areSystemMessages) {
        return countSentMessages(startDate, endDate, chatType, areSystemMessages)
                .flatMap(totalDeliveredMessages -> {
                    if (totalDeliveredMessages == 0) {
                        return Mono.just(0L);
                    } else {
                        return countUsersWhoSentMessage(startDate, endDate, chatType, areSystemMessages)
                                .map(totalUsers -> {
                                    if (totalUsers == 0) {
                                        return Long.MAX_VALUE;
                                    } else {
                                        return totalDeliveredMessages / totalUsers;
                                    }
                                });
                    }
                });
    }

    public Mono<Long> countAcknowledgedMessages(
            @Nullable Date startDate,
            @Nullable Date endDate,
            @Nullable ChatType chatType,
            @Nullable Boolean areSystemMessages) {
        Query query = QueryBuilder.newBuilder()
                .addBetweenIfNotNull(MessageStatus.Fields.receptionDate, startDate, endDate)
                .addIfNotNull(Criteria.where(MessageStatus.Fields.isSystemMessage).is(areSystemMessages), areSystemMessages)
                .buildQuery();
        if (chatType != null) {
            if (chatType == ChatType.GROUP) {
                query.addCriteria(Criteria.where(MessageStatus.Fields.groupId).ne(null));
            } else if (chatType == ChatType.PRIVATE) {
                query.addCriteria(Criteria.where(MessageStatus.Fields.groupId).is(null));
            }
        }
        return mongoTemplate.count(query, MessageStatus.class);
    }

    public Mono<Long> countAcknowledgedMessagesOnAverage(
            @Nullable Date startDate,
            @Nullable Date endDate,
            @Nullable ChatType chatType,
            @Nullable Boolean areSystemMessages) {
        return countAcknowledgedMessages(startDate, endDate, chatType, areSystemMessages)
                .flatMap(totalAcknowledgedMessages -> {
                    if (totalAcknowledgedMessages == 0) {
                        return Mono.just(0L);
                    } else {
                        return countUsersWhoAcknowledgedMessage(startDate, endDate, chatType)
                                .map(totalUsers -> {
                                    if (totalUsers == 0) {
                                        return Long.MAX_VALUE;
                                    } else {
                                        return totalAcknowledgedMessages / totalUsers;
                                    }
                                });
                    }
                });
    }

    public Mono<Boolean> authAndUpdateMessageAndMessageStatus(
            @NotNull Long requesterId,
            @NotNull Long messageId,
            @NotNull Long recipientId,
            @Nullable String text,
            @Nullable List<byte[]> records,
            @Nullable Date recallDate,
            @Nullable Date readDate) {
        boolean updateMessageContent = text != null || (records != null && !records.isEmpty());
        if (updateMessageContent || recallDate != null) {
            if (recallDate != null && !turmsClusterManager.getTurmsProperties()
                    .getMessage().isAllowRecallingMessage()) {
                throw TurmsBusinessException.get(DISABLE_FUNCTION);
            }
            if (updateMessageContent && !turmsClusterManager.getTurmsProperties()
                    .getMessage().isAllowEditingMessageBySender()) {
                throw TurmsBusinessException.get(DISABLE_FUNCTION);
            }
            return isMessageSentByUser(messageId, requesterId)
                    .flatMap(isSentByUser -> {
                        if (isSentByUser == null || !isSentByUser) {
                            return Mono.error(TurmsBusinessException.get(UNAUTHORIZED));
                        } else if (recallDate != null) {
                            return isMessageRecallable(messageId)
                                    .flatMap(recallable -> {
                                        if (recallable == null || !recallable) {
                                            return Mono.error(TurmsBusinessException.get(EXPIRY_RESOURCE));
                                        }
                                        return updateMessageAndMessageStatus(messageId, recipientId, text, records, recallDate, readDate);
                                    });
                        } else {
                            return updateMessageAndMessageStatus(messageId, recipientId, text, records, recallDate, readDate);
                        }
                    });
        } else {
            throw TurmsBusinessException.get(ILLEGAL_ARGUMENTS);
        }
    }

    public Mono<Boolean> updateMessageAndMessageStatus(
            @NotNull Long messageId,
            @NotNull Long recipientId,
            @Nullable String text,
            @Nullable List<byte[]> records,
            @Nullable Date recallDate,
            @Nullable Date readDate) {
        boolean readyUpdateMessage = text != null || (records != null && !records.isEmpty());
        boolean readyUpdateMessageStatus = recallDate != null || readDate != null;
        if (readyUpdateMessage || readyUpdateMessageStatus) {
            return mongoTemplate.inTransaction()
                    .execute(operations -> {
                        List<Mono<Boolean>> updateMonos = new ArrayList<>(2);
                        if (readyUpdateMessage) {
                            updateMonos.add(updateMessage(messageId, text, records, operations));
                        }
                        if (readyUpdateMessageStatus) {
                            updateMonos.add(messageStatusService.updateMessageStatus(messageId, recipientId, recallDate, readDate, operations));
                        }
                        return Mono.zip(updateMonos, objects -> objects)
                                .thenReturn(true);
                    })
                    .retryBackoff(MONGO_TRANSACTION_RETRIES_NUMBER, MONGO_TRANSACTION_BACKOFF)
                    .single();
        } else {
            return Mono.just(true);
        }
    }

    public Flux<Long> queryMessageRecipients(@NotNull Long messageId) {
        Query query = new Query().addCriteria(Criteria.where(ID_MESSAGE_ID).is(messageId));
        query.fields().include(ID_RECIPIENT_ID);
        return mongoTemplate.find(query, MessageStatus.class)
                .map(status -> status.getKey().getRecipientId());
    }

    public Mono<Long> queryPrivateMessageRecipient(@NotNull Long messageId) {
        Query query = new Query()
                .addCriteria(Criteria.where(ID_MESSAGE_ID).is(messageId))
                .addCriteria(Criteria.where(MessageStatus.Fields.groupId).is(null));
        query.fields().include(ID_RECIPIENT_ID);
        return mongoTemplate.findOne(query, MessageStatus.class)
                .map(status -> status.getKey().getRecipientId());
    }

    public Mono<Long> queryMessageSenderId(@NotNull Long messageId) {
        Query query = new Query().addCriteria(Criteria.where(ID).is(messageId));
        query.fields().include(Message.Fields.senderId);
        return mongoTemplate.findOne(query, Message.class)
                .map(Message::getSenderId);
    }

    // messageId - recipientsIds
    public Mono<Pair<Long, Set<Long>>> authAndSendMessage(
            @NotNull Long senderId,
            @NotNull Long targetId,
            @NotNull ChatType chatType,
            @NotNull Boolean isSystemMessage,
            @Nullable String text,
            @Nullable List<byte[]> records,
            @Nullable Integer burnAfter,
            @Nullable Date deliveryDate,
            @Nullable Long referenceId) {
        boolean messagePersistent = turmsClusterManager.getTurmsProperties().getMessage().isMessagePersistent();
        boolean messageStatusPersistent = turmsClusterManager.getTurmsProperties().getMessage().isMessageStatusPersistent();
        if (chatType == ChatType.PRIVATE || chatType == ChatType.GROUP) {
            return userService.isAllowedToSendMessageToTarget(chatType, isSystemMessage, senderId, targetId)
                    .flatMap(allowed -> {
                        if (allowed == null || !allowed) {
                            return Mono.error(TurmsBusinessException.get(TurmsStatusCode.UNAUTHORIZED));
                        }
                        Mono<Set<Long>> queryRecipientsIds;
                        if (chatType == ChatType.PRIVATE) {
                            queryRecipientsIds = Mono.just(Collections.singleton(targetId));
                        } else {
                            queryRecipientsIds = groupMemberService.getMembersIdsByGroupId(targetId)
                                    .collect(Collectors.toSet());
                        }
                        return queryRecipientsIds.flatMap(recipientsIds -> {
                            if (!messagePersistent) {
                                if (recipientsIds.isEmpty()) {
                                    return Mono.empty();
                                } else {
                                    return Mono.just(Pair.of(null, recipientsIds));
                                }
                            }
                            Mono<Message> saveMono;
                            if (messageStatusPersistent) {
                                saveMono = saveMessageAndMessagesStatus(
                                        senderId, targetId, chatType, isSystemMessage, text,
                                        records, burnAfter, deliveryDate, referenceId);
                            } else {
                                saveMono = saveMessage(
                                        senderId, targetId, chatType, isSystemMessage, text,
                                        records, burnAfter, deliveryDate, referenceId, null);
                            }
                            return saveMono.map(message -> Pair.of(message.getId(), recipientsIds));
                        });
                    });
        } else {
            throw TurmsBusinessException.get(ILLEGAL_ARGUMENTS);
        }
    }

    /**
     * Use clone rather than reference
     */
    public Mono<Pair<Long, Set<Long>>> authAndCloneAndSendMessage(
            @NotNull Long requesterId,
            @NotNull Long messageId,
            @NotNull ChatType chatType,
            @NotNull Boolean isSystemMessage,
            @NotNull Long targetId) {
        return queryMessage(messageId)
                .flatMap(message -> authAndSendMessage(
                        requesterId,
                        targetId,
                        chatType,
                        isSystemMessage,
                        message.getText(),
                        message.getRecords(),
                        message.getBurnAfter(),
                        message.getDeliveryDate(),
                        messageId));
    }

    public Mono<Boolean> sendAdminMessage(
            boolean deliver,
            @NotNull CreateMessageDTO createMessageDTO) {
        Message message = new Message();
        message.setChatType(createMessageDTO.getChatType());
        Boolean areSystemMessages = createMessageDTO.getIsSystemMessage();
        areSystemMessages = areSystemMessages != null ? areSystemMessages : true;
        message.setIsSystemMessage(areSystemMessages);
        message.setText(createMessageDTO.getText());
        message.setSenderId(createMessageDTO.getSenderId());
        message.setTargetId(createMessageDTO.getTargetId());
        message.setRecords(createMessageDTO.getRecords());
        message.setBurnAfter(createMessageDTO.getBurnAfter());
        message.setReferenceId(createMessageDTO.getReferenceId());
        return sendAdminMessage(deliver, message);
    }

    public Mono<Boolean> sendAdminMessage(
            boolean deliver,
            @NotNull Message message) {
        if (message.getTargetId() == null
                || message.getChatType() == null
                || message.getChatType() == ChatType.UNRECOGNIZED
                || message.getIsSystemMessage() == null
                || (message.getText() == null && message.getRecords() == null)) {
            throw new IllegalArgumentException();
        }
        if (deliver) {
            return authAndSendMessage(
                    ADMIN_REQUESTER_ID,
                    message.getTargetId(),
                    message.getChatType(),
                    message.getIsSystemMessage(),
                    message.getText(),
                    message.getRecords(),
                    message.getBurnAfter(),
                    new Date(),
                    null)
                    .flatMap(pair -> {
                        TurmsRequest request = TurmsRequest
                                .newBuilder()
                                .setCreateMessageRequest(ProtoUtil.message2createMessageRequest(message))
                                .build();
                        byte[] response = TurmsNotification
                                .newBuilder()
                                .setRelayedRequest(request)
                                .setRequestId(Int64Value.newBuilder().setValue(0).build())
                                .buildPartial()
                                .toByteArray();
                        return outboundMessageService.relayClientMessageToClient(
                                null,
                                response,
                                message.getTargetId(),
                                true);
                    });
        } else {
            if (turmsClusterManager.getTurmsProperties().getMessage().isMessageStatusPersistent()) {
                return saveMessageAndMessagesStatus(message, null)
                        .thenReturn(true);
            } else {
                return saveMessage(message, message.getReferenceId(), null)
                        .thenReturn(true);
            }
        }
    }
}