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

package im.turms.turms.workflow.service.impl.log;

import com.google.common.collect.Sets;
import im.turms.common.constant.DeviceType;
import im.turms.common.model.dto.request.TurmsRequest;
import im.turms.server.common.cluster.node.Node;
import im.turms.server.common.log4j.UserActivityLogging;
import im.turms.server.common.property.constant.ActivityLoggingCategoryName;
import im.turms.server.common.property.env.service.business.activity.ActivityLoggingProperties;
import im.turms.server.common.property.env.service.business.activity.property.ActivityLoggingCategoryProperties;
import im.turms.server.common.property.env.service.business.activity.property.ActivityLoggingRequestProperties;
import im.turms.turms.bo.UserActionLog;
import im.turms.turms.plugin.extension.handler.UserActionLogHandler;
import im.turms.turms.plugin.manager.TurmsPluginManager;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import javax.validation.constraints.NotNull;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

import static im.turms.common.model.dto.request.TurmsRequest.KindCase.*;

/**
 * @author James Chen
 */
@Service
public class UserActionLogService {

    private final Node node;
    private final TurmsPluginManager turmsPluginManager;
    private final Map<TurmsRequest.KindCase, ActivityLoggingRequestProperties> includedRequests;

    public UserActionLogService(
            Node node,
            TurmsPluginManager turmsPluginManager) {
        this.node = node;
        this.turmsPluginManager = turmsPluginManager;
        ActivityLoggingProperties loggingProperties = node.getSharedProperties().getService().getActivityLogging();
        includedRequests = getIncludedRequests(loggingProperties);
    }

    public void tryLogAndTriggerLogHandlers(@NotNull Long userId, @NotNull DeviceType deviceType, @NotNull TurmsRequest request) {
        boolean logUserAction = node.getSharedProperties().getService().getLog().isLogUserAction();
        List<UserActionLogHandler> handlerList = turmsPluginManager.getUserActionLogHandlerList();
        boolean triggerHandlers = turmsPluginManager.isEnabled() && !handlerList.isEmpty();
        if (logUserAction || triggerHandlers) {
            UserActionLog userActionLog;
            // Note that we use toString() instead of JSON for better performance
            String actionDetails = node.getSharedProperties().getService().getLog().isLogUserActionDetails()
                    ? request.toString()
                    : null;
            userActionLog = new UserActionLog(userId, deviceType, new Date(), request.getKindCase().name(), actionDetails);
            if (logUserAction && shouldLogClientRequest(request.getKindCase())) {
                UserActivityLogging.log(userActionLog);
            }
            if (triggerHandlers) {
                List<Mono<Void>> monos = new ArrayList<>(handlerList.size());
                for (UserActionLogHandler handler : handlerList) {
                    monos.add(handler.handleUserActionLog(userActionLog));
                }
                Mono.when(monos).subscribe();
            }
        }
    }

    private boolean shouldLogClientRequest(TurmsRequest.KindCase requestType) {
        ActivityLoggingRequestProperties loggingRequest = includedRequests.get(requestType);
        if (loggingRequest == null) {
            return false;
        }
        float sampleRate = loggingRequest.getSampleRate();
        if (sampleRate > 0) {
            if (sampleRate < 1.0f) {
                return ThreadLocalRandom.current().nextFloat() < sampleRate;
            } else {
                return true;
            }
        }
        return false;
    }

    private Map<TurmsRequest.KindCase, ActivityLoggingRequestProperties> getIncludedRequests(ActivityLoggingProperties properties) {
        Map<TurmsRequest.KindCase, ActivityLoggingRequestProperties> result = new EnumMap<>(TurmsRequest.KindCase.class);

        // 1. handle included categories
        Set<ActivityLoggingCategoryProperties> includedCategories = properties.getIncludedCategories();
        for (ActivityLoggingCategoryProperties includedCategory : includedCategories) {
            Set<ActivityLoggingRequestProperties> loggingRequests = getRequestsFromCategory(includedCategory);
            for (ActivityLoggingRequestProperties loggingRequest : loggingRequests) {
                result.put(loggingRequest.getName(), loggingRequest);
            }
        }

        // 2. handle included requests
        Set<ActivityLoggingRequestProperties> loggingRequests = properties.getIncludedRequests();
        for (ActivityLoggingRequestProperties loggingRequest : loggingRequests) {
            result.put(loggingRequest.getName(), loggingRequest);
        }

        // 3. handle excluded category names
        Set<ActivityLoggingCategoryName> excludedCategoryNames = properties.getExcludedCategoryNames();
        for (ActivityLoggingCategoryName excludedCategoryName : excludedCategoryNames) {
            Set<TurmsRequest.KindCase> excludedRequestNames = getRequestsFromCategoryName(excludedCategoryName);
            for (TurmsRequest.KindCase excludedRequestName : excludedRequestNames) {
                result.remove(excludedRequestName);
            }
        }

        // 4. handle excluded request names
        Set<TurmsRequest.KindCase> excludedRequestNames = properties.getExcludedRequestNames();
        for (TurmsRequest.KindCase excludedRequestName : excludedRequestNames) {
            result.remove(excludedRequestName);
        }

        return result;
    }

