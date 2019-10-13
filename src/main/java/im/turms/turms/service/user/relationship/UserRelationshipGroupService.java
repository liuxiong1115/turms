package im.turms.turms.service.user.relationship;

import com.google.protobuf.Int64Value;
import im.turms.turms.cluster.TurmsClusterManager;
import im.turms.turms.common.ProtoUtil;
import im.turms.turms.common.TurmsStatusCode;
import im.turms.turms.exception.TurmsBusinessException;
import im.turms.turms.pojo.domain.UserRelationshipGroup;
import im.turms.turms.pojo.domain.UserRelationshipGroupMember;
import im.turms.turms.pojo.response.Int64ValuesWithVersion;
import im.turms.turms.pojo.response.UserRelationshipGroupsWithVersion;
import im.turms.turms.service.user.UserVersionService;
import org.springframework.context.annotation.Lazy;
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
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;

import static im.turms.turms.common.Constants.*;

@Service
public class UserRelationshipGroupService {
    private final TurmsClusterManager turmsClusterManager;
    private final ReactiveMongoTemplate mongoTemplate;
    private final UserVersionService userVersionService;
    private final UserRelationshipService userRelationshipService;
    private static final UserRelationshipGroup EMPTY_RELATIONSHIP_GROUP = new UserRelationshipGroup();

    public UserRelationshipGroupService(ReactiveMongoTemplate mongoTemplate, TurmsClusterManager turmsClusterManager, UserVersionService userVersionService, @Lazy UserRelationshipService userRelationshipService) {
        this.mongoTemplate = mongoTemplate;
        this.turmsClusterManager = turmsClusterManager;
        this.userVersionService = userVersionService;
        this.userRelationshipService = userRelationshipService;
    }

    public Mono<UserRelationshipGroup> createRelationshipGroup(
            @NotNull Long ownerId,
            @NotNull String groupName) {
        UserRelationshipGroup group = new UserRelationshipGroup(
                ownerId,
                turmsClusterManager.generateRandomId().intValue(),
                groupName);
        return mongoTemplate.insert(group);
    }

    public Flux<Integer> queryRelationshipGroupsIndexes(@NotNull Long ownerId) {
        Query query = new Query()
                .addCriteria(Criteria.where(ID_OWNER_ID).is(ownerId));
        query.fields().include(ID_INDEX);
        return mongoTemplate.find(query, UserRelationshipGroup.class)
                .map(group -> group.getKey().getIndex());
    }

    public Flux<UserRelationshipGroup> queryRelationshipGroupsInfos(@NotNull Long ownerId) {
        Query query = new Query()
                .addCriteria(Criteria.where(ID_OWNER_ID).is(ownerId));
        return mongoTemplate.find(query, UserRelationshipGroup.class);
    }

    public Mono<Int64ValuesWithVersion> queryRelationshipGroupsIndexesWithVersion(
            @NotNull Long ownerId,
            @Nullable Date lastUpdatedDate) {
        return userVersionService.queryRelationshipGroupsLastUpdatedDate(ownerId)
                .defaultIfEmpty(MAX_DATE)
                .flatMap(date -> {
                    if (lastUpdatedDate == null || lastUpdatedDate.before(date)) {
                        Int64ValuesWithVersion.Builder builder = Int64ValuesWithVersion.newBuilder();
                        builder.setLastUpdatedDate(Int64Value.newBuilder().setValue(date.getTime()).build());
                        return queryRelationshipGroupsIndexes(ownerId)
                                .map(builder::addValues)
                                .then(Mono.just(builder.build()));
                    } else {
                        return Mono.error(TurmsBusinessException.get(TurmsStatusCode.ALREADY_UP_TO_DATE));
                    }
                });
    }

    public Mono<UserRelationshipGroupsWithVersion> queryRelationshipGroupsInfosWithVersion(
            @NotNull Long ownerId,
            @Nullable Date lastUpdatedDate) {
        return userVersionService.queryRelationshipGroupsLastUpdatedDate(ownerId)
                .defaultIfEmpty(MAX_DATE)
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
                });
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
                .addCriteria(Criteria.where(ID_INDEX).is(groupIndex));
        Update update = new Update().set(UserRelationshipGroup.Fields.name, newGroupName);
        return mongoTemplate.findAndModify(query, update, UserRelationshipGroup.class)
                .defaultIfEmpty(EMPTY_RELATIONSHIP_GROUP)
                .flatMap(group -> {
                    if (EMPTY_RELATIONSHIP_GROUP == group) {
                        return Mono.error(TurmsBusinessException.get(TurmsStatusCode.NOT_FOUND));
                    } else {
                        return userVersionService.updateRelationshipGroupsVersion(ownerId)
                                .thenReturn(true);
                    }
                });
    }

    public Mono<Boolean> addRelatedUserToRelationshipGroups(
            @NotNull Long ownerId,
            @NotEmpty Set<Integer> groupIndexes,
            @NotNull Long relatedUserId,
            @Nullable ReactiveMongoOperations operations) {
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
                        return Mono.zip(monos, objects -> objects)
                                .thenReturn(true);
                    } else {
                        return Mono.error(TurmsBusinessException.get(TurmsStatusCode.UNAUTHORIZED));
                    }
                });
    }

    public Mono<Boolean> deleteRelationshipGroupAndMoveMembers(
            @NotNull Long ownerId,
            @NotNull Integer deleteGroupIndex,
            @NotNull Integer existingUsersToTargetGroupIndex) {
        if (!deleteGroupIndex.equals(existingUsersToTargetGroupIndex)) {
            return mongoTemplate.inTransaction()
                    .execute(operations -> {
                        Query query = new Query()
                                .addCriteria(Criteria.where(ID_OWNER_ID).is(ownerId))
                                .addCriteria(Criteria.where(ID_GROUP_INDEX).is(deleteGroupIndex));
                        Update update = new Update().set(ID_GROUP_INDEX, existingUsersToTargetGroupIndex);
                        return operations.findAndModify(query, update, UserRelationshipGroupMember.class)
                                .zipWith(operations.remove(query, UserRelationshipGroup.class))
                                .zipWith(userVersionService.updateRelationshipGroupsVersion(ownerId))
                                .thenReturn(true);
                    })
                    .single();
        } else {
            return Mono.just(true);
        }
    }

    public Mono<Boolean> deleteRelatedUserFromAllRelationshipGroups(
            @NotNull Long ownerId,
            @NotNull Long relatedUserId,
            @Nullable ReactiveMongoOperations operations) {
        Query query = new Query()
                .addCriteria(Criteria.where(ID_OWNER_ID).is(ownerId))
                .addCriteria(Criteria.where(ID_RELATED_USER_ID).is(relatedUserId));
        ReactiveMongoOperations mongoOperations = operations != null ? operations : mongoTemplate;
        return mongoOperations.remove(query, UserRelationshipGroupMember.class)
                .zipWith(userVersionService.updateRelationshipGroupsVersion(ownerId))
                .map(results -> results.getT1().wasAcknowledged());
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
}
