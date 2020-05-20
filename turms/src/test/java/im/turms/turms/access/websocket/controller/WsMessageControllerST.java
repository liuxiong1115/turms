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

import helper.Constants;
import im.turms.client.TurmsClient;
import im.turms.common.constant.ProfileAccessStrategy;
import im.turms.turms.pojo.domain.User;
import im.turms.turms.pojo.domain.UserRelationship;
import im.turms.turms.util.TurmsPasswordUtil;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;

import java.util.Date;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static im.turms.turms.constant.Common.DEFAULT_USER_PERMISSION_GROUP_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class WsMessageControllerST extends BaseController {

    @LocalServerPort
    private Integer port;

    @BeforeEach
    void setUp(@Autowired MongoTemplate mongoTemplate, @Autowired TurmsPasswordUtil passwordUtil) {
        Date now = new Date();
        User userOne = new User(1L, passwordUtil.encodeUserPassword("123"), "", "",
                ProfileAccessStrategy.ALL, DEFAULT_USER_PERMISSION_GROUP_ID, now, null, true, now);
        User userTwo = new User(2L, passwordUtil.encodeUserPassword("123"), "", "",
                ProfileAccessStrategy.ALL, DEFAULT_USER_PERMISSION_GROUP_ID, now, null, true, now);
        User userThree = new User(3L, passwordUtil.encodeUserPassword("123"), "", "",
                ProfileAccessStrategy.ALL, DEFAULT_USER_PERMISSION_GROUP_ID, now, null, true, now);
        UserRelationship relationshipOne = new UserRelationship(1L, 2L, false, now);
        UserRelationship relationshipTwo = new UserRelationship(2L, 1L, false, now);
        mongoTemplate.save(userOne);
        mongoTemplate.save(userTwo);
        mongoTemplate.save(relationshipOne);
        mongoTemplate.save(relationshipTwo);
    }

    @AfterAll
    public static void tearDown(@Autowired MongoTemplate mongoTemplate) {
        mongoTemplate.remove(new Query(), User.class);
        mongoTemplate.remove(new Query(), UserRelationship.class);
    }

    @Test
    public void handleCreateMessageRequest_shouldRelayMessageImmediately() throws InterruptedException, TimeoutException, ExecutionException {
        TurmsClient clientOne = new TurmsClient(Constants.WS_URL);
        clientOne.getUserService()
                .login(1L, "123")
                .get(5, TimeUnit.SECONDS);
        TurmsClient clientTwo = new TurmsClient(Constants.WS_URL);
        clientTwo.getUserService()
                .login(2L, "123")
                .get(5, TimeUnit.SECONDS);
        Long sentMessageId = clientOne.getMessageService()
                .sendMessage(false, 2L, null, "test", null, null)
                .get(5, TimeUnit.SECONDS);
        long receivedMessageId = clientTwo.getMessageService()
                .queryMessages(null, null, null, 1L, null, null, null, null)
                .get(5, TimeUnit.SECONDS)
                .get(0)
                .getId()
                .getValue();
        assertEquals(sentMessageId, receivedMessageId);
    }
}
