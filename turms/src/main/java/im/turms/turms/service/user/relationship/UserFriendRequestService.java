package im.turms.turms.service.user.relationship;

import com.google.protobuf.Int64Value;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;
import im.turms.common.TurmsStatusCode;
import im.turms.common.constant.RequestStatus;
import im.turms.common.constant.ResponseAction;
import im.turms.common.exception.TurmsBusinessException;
import im.turms.common.model.bo.user.UserFriendRequestsWithVersion;
import im.turms.common.util.Validator;
import im.turms.turms.annotation.constraint.RequestStatusConstraint;
import im.turms.turms.annotation.constraint.ResponseActionConstraint;
import im.turms.turms.builder.QueryBuilder;
import im.turms.turms.builder.UpdateBuilder;
import im.turms.turms.manager.TurmsClusterManager;
import im.turms.turms.pojo.bo.DateRange;
import im.turms.turms.pojo.domain.UserFriendRequest;
import im.turms.turms.service.user.UserVersionService;
import im.turms.turms.util.ProtoUtil;
import im.turms.turms.util.RequestStatusUtil;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.mongodb.core.ReactiveMongoOperations;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;

import javax.annotation.Nullable;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.PastOrPresent;
import java.util.Calendar;
import java.util.Date;
import java.util.Set;

import static im.turms.turms.constant.Common.*;

@Service
@Validated
public class UserFriendRequestService {
    private final TurmsClusterManager turmsClusterManager;
    private final ReactiveMongoTemplate mongoTemplate;
    private final UserVersionService userVersionService;
    private final UserRelationshipService userRelationshipService;

    public UserFriendRequestService(@Lazy TurmsClusterManager turmsClusterManager, ReactiveMongoTemplate mongoTemplate, UserVersionService userVersionService, @Lazy UserRelationshipService userRelationshipService) {
        this.turmsClusterManager = turmsClusterManager;
        this.mongoTemplate = mongoTemplate;
        this.userVersionService = userVersionService;
        this.userRelationshipService = userRelationshipService;
    }

    @Scheduled(cron = EXPIRED_USER_FRIEND_REQUESTS_CLEANER_CRON)
    public void userFriendRequestsCleaner() {
        if (turmsClusterManager.isCurrentMemberMaster()) {
            if (turmsClusterManager.getTurmsProperties().getUser()
                    .getFriendRequest().isDeleteExpiredRequestsAutomatically()) {
                removeAllExpiredFriendRequests().subscribe();
            } else {
                updateExpiredRequestsStatus().subscribe();
            }
        }
    }

    public Mono<Boolean> removeAllExpiredFriendRequests() {
        Date now = new Date();
        Query query = new Query()
                .addCriteria(Criteria.where(UserFriendRequest.Fields.expirationDate).lt(now));
        return mongoTemplate.remove(query, UserFriendRequest.class)
                .map(DeleteResult::wasAcknowledged);
    }

    /**
     * Warning: Only use expirationDate to check whether a request is expired.
     * Because of the excessive resource consumption, the request status of requests
     * won't be expired immediately when reaching the expiration date.
     */
    public Mono<Boolean> updateExpiredRequestsStatus() {
        Date now = new Date();
        Query query = new Query()
                .addCriteria(Criteria.where(UserFriendRequest.Fields.expirationDate).lt(now))
                .addCriteria(Criteria.where(UserFriendRequest.Fields.status).is(RequestStatus.PENDING));
        Update update = new Update().set(UserFriendRequest.Fields.status, RequestStatus.EXPIRED);
        return mongoTemplate.updateMulti(query, update, UserFriendRequest.class)
                .map(UpdateResult::wasAcknowledged);
    }

    public Mono<Boolean> hasPendingFriendRequest(
            @NotNull Long requesterId,
            @NotNull Long recipientId) {
        Date now = new Date();
        Query query = new Query()
                .addCriteria(Criteria.where(UserFriendRequest.Fields.requesterId).is(requesterId))
                .addCriteria(Criteria.where(UserFriendRequest.Fields.recipientId).is(recipientId))
                .addCriteria(Criteria.where(UserFriendRequest.Fields.status).is(RequestStatus.PENDING))
                .addCriteria(Criteria.where(UserFriendRequest.Fields.expirationDate).gt(now));
        return mongoTemplate.exists(query, UserFriendRequest.class);
    }

