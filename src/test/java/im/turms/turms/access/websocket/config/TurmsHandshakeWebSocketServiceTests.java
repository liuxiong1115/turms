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

package im.turms.turms.access.websocket.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.HttpHeaders;
import org.springframework.web.reactive.socket.HandshakeInfo;
import org.springframework.web.reactive.socket.WebSocketSession;
import org.springframework.web.reactive.socket.client.ReactorNettyWebSocketClient;

import java.net.URISyntaxException;
import java.time.Duration;

import static helper.util.LoginUtil.getLoginParams;
import static helper.util.LoginUtil.getServerUrl;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class TurmsHandshakeWebSocketServiceTests {

    @LocalServerPort
    Integer port;

    @Test
    public void handleRequest_shouldLogin_withCorrectCredential() throws URISyntaxException {
        ReactorNettyWebSocketClient client = new ReactorNettyWebSocketClient();
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.COOKIE, getLoginParams(1L, "123"));
        final HandshakeInfo[] handshakeInfo = new HandshakeInfo[1];
        client.execute(getServerUrl(port),
                headers,
                session -> {
                    handshakeInfo[0] = session.getHandshakeInfo();
                    return session.close();
                })
                .block(Duration.ofSeconds(5));
        assertEquals(handshakeInfo[0].getHeaders().getFirst("upgrade"),
                "websocket");
    }

    @Test
    public void handleRequest_shouldNotLogin_withIncorrectCredential() {
        ReactorNettyWebSocketClient client = new ReactorNettyWebSocketClient();
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.COOKIE, getLoginParams(1L, "123456"));
        Exception exception = null;
        try {
            client.execute(getServerUrl(port),
                    headers,
                    WebSocketSession::close)
                    .block(Duration.ofSeconds(5));
        } catch (Exception e) {
            exception = e;
        }
        assertNotNull(exception);
    }
}
