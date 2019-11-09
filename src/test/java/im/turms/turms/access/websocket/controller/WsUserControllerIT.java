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
import helper.client.SimpleTurmsClient;
import im.turms.turms.constant.ProfileAccessStrategy;
import im.turms.turms.pojo.domain.User;
import im.turms.turms.pojo.notification.TurmsNotification;
import im.turms.turms.pojo.request.TurmsRequest;
import im.turms.turms.pojo.request.user.QueryUserGroupInvitationsRequest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;

import java.util.Date;
import java.util.function.Function;

import static im.turms.turms.pojo.notification.TurmsNotification.Data.KindCase.GROUP_INVITATIONS_WITH_VERSION;
import static org.mockito.Mockito.*;

public class WsUserControllerIT extends BaseControllerIT {
    @LocalServerPort Integer port;

    @BeforeAll
    public static void initUser(@Autowired MongoTemplate mongoTemplate) {
        User user = new User(1L, "123", "", "",
                "", ProfileAccessStrategy.ALL, new Date(), new Date(), true);
        mongoTemplate.save(user);
    }

    @AfterAll
    public static void tearDown(@Autowired MongoTemplate mongoTemplate) {
        mongoTemplate.remove(new Query(), User.class);
    }

    @Test
    public void queryUserGroupInvitations_shouldReturn() throws InterruptedException {
        SimpleTurmsClient simpleTurmsClient = new SimpleTurmsClient(
                port,
                1L,
                "123",
                null);
        TurmsRequest.Builder builder = TurmsRequest
                .newBuilder()
                .setRequestId(Int64Value.newBuilder().setValue(1).build())
                .setQueryUserGroupInvitationsRequest(
                        QueryUserGroupInvitationsRequest
                                .newBuilder()
                                .build());
        Function<TurmsNotification, Void> callback = mock(Function.class);
        simpleTurmsClient.send(builder, callback);
        verify(callback, timeout(5000)).apply(argThat(argument ->
                argument.hasData() && argument.getData().getKindCase() == GROUP_INVITATIONS_WITH_VERSION));
    }
}
