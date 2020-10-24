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
import im.turms.common.model.dto.request.TurmsRequest;
import im.turms.server.common.bo.property.ActivityLoggingCategory;
import im.turms.server.common.bo.property.ActivityLoggingRequest;
import im.turms.server.common.cluster.node.Node;
import im.turms.server.common.property.constant.ActivityLoggingCategoryName;
import im.turms.server.common.property.env.service.business.activity.ActivityLoggingProperties;
import im.turms.turms.workflow.access.servicerequest.dto.ClientRequest;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

import javax.validation.constraints.NotNull;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

import static im.turms.common.model.dto.request.TurmsRequest.KindCase.*;

/**
 * @author James Chen
 */
@Service
@Log4j2
public class ActivityLogService {

    private final Map<TurmsRequest.KindCase, ActivityLoggingRequest> includedRequests;

    public ActivityLogService(Node node) {
        ActivityLoggingProperties loggingProperties = node.getSharedProperties().getService().getActivityLogging();
        includedRequests = applyProperties(loggingProperties);
    }

    public void tryLogClientRequest(@NotNull ClientRequest clientRequest) {
        TurmsRequest.KindCase requestName = clientRequest.getTurmsRequest().getKindCase();
        ActivityLoggingRequest loggingRequest = includedRequests.get(requestName);
        if (loggingRequest != null) {
            float sampleRate = loggingRequest.getSampleRate();
            if (sampleRate > 0) {
                if (sampleRate < 1.0f) {
                    boolean shouldLog = ThreadLocalRandom.current().nextFloat() < sampleRate;
                    if (shouldLog) {
                        log.info(clientRequest);
                    }
                } else {
                    log.info(clientRequest);
                }
            }
        }
    }

    private Map<TurmsRequest.KindCase, ActivityLoggingRequest> applyProperties(ActivityLoggingProperties properties) {
        Map<TurmsRequest.KindCase, ActivityLoggingRequest> result = new EnumMap<>(TurmsRequest.KindCase.class);

        // 1. handle included categories
        Set<ActivityLoggingCategory> includedCategories = properties.getIncludedCategories();
        if (includedCategories != null) {
            for (ActivityLoggingCategory includedCategory : includedCategories) {
                Set<ActivityLoggingRequest> loggingRequests = getRequestsFromCategory(includedCategory);
                for (ActivityLoggingRequest loggingRequest : loggingRequests) {
                    result.put(loggingRequest.getName(), loggingRequest);
                }
            }
        }

        // 2. handle included requests
        LinkedHashSet<ActivityLoggingRequest> loggingRequests = properties.getIncludedRequests();
        if (loggingRequests != null) {
            for (ActivityLoggingRequest loggingRequest : loggingRequests) {
                result.put(loggingRequest.getName(), loggingRequest);
            }
        }

        // 3. handle excluded category names
        LinkedHashSet<ActivityLoggingCategoryName> excludedCategoryNames = properties.getExcludedCategoryNames();
        if (excludedCategoryNames != null) {
            for (ActivityLoggingCategoryName excludedCategoryName : excludedCategoryNames) {
                Set<TurmsRequest.KindCase> excludedRequestNames = getRequestsFromCategoryName(excludedCategoryName);
                for (TurmsRequest.KindCase excludedRequestName : excludedRequestNames) {
                    result.remove(excludedRequestName);
                }
            }
        }

        // 4. handle excluded request names
        LinkedHashSet<TurmsRequest.KindCase> excludedRequestNames = properties.getExcludedRequestNames();
        if (excludedRequestNames != null) {
            for (TurmsRequest.KindCase excludedRequestName : excludedRequestNames) {
                result.remove(excludedRequestName);
            }
        }

        return result;
    }

    private Set<ActivityLoggingRequest> getRequestsFromCategory(ActivityLoggingCategory category) {
        ActivityLoggingCategoryName categoryName = category.getName();
        Set<TurmsRequest.KindCase> requests = getRequestsFromCategoryName(categoryName);
        if (requests.isEmpty()) {
            return Collections.emptySet();
        } else {
            Set<ActivityLoggingRequest> loggingRequests = Sets.newHashSetWithExpectedSize(requests.size());
            for (TurmsRequest.KindCase request : requests) {
                loggingRequests.add(new ActivityLoggingRequest(request, category.getSampleRate()));
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
