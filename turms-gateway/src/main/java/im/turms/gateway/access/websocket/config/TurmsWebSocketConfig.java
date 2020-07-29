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

package im.turms.gateway.access.websocket.config;

import im.turms.server.common.property.TurmsPropertiesManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.reactive.HandlerMapping;
import org.springframework.web.reactive.handler.SimpleUrlHandlerMapping;
import org.springframework.web.reactive.socket.server.support.WebSocketHandlerAdapter;

import java.util.Map;

/**
 * @author James Chen
 */
@Configuration
public class TurmsWebSocketConfig {

    private final TurmsWebSocketHandler turmsWebSocketHandler;
    private final TurmsHandshakeWebSocketService turmsHandshakeWebSocketService;

    @Autowired
    public TurmsWebSocketConfig(TurmsHandshakeWebSocketService turmsHandshakeWebSocketService, TurmsWebSocketHandler turmsWebSocketHandler) {
        this.turmsHandshakeWebSocketService = turmsHandshakeWebSocketService;
        this.turmsWebSocketHandler = turmsWebSocketHandler;
    }

    @Bean
    public HandlerMapping handlerMapping(TurmsPropertiesManager turmsPropertiesManager) {
        SimpleUrlHandlerMapping mapping = new SimpleUrlHandlerMapping();
        String url = turmsPropertiesManager.getLocalProperties().getGateway().getUrl();
        mapping.setUrlMap(Map.of(url, turmsWebSocketHandler));
        mapping.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return mapping;
    }

    @Bean
    public WebSocketHandlerAdapter handlerAdapter() {
        return new WebSocketHandlerAdapter(turmsHandshakeWebSocketService);
    }

}
