package im.turms.turms.service.message;

import com.google.common.collect.HashMultimap;
import com.mongodb.client.result.UpdateResult;
import im.turms.turms.annotation.constraint.MessageDeliveryStatusConstraint;
import im.turms.turms.annotation.constraint.MessageStatusKeyConstraint;
import im.turms.turms.cluster.TurmsClusterManager;
import im.turms.turms.common.QueryBuilder;
import im.turms.turms.common.TurmsStatusCode;
import im.turms.turms.common.UpdateBuilder;
import im.turms.turms.common.Validator;
import im.turms.turms.constant.ChatType;
import im.turms.turms.constant.MessageDeliveryStatus;
import im.turms.turms.exception.TurmsBusinessException;
import im.turms.turms.pojo.bo.common.DateRange;
import im.turms.turms.pojo.domain.MessageStatus;
import org.springframework.data.mongodb.core.ReactiveMongoOperations;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.annotation.Nullable;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.PastOrPresent;
import java.util.*;

import static im.turms.turms.common.Constants.*;

@Service
@Validated
public class MessageStatusService {
    private static final MessageStatus EMPTY_MESSAGE_STATUS = new MessageStatus();
    private final ReactiveMongoTemplate mongoTemplate;
    private final TurmsClusterManager turmsClusterManager;

    public MessageStatusService(ReactiveMongoTemplate mongoTemplate, TurmsClusterManager turmsClusterManager) {
        this.mongoTemplate = mongoTemplate;
        this.turmsClusterManager = turmsClusterManager;
    }

    public Mono<Boolean> isMessageRead(@NotNull Long messageId, @NotNull Long recipientId) {
        Query query = new Query()
                .addCriteria(Criteria.where(ID_MESSAGE_ID).is(messageId))
                .addCriteria(Criteria.where(ID_RECIPIENT_ID).is(recipientId))
                .addCriteria(Criteria.where(MessageStatus.Fields.readDate).ne(null));
        return mongoTemplate.exists(query, MessageStatus.class);
    }

    public Flux<Long> queryMessagesIdsByDeliveryStatusAndTargetId(
            @NotNull MessageDeliveryStatus deliveryStatus,
            @Nullable ChatType chatType,
            @Nullable Long targetId) {
        Query query = new Query()
                .addCriteria(Criteria.where(MessageStatus.Fields.deliveryStatus).is(deliveryStatus));
        if (chatType == ChatType.PRIVATE || chatType == ChatType.GROUP) {
            if (targetId == null) {
                return Flux.empty();
            }
            if (chatType == ChatType.PRIVATE) {
                query.addCriteria(Criteria.where(ID_RECIPIENT_ID).is(targetId));
            } else {
                query.addCriteria(Criteria.where(MessageStatus.Fields.groupId).is(targetId));
            }
        }
        query.fields().include(ID_MESSAGE_ID);
        return mongoTemplate
                .find(query, MessageStatus.class)
                .map(status -> status.getKey().getMessageId());
    }

    public Mono<Boolean> updateMessageStatuses(
            @NotEmpty Set<MessageStatus.@MessageStatusKeyConstraint Key> keys,
            @Nullable @PastOrPresent Date recallDate,
            @Nullable @PastOrPresent Date readDate,
            @Nullable @PastOrPresent Date receptionDate,
            @Nullable ReactiveMongoOperations operations) {
        Validator.throwIfAllNull(recallDate, readDate, receptionDate);
        boolean isIllegalRecall = recallDate != null
                && !turmsClusterManager.getTurmsProperties().getMessage().isAllowRecallingMessage();
        boolean isIllegalRead = readDate != null
                && !turmsClusterManager.getTurmsProperties().getMessage().getReadReceipt().isEnabled();
        if (isIllegalRecall || isIllegalRead) {
            throw TurmsBusinessException.get(TurmsStatusCode.DISABLE_FUNCTION);
        }
        HashMultimap<Long, Long> multimap = HashMultimap.create();
        for (MessageStatus.Key key : keys) {
            multimap.put(key.getMessageId(), key.getRecipientId());
        }
        ArrayList<Mono<Boolean>> monos = new ArrayList<>(multimap.keySet().size());
        for (Long messageId : multimap.keySet()) {
            Set<Long> recipientIds = multimap.get(messageId);
            monos.add(updateMessageStatuses(messageId, recipientIds, recallDate, readDate, receptionDate, operations));
        }
        return Flux.merge(monos).all(value -> value);
    }

