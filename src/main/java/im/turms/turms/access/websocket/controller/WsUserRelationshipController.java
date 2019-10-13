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

package im.turms.turms.access.websocket.controller;

import im.turms.turms.annotation.websocket.TurmsRequestMapping;
import im.turms.turms.cluster.TurmsClusterManager;
import im.turms.turms.constant.ResponseAction;
import im.turms.turms.pojo.bo.RequestResult;
import im.turms.turms.pojo.dto.TurmsRequestWrapper;
import im.turms.turms.pojo.request.*;
import im.turms.turms.pojo.response.TurmsResponse;
import im.turms.turms.service.user.relationship.UserFriendRequestService;
import im.turms.turms.service.user.relationship.UserRelationshipGroupService;
import im.turms.turms.service.user.relationship.UserRelationshipService;
import org.springframework.stereotype.Controller;
import reactor.core.publisher.Mono;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static im.turms.turms.common.Constants.DEFAULT_RELATIONSHIP_GROUP_INDEX;

@Controller
public class WsUserRelationshipController {
    private final UserRelationshipService userRelationshipService;
    private final UserRelationshipGroupService userRelationshipGroupService;
    private final UserFriendRequestService userFriendRequestService;
    private final TurmsClusterManager turmsClusterManager;

    public WsUserRelationshipController(UserRelationshipService userRelationshipService, UserRelationshipGroupService userRelationshipGroupService, UserFriendRequestService userFriendRequestService, TurmsClusterManager turmsClusterManager) {
        this.userRelationshipService = userRelationshipService;
        this.userRelationshipGroupService = userRelationshipGroupService;
        this.userFriendRequestService = userFriendRequestService;
        this.turmsClusterManager = turmsClusterManager;
    }

    @TurmsRequestMapping(TurmsRequest.KindCase.CREATE_FRIEND_REQUEST_REQUEST)
    public Function<TurmsRequestWrapper, Mono<RequestResult>> handleCreateFriendRequestRequest() {
        return turmsRequestWrapper -> {
            CreateFriendRequestRequest request = turmsRequestWrapper.getTurmsRequest().getCreateFriendRequestRequest();
            return userFriendRequestService.authAndCreateFriendRequest(
                    turmsRequestWrapper.getUserId(),
                    request.getRecipientId(),
                    request.getContent(),
                    new Date())
                    .map(friendRequest -> {
                        if (turmsClusterManager.getTurmsProperties()
                                .getNotification().isNotifyRecipientAfterReceivedFriendRequest()) {
                            return RequestResult.responseIdAndRecipientData(
                                    friendRequest.getId(),
                                    request.getRecipientId(),
                                    turmsRequestWrapper.getTurmsRequest());
                        }
                        return RequestResult.ok();
                    });
        };
    }

    @TurmsRequestMapping(TurmsRequest.KindCase.CREATE_RELATIONSHIP_GROUP_REQUEST)
    public Function<TurmsRequestWrapper, Mono<RequestResult>> handleCreateRelationshipGroupRequest() {
        return turmsRequestWrapper -> {
            CreateRelationshipGroupRequest request = turmsRequestWrapper.getTurmsRequest().getCreateRelationshipGroupRequest();
            return userRelationshipGroupService.createRelationshipGroup(
                    turmsRequestWrapper.getUserId(),
                    request.getName())
                    .map(group -> RequestResult.responseId(group.getKey().getIndex().longValue()));
        };
    }

