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

package im.turms.turms.workflow.service.impl.message;

import com.mongodb.client.result.UpdateResult;
import im.turms.common.constant.MessageDeliveryStatus;
import im.turms.server.common.constant.TurmsStatusCode;
import im.turms.server.common.exception.TurmsBusinessException;
import im.turms.common.util.Validator;
import im.turms.server.common.cluster.node.Node;
import im.turms.server.common.util.AssertUtil;
import im.turms.turms.bo.DateRange;
import im.turms.turms.constraint.ValidMessageDeliveryStatus;
import im.turms.turms.constraint.ValidMessageStatusKey;
import im.turms.turms.util.MapUtil;
import im.turms.turms.workflow.dao.builder.QueryBuilder;
import im.turms.turms.workflow.dao.builder.UpdateBuilder;
import im.turms.turms.workflow.dao.domain.MessageStatus;
import im.turms.turms.workflow.service.impl.statistics.MetricsService;
import im.turms.turms.workflow.service.util.DomainConstraintUtil;
import io.micrometer.core.instrument.Counter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.mongodb.core.ReactiveMongoOperations;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.annotation.Nullable;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.PastOrPresent;
import java.util.Date;
import java.util.Set;

import static im.turms.turms.constant.MetricsConstant.ACKNOWLEDGED_MESSAGES_COUNTER_NAME;

/**
 * @author James Chen
 */
@Service
public class MessageStatusService {

    private static final MessageStatus EMPTY_MESSAGE_STATUS = new MessageStatus(null, null, null, null, null, null, null, null);
    private final ReactiveMongoTemplate mongoTemplate;
    private final Node node;
    private final Counter acknowledgedMessagesCounter;

    public MessageStatusService(
            @Qualifier("messageMongoTemplate") ReactiveMongoTemplate mongoTemplate,
            Node node,
            MetricsService metricsService) {
        this.mongoTemplate = mongoTemplate;
        this.node = node;
        acknowledgedMessagesCounter = metricsService.getRegistry().counter(ACKNOWLEDGED_MESSAGES_COUNTER_NAME);
    }

    public Flux<Long> queryMessagesIdsByDeliveryStatusesAndTargetIds(
            @NotEmpty Set<@ValidMessageDeliveryStatus MessageDeliveryStatus> deliveryStatuses,
            @Nullable Boolean areGroupMessages,
            @Nullable Set<Long> targetIds) {
        try {
            AssertUtil.notEmpty(deliveryStatuses, "deliveryStatuses");
            for (MessageDeliveryStatus status : deliveryStatuses) {
                DomainConstraintUtil.validMessageDeliveryStatus(status);
            }
        } catch (TurmsBusinessException e) {
            return Flux.error(e);
        }
        Query query = new Query()
                .addCriteria(Criteria.where(MessageStatus.Fields.DELIVERY_STATUS).in(deliveryStatuses));
        if (areGroupMessages != null) {
            if (targetIds == null || targetIds.isEmpty()) {
                return Flux.empty();
            }
            if (areGroupMessages) {
                query.addCriteria(Criteria.where(MessageStatus.Fields.GROUP_ID).in(targetIds));
            } else {
                query.addCriteria(Criteria.where(MessageStatus.Fields.ID_RECIPIENT_ID).in(targetIds));
            }
        }
        query.fields().include(MessageStatus.Fields.ID_MESSAGE_ID);
        return mongoTemplate
                .find(query, MessageStatus.class)
                .map(status -> status.getKey().getMessageId());
    }

