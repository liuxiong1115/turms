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
import im.turms.turms.pojo.request.QueryUserGroupInvitationsRequest;
import im.turms.turms.pojo.request.TurmsRequest;
import im.turms.turms.pojo.response.TurmsResponse;
import org.junit.jupiter.api.Test;
import org.springframework.boot.web.server.LocalServerPort;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

public class WsUserControllerIT extends BaseControllerIT {

    protected WsUserControllerIT(@LocalServerPort Integer port) throws InterruptedException {
        super(port);
    }

    @Test
    public void queryUserGroupInvitations_shouldReturn() {
        TurmsRequest.Builder builder = TurmsRequest
                .newBuilder()
                .setRequestId(Int64Value.newBuilder().setValue(1).build())
                .setQueryUserGroupInvitationsRequest(
                        QueryUserGroupInvitationsRequest
                                .newBuilder()
                                .build());
        Mono<TurmsResponse> response = simpleTurmsClient.send(builder);
        StepVerifier.create(response)
                .expectNextMatches(turmsResponse -> turmsResponse.getData().getKindCase() ==
                        TurmsResponse.Data.KindCase.GROUP_INVITATIONS_WITH_VERSION)
                .verifyComplete();
    }
}