    public Mono<UserFriendRequest> createFriendRequest(
            @Nullable Long id,
            @NotNull Long requesterId,
            @NotNull Long recipientId,
            @NotNull String content,
            @Nullable @RequestStatusConstraint RequestStatus status,
            @Nullable @PastOrPresent Date creationDate,
            @Nullable @PastOrPresent Date responseDate,
            @Nullable Date expirationDate,
            @Nullable String reason) {
        if (status == RequestStatus.UNRECOGNIZED || requesterId.equals(recipientId)) {
            String failedReason = status == RequestStatus.UNRECOGNIZED ?
                    "The request status must not be UNRECOGNIZED" :
                    "The requester ID must not equal the recipient ID";
            throw TurmsBusinessException.get(TurmsStatusCode.ILLEGAL_ARGUMENTS, failedReason);
        }
        id = id != null ? id : turmsClusterManager.generateRandomId();
        Date now = new Date();
        if (creationDate == null) {
            creationDate = now;
        } else {
            creationDate = creationDate.before(now) ? creationDate : now;
        }
        responseDate = RequestStatusUtil.getResponseDateBasedOnStatus(status, responseDate, now);
        if (expirationDate == null) {
            int timeToLiveHours = turmsClusterManager.getTurmsProperties()
                    .getUser().getFriendRequest().getFriendRequestTimeToLiveHours();
            if (timeToLiveHours != 0) {
                Calendar calendar = Calendar.getInstance();
                calendar.add(Calendar.HOUR, turmsClusterManager.getTurmsProperties()
                        .getUser().getFriendRequest().getFriendRequestTimeToLiveHours());
                expirationDate = calendar.getTime();
            }
        }
        if (status == null) {
            status = RequestStatus.PENDING;
        }
        UserFriendRequest userFriendRequest = new UserFriendRequest(id, content, status, reason, creationDate,
                expirationDate, responseDate, requesterId, recipientId);
        return mongoTemplate.insert(userFriendRequest)
                .flatMap(request -> userVersionService.updateReceivedFriendRequestsVersion(recipientId)
                        .then(userVersionService.updateSentFriendRequestsVersion(requesterId))
                        .thenReturn(request));
    }

    public Mono<UserFriendRequest> authAndCreateFriendRequest(
            @NotNull Long requesterId,
            @NotNull Long recipientId,
            @NotNull String content,
            @NotNull @PastOrPresent Date creationDate) {
        int contentLimit = turmsClusterManager.getTurmsProperties()
                .getUser().getFriendRequest().getContentLimit();
        boolean hasExceededLimit = contentLimit != 0 && content.length() > contentLimit;
        if (hasExceededLimit || requesterId.equals(recipientId)) {
            String reason = hasExceededLimit ?
                    String.format("The content has exceeded the character limit (%d)", contentLimit) :
                    "The requester ID must not equal the recipient ID";
            throw TurmsBusinessException.get(TurmsStatusCode.ILLEGAL_ARGUMENTS, reason);
        }
        // if requester is stranger for recipient, requester isn't blocked and already a friend.
        return userRelationshipService.isStranger(recipientId, requesterId)
                .flatMap(isStranger -> {
                    if (isStranger != null && !isStranger) {
                        Mono<Boolean> requestExistsMono;
                        // Allow to create a friend request even there is already an accepted request
                        // because the relationships can be deleted and rebuilt
                        if (turmsClusterManager.getTurmsProperties().getUser()
                                .getFriendRequest().isAllowResendingRequestAfterDeclinedOrIgnoredOrExpired()) {
                            requestExistsMono = hasPendingFriendRequest(requesterId, recipientId);
                        } else {
                            requestExistsMono = hasPendingOrDeclinedOrIgnoredOrExpiredRequest(requesterId, recipientId);
                        }
                        return requestExistsMono.flatMap(requestExists -> {
                            if (requestExists != null && !requestExists) {
                                return createFriendRequest(null, requesterId, recipientId, content, RequestStatus.PENDING, creationDate, null, null, null);
                            } else {
                                return Mono.error(TurmsBusinessException.get(TurmsStatusCode.FRIEND_REQUEST_HAS_EXISTED));
                            }
                        });
                    } else {
                        return Mono.error(TurmsBusinessException.get(TurmsStatusCode.UNAUTHORIZED));
                    }
                });
    }

