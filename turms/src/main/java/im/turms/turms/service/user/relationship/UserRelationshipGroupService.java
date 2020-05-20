package im.turms.turms.service.user.relationship;

import com.google.common.collect.HashMultimap;
import com.google.protobuf.Int64Value;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;
import im.turms.common.TurmsStatusCode;
import im.turms.common.exception.TurmsBusinessException;
import im.turms.common.model.bo.user.UserRelationshipGroupsWithVersion;
import im.turms.turms.annotation.constraint.UserRelationshipGroupKeyConstraint;
import im.turms.turms.builder.QueryBuilder;
import im.turms.turms.builder.UpdateBuilder;
import im.turms.turms.manager.TurmsClusterManager;
import im.turms.turms.pojo.bo.DateRange;
import im.turms.turms.pojo.domain.UserRelationshipGroup;
import im.turms.turms.pojo.domain.UserRelationshipGroupMember;
import im.turms.turms.service.user.UserVersionService;
import im.turms.turms.util.ProtoUtil;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Lazy;
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
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;

import static im.turms.turms.constant.Common.DEFAULT_RELATIONSHIP_GROUP_INDEX;
import static im.turms.turms.constant.Common.TRANSACTION_RETRY;
import static im.turms.turms.pojo.domain.UserRelationshipGroup.Fields.ID_GROUP_INDEX;
import static im.turms.turms.pojo.domain.UserRelationshipGroup.Fields.ID_OWNER_ID;
import static im.turms.turms.pojo.domain.UserRelationshipGroupMember.Fields.ID_RELATED_USER_ID;

@Service
@Validated
public class UserRelationshipGroupService {
    private final TurmsClusterManager turmsClusterManager;
    private final ReactiveMongoTemplate mongoTemplate;
    private final UserVersionService userVersionService;
    private final UserRelationshipService userRelationshipService;
    private static final UserRelationshipGroup EMPTY_RELATIONSHIP_GROUP = new UserRelationshipGroup(null, null, null, null);

    public UserRelationshipGroupService(
            @Qualifier("userMongoTemplate") ReactiveMongoTemplate mongoTemplate,
            TurmsClusterManager turmsClusterManager,
            UserVersionService userVersionService,
            @Lazy UserRelationshipService userRelationshipService) {
        this.mongoTemplate = mongoTemplate;
        this.turmsClusterManager = turmsClusterManager;
        this.userVersionService = userVersionService;
        this.userRelationshipService = userRelationshipService;
    }

    public Mono<UserRelationshipGroup> createRelationshipGroup(
            @NotNull Long ownerId,
            @Nullable Integer groupIndex,
            @NotNull String groupName,
            @Nullable @PastOrPresent Date creationDate,
            @Nullable ReactiveMongoOperations operations) {
        if (groupIndex == null) {
            groupIndex = turmsClusterManager.generateRandomId().intValue();
        }
        if (creationDate == null) {
            creationDate = new Date();
        }
        UserRelationshipGroup group = new UserRelationshipGroup(
                ownerId,
                groupIndex,
                groupName,
                creationDate);
        ReactiveMongoOperations mongoOperations = operations != null ? operations : mongoTemplate;
        return mongoOperations.insert(group);
    }

    public Flux<UserRelationshipGroup> queryRelationshipGroupsInfos(@NotNull Long ownerId) {
        Query query = new Query()
                .addCriteria(Criteria.where(ID_OWNER_ID).is(ownerId));
        return mongoTemplate.find(query, UserRelationshipGroup.class);
    }

    public Mono<UserRelationshipGroupsWithVersion> queryRelationshipGroupsInfosWithVersion(
            @NotNull Long ownerId,
            @Nullable Date lastUpdatedDate) {
        return userVersionService.queryRelationshipGroupsLastUpdatedDate(ownerId)
                .flatMap(date -> {
                    if (lastUpdatedDate == null || lastUpdatedDate.before(date)) {
                        UserRelationshipGroupsWithVersion.Builder builder = UserRelationshipGroupsWithVersion.newBuilder();
                        builder.setLastUpdatedDate(Int64Value.newBuilder().setValue(date.getTime()).build());
                        return queryRelationshipGroupsInfos(ownerId)
                                .map(group -> builder.addUserRelationshipGroups(ProtoUtil.relationshipGroup2proto(group)))
                                .then(Mono.just(builder.build()));
                    } else {
                        return Mono.error(TurmsBusinessException.get(TurmsStatusCode.ALREADY_UP_TO_DATE));
                    }
                })
                .switchIfEmpty(Mono.error(TurmsBusinessException.get(TurmsStatusCode.ALREADY_UP_TO_DATE)));
    }

