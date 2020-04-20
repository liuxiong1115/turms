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

package im.turms.turms.pojo.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.experimental.FieldNameConstants;
import org.springframework.data.annotation.Id;

import java.util.Date;

@Data
@FieldNameConstants
@AllArgsConstructor
public final class UserVersion {
    @Id
    private final Long userId;
    private final Date sentFriendRequests;
    private final Date receivedFriendRequests;
    private final Date relationships;
    private final Date relationshipGroups;
    private final Date relationshipGroupsMembers;
    private final Date groupJoinRequests; // sent group join requests
    private final Date sentGroupInvitations;
    private final Date receivedGroupInvitations;
    private final Date joinedGroups;
}