    @TurmsRequestMapping(TurmsRequest.KindCase.CREATE_RELATIONSHIP_REQUEST)
    public Function<TurmsRequestWrapper, Mono<RequestResult>> handleCreateRelationshipRequest() {
        return turmsRequestWrapper -> {
            CreateRelationshipRequest request = turmsRequestWrapper.getTurmsRequest().getCreateRelationshipRequest();
            // It is unnecessary to check whether requester is in the blacklist of the target user
            // Because only create a one-sided relationship here
            int groupIndex = request.hasGroupIndex() ?
                    request.getGroupIndex().getValue() : DEFAULT_RELATIONSHIP_GROUP_INDEX;
            return userRelationshipService.upsertOneSidedRelationship(
                    turmsRequestWrapper.getUserId(),
                    request.getUserId(),
                    request.getIsBlocked(),
                    groupIndex,
                    null,
                    new Date(),
                    false,
                    null)
                    .map(upserted -> {
                        if (upserted != null && upserted
                                && turmsClusterManager.getTurmsProperties().getNotification()
                                .isNotifyRelatedUserAfterAddedToOneSidedRelationshipGroupByOthers()) {
                            return RequestResult.recipientData(
                                    request.getUserId(),
                                    turmsRequestWrapper.getTurmsRequest());
                        }
                        return RequestResult.okIfTrue(upserted);
                    });
        };
    }

    @TurmsRequestMapping(TurmsRequest.KindCase.DELETE_RELATIONSHIP_GROUP_REQUEST)
    public Function<TurmsRequestWrapper, Mono<RequestResult>> handleDeleteRelationshipGroupRequest() {
        return turmsRequestWrapper -> {
            DeleteRelationshipGroupRequest request = turmsRequestWrapper.getTurmsRequest().getDeleteRelationshipGroupRequest();
            Integer groupIndex = request.getGroupIndex();
            int targetGroupIndex = request.hasTargetGroupIndex() ?
                    request.getTargetGroupIndex().getValue() : DEFAULT_RELATIONSHIP_GROUP_INDEX;
            if (turmsClusterManager.getTurmsProperties().getNotification()
                    .isNotifyRelatedUserAfterOneSidedRelationshipGroupUpdatedByOthers()) {
                return userRelationshipGroupService.queryRelatedUsersIdsInRelationshipGroup(
                        turmsRequestWrapper.getUserId(),
                        groupIndex)
                        .collect(Collectors.toSet())
                        .flatMap(ids -> userRelationshipGroupService.deleteRelationshipGroupAndMoveMembers(
                                turmsRequestWrapper.getUserId(),
                                groupIndex,
                                targetGroupIndex)
                                .map(deleted -> {
                                    if (!ids.isEmpty()) {
                                        return RequestResult.recipientData(
                                                ids, turmsRequestWrapper.getTurmsRequest());
                                    }
                                    return RequestResult.okIfTrue(deleted);
                                }));
            }
            return userRelationshipGroupService.deleteRelationshipGroupAndMoveMembers(
                    turmsRequestWrapper.getUserId(),
                    groupIndex,
                    targetGroupIndex)
                    .map(RequestResult::okIfTrue);
        };
    }

    @TurmsRequestMapping(TurmsRequest.KindCase.DELETE_RELATIONSHIP_REQUEST)
    public Function<TurmsRequestWrapper, Mono<RequestResult>> handleDeleteRelationshipRequest() {
        return turmsRequestWrapper -> {
            DeleteRelationshipRequest request = turmsRequestWrapper.getTurmsRequest().getDeleteRelationshipRequest();
            boolean deleteTwoSidedRelationships = turmsClusterManager.getTurmsProperties().getUser().isDeleteTwoSidedRelationships();
            Mono<Boolean> deleteMono;
            if (deleteTwoSidedRelationships) {
                deleteMono = userRelationshipService.deleteTwoSidedRelationships(
                        turmsRequestWrapper.getUserId(),
                        request.getRelatedUserId());
            } else {
                deleteMono = userRelationshipService.deleteOneSidedRelationship(
                        turmsRequestWrapper.getUserId(),
                        request.getRelatedUserId(),
                        null);
            }
            return deleteMono.map(deleted -> {
                if (deleted != null && deleted
                        && turmsClusterManager.getTurmsProperties().getNotification()
                        .isNotifyRelatedUserAfterRemoveFromRelationshipGroupByOthers()) {
                    return RequestResult.recipientData(
                            request.getRelatedUserId(),
                            turmsRequestWrapper.getTurmsRequest());
                }
                return RequestResult.okIfTrue(deleted);
            });
        };
    }

