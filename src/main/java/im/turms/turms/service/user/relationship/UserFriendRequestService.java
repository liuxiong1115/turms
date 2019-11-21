package im.turms.turms.service.user.relationship;

import com.google.protobuf.Int64Value;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;
import im.turms.turms.cluster.TurmsClusterManager;
import im.turms.turms.common.ProtoUtil;
import im.turms.turms.common.TurmsStatusCode;
import im.turms.turms.constant.RequestStatus;
import im.turms.turms.constant.ResponseAction;
import im.turms.turms.exception.TurmsBusinessException;
import im.turms.turms.pojo.bo.user.UserFriendRequestsWithVersion;
import im.turms.turms.pojo.domain.UserFriendRequest;
import im.turms.turms.service.user.UserVersionService;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.mongodb.core.ReactiveMongoOperations;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;
import java.util.Calendar;
import java.util.Date;

import static im.turms.turms.common.Constants.*;

@Service
public class UserFriendRequestService {
    private final TurmsClusterManager turmsClusterManager;
    private final ReactiveMongoTemplate mongoTemplate;
    private final UserVersionService userVersionService;
    private final UserRelationshipService userRelationshipService;
    private static final UserFriendRequest EMPTY_FRIEND_REQUEST = new UserFriendRequest();

    public UserFriendRequestService(@Lazy TurmsClusterManager turmsClusterManager, ReactiveMongoTemplate mongoTemplate, UserVersionService userVersionService, @Lazy UserRelationshipService userRelationshipService, TaskScheduler taskScheduler) {
        this.turmsClusterManager = turmsClusterManager;
        this.mongoTemplate = mongoTemplate;
        this.userVersionService = userVersionService;
        this.userRelationshipService = userRelationshipService;
    }

    @Scheduled(cron = EXPIRY_USER_FRIEND_REQUESTS_CLEANER_CRON)
    public void userFriendRequestsCleaner() {
        if (turmsClusterManager.isCurrentMemberMaster()) {
            if (turmsClusterManager.getTurmsProperties().getUser()
                    .getFriendRequest().isDeleteExpiryRequests()) {
                removeAllExpiryFriendRequests().subscribe();
            } else {
                updateExpiryRequestsStatus().subscribe();
            }
        }
    }

    public Mono<Boolean> removeAllExpiryFriendRequests() {
        Date now = new Date();
        Query query = new Query()
                .addCriteria(Criteria.where(UserFriendRequest.Fields.expirationDate).lt(now));
        return mongoTemplate.remove(query, UserFriendRequest.class)
                .map(DeleteResult::wasAcknowledged);
    }

    /**
     * Warning: Only use expirationDate to check whether a request is expiry.
     * Because of the excessive resource consumption, the request status of requests
     * won't be expiry immediately when reaching the expiration date.
     */
    public Mono<Boolean> updateExpiryRequestsStatus() {
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
            @NotNull Long requesterId,
            @NotNull Long recipientId,
            @NotNull String content,
            @NotNull Date creationDate) {
        UserFriendRequest userFriendRequest = new UserFriendRequest();
        userFriendRequest.setId(turmsClusterManager.generateRandomId());
        userFriendRequest.setContent(content);
        Date now = new Date();
        userFriendRequest.setCreationDate(creationDate.before(now) ? creationDate : now);
        int friendRequestExpiryHours = turmsClusterManager.getTurmsProperties()
                .getUser().getFriendRequestExpiryHours();
        if (friendRequestExpiryHours != 0) {
            Calendar calendar = Calendar.getInstance();
            calendar.add(Calendar.HOUR, turmsClusterManager.getTurmsProperties()
                    .getUser().getFriendRequestExpiryHours());
            userFriendRequest.setExpirationDate(calendar.getTime());
        }
        userFriendRequest.setRequesterId(requesterId);
        userFriendRequest.setRecipientId(recipientId);
        userFriendRequest.setStatus(RequestStatus.PENDING);
        return mongoTemplate.insert(userFriendRequest)
                .zipWith(userVersionService.updateFriendRequestsVersion(recipientId))
                .map(Tuple2::getT1);
    }