    private Mono<Boolean> hasPendingOrDeclinedOrIgnoredOrExpiredRequest(
            @NotNull Long requesterId,
            @NotNull Long recipientId) {
        // Do not need to check expirationDate because both PENDING status or EXPIRED status has been used
        Query query = new Query()
                .addCriteria(Criteria.where(UserFriendRequest.Fields.requesterId).is(requesterId))
                .addCriteria(Criteria.where(UserFriendRequest.Fields.recipientId).is(recipientId))
                .addCriteria(Criteria.where(UserFriendRequest.Fields.status)
                        .in(RequestStatus.PENDING, RequestStatus.DECLINED, RequestStatus.IGNORED, RequestStatus.EXPIRED));
        return mongoTemplate.exists(query, UserFriendRequest.class);
    }

    public Mono<Boolean> updatePendingFriendRequestStatus(
            @NotNull Long requestId,
            @NotNull @RequestStatusConstraint RequestStatus requestStatus,
            @Nullable String reason,
            @Nullable ReactiveMongoOperations operations) {
        if (requestStatus == RequestStatus.UNRECOGNIZED || requestStatus == RequestStatus.PENDING) {
            String failedReason = requestStatus == RequestStatus.UNRECOGNIZED ?
                    "The request status must not be UNRECOGNIZED" :
                    "The request status must not be PENDING";
            throw TurmsBusinessException.get(TurmsStatusCode.ILLEGAL_ARGUMENTS, failedReason);
        }
        Query query = new Query()
                .addCriteria(Criteria.where(ID).is(requestId))
                .addCriteria(Criteria.where(UserFriendRequest.Fields.status).is(RequestStatus.PENDING));
        Update update = new Update()
                .set(UserFriendRequest.Fields.status, requestStatus)
                .unset(UserFriendRequest.Fields.expirationDate);
        if (reason != null) {
            update.set(UserFriendRequest.Fields.reason, reason);
        }
        ReactiveMongoOperations mongoOperations = operations != null ? operations : mongoTemplate;
        return mongoOperations.findAndModify(query, update, UserFriendRequest.class)
                .thenReturn(true)
                .defaultIfEmpty(false)
                .zipWith(queryRecipientId(requestId)
                        .map(userVersionService::updateSentFriendRequestsVersion))
                .map(Tuple2::getT1);
    }

    public Mono<Boolean> updateFriendRequests(
            @NotEmpty Set<Long> ids,
            @Nullable Long requesterId,
            @Nullable Long recipientId,
            @Nullable String content,
            @Nullable @RequestStatusConstraint RequestStatus status,
            @Nullable String reason,
            @Nullable @PastOrPresent Date creationDate,
            @Nullable @PastOrPresent Date responseDate,
            @Nullable Date expirationDate) {
        if (Validator.areAllNull(requesterId, recipientId, content, status, reason, creationDate, responseDate, expirationDate)) {
            return Mono.just(true);
        }
        if (requesterId != null && requesterId.equals(recipientId)) {
            throw TurmsBusinessException.get(TurmsStatusCode.ILLEGAL_ARGUMENTS, "The requester ID must not equal the recipient ID");
        }
        Query query = new Query().addCriteria(Criteria.where(ID).in(ids));
        Update update = UpdateBuilder
                .newBuilder()
                .setIfNotNull(UserFriendRequest.Fields.requesterId, requesterId)
                .setIfNotNull(UserFriendRequest.Fields.recipientId, recipientId)
                .setIfNotNull(UserFriendRequest.Fields.content, content)
                .setIfNotNull(UserFriendRequest.Fields.reason, reason)
                .setIfNotNull(UserFriendRequest.Fields.creationDate, creationDate)
                .setIfNotNull(UserFriendRequest.Fields.expirationDate, expirationDate)
                .build();
        RequestStatusUtil.updateResponseDateBasedOnStatus(update, status, new Date());
        return mongoTemplate.updateMulti(query, update, UserFriendRequest.class)
                .map(UpdateResult::wasAcknowledged);
    }