    public Mono<Boolean> updateMessageStatuses(
            @NotNull Long messageId,
            @NotEmpty Set<Long> recipientIds,
            @Nullable @PastOrPresent Date recallDate,
            @Nullable @PastOrPresent Date readDate,
            @Nullable @PastOrPresent Date receptionDate,
            @Nullable ReactiveMongoOperations operations) {
        Validator.throwIfAllNull(recallDate, readDate, receptionDate);
        Query query = QueryBuilder
                .newBuilder()
                .addIsIfNotNull(ID_MESSAGE_ID, messageId)
                .addInIfNotNull(ID_RECIPIENT_ID, recipientIds)
                .buildQuery();
        Update update = UpdateBuilder.newBuilder()
                .setIfNotNull(MessageStatus.Fields.recallDate, recallDate)
                .setIfNotNull(MessageStatus.Fields.readDate, readDate)
                .setIfNotNull(MessageStatus.Fields.receptionDate, receptionDate)
                .build();
        ReactiveMongoOperations mongoOperations = operations != null ? operations : mongoTemplate;
        return mongoOperations.updateFirst(query, update, MessageStatus.class)
                .map(UpdateResult::wasAcknowledged);
    }

    public Mono<Boolean> updateMessageStatus(
            @NotNull Long messageId,
            @NotNull Long recipientId,
            @Nullable @PastOrPresent Date recallDate,
            @Nullable @PastOrPresent Date readDate,
            @Nullable @PastOrPresent Date receptionDate,
            @Nullable ReactiveMongoOperations operations) {
        return updateMessageStatuses(messageId, Collections.singleton(recipientId),
                recallDate, readDate, receptionDate, operations);
    }

    public Mono<Boolean> authAndUpdateMessagesDeliveryStatus(
            @NotNull Long recipientId,
            @NotEmpty Collection<Long> messagesIds,
            @NotNull @MessageDeliveryStatusConstraint MessageDeliveryStatus deliveryStatus) {
        Query query = new Query()
                .addCriteria(Criteria.where(ID_MESSAGE_ID).in(messagesIds))
                .addCriteria(Criteria.where(ID_RECIPIENT_ID).is(recipientId));
        Update update = new Update().set(MessageStatus.Fields.deliveryStatus, deliveryStatus);
        return mongoTemplate.updateMulti(query, update, MessageStatus.class)
                .map(UpdateResult::wasAcknowledged);
    }

    public Mono<Boolean> updateMessagesReadDate(
            @NotNull Long messageId,
            @Nullable @PastOrPresent Date readDate) {
        Query query = new Query()
                .addCriteria(Criteria.where(ID_MESSAGE_ID).is(messageId))
                .addCriteria(Criteria.where(MessageStatus.Fields.readDate).is(null));
        Update update;
        if (readDate != null) {
            update = new Update().set(MessageStatus.Fields.readDate, readDate);
        } else {
            update = new Update().unset(MessageStatus.Fields.readDate);
        }
        return mongoTemplate.findAndModify(query, update, MessageStatus.class)
                .defaultIfEmpty(EMPTY_MESSAGE_STATUS)
                .map(status -> EMPTY_MESSAGE_STATUS != status);
    }

    public Mono<MessageStatus> queryMessageStatus(@NotNull Long messageId) {
        Query query = new Query().addCriteria(Criteria.where(ID_MESSAGE_ID).is(messageId));
        return mongoTemplate.findOne(query, MessageStatus.class);
    }

