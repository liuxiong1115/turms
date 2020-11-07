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

package im.turms.turms.workflow.access.http.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.config.CorsRegistry;
import org.springframework.web.reactive.config.WebFluxConfigurer;
import org.springframework.web.reactive.handler.AbstractHandlerMapping;

/**
 * @author James Chen
 */
@Configuration
public class WebFluxConfig implements WebFluxConfigurer {

    /**
     * @see AbstractHandlerMapping#getHandler(org.springframework.web.server.ServerWebExchange)
     * @see org.springframework.web.cors.reactive.DefaultCorsProcessor
     */
    @Override
    public void addCorsMappings(CorsRegistry corsRegistry) {
        // We don't expose configs for developers to customize the cors config
        // because it's better to be done by firewall/ECS/EC2 and so on for better flexibility
        corsRegistry.addMapping("/**")
                .allowCredentials(true)
                .allowedOriginPatterns("*")
                .allowedMethods("*")
                .allowedHeaders("*");
    }

}