    public Flux<Integer> queryGroupIndexes(
            @NotNull Long ownerId,
            @NotNull Long relatedUserId) {
        Query query = new Query()
                .addCriteria(Criteria.where(ID_OWNER_ID).is(ownerId))
                .addCriteria(Criteria.where(ID_RELATED_USER_ID).is(relatedUserId));
        query.fields().include(ID_GROUP_INDEX);
        return mongoTemplate.find(query, UserRelationshipGroupMember.class)
                .map(member -> member.getKey().getGroupIndex());
    }

    public Flux<Long> queryRelatedUsersIdsInRelationshipGroup(
            @NotNull Long ownerId,
            @NotNull Integer groupIndex) {
        Query query = new Query()
                .addCriteria(Criteria.where(ID_OWNER_ID).is(ownerId))
                .addCriteria(Criteria.where(ID_GROUP_INDEX).is(groupIndex));
        query.fields().include(ID_RELATED_USER_ID);
        return mongoTemplate.find(query, UserRelationshipGroupMember.class)
                .map(member -> member.getKey().getRelatedUserId());
    }

    public Mono<Boolean> updateRelationshipGroupName(
            @NotNull Long ownerId,
            @NotNull Integer groupIndex,
            @NotNull String newGroupName) {
        Query query = new Query()
                .addCriteria(Criteria.where(ID_OWNER_ID).is(ownerId))
                .addCriteria(Criteria.where(ID_GROUP_INDEX).is(groupIndex));
        Update update = new Update().set(UserRelationshipGroup.Fields.NAME, newGroupName);
        return mongoTemplate.findAndModify(query, update, UserRelationshipGroup.class)
                .defaultIfEmpty(EMPTY_RELATIONSHIP_GROUP)
                .flatMap(group -> {
                    if (EMPTY_RELATIONSHIP_GROUP == group) {
                        return Mono.error(TurmsBusinessException.get(TurmsStatusCode.NO_CONTENT));
                    } else {
                        return userVersionService.updateRelationshipGroupsVersion(ownerId)
                                .thenReturn(true);
                    }
                });
    }

    public Mono<Boolean> updateRelationshipGroups(
            @NotEmpty Set<UserRelationshipGroup.@UserRelationshipGroupKeyConstraint Key> keys,
            @Nullable String name,
            @Nullable @PastOrPresent Date creationDate) {
        HashMultimap<Long, Integer> multimap = HashMultimap.create();
        for (UserRelationshipGroup.Key key : keys) {
            multimap.put(key.getOwnerId(), key.getGroupIndex());
        }
        ArrayList<Mono<Boolean>> monos = new ArrayList<>(multimap.keySet().size());
        for (Long ownerId : multimap.keySet()) {
            Set<Integer> indexes = multimap.get(ownerId);
            monos.add(updateRelationshipGroups(ownerId, indexes, name, creationDate));
        }
        return Flux.merge(monos).all(value -> value);
    }

    public Mono<Boolean> updateRelationshipGroups(
            @NotNull Long ownerId,
            @Nullable Set<Integer> indexes,
            @Nullable String name,
            @Nullable @PastOrPresent Date creationDate) {
        Query query = QueryBuilder
                .newBuilder()
                .addIsIfNotNull(ID_OWNER_ID, ownerId)
                .addInIfNotNull(ID_GROUP_INDEX, indexes)
                .buildQuery();
        Update update = UpdateBuilder
                .newBuilder()
                .setIfNotNull(UserRelationshipGroup.Fields.NAME, name)
                .setIfNotNull(UserRelationshipGroup.Fields.CREATION_DATE, creationDate)
                .build();
        return mongoTemplate.updateMulti(query, update, UserRelationshipGroup.class)
                .map(UpdateResult::wasAcknowledged);
    }