    public Mono<Boolean> updateMessageStatuses(
            @NotEmpty Set<MessageStatus.@ValidMessageStatusKey Key> keys,
            @Nullable @PastOrPresent Date recallDate,
            @Nullable @PastOrPresent Date readDate,
            @Nullable @PastOrPresent Date receptionDate,
            @Nullable ReactiveMongoOperations operations) {
        try {
            AssertUtil.notEmpty(keys, "keys");
            for (MessageStatus.Key key : keys) {
                DomainConstraintUtil.validMessageStatusKey(key);
            }
            AssertUtil.pastOrPresent(recallDate, "recallDate");
            AssertUtil.pastOrPresent(readDate, "readDate");
            AssertUtil.pastOrPresent(receptionDate, "receptionDate");
        } catch (TurmsBusinessException e) {
            return Mono.error(e);
        }
        if (Validator.areAllNull(recallDate, readDate, receptionDate)) {
            return Mono.just(true);
        }
        boolean isIllegalRecall = recallDate != null
                && !node.getSharedProperties().getService().getMessage().isAllowRecallingMessage();
        boolean isIllegalRead = readDate != null
                && !node.getSharedProperties().getService().getMessage().getReadReceipt().isEnabled();
        if (isIllegalRecall || isIllegalRead) {
            return Mono.error(TurmsBusinessException.get(TurmsStatusCode.DISABLED_FUNCTION));
        }
        return MapUtil.fluxMerge(multimap -> {
            for (MessageStatus.Key key : keys) {
                multimap.put(key.getMessageId(), key.getRecipientId());
            }
        }, (monos, key, values) -> monos.add(updateMessageStatuses(
                key,
                values,
                recallDate,
                readDate,
                receptionDate,
                operations)));
    }

    public Mono<Boolean> updateMessageStatuses(
            @NotNull Long messageId,
            @Nullable Set<Long> recipientIds,
            @Nullable @PastOrPresent Date recallDate,
            @Nullable @PastOrPresent Date readDate,
            @Nullable @PastOrPresent Date receptionDate,
            @Nullable ReactiveMongoOperations operations) {
        try {
            AssertUtil.notNull(messageId, "messageId");
            AssertUtil.pastOrPresent(recallDate, "recallDate");
            AssertUtil.pastOrPresent(readDate, "readDate");
            AssertUtil.pastOrPresent(receptionDate, "receptionDate");
        } catch (TurmsBusinessException e) {
            return Mono.error(e);
        }
        if (Validator.areAllNull(recallDate, readDate, receptionDate)) {
            return Mono.just(true);
        }
        Query query = QueryBuilder
                .newBuilder()
                .addIsIfNotNull(MessageStatus.Fields.ID_MESSAGE_ID, messageId)
                .addInIfNotNull(MessageStatus.Fields.ID_RECIPIENT_ID, recipientIds)
                .buildQuery();
        Update update = UpdateBuilder.newBuilder()
                .setIfNotNull(MessageStatus.Fields.RECALL_DATE, recallDate)
                .setIfNotNull(MessageStatus.Fields.READ_DATE, readDate)
                .setIfNotNull(MessageStatus.Fields.RECEPTION_DATE, receptionDate)
                .build();
        ReactiveMongoOperations mongoOperations = operations != null ? operations : mongoTemplate;
        return mongoOperations.updateMulti(query, update, MessageStatus.class, MessageStatus.COLLECTION_NAME)
                .map(UpdateResult::wasAcknowledged);
    }

    public Mono<Boolean> updateMessageStatus(
            @NotNull Long messageId,
            @Nullable Long recipientId,
            @Nullable @PastOrPresent Date recallDate,
            @Nullable @PastOrPresent Date readDate,
            @Nullable @PastOrPresent Date receptionDate,
            @Nullable ReactiveMongoOperations operations) {
        Set<Long> recipientIds = recipientId != null
                ? Set.of(recipientId)
                : null;
        return updateMessageStatuses(messageId, recipientIds, recallDate, readDate, receptionDate, operations);
    }

