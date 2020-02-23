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

import com.google.protobuf.Int64Value;
import helper.Constants;
import im.turms.client.TurmsClient;
import im.turms.common.constant.ProfileAccessStrategy;
import im.turms.common.model.bo.group.GroupInvitationsWithVersion;
import im.turms.common.model.dto.request.TurmsRequest;
import im.turms.common.model.dto.request.user.QueryUserGroupInvitationsRequest;
import im.turms.turms.common.TurmsPasswordUtil;
import im.turms.turms.pojo.domain.User;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;

import java.util.Date;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static im.turms.turms.common.Constants.DEFAULT_USER_PERMISSION_GROUP_ID;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class WsUserControllerST extends BaseController {
    @LocalServerPort
    Integer port;

    @BeforeAll
    public static void initUser(@Autowired MongoTemplate mongoTemplate, @Autowired TurmsPasswordUtil passwordUtil) {
        Date now = new Date();
        User user = new User(1L, passwordUtil.encodeUserPassword("123"), "", "",
                "", ProfileAccessStrategy.ALL, DEFAULT_USER_PERMISSION_GROUP_ID, now, null, true, now);
        mongoTemplate.save(user);
    }

    @AfterAll
    public static void tearDown(@Autowired MongoTemplate mongoTemplate) {
        mongoTemplate.remove(new Query(), User.class);
    }

    @Test
    public void queryUserGroupInvitations_shouldReturn() throws InterruptedException, TimeoutException, ExecutionException {
        TurmsClient client = new TurmsClient(Constants.WS_URL, null, null);
        TurmsRequest.Builder builder = TurmsRequest
                .newBuilder()
                .setRequestId(Int64Value.newBuilder().setValue(1).build())
                .setQueryUserGroupInvitationsRequest(
                        QueryUserGroupInvitationsRequest
                                .newBuilder()
                                .build());
        GroupInvitationsWithVersion version = client.getUserService().queryUserGroupInvitations(null)
                .get(5, TimeUnit.SECONDS);
        assertNotNull(version);
    }
}
