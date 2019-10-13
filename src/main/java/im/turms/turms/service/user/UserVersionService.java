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

package im.turms.turms.service.user;

import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;
import im.turms.turms.pojo.domain.UserVersion;
import org.springframework.data.mongodb.core.ReactiveMongoOperations;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import javax.annotation.Nullable;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.Date;
import java.util.Set;

import static im.turms.turms.common.Constants.ID;

@Service
public class UserVersionService {
    private final ReactiveMongoTemplate mongoTemplate;

    public UserVersionService(ReactiveMongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    public Mono<Date> queryInformationLastUpdatedDate(@NotNull Long userId) {
        Query query = new Query().addCriteria(Criteria.where(ID).is(userId));
        query.fields().include(UserVersion.Fields.info);
        return mongoTemplate.findOne(query, UserVersion.class)
                .map(UserVersion::getInfo);
    }

    public Mono<Date> queryRelationshipsLastUpdatedDate(@NotNull Long userId) {
        Query query = new Query().addCriteria(Criteria.where(ID).is(userId));
        query.fields().include(UserVersion.Fields.relationships);
        return mongoTemplate.findOne(query, UserVersion.class)
                .map(UserVersion::getRelationships);
    }

    public Mono<Date> queryGroupInvitationsLastUpdatedDate(@NotNull Long userId) {
        Query query = new Query().addCriteria(Criteria.where(ID).is(userId));
        query.fields().include(UserVersion.Fields.groupInvitations);
        return mongoTemplate.findOne(query, UserVersion.class)
                .map(UserVersion::getGroupInvitations);
    }

    public Mono<Date> queryRelationshipGroupsLastUpdatedDate(@NotNull Long userId) {
        Query query = new Query().addCriteria(Criteria.where(ID).is(userId));
        query.fields().include(UserVersion.Fields.relationshipGroups);
        return mongoTemplate.findOne(query, UserVersion.class)
                .map(UserVersion::getRelationshipGroups);
    }

    public Mono<Date> queryJoinedGroupVersion(@NotNull Long userId) {
        Query query = new Query().addCriteria(Criteria.where(ID).is(userId));
        query.fields().include(UserVersion.Fields.joinedGroups);
        return mongoTemplate.findOne(query, UserVersion.class)
                .map(UserVersion::getJoinedGroups);
    }

    public Mono<Date> queryFriendRequestsVersion(@NotNull Long userId) {
        Query query = new Query().addCriteria(Criteria.where(ID).is(userId));
        query.fields().include(UserVersion.Fields.friendRequests);
        return mongoTemplate.findOne(query, UserVersion.class)
                .map(UserVersion::getFriendRequests);
    }

    public Mono<UserVersion> upsertEmptyUserVersion(
            @NotNull Long userId,
            @Nullable ReactiveMongoOperations operations) {
        UserVersion userVersion = new UserVersion();
        userVersion.setUserId(userId);
        ReactiveMongoOperations mongoOperations = operations != null ? operations : mongoTemplate;
        return mongoOperations.save(userVersion);
    }

    public Mono<Boolean> updateInformationVersion(@NotNull Long userId) {
        return updateSpecificVersion(userId, UserVersion.Fields.info);
    }

    public Mono<Boolean> updateRelationshipsVersion(@NotNull Long userId) {
        return updateSpecificVersion(userId, UserVersion.Fields.relationships);
    }

    public Mono<Boolean> updateFriendRequestsVersion(@NotNull Long userId) {
        return updateSpecificVersion(userId, UserVersion.Fields.friendRequests);
    }

    public Mono<Boolean> updateRelationshipGroupsVersion(@NotNull Long userId) {
        return updateSpecificVersion(userId, UserVersion.Fields.relationshipGroups);
    }

    public Mono<Boolean> updateGroupInvitationsVersion(@NotNull Long userId) {
        return updateSpecificVersion(userId, UserVersion.Fields.groupInvitations);
    }

    public Mono<Boolean> updateJoinedGroupsVersion(@NotNull Long userId) {
        return updateSpecificVersion(userId, UserVersion.Fields.joinedGroups);
    }

    public Mono<Boolean> updateSpecificVersion(@NotNull Long userId, @NotNull String field) {
        Query query = new Query().addCriteria(Criteria.where(ID).is(userId));
        Update update = new Update().set(field, new Date());
        return mongoTemplate.updateFirst(query, update, UserVersion.class)
                .map(UpdateResult::wasAcknowledged);
    }

    public Mono<Boolean> delete(
            @NotEmpty Set<Long> userIds,
            @Nullable ReactiveMongoOperations operations) {
        Query query = new Query().addCriteria(Criteria.where(ID).in(userIds));
        ReactiveMongoOperations mongoOperations = operations != null ? operations : mongoTemplate;
        return mongoOperations.remove(query).map(DeleteResult::wasAcknowledged);
    }
}
