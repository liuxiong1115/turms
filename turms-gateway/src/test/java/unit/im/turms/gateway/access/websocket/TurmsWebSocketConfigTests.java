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

package unit.im.turms.gateway.access.websocket;

import im.turms.gateway.access.websocket.config.TurmsHandshakeWebSocketService;
import im.turms.gateway.access.websocket.config.TurmsWebSocketConfig;
import im.turms.gateway.access.websocket.config.TurmsWebSocketHandler;
import im.turms.server.common.property.TurmsProperties;
import im.turms.server.common.property.TurmsPropertiesManager;
import im.turms.server.common.property.env.gateway.GatewayProperties;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.handler.SimpleUrlHandlerMapping;
import org.springframework.web.reactive.socket.server.support.WebSocketHandlerAdapter;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author James Chen
 */
class TurmsWebSocketConfigTests {

    @Test
    void handlerMapping_shouldGetHandlerMappingWithTurmsHandler() {
        TurmsHandshakeWebSocketService webSocketService = mock(TurmsHandshakeWebSocketService.class);
        TurmsWebSocketHandler handler = mock(TurmsWebSocketHandler.class);

        String url = "/test-handler";
        TurmsPropertiesManager propertiesManager = mock(TurmsPropertiesManager.class);
        TurmsProperties properties = new TurmsProperties();
        GatewayProperties gateway = new GatewayProperties();
        gateway.setUrl(url);
        properties.setGateway(gateway);
        when(propertiesManager.getLocalProperties())
                .thenReturn(properties);

        TurmsWebSocketConfig config = new TurmsWebSocketConfig(webSocketService, handler);
        SimpleUrlHandlerMapping mapping = (SimpleUrlHandlerMapping) config.handlerMapping(propertiesManager);
        Map<String, ?> urlMap = mapping.getUrlMap();

        assertEquals(1, urlMap.size());
        assertEquals(handler, urlMap.get(url));
    }

    @Test
    void handlerAdapter_shouldGetAdapter() {
        TurmsHandshakeWebSocketService webSocketService = mock(TurmsHandshakeWebSocketService.class);
        TurmsWebSocketHandler handler = mock(TurmsWebSocketHandler.class);
        TurmsWebSocketConfig config = new TurmsWebSocketConfig(webSocketService, handler);
        WebSocketHandlerAdapter adapter = config.handlerAdapter();

        assertEquals(webSocketService, adapter.getWebSocketService());
    }

}