    @TurmsRequestMapping(TurmsRequest.KindCase.QUERY_FRIEND_REQUESTS_REQUEST)
    public Function<TurmsRequestWrapper, Mono<RequestResult>> handleQueryFriendRequestsRequest() {
        return turmsRequestWrapper -> {
            QueryFriendRequestsRequest request = turmsRequestWrapper.getTurmsRequest().getQueryFriendRequestsRequest();
            Date lastUpdatedDate = request.hasLastUpdatedDate() ? new Date(request.getLastUpdatedDate().getValue()) : null;
            return userFriendRequestService.queryFriendRequestsWithVersion(
                    turmsRequestWrapper.getUserId(),
                    lastUpdatedDate)
                    .map(friendRequestsWithVersion -> RequestResult
                            .responseData(TurmsResponse.Data
                                    .newBuilder()
                                    .setUserFriendRequestsWithVersion(friendRequestsWithVersion)
                                    .build()));
        };
    }

    @TurmsRequestMapping(TurmsRequest.KindCase.QUERY_RELATED_USERS_IDS_REQUEST)
    public Function<TurmsRequestWrapper, Mono<RequestResult>> handleQueryRelatedUsersIdsRequest() {
        return turmsRequestWrapper -> {
            QueryRelatedUsersIdsRequest request = turmsRequestWrapper.getTurmsRequest().getQueryRelatedUsersIdsRequest();
            int groupIndex = request.hasGroupIndex() ? request.getGroupIndex().getValue() : DEFAULT_RELATIONSHIP_GROUP_INDEX;
            Date lastUpdatedDate = request.hasLastUpdatedDate() ? new Date(request.getLastUpdatedDate().getValue()) : null;
            Boolean isBlocked = request.hasIsBlocked() ? request.getIsBlocked().getValue() : null;
            return userRelationshipService.queryRelatedUsersIdsWithVersion(
                    turmsRequestWrapper.getUserId(),
                    groupIndex,
                    isBlocked,
                    lastUpdatedDate)
                    .map(idsWithVersion -> RequestResult
                            .responseData(TurmsResponse.Data
                                    .newBuilder()
                                    .setIdsWithVersion(idsWithVersion)
                                    .build()));
        };
    }

    @TurmsRequestMapping(TurmsRequest.KindCase.QUERY_RELATIONSHIP_GROUPS_REQUEST)
    public Function<TurmsRequestWrapper, Mono<RequestResult>> handleQueryRelationshipGroupsRequest() {
        return turmsRequestWrapper -> {
            QueryRelationshipGroupsRequest request = turmsRequestWrapper.getTurmsRequest()
                    .getQueryRelationshipGroupsRequest();
            Date lastUpdatedDate = request.hasLastUpdatedDate() ?
                    new Date(request.getLastUpdatedDate().getValue()) : null;
            return userRelationshipGroupService.queryRelationshipGroupsInfosWithVersion(
                    turmsRequestWrapper.getUserId(),
                    lastUpdatedDate)
                    .map(groupsWithVersion -> RequestResult
                            .responseData(TurmsResponse.Data
                                    .newBuilder()
                                    .setUserRelationshipGroupsWithVersion(groupsWithVersion)
                                    .build()));
        };
    }