    public Mono<Boolean> updateMessagesDeliveryStatus(
            @NotNull Long recipientId,
            @NotEmpty Set<Long> messageIds,
            @NotNull @ValidMessageDeliveryStatus MessageDeliveryStatus deliveryStatus,
            boolean updateReadDate) {
        try {
            AssertUtil.notNull(recipientId, "recipientId");
            AssertUtil.notEmpty(messageIds, "messageIds");
            AssertUtil.notNull(deliveryStatus, "deliveryStatus");
            DomainConstraintUtil.validMessageDeliveryStatus(deliveryStatus);
        } catch (TurmsBusinessException e) {
            return Mono.error(e);
        }
        Query query = new Query()
                .addCriteria(Criteria.where(MessageStatus.Fields.ID_MESSAGE_ID).in(messageIds))
                .addCriteria(Criteria.where(MessageStatus.Fields.ID_RECIPIENT_ID).is(recipientId));
        Update update = new Update().set(MessageStatus.Fields.DELIVERY_STATUS, deliveryStatus);
        if (updateReadDate) {
            Date now = new Date();
            update.set(MessageStatus.Fields.READ_DATE, now);
        }
        return mongoTemplate.updateMulti(query, update, MessageStatus.class, MessageStatus.COLLECTION_NAME)
                .map(updateResult -> {
                    if (deliveryStatus == MessageDeliveryStatus.RECEIVED) {
                        long modifiedCount = updateResult.getModifiedCount();
                        if (modifiedCount > 0) {
                            acknowledgedMessagesCounter.increment(modifiedCount);
                        }
                    }
                    return updateResult.wasAcknowledged();
                });
    }

    public Mono<Boolean> updateMessagesReadDate(
            @NotNull Long messageId,
            @Nullable @PastOrPresent Date readDate) {
        try {
            AssertUtil.notNull(messageId, "messageId");
            AssertUtil.pastOrPresent(readDate, "readDate");
        } catch (TurmsBusinessException e) {
            return Mono.error(e);
        }
        Query query = new Query().addCriteria(Criteria.where(MessageStatus.Fields.ID_MESSAGE_ID).is(messageId));
        Update update;
        if (readDate != null) {
            query.addCriteria(Criteria.where(MessageStatus.Fields.READ_DATE).is(null));
            update = new Update().set(MessageStatus.Fields.READ_DATE, readDate);
        } else {
            if (node.getSharedProperties().getService().getMessage().getReadReceipt().isAllowMarkingReadMessagesAsUnread()) {
                update = new Update().unset(MessageStatus.Fields.READ_DATE);
            } else {
                return Mono.error(TurmsBusinessException.get(TurmsStatusCode.DISABLED_FUNCTION));
            }
        }
        return mongoTemplate.findAndModify(query, update, MessageStatus.class, MessageStatus.COLLECTION_NAME)
                .defaultIfEmpty(EMPTY_MESSAGE_STATUS)
                .map(status -> EMPTY_MESSAGE_STATUS != status);
    }

    public Mono<MessageStatus> queryMessageStatus(@NotNull Long messageId) {
        try {
            AssertUtil.notNull(messageId, "messageId");
        } catch (TurmsBusinessException e) {
            return Mono.error(e);
        }
        Query query = new Query().addCriteria(Criteria.where(MessageStatus.Fields.ID_MESSAGE_ID).is(messageId));
        return mongoTemplate.findOne(query, MessageStatus.class, MessageStatus.COLLECTION_NAME);
    }

