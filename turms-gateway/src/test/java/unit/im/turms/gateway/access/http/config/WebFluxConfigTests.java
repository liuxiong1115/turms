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

package unit.im.turms.gateway.access.http.config;

import im.turms.gateway.access.http.config.WebFluxConfig;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.reactive.config.CorsRegistry;

import java.util.Map;

/**
 * @author James Chen
 */
class WebFluxConfigTests {

    private static class MyCorsRegistry extends CorsRegistry {
        @Override
        protected Map<String, CorsConfiguration> getCorsConfigurations() {
            return super.getCorsConfigurations();
        }
    }

    @Test
    void addCorsMappings_shouldPassAnyRequest() {
        WebFluxConfig config = new WebFluxConfig();
        MyCorsRegistry corsRegistry = new MyCorsRegistry();
        config.addCorsMappings(corsRegistry);
        Map<String, CorsConfiguration> configurations = corsRegistry.getCorsConfigurations();

        Assertions.assertEquals(1, configurations.size());
        Map.Entry<String, CorsConfiguration> entry = configurations.entrySet().iterator().next();
        Assertions.assertEquals("/**", entry.getKey());
        Assertions.assertEquals(true, entry.getValue().getAllowCredentials());
        Assertions.assertTrue(entry.getValue().getAllowedOriginPatterns().contains("*"));
        Assertions.assertTrue(entry.getValue().getAllowedMethods().contains("*"));
        Assertions.assertTrue(entry.getValue().getAllowedHeaders().contains("*"));
    }

}