    public Mono<Long> queryRecipientId(@NotNull Long requestId) {
        Query query = new Query().addCriteria(Criteria.where(ID).is(requestId));
        query.fields().include(UserFriendRequest.Fields.recipientId);
        return mongoTemplate.findOne(query, UserFriendRequest.class)
                .map(UserFriendRequest::getRecipientId);
    }

    public Mono<UserFriendRequest> queryRequesterAndRecipient(@NotNull Long requestId) {
        Query query = new Query().addCriteria(Criteria.where(ID).is(requestId));
        query.fields()
                .include(UserFriendRequest.Fields.requesterId)
                .include(UserFriendRequest.Fields.recipientId);
        return mongoTemplate.findOne(query, UserFriendRequest.class);
    }

    public Mono<Boolean> handleFriendRequest(
            @NotNull Long friendRequestId,
            @NotNull Long requesterId,
            @NotNull @ResponseActionConstraint ResponseAction action,
            @Nullable String reason) {
        if (action != ResponseAction.UNRECOGNIZED) {
            return queryRequesterAndRecipient(friendRequestId)
                    .flatMap(request -> {
                        if (request.getRecipientId().equals(requesterId)) {
                            switch (action) {
                                case ACCEPT:
                                    return mongoTemplate.inTransaction()
                                            .execute(operations -> updatePendingFriendRequestStatus(friendRequestId, RequestStatus.ACCEPTED, reason, operations)
                                                    .then(userRelationshipService.friendTwoUsers(request.getRequesterId(), requesterId, operations))
                                                    .thenReturn(true))
                                            .retryWhen(TRANSACTION_RETRY)
                                            .singleOrEmpty();
                                case IGNORE:
                                    return updatePendingFriendRequestStatus(friendRequestId, RequestStatus.IGNORED, reason, null);
                                case DECLINE:
                                    return updatePendingFriendRequestStatus(friendRequestId, RequestStatus.DECLINED, reason, null);
                                default:
                                    return Mono.error(TurmsBusinessException.get(TurmsStatusCode.ILLEGAL_ARGUMENTS, "The response action must not be UNRECOGNIZED"));
                            }
                        } else {
                            return Mono.error(TurmsBusinessException.get(TurmsStatusCode.UNAUTHORIZED));
                        }
                    });
        } else {
            throw TurmsBusinessException.get(TurmsStatusCode.ILLEGAL_ARGUMENTS, "The response action must not be UNRECOGNIZED");
        }
    }

    public Mono<UserFriendRequestsWithVersion> queryFriendRequestsWithVersion(
            @NotNull Long userId,
            boolean areSentByUser,
            @Nullable Date lastUpdatedDate) {
        Mono<Date> versionMono = areSentByUser
                ? userVersionService.querySentFriendRequestsVersion(userId)
                : userVersionService.queryReceivedFriendRequestsVersion(userId);
        return versionMono
                .flatMap(version -> {
                    if (lastUpdatedDate == null || lastUpdatedDate.before(version)) {
                        Flux<UserFriendRequest> requestFlux = areSentByUser
                                ? queryFriendRequestsByRequesterId(userId)
                                : queryFriendRequestsByRecipientId(userId);
                        return requestFlux
                                .collectList()
                                .map(requests -> {
                                    if (!requests.isEmpty()) {
                                        UserFriendRequestsWithVersion.Builder builder = UserFriendRequestsWithVersion.newBuilder();
                                        for (UserFriendRequest request : requests) {
                                            builder.addUserFriendRequests(ProtoUtil.friendRequest2proto(request));
                                        }
                                        return builder
                                                .setLastUpdatedDate(Int64Value.newBuilder().setValue(version.getTime()).build())
                                                .build();
                                    } else {
                                        throw TurmsBusinessException.get(TurmsStatusCode.NO_CONTENT);
                                    }
                                });
                    } else {
                        return Mono.error(TurmsBusinessException.get(TurmsStatusCode.ALREADY_UP_TO_DATE));
                    }
                })
                .switchIfEmpty(Mono.error(TurmsBusinessException.get(TurmsStatusCode.ALREADY_UP_TO_DATE)));
    }