    public Flux<MessageStatus> queryMessageStatuses(
            @Nullable Set<Long> messageIds,
            @Nullable Set<Long> recipientIds,
            @Nullable Boolean isSystemMessage,
            @Nullable Set<Long> senderIds,
            @Nullable Set<MessageDeliveryStatus> deliveryStatuses,
            @Nullable DateRange receptionDateRange,
            @Nullable DateRange readDateRange,
            @Nullable DateRange recallDateRange,
            @Nullable Integer page,
            @Nullable Integer size) {
        Query query = QueryBuilder
                .newBuilder()
                .addInIfNotNull(MessageStatus.Fields.ID_MESSAGE_ID, messageIds)
                .addInIfNotNull(MessageStatus.Fields.ID_RECIPIENT_ID, recipientIds)
                .addIsIfNotNull(MessageStatus.Fields.IS_SYSTEM_MESSAGE, isSystemMessage)
                .addInIfNotNull(MessageStatus.Fields.SENDER_ID, senderIds)
                .addInIfNotNull(MessageStatus.Fields.DELIVERY_STATUS, deliveryStatuses)
                .addBetweenIfNotNull(MessageStatus.Fields.RECEPTION_DATE, receptionDateRange)
                .addBetweenIfNotNull(MessageStatus.Fields.READ_DATE, readDateRange)
                .addBetweenIfNotNull(MessageStatus.Fields.RECALL_DATE, recallDateRange)
                .paginateIfNotNull(page, size);
        return mongoTemplate.find(query, MessageStatus.class, MessageStatus.COLLECTION_NAME);
    }

    public Mono<Long> countMessageStatuses(
            @Nullable Set<Long> messageIds,
            @Nullable Set<Long> recipientIds,
            @Nullable Boolean isSystemMessage,
            @Nullable Set<Long> senderIds,
            @Nullable Set<MessageDeliveryStatus> deliveryStatuses,
            @Nullable DateRange receptionDateRange,
            @Nullable DateRange readDateRange,
            @Nullable DateRange recallDateRange) {
        Query query = QueryBuilder
                .newBuilder()
                .addInIfNotNull(MessageStatus.Fields.ID_MESSAGE_ID, messageIds)
                .addInIfNotNull(MessageStatus.Fields.ID_RECIPIENT_ID, recipientIds)
                .addIsIfNotNull(MessageStatus.Fields.IS_SYSTEM_MESSAGE, isSystemMessage)
                .addInIfNotNull(MessageStatus.Fields.SENDER_ID, senderIds)
                .addInIfNotNull(MessageStatus.Fields.DELIVERY_STATUS, deliveryStatuses)
                .addBetweenIfNotNull(MessageStatus.Fields.RECEPTION_DATE, receptionDateRange)
                .addBetweenIfNotNull(MessageStatus.Fields.READ_DATE, readDateRange)
                .addBetweenIfNotNull(MessageStatus.Fields.RECALL_DATE, recallDateRange)
                .buildQuery();
        return mongoTemplate.count(query, MessageStatus.class, MessageStatus.COLLECTION_NAME);
    }

    public Mono<Long> countPendingMessages(
            @NotNull Boolean areGroupMessages,
            @Nullable Boolean areSystemMessages,
            @NotNull Long groupOrSenderId,
            @NotNull Long recipientId) {
        try {
            AssertUtil.notNull(areGroupMessages, "areGroupMessages");
            AssertUtil.notNull(groupOrSenderId, "groupOrSenderId");
            AssertUtil.notNull(recipientId, "recipientId");
        } catch (TurmsBusinessException e) {
            return Mono.error(e);
        }
        Query query = new Query()
                .addCriteria(Criteria.where(MessageStatus.Fields.ID_RECIPIENT_ID).is(recipientId))
                .addCriteria(Criteria.where(MessageStatus.Fields.DELIVERY_STATUS).is(MessageDeliveryStatus.READY));
        if (areSystemMessages != null) {
            query.addCriteria(Criteria.where(MessageStatus.Fields.IS_SYSTEM_MESSAGE).is(areSystemMessages));
        }
        if (areGroupMessages) {
            query.addCriteria(Criteria.where(MessageStatus.Fields.GROUP_ID).is(groupOrSenderId));
        } else {
            query.addCriteria(Criteria.where(MessageStatus.Fields.GROUP_ID).is(null))
                    .addCriteria(Criteria.where(MessageStatus.Fields.SENDER_ID).is(groupOrSenderId));
        }
        return mongoTemplate.count(query, MessageStatus.class, MessageStatus.COLLECTION_NAME);
    }

}