    @TurmsRequestMapping(TurmsRequest.KindCase.QUERY_RELATIONSHIPS_REQUEST)
    public Function<TurmsRequestWrapper, Mono<RequestResult>> handleQueryRelationshipsRequest() {
        return turmsRequestWrapper -> {
            QueryRelationshipsRequest request = turmsRequestWrapper.getTurmsRequest()
                    .getQueryRelationshipsRequest();
            Set<Long> ids = request.getRelatedUsersIdsCount() != 0 ?
                    new HashSet<>(request.getRelatedUsersIdsList()) : null;
            int groupIndex = request.hasGroupIndex() ?
                    request.getGroupIndex().getValue() : DEFAULT_RELATIONSHIP_GROUP_INDEX;
            Boolean isBlocked = request.hasIsBlocked() ? request.getIsBlocked().getValue() : null;
            Date lastUpdatedDate = request.hasLastUpdatedDate() ?
                    new Date(request.getLastUpdatedDate().getValue()) : null;
            return userRelationshipService.queryRelationshipsWithVersion(
                    turmsRequestWrapper.getUserId(),
                    ids,
                    groupIndex,
                    isBlocked,
                    lastUpdatedDate)
                    .map(relationshipsWithVersion -> RequestResult
                            .responseData(TurmsResponse.Data
                                    .newBuilder()
                                    .setUserRelationshipsWithVersion(relationshipsWithVersion)
                                    .build()));
        };
    }

    @TurmsRequestMapping(TurmsRequest.KindCase.UPDATE_FRIEND_REQUEST_REQUEST)
    public Function<TurmsRequestWrapper, Mono<RequestResult>> handleUpdateFriendRequestRequest() {
        return turmsRequestWrapper -> {
            UpdateFriendRequestRequest request = turmsRequestWrapper.getTurmsRequest().getUpdateFriendRequestRequest();
            ResponseAction action = request.getResponseAction();
            String reason = request.hasReason() ? request.getReason().getValue() : null;
            return userFriendRequestService.handleFriendRequest(
                    request.getRequestId(),
                    turmsRequestWrapper.getUserId(),
                    action,
                    reason)
                    .map(handled -> {
                        if (handled != null && handled
                                && turmsClusterManager.getTurmsProperties().getNotification()
                                .isNotifyRequesterAfterFriendRequestUpdated()) {
                            return RequestResult.recipientData(
                                    request.getRequestId(),
                                    turmsRequestWrapper.getTurmsRequest());
                        }
                        return RequestResult.okIfTrue(handled);
                    });
        };
    }

    @TurmsRequestMapping(TurmsRequest.KindCase.UPDATE_RELATIONSHIP_GROUP_REQUEST)
    public Function<TurmsRequestWrapper, Mono<RequestResult>> handleUpdateRelationshipGroupRequest() {
        return turmsRequestWrapper -> {
            UpdateRelationshipGroupRequest request = turmsRequestWrapper.getTurmsRequest().getUpdateRelationshipGroupRequest();
            return userRelationshipGroupService.updateRelationshipGroupName(
                    turmsRequestWrapper.getUserId(),
                    request.getGroupIndex(),
                    request.getNewName())
                    .map(RequestResult::okIfTrue);
        };
    }

    @TurmsRequestMapping(TurmsRequest.KindCase.UPDATE_RELATIONSHIP_REQUEST)
    public Function<TurmsRequestWrapper, Mono<RequestResult>> handleUpdateRelationshipRequest() {
        return turmsRequestWrapper -> {
            UpdateRelationshipRequest request = turmsRequestWrapper.getTurmsRequest().getUpdateRelationshipRequest();
            Boolean isBlocked = request.hasBlocked() ? request.getBlocked().getValue() : null;
            Integer newGroupIndex = request.hasNewGroupIndex() ? request.getNewGroupIndex().getValue() : null;
            Integer deleteGroupIndex = request.hasDeleteGroupIndex() ? request.getDeleteGroupIndex().getValue() : null;
            return userRelationshipService.upsertOneSidedRelationship(
                    turmsRequestWrapper.getUserId(),
                    request.getRelatedUserId(),
                    isBlocked,
                    newGroupIndex,
                    deleteGroupIndex,
                    null,
                    true,
                    null)
                    .map(upserted -> {
                        if (upserted != null && upserted
                                && turmsClusterManager.getTurmsProperties().getNotification()
                                .isNotifyRelatedUserAfterOneSidedRelationshipUpdatedByOthers()) {
                            return RequestResult.recipientData(
                                    request.getRelatedUserId(),
                                    turmsRequestWrapper.getTurmsRequest());
                        }
                        return RequestResult.okIfTrue(upserted);
                    });
        };
    }
}