    public Flux<UserFriendRequest> queryFriendRequestsByRecipientId(@NotNull Long recipientId) {
        Query query = new Query()
                .addCriteria(Criteria.where(UserFriendRequest.Fields.recipientId).is(recipientId));
        return queryExpirableData(query);
    }

    public Flux<UserFriendRequest> queryFriendRequestsByRequesterId(@NotNull Long requesterId) {
        Query query = new Query()
                .addCriteria(Criteria.where(UserFriendRequest.Fields.requesterId).is(requesterId));
        return queryExpirableData(query);
    }

    public Flux<UserFriendRequest> queryFriendRequestsByRecipientIdOrRequesterId(@NotNull Long userId) {
        Query query = new Query()
                .addCriteria(Criteria.where(UserFriendRequest.Fields.recipientId).is(userId)
                        .orOperator(Criteria.where(UserFriendRequest.Fields.requesterId).is(userId)));
        return queryExpirableData(query);
    }

    private Flux<UserFriendRequest> queryExpirableData(Query query) {
        return mongoTemplate.find(query, UserFriendRequest.class)
                .map(friendRequest -> {
                    Date expirationDate = friendRequest.getExpirationDate();
                    if (expirationDate != null
                            && friendRequest.getStatus() == RequestStatus.PENDING
                            && expirationDate.getTime() < System.currentTimeMillis()) {
                        return friendRequest.toBuilder().status(RequestStatus.EXPIRED).build();
                    } else {
                        return friendRequest;
                    }
                });
    }

    public Mono<Boolean> deleteFriendRequests(@Nullable Set<Long> ids) {
        Query query = QueryBuilder
                .newBuilder()
                .addInIfNotNull(ID, ids)
                .buildQuery();
        return mongoTemplate.remove(query, UserFriendRequest.class)
                .map(DeleteResult::wasAcknowledged);
    }

    public Flux<UserFriendRequest> queryFriendRequests(
            @Nullable Set<Long> ids,
            @Nullable Set<Long> requesterIds,
            @Nullable Set<Long> recipientIds,
            @Nullable Set<RequestStatus> statuses,
            @Nullable DateRange creationDateRange,
            @Nullable DateRange responseDateRange,
            @Nullable DateRange expirationDateRange,
            @Nullable Integer page,
            @Nullable Integer size) {
        Query query = QueryBuilder
                .newBuilder()
                .addInIfNotNull(ID, ids)
                .addInIfNotNull(UserFriendRequest.Fields.requesterId, requesterIds)
                .addInIfNotNull(UserFriendRequest.Fields.recipientId, recipientIds)
                .addInIfNotNull(UserFriendRequest.Fields.status, statuses)
                .addBetweenIfNotNull(UserFriendRequest.Fields.creationDate, creationDateRange)
                .addBetweenIfNotNull(UserFriendRequest.Fields.responseDate, responseDateRange)
                .addBetweenIfNotNull(UserFriendRequest.Fields.expirationDate, expirationDateRange)
                .paginateIfNotNull(page, size);
        return mongoTemplate.find(query, UserFriendRequest.class);
    }

    public Mono<Long> countFriendRequests(
            @Nullable Set<Long> ids,
            @Nullable Set<Long> requesterIds,
            @Nullable Set<Long> recipientIds,
            @Nullable Set<RequestStatus> statuses,
            @Nullable DateRange creationDateRange,
            @Nullable DateRange responseDateRange,
            @Nullable DateRange expirationDateRange) {
        Query query = QueryBuilder
                .newBuilder()
                .addInIfNotNull(ID, ids)
                .addInIfNotNull(UserFriendRequest.Fields.requesterId, requesterIds)
                .addInIfNotNull(UserFriendRequest.Fields.recipientId, recipientIds)
                .addInIfNotNull(UserFriendRequest.Fields.status, statuses)
                .addBetweenIfNotNull(UserFriendRequest.Fields.creationDate, creationDateRange)
                .addBetweenIfNotNull(UserFriendRequest.Fields.responseDate, responseDateRange)
                .addBetweenIfNotNull(UserFriendRequest.Fields.expirationDate, expirationDateRange)
                .buildQuery();
        return mongoTemplate.count(query, UserFriendRequest.class);
    }
}