    public Mono<Boolean> addRelatedUserToRelationshipGroups(
            @NotNull Long ownerId,
            @NotEmpty Set<Integer> groupIndexes,
            @NotNull Long relatedUserId,
            @Nullable ReactiveMongoOperations operations) {
        if (ownerId.equals(relatedUserId)) {
            throw TurmsBusinessException.get(TurmsStatusCode.ILLEGAL_ARGUMENTS, "The owner ID must not equal the related user ID");
        }
        return userRelationshipService.hasOneSidedRelationship(ownerId, relatedUserId)
                .flatMap(hasRelationship -> {
                    if (hasRelationship != null && hasRelationship) {
                        List<Mono<?>> monos = new ArrayList<>(groupIndexes.size());
                        for (Integer groupIndex : groupIndexes) {
                            UserRelationshipGroupMember member = new UserRelationshipGroupMember(
                                    ownerId, groupIndex, relatedUserId, new Date());
                            ReactiveMongoOperations mongoOperations = operations != null ? operations : mongoTemplate;
                            monos.add(mongoOperations.save(member));
                        }
                        monos.add(userVersionService.updateRelationshipGroupsVersion(ownerId));
                        return Mono.when(monos).thenReturn(true);
                    } else {
                        return Mono.error(TurmsBusinessException.get(TurmsStatusCode.UNAUTHORIZED));
                    }
                });
    }

    public Mono<Boolean> deleteRelationshipGroupAndMoveMembers(
            @NotNull Long ownerId,
            @NotNull Integer deleteGroupIndex,
            @NotNull Integer existingUsersToTargetGroupIndex) {
        if (deleteGroupIndex.equals(DEFAULT_RELATIONSHIP_GROUP_INDEX)) {
            throw TurmsBusinessException.get(TurmsStatusCode.ILLEGAL_ARGUMENTS, "The default relationship group cannot be deleted");
        }
        if (deleteGroupIndex.equals(existingUsersToTargetGroupIndex)) {
            return Mono.just(true);
        } else {
            return mongoTemplate.inTransaction()
                    .execute(operations -> {
                        Query query = new Query()
                                .addCriteria(Criteria.where(ID_OWNER_ID).is(ownerId))
                                .addCriteria(Criteria.where(ID_GROUP_INDEX).is(deleteGroupIndex));
                        Update update = new Update().set(ID_GROUP_INDEX, existingUsersToTargetGroupIndex);
                        return operations.findAndModify(query, update, UserRelationshipGroupMember.class)
                                .then(operations.remove(query, UserRelationshipGroup.class))
                                .then(userVersionService.updateRelationshipGroupsVersion(ownerId))
                                .thenReturn(true);
                    })
                    .retryWhen(TRANSACTION_RETRY)
                    .singleOrEmpty();
        }
    }

    public Mono<Boolean> deleteAllRelationshipGroups(
            @NotEmpty Set<Long> ownerIds,
            @Nullable ReactiveMongoOperations operations,
            boolean updateRelationshipGroupsVersion) {
        Query query = new Query()
                .addCriteria(Criteria.where(ID_OWNER_ID).in(ownerIds))
                .addCriteria(Criteria.where(ID_GROUP_INDEX).ne(0));
        ReactiveMongoOperations mongoOperations = operations != null ? operations : mongoTemplate;
        if (updateRelationshipGroupsVersion) {
            return mongoOperations.remove(query, UserRelationshipGroup.class)
                    .flatMap(result -> {
                        if (result.wasAcknowledged()) {
                            return userVersionService.updateRelationshipGroupsVersion(ownerIds)
                                    .thenReturn(true);
                        } else {
                            return Mono.just(false);
                        }
                    });
        } else {
            return mongoOperations.remove(query, UserRelationshipGroup.class)
                    .map(DeleteResult::wasAcknowledged);
        }
    }

    public Mono<Boolean> deleteRelatedUserFromAllRelationshipGroups(
            @NotNull Long ownerId,
            @NotNull Long relatedUserId,
            @Nullable ReactiveMongoOperations operations,
            boolean updateRelationshipGroupsMembersVersion) {
        return deleteRelatedUsersFromAllRelationshipGroups(Set.of(ownerId), Set.of(relatedUserId), operations, updateRelationshipGroupsMembersVersion);
    }

    public Mono<Boolean> deleteRelatedUsersFromAllRelationshipGroups(
            @NotEmpty Set<Long> ownerIds,
            @NotEmpty Set<Long> relatedUserIds,
            @Nullable ReactiveMongoOperations operations,
            boolean updateRelationshipGroupsMembersVersion) {
        Query query = new Query()
                .addCriteria(Criteria.where(ID_OWNER_ID).in(ownerIds))
                .addCriteria(Criteria.where(ID_RELATED_USER_ID).in(relatedUserIds));
        ReactiveMongoOperations mongoOperations = operations != null ? operations : mongoTemplate;
        if (updateRelationshipGroupsMembersVersion) {
            return mongoOperations.remove(query, UserRelationshipGroupMember.class)
                    .flatMap(result -> {
                        if (result.wasAcknowledged()) {
                            return userVersionService.updateRelationshipGroupsVersion(ownerIds)
                                    .thenReturn(true);
                        } else {
                            return Mono.just(false);
                        }
                    });
        } else {
            return mongoOperations.remove(query, UserRelationshipGroupMember.class)
                    .map(DeleteResult::wasAcknowledged);
        }
    }

