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
import com.google.protobuf.StringValue;
import helper.client.SimpleTurmsClient;
import im.turms.turms.common.TurmsPasswordUtil;
import im.turms.turms.constant.ChatType;
import im.turms.turms.constant.ProfileAccessStrategy;
import im.turms.turms.pojo.domain.User;
import im.turms.turms.pojo.domain.UserRelationship;
import im.turms.turms.pojo.notification.TurmsNotification;
import im.turms.turms.pojo.request.TurmsRequest;
import im.turms.turms.pojo.request.message.CreateMessageRequest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;

import java.util.Date;
import java.util.function.Function;

import static org.mockito.Mockito.*;

public class WsMessageControllerIT extends BaseControllerIT {

    @LocalServerPort
    private Integer port;

    @BeforeEach
    void setUp(@Autowired MongoTemplate mongoTemplate, @Autowired TurmsPasswordUtil passwordUtil) {
        User userOne = new User(1L, passwordUtil.encodeUserPassword("123"), "", "",
                "", ProfileAccessStrategy.ALL, new Date(), new Date(), true);
        User userTwo = new User(2L, passwordUtil.encodeUserPassword("123"), "", "",
                "", ProfileAccessStrategy.ALL, new Date(), new Date(), true);
        User userThree = new User(3L, passwordUtil.encodeUserPassword("123"), "", "",
                "", ProfileAccessStrategy.ALL, new Date(), new Date(), true);
        UserRelationship relationshipOne = new UserRelationship(1L, 2L, false, new Date());
        UserRelationship relationshipTwo = new UserRelationship(2L, 1L, false, new Date());
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
    public void handleCreateMessageRequest_shouldRelayMessageImmediately() throws InterruptedException {
        Function<TurmsNotification, Void> callback = mock(Function.class);
        SimpleTurmsClient clientOne = new SimpleTurmsClient(port, 1L, "123", null);
        SimpleTurmsClient clientTwo = new SimpleTurmsClient(port, 2L, "123", callback);
        clientTwo.returnAfterConnectedOrFailed();
        TurmsRequest.Builder builder = TurmsRequest
                .newBuilder()
                .setRequestId(Int64Value.newBuilder().setValue(1).build())
                .setCreateMessageRequest(
                        CreateMessageRequest
                                .newBuilder()
                                .setChatType(ChatType.PRIVATE)
                                .setToId(2L)
                                .setText(StringValue.newBuilder().setValue("test").build())
                                .build());
        clientOne.send(builder, null);
        verify(callback, timeout(5000)).apply(argThat(argument -> argument.hasRelayedRequest()
                && argument.getRelayedRequest().getKindCase() == TurmsRequest.KindCase.CREATE_MESSAGE_REQUEST));
    }
}
