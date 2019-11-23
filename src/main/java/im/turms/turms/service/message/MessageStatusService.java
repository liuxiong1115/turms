package im.turms.turms.service.message;

import com.mongodb.client.result.UpdateResult;
import im.turms.turms.cluster.TurmsClusterManager;
import im.turms.turms.common.QueryBuilder;
import im.turms.turms.common.UpdateBuilder;
import im.turms.turms.constant.ChatType;
import im.turms.turms.constant.MessageDeliveryStatus;
import im.turms.turms.pojo.domain.MessageStatus;
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
import java.util.Collection;
import java.util.Date;
import java.util.Set;

import static im.turms.turms.common.Constants.*;

@Service
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

    public Flux<Long> getMessagesIdsByDeliveryStatusAndRecipientId(
            @NotNull MessageDeliveryStatus deliveryStatus,
            @Nullable Long recipientId) {
        Query query = QueryBuilder.newBuilder()
                .add(Criteria.where(MessageStatus.Fields.deliveryStatus).is(deliveryStatus))
                .addIsIfNotNull(ID_RECIPIENT_ID, recipientId)
                .buildQuery();
        query.fields().include(ID_MESSAGE_ID);
        return mongoTemplate
                .find(query, MessageStatus.class)
                .map(status -> status.getKey().getMessageId());
    }

    public Mono<Boolean> updateMessageStatus(
            @NotNull Long messageId,
            @NotNull Long recipientId,
            @Nullable Date recallDate,
            @Nullable Date readDate,
            @Nullable ReactiveMongoOperations operations) {
        boolean unrecallable = recallDate != null
                && !turmsClusterManager.getTurmsProperties().getMessage().isAllowRecallingMessage();
        boolean unreadable = readDate != null
                && !turmsClusterManager.getTurmsProperties().getMessage().getReadReceipt().isEnabled();
        if (unrecallable || unreadable) {
            return Mono.just(false);
        }
        Query query = new Query()
                .addCriteria(Criteria.where(ID_MESSAGE_ID).is(messageId))
                .addCriteria(Criteria.where(ID_RECIPIENT_ID).is(recipientId));
        Update update = UpdateBuilder.newBuilder()
                .setIfNotNull(MessageStatus.Fields.recallDate, recallDate)
                .setIfNotNull(MessageStatus.Fields.readDate, readDate)
                .build();
        ReactiveMongoOperations mongoOperations = operations != null ? operations : mongoTemplate;
        return mongoOperations.updateFirst(query, update, MessageStatus.class)
                .map(UpdateResult::wasAcknowledged);
    }

    public Mono<Boolean> authAndUpdateMessagesDeliveryStatus(
            @NotNull Long recipientId,
            @NotEmpty Collection<Long> messagesIds,
            @NotNull MessageDeliveryStatus deliveryStatus) {
        Query query = new Query()
                .addCriteria(Criteria.where(ID_MESSAGE_ID).in(messagesIds))
                .addCriteria(Criteria.where(ID_RECIPIENT_ID).is(recipientId));
        Update update = new Update().set(MessageStatus.Fields.deliveryStatus, deliveryStatus);
        return mongoTemplate.updateMulti(query, update, MessageStatus.class)
                .map(UpdateResult::wasAcknowledged);
    }

    public Mono<Boolean> updateMessagesReadDate(
            @NotNull Long messageId,
            @Nullable Date readDate) {
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

    public Flux<MessageStatus> queryMessageStatuses(@NotNull Long messageId) {
        Query query = new Query().addCriteria(Criteria.where(ID_MESSAGE_ID).is(messageId));
        return mongoTemplate.find(query, MessageStatus.class);
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