    public Mono<UserFriendRequest> authAndCreateFriendRequest(
            @NotNull Long requesterId,
            @NotNull Long recipientId,
            @NotNull String content,
            @NotNull Date creationDate) {
        int contentLimit = turmsClusterManager.getTurmsProperties()
                .getUser().getFriendRequest().getContentLimit();
        if (contentLimit != 0 && content.length() > contentLimit) {
            throw TurmsBusinessException.get(TurmsStatusCode.ILLEGAL_ARGUMENTS);
        }
        // if requester is stranger for recipient, requester isn't blocked and already a friend.
        return userRelationshipService.isStranger(recipientId, requesterId)
                .flatMap(isStranger -> {
                    if (isStranger != null && !isStranger) {
                        Mono<Boolean> requestExistsMono;
                        // Allow to create a friend request even there is already an accepted request
                        // because the relationships can be deleted and rebuilt
                        if (turmsClusterManager.getTurmsProperties().getUser()
                                .getFriendRequest().isAllowResendRequestAfterDeclinedOrIgnoredOrExpired()) {
                            requestExistsMono = hasPendingFriendRequest(requesterId, recipientId);
                        } else {
                            requestExistsMono = hasPendingOrDeclinedOrIgnoredOrExpiredRequest(requesterId, recipientId);
                        }
                        return requestExistsMono.flatMap(requestExists -> {
                            if (requestExists != null && !requestExists) {
                                return createFriendRequest(requesterId, recipientId, content, creationDate);
                            } else {
                                return Mono.error(TurmsBusinessException.get(TurmsStatusCode.OWNED_RESOURCE_LIMIT_REACHED));
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
            @NotNull RequestStatus requestStatus,
            @Nullable String reason,
            @Nullable ReactiveMongoOperations operations) {
        if (requestStatus == RequestStatus.UNRECOGNIZED || requestStatus == RequestStatus.PENDING) {
            throw TurmsBusinessException.get(TurmsStatusCode.ILLEGAL_ARGUMENTS);
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
                .zipWith(
                queryRecipientId(requestId)
                        .map(userVersionService::updateFriendRequestsVersion))
                .map(Tuple2::getT1);
    }

    public Mono<Long> queryRecipientId(@NotNull Long requestId) {
        Query query = new Query().addCriteria(Criteria.where(ID).is(requestId));
        query.fields().include(UserFriendRequest.Fields.recipientId);
        return mongoTemplate.findOne(query, UserFriendRequest.class)
                .map(UserFriendRequest::getRecipientId);
    }

    public Mono<Boolean> isFriendRequestRecipient(@NotNull Long requestId, @NotNull Long userId) {
        Query query = new Query()
                .addCriteria(Criteria.where(ID).is(requestId))
                .addCriteria(Criteria.where(UserFriendRequest.Fields.recipientId).is(userId));
        return mongoTemplate.exists(query, UserFriendRequest.class);
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
            @NotNull ResponseAction action,
            @Nullable String reason) {
        if (action != ResponseAction.UNRECOGNIZED) {
            return queryRequesterAndRecipient(friendRequestId)
                    .flatMap(request -> {
                        if (request.getRecipientId().equals(requesterId)) {
                            switch (action) {
                                case ACCEPT:
                                    return mongoTemplate.inTransaction()
                                            .execute(operations -> updatePendingFriendRequestStatus(friendRequestId, RequestStatus.ACCEPTED, reason, operations)
                                                    .zipWith(userRelationshipService.friendTwoUsers(request.getRequesterId(), requesterId, operations))
                                                    .thenReturn(true))
                                            .retryBackoff(MONGO_TRANSACTION_RETRIES_NUMBER, MONGO_TRANSACTION_BACKOFF)
                                            .single();
                                case IGNORE:
                                    return updatePendingFriendRequestStatus(friendRequestId, RequestStatus.IGNORED, reason, null);
                                case DECLINE:
                                    return updatePendingFriendRequestStatus(friendRequestId, RequestStatus.DECLINED, reason, null);
                                default:
                                    return Mono.error(TurmsBusinessException.get(TurmsStatusCode.ILLEGAL_ARGUMENTS));
                            }
                        } else {
                            return Mono.error(TurmsBusinessException.get(TurmsStatusCode.UNAUTHORIZED));
                        }
                    });
        } else {
            throw TurmsBusinessException.get(TurmsStatusCode.ILLEGAL_ARGUMENTS);
        }
    }

    public Mono<UserFriendRequestsWithVersion> queryFriendRequestsWithVersion(
            @NotNull Long recipientId,
            @Nullable Date lastUpdatedDate) {
        return userVersionService.queryFriendRequestsVersion(recipientId)
                .defaultIfEmpty(MAX_DATE)
                .flatMap(version -> {
                    if (lastUpdatedDate == null || lastUpdatedDate.before(version)) {
                        Query query = new Query()
                                .addCriteria(Criteria.where(UserFriendRequest.Fields.recipientId).is(recipientId));
                        return mongoTemplate.find(query, UserFriendRequest.class)
                                .collectList()
                                .map(requests -> {
                                    if (!requests.isEmpty()) {
                                        UserFriendRequestsWithVersion.Builder builder = UserFriendRequestsWithVersion.newBuilder();
                                        builder.setLastUpdatedDate(Int64Value.newBuilder().setValue(version.getTime()).build());
                                        for (UserFriendRequest request : requests) {
                                            if (request.getStatus() == RequestStatus.PENDING
                                                    && request.getExpirationDate().before(new Date())) {
                                                request.setStatus(RequestStatus.EXPIRED);
                                            }
                                            builder.addUserFriendRequests(ProtoUtil.friendRequest2proto(request));
                                        }
                                        return builder.build();
                                    } else {
                                        throw TurmsBusinessException.get(TurmsStatusCode.NO_CONTENT);
                                    }
                                });
                    } else {
                        return Mono.error(TurmsBusinessException.get(TurmsStatusCode.ALREADY_UP_TO_DATE));
                    }
                });
    }
}