    private Set<ActivityLoggingRequestProperties> getRequestsFromCategory(ActivityLoggingCategoryProperties category) {
        ActivityLoggingCategoryName categoryName = category.getName();
        Set<TurmsRequest.KindCase> requests = getRequestsFromCategoryName(categoryName);
        if (requests.isEmpty()) {
            return Collections.emptySet();
        } else {
            Set<ActivityLoggingRequestProperties> loggingRequests = Sets.newHashSetWithExpectedSize(requests.size());
            for (TurmsRequest.KindCase request : requests) {
                loggingRequests.add(new ActivityLoggingRequestProperties(request, category.getSampleRate()));
            }
            return loggingRequests;
        }
    }

    private Set<TurmsRequest.KindCase> getRequestsFromCategoryName(ActivityLoggingCategoryName name) {
        switch (name) {
            case ALL:
                Set<TurmsRequest.KindCase> createRequests = getRequestsFromCategoryName(ActivityLoggingCategoryName.CREATE);
                Set<TurmsRequest.KindCase> deleteRequests = getRequestsFromCategoryName(ActivityLoggingCategoryName.DELETE);
                Set<TurmsRequest.KindCase> updateRequests = getRequestsFromCategoryName(ActivityLoggingCategoryName.UPDATE);
                Set<TurmsRequest.KindCase> queryRequests = getRequestsFromCategoryName(ActivityLoggingCategoryName.QUERY);
                Set<TurmsRequest.KindCase> result = Sets.newHashSetWithExpectedSize(
                        createRequests.size()
                                + deleteRequests.size()
                                + updateRequests.size()
                                + queryRequests.size());
                result.addAll(createRequests);
                result.addAll(deleteRequests);
                result.addAll(updateRequests);
                result.addAll(queryRequests);
                return result;
            case CREATE:
                return Set.of(
                        CREATE_MESSAGE_REQUEST,
                        CREATE_FRIEND_REQUEST_REQUEST,
                        CREATE_RELATIONSHIP_GROUP_REQUEST,
                        CREATE_RELATIONSHIP_REQUEST,
                        CREATE_GROUP_REQUEST,
                        CREATE_GROUP_BLACKLISTED_USER_REQUEST,
                        CREATE_GROUP_INVITATION_REQUEST,
                        CREATE_GROUP_JOIN_REQUEST_REQUEST,
                        CREATE_GROUP_JOIN_QUESTION_REQUEST,
                        CREATE_GROUP_MEMBER_REQUEST);
            case DELETE:
                return Set.of(
                        DELETE_RESOURCE_REQUEST,
                        DELETE_RELATIONSHIP_GROUP_REQUEST,
                        DELETE_RELATIONSHIP_REQUEST,
                        DELETE_GROUP_REQUEST,
                        DELETE_GROUP_BLACKLISTED_USER_REQUEST,
                        DELETE_GROUP_INVITATION_REQUEST,
                        DELETE_GROUP_JOIN_REQUEST_REQUEST,
                        DELETE_GROUP_JOIN_QUESTION_REQUEST,
                        DELETE_GROUP_MEMBER_REQUEST);
            case UPDATE:
                return Set.of(
                        ACK_REQUEST,
                        UPDATE_MESSAGE_REQUEST,
                        UPDATE_TYPING_STATUS_REQUEST,
                        UPDATE_USER_LOCATION_REQUEST,
                        UPDATE_USER_ONLINE_STATUS_REQUEST,
                        UPDATE_USER_REQUEST,
                        UPDATE_FRIEND_REQUEST_REQUEST,
                        UPDATE_RELATIONSHIP_GROUP_REQUEST,
                        UPDATE_RELATIONSHIP_REQUEST,
                        UPDATE_GROUP_REQUEST,
                        UPDATE_GROUP_JOIN_QUESTION_REQUEST,
                        UPDATE_GROUP_MEMBER_REQUEST);
            case QUERY:
                return Set.of(
                        QUERY_SIGNED_GET_URL_REQUEST,
                        QUERY_SIGNED_PUT_URL_REQUEST,
                        QUERY_MESSAGE_STATUSES_REQUEST,
                        QUERY_MESSAGES_REQUEST,
                        QUERY_PENDING_MESSAGES_WITH_TOTAL_REQUEST,
                        QUERY_USER_PROFILE_REQUEST,
                        QUERY_USER_IDS_NEARBY_REQUEST,
                        QUERY_USER_INFOS_NEARBY_REQUEST,
                        QUERY_USER_ONLINE_STATUSES_REQUEST,
                        QUERY_FRIEND_REQUESTS_REQUEST,
                        QUERY_RELATED_USER_IDS_REQUEST,
                        QUERY_RELATIONSHIP_GROUPS_REQUEST,
                        QUERY_RELATIONSHIPS_REQUEST,
                        QUERY_GROUP_REQUEST,
                        QUERY_JOINED_GROUP_IDS_REQUEST,
                        QUERY_JOINED_GROUP_INFOS_REQUEST,
                        QUERY_GROUP_BLACKLISTED_USER_IDS_REQUEST,
                        QUERY_GROUP_BLACKLISTED_USER_INFOS_REQUEST,
                        QUERY_GROUP_INVITATIONS_REQUEST,
                        QUERY_GROUP_JOIN_REQUESTS_REQUEST,
                        QUERY_GROUP_JOIN_QUESTIONS_REQUEST,
                        QUERY_GROUP_MEMBERS_REQUEST);
            case STORAGE:
                return Set.of(DELETE_RESOURCE_REQUEST,
                        QUERY_SIGNED_GET_URL_REQUEST,
                        QUERY_SIGNED_PUT_URL_REQUEST);
            case MESSAGE:
                return Set.of(CREATE_MESSAGE_REQUEST,
                        QUERY_MESSAGE_STATUSES_REQUEST,
                        QUERY_MESSAGES_REQUEST,
                        QUERY_PENDING_MESSAGES_WITH_TOTAL_REQUEST,
                        UPDATE_MESSAGE_REQUEST,
                        UPDATE_TYPING_STATUS_REQUEST);
            case USER:
                return Set.of(QUERY_USER_PROFILE_REQUEST,
                        QUERY_USER_IDS_NEARBY_REQUEST,
                        QUERY_USER_INFOS_NEARBY_REQUEST,
                        QUERY_USER_ONLINE_STATUSES_REQUEST,
                        UPDATE_USER_LOCATION_REQUEST,
                        UPDATE_USER_ONLINE_STATUS_REQUEST,
                        UPDATE_USER_REQUEST);
            case USER_RELATIONSHIP:
                return Set.of(CREATE_FRIEND_REQUEST_REQUEST,
                        CREATE_RELATIONSHIP_GROUP_REQUEST,
                        CREATE_RELATIONSHIP_REQUEST,
                        DELETE_RELATIONSHIP_GROUP_REQUEST,
                        DELETE_RELATIONSHIP_REQUEST,
                        QUERY_FRIEND_REQUESTS_REQUEST,
                        QUERY_RELATED_USER_IDS_REQUEST,
                        QUERY_RELATIONSHIP_GROUPS_REQUEST,
                        QUERY_RELATIONSHIPS_REQUEST,
                        UPDATE_FRIEND_REQUEST_REQUEST,
                        UPDATE_RELATIONSHIP_GROUP_REQUEST,
                        UPDATE_RELATIONSHIP_REQUEST);
            case GROUP:
                return Set.of(CREATE_GROUP_REQUEST,
                        DELETE_GROUP_REQUEST,
                        QUERY_GROUP_REQUEST,
                        QUERY_JOINED_GROUP_IDS_REQUEST,
                        QUERY_JOINED_GROUP_INFOS_REQUEST,
                        UPDATE_GROUP_REQUEST);
            case GROUP_BLACKLIST:
                return Set.of(CREATE_GROUP_BLACKLISTED_USER_REQUEST,
                        DELETE_GROUP_BLACKLISTED_USER_REQUEST,
                        QUERY_GROUP_BLACKLISTED_USER_IDS_REQUEST,
                        QUERY_GROUP_BLACKLISTED_USER_INFOS_REQUEST);
            case GROUP_ENROLLMENT:
                return Set.of(CHECK_GROUP_JOIN_QUESTIONS_ANSWERS_REQUEST,
                        CREATE_GROUP_INVITATION_REQUEST,
                        CREATE_GROUP_JOIN_REQUEST_REQUEST,
                        CREATE_GROUP_JOIN_QUESTION_REQUEST,
                        DELETE_GROUP_INVITATION_REQUEST,
                        DELETE_GROUP_JOIN_REQUEST_REQUEST,
                        DELETE_GROUP_JOIN_QUESTION_REQUEST,
                        QUERY_GROUP_INVITATIONS_REQUEST,
                        QUERY_GROUP_JOIN_REQUESTS_REQUEST,
                        QUERY_GROUP_JOIN_QUESTIONS_REQUEST,
                        UPDATE_GROUP_JOIN_QUESTION_REQUEST);
            case GROUP_MEMBER:
                return Set.of(CREATE_GROUP_MEMBER_REQUEST,
                        DELETE_GROUP_MEMBER_REQUEST,
                        QUERY_GROUP_MEMBERS_REQUEST,
                        UPDATE_GROUP_MEMBER_REQUEST);
            case NONE:
                return Collections.emptySet();
            default:
                throw new IllegalStateException("Unexpected value: " + name);
        }
    }
}