    public Flux<MessageStatus> queryMessageStatuses(
            @Nullable Set<Long> messageIds,
            @Nullable Set<Long> recipientIds,
            @Nullable Boolean isSystemMessage,
            @Nullable Long senderId,
            @Nullable MessageDeliveryStatus deliveryStatus,
            @Nullable DateRange receptionDateRange,
            @Nullable DateRange readDateRange,
            @Nullable DateRange recallDateRange,
            @Nullable Integer page,
            @Nullable Integer size) {
        Query query = QueryBuilder
                .newBuilder()
                .addInIfNotNull(ID_MESSAGE_ID, messageIds)
                .addInIfNotNull(ID_RECIPIENT_ID, recipientIds)
                .addIsIfNotNull(MessageStatus.Fields.isSystemMessage, isSystemMessage)
                .addIsIfNotNull(MessageStatus.Fields.senderId, senderId)
                .addIsIfNotNull(MessageStatus.Fields.deliveryStatus, deliveryStatus)
                .addBetweenIfNotNull(MessageStatus.Fields.receptionDate, receptionDateRange)
                .addBetweenIfNotNull(MessageStatus.Fields.readDate, readDateRange)
                .addBetweenIfNotNull(MessageStatus.Fields.recallDate, recallDateRange)
                .paginateIfNotNull(page, size);
        return mongoTemplate.find(query, MessageStatus.class);
    }

    public Mono<Long> countMessageStatuses(
            @Nullable Set<Long> messageIds,
            @Nullable Set<Long> recipientIds,
            @Nullable Boolean isSystemMessage,
            @Nullable Long senderId,
            @Nullable MessageDeliveryStatus deliveryStatus,
            @Nullable DateRange receptionDateRange,
            @Nullable DateRange readDateRange,
            @Nullable DateRange recallDateRange) {
        Query query = QueryBuilder
                .newBuilder()
                .addInIfNotNull(ID_MESSAGE_ID, messageIds)
                .addInIfNotNull(ID_RECIPIENT_ID, recipientIds)
                .addIsIfNotNull(MessageStatus.Fields.isSystemMessage, isSystemMessage)
                .addIsIfNotNull(MessageStatus.Fields.senderId, senderId)
                .addIsIfNotNull(MessageStatus.Fields.deliveryStatus, deliveryStatus)
                .addBetweenIfNotNull(MessageStatus.Fields.receptionDate, receptionDateRange)
                .addBetweenIfNotNull(MessageStatus.Fields.readDate, readDateRange)
                .addBetweenIfNotNull(MessageStatus.Fields.recallDate, recallDateRange)
                .buildQuery();
        return mongoTemplate.count(query, MessageStatus.class);
    }

    public Mono<Long> countPendingMessages(
            @NotNull ChatType chatType,
            @Nullable Boolean areSystemMessages,
            @NotNull Long groupOrSenderId,
            @NotNull Long recipientId) {
        Query query = new Query()
                .addCriteria(Criteria.where(ID_RECIPIENT_ID).is(recipientId))
                .addCriteria(Criteria.where(MessageStatus.Fields.deliveryStatus).is(MessageDeliveryStatus.READY));
        if (areSystemMessages != null) {
            query.addCriteria(Criteria.where(MessageStatus.Fields.isSystemMessage).is(areSystemMessages));
        }
        switch (chatType) {
            case PRIVATE:
                query.addCriteria(Criteria.where(MessageStatus.Fields.groupId).is(null))
                        .addCriteria(Criteria.where(MessageStatus.Fields.senderId).is(groupOrSenderId));
                break;
            case GROUP:
                query.addCriteria(Criteria.where(MessageStatus.Fields.groupId).is(groupOrSenderId));
                break;
            default:
                throw new UnsupportedOperationException("");
        }
        return mongoTemplate.count(query, MessageStatus.class);
    }

    public Mono<Boolean> acknowledge(@NotEmpty Set<Long> messagesIds) {
        Query query = new Query().addCriteria(Criteria.where(ID).in(messagesIds));
        Update update = new Update().set(MessageStatus.Fields.deliveryStatus, MessageDeliveryStatus.RECEIVED);
        if (turmsClusterManager.getTurmsProperties().getMessage()
                .isUpdateReadDateAutomaticallyAfterUserQueryingMessage()) {
            Date now = new Date();
            update.set(MessageStatus.Fields.readDate, now);
        }
        return mongoTemplate.updateMulti(query, update, MessageStatus.class)
                .map(UpdateResult::wasAcknowledged);
    }
}
