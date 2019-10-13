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

package im.turms.turms.service.group;

import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;
import im.turms.turms.pojo.domain.GroupVersion;
import org.springframework.data.mongodb.core.ReactiveMongoOperations;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import javax.validation.constraints.NotNull;
import java.util.Date;

import static im.turms.turms.common.Constants.ID;
import static im.turms.turms.common.Constants.ID_GROUP_ID;

@Service
public class GroupVersionService {
    private final ReactiveMongoTemplate mongoTemplate;

    public GroupVersionService(ReactiveMongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    public Mono<Date> queryInfoVersion(@NotNull Long groupId) {
        Query query = new Query(Criteria.where(ID_GROUP_ID).is(groupId));
        query.fields().include(GroupVersion.Fields.info);
        return mongoTemplate.findOne(query, GroupVersion.class).map(GroupVersion::getInfo);
    }

    public Mono<Date> queryMembersVersion(@NotNull Long groupId) {
        Query query = new Query(Criteria.where(ID_GROUP_ID).is(groupId));
        query.fields().include(GroupVersion.Fields.members);
        return mongoTemplate.findOne(query, GroupVersion.class).map(GroupVersion::getMembers);
    }

    public Mono<Date> queryBlacklistVersion(@NotNull Long groupId) {
        Query query = new Query(Criteria.where(ID_GROUP_ID).is(groupId));
        query.fields().include(GroupVersion.Fields.blacklist);
        return mongoTemplate.findOne(query, GroupVersion.class).map(GroupVersion::getBlacklist);
    }

    public Mono<Date> queryGroupJoinRequestsVersion(@NotNull Long groupId) {
        Query query = new Query(Criteria.where(ID_GROUP_ID).is(groupId));
        query.fields().include(GroupVersion.Fields.joinRequests);
        return mongoTemplate.findOne(query, GroupVersion.class).map(GroupVersion::getJoinRequests);
    }

    public Mono<Date> queryGroupJoinQuestionsVersion(@NotNull Long groupId) {
        Query query = new Query(Criteria.where(ID_GROUP_ID).is(groupId));
        query.fields().include(GroupVersion.Fields.joinQuestions);
        return mongoTemplate.findOne(query, GroupVersion.class).map(GroupVersion::getJoinQuestions);
    }

    public Mono<Date> queryGroupInvitationsVersion(@NotNull Long groupId) {
        Query query = new Query(Criteria.where(ID_GROUP_ID).is(groupId));
        query.fields().include(GroupVersion.Fields.joinQuestions);
        return mongoTemplate.findOne(query, GroupVersion.class).map(GroupVersion::getInvitations);
    }

    public Mono<Boolean> updateVersion(
            @NotNull Long groupId,
            boolean updateInfo,
            boolean updateMembers,
            boolean updateBlacklist,
            boolean joinRequests,
            boolean joinQuestions) {
        Query query = new Query().addCriteria(Criteria.where(ID).is(groupId));
        Update update = new Update();
        Date now = new Date();
        if (updateInfo) update.set(GroupVersion.Fields.info, now);
        if (updateMembers) update.set(GroupVersion.Fields.members, now);
        if (updateBlacklist) update.set(GroupVersion.Fields.blacklist, now);
        if (joinRequests) update.set(GroupVersion.Fields.joinRequests, now);
        if (joinQuestions) update.set(GroupVersion.Fields.joinQuestions, now);
        return mongoTemplate.updateFirst(query, update, GroupVersion.class)
                .map(UpdateResult::wasAcknowledged);
    }

    public Mono<Boolean> updateInformation(@NotNull Long groupId) {
        return updateSpecificVersion(groupId, GroupVersion.Fields.info);
    }

    public Mono<Boolean> updateMembersVersion(@NotNull Long groupId) {
        return updateSpecificVersion(groupId, GroupVersion.Fields.members);
    }

    public Mono<Boolean> updateBlacklistVersion(@NotNull Long groupId) {
        return updateSpecificVersion(groupId, GroupVersion.Fields.blacklist);
    }

    public Mono<Boolean> updateJoinRequestsVersion(@NotNull Long groupId) {
        return updateSpecificVersion(groupId, GroupVersion.Fields.joinRequests);
    }

    public Mono<Boolean> updateJoinQuestionsVersion(@NotNull Long groupId) {
        return updateSpecificVersion(groupId, GroupVersion.Fields.joinQuestions);
    }

    public Mono<Boolean> updateGroupInvitationsVersion(@NotNull Long groupId) {
        return updateSpecificVersion(groupId, GroupVersion.Fields.invitations);
    }

    public Mono<Boolean> updateSpecificVersion(@NotNull Long groupId, @NotNull String field) {
        Query query = new Query().addCriteria(Criteria.where(ID).is(groupId));
        Update update = new Update().set(field, new Date());
        return mongoTemplate.updateFirst(query, update, GroupVersion.class)
                .map(UpdateResult::wasAcknowledged);
    }

    public Mono<Boolean> upsert(@NotNull Long groupId) {
        GroupVersion version = GroupVersion.builder().groupId(groupId).build();
        return mongoTemplate.insert(version).thenReturn(true);
    }

    public Mono<Boolean> delete(@NotNull Long groupId, ReactiveMongoOperations operations) {
        Query query = new Query().addCriteria(Criteria.where(ID).is(groupId));
        ReactiveMongoOperations mongoOperations = operations != null ? operations : mongoTemplate;
        return mongoOperations.remove(query, GroupVersion.class)
                .map(DeleteResult::wasAcknowledged);
    }
}