    public Mono<Boolean> removeRelatedUserFromRelationshipGroup(
            @NotNull Long ownerId,
            @NotNull Long relatedUserId,
            @NotNull Integer currentGroupIndex,
            @NotNull Integer targetGroupIndex) {
        Query query = new Query()
                .addCriteria(Criteria.where(ID_OWNER_ID).is(ownerId))
                .addCriteria(Criteria.where(ID_RELATED_USER_ID).is(relatedUserId))
                .addCriteria(Criteria.where(ID_GROUP_INDEX).is(currentGroupIndex));
        if (currentGroupIndex.equals(targetGroupIndex)) {
            return Mono.just(true);
        } else {
            Update update = new Update().set(ID_GROUP_INDEX, targetGroupIndex);
            return mongoTemplate.findAndModify(query, update, UserRelationshipGroupMember.class)
                    .thenReturn(true);
        }
    }

    public Mono<Boolean> deleteRelationshipGroups() {
        return mongoTemplate.remove(new Query(), UserRelationshipGroup.class)
                .map(DeleteResult::wasAcknowledged);
    }

    public Mono<Boolean> deleteRelationshipGroups(@NotEmpty Set<UserRelationshipGroup.@UserRelationshipGroupKeyConstraint Key> keys) {
        HashMultimap<Long, Integer> multimap = HashMultimap.create();
        for (UserRelationshipGroup.Key key : keys) {
            multimap.put(key.getOwnerId(), key.getGroupIndex());
        }
        ArrayList<Mono<Boolean>> monos = new ArrayList<>(multimap.keySet().size());
        for (Long ownerId : multimap.keySet()) {
            Set<Integer> indexes = multimap.get(ownerId);
            monos.add(deleteRelationshipGroups(ownerId, indexes));
        }
        return Flux.merge(monos).all(value -> value);
    }

    public Mono<Boolean> deleteRelationshipGroups(@Nullable Long ownerId, @Nullable Set<Integer> indexes) {
        Query query = QueryBuilder
                .newBuilder()
                .addIsIfNotNull(ID_OWNER_ID, ownerId)
                .addInIfNotNull(ID_GROUP_INDEX, indexes)
                .buildQuery();
        return mongoTemplate.remove(query, UserRelationshipGroup.class)
                .map(DeleteResult::wasAcknowledged);
    }

    public Flux<UserRelationshipGroup> queryRelationshipGroups(
            @Nullable Set<Long> ownerIds,
            @Nullable Set<Integer> indexes,
            @Nullable Set<String> names,
            @Nullable DateRange creationDateRange,
            @Nullable Integer page,
            @Nullable Integer size) {
        Query query = QueryBuilder
                .newBuilder()
                .addInIfNotNull(ID_OWNER_ID, ownerIds)
                .addInIfNotNull(ID_GROUP_INDEX, indexes)
                .addInIfNotNull(UserRelationshipGroup.Fields.NAME, names)
                .addBetweenIfNotNull(UserRelationshipGroup.Fields.CREATION_DATE, creationDateRange)
                .paginateIfNotNull(page, size);
        return mongoTemplate.find(query, UserRelationshipGroup.class);
    }

    public Mono<Long> countRelationshipGroups(
            @Nullable Set<Long> ownerIds,
            @Nullable Set<Integer> indexes,
            @Nullable Set<String> names,
            @Nullable DateRange creationDateRange) {
        Query query = QueryBuilder
                .newBuilder()
                .addInIfNotNull(ID_OWNER_ID, ownerIds)
                .addInIfNotNull(ID_GROUP_INDEX, indexes)
                .addInIfNotNull(UserRelationshipGroup.Fields.NAME, names)
                .addBetweenIfNotNull(UserRelationshipGroup.Fields.CREATION_DATE, creationDateRange)
                .buildQuery();
        return mongoTemplate.count(query, UserRelationshipGroup.class);
    }
}
