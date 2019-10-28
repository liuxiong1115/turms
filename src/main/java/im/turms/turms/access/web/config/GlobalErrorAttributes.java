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

package im.turms.turms.access.web.config;

import org.springframework.boot.web.reactive.error.DefaultErrorAttributes;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;

import java.util.Map;

import static im.turms.turms.common.Constants.STATUS;

@Component
public class GlobalErrorAttributes extends DefaultErrorAttributes {
    @Override
    public Map<String, Object> getErrorAttributes(
            ServerRequest request,
            boolean includeStackTrace) {
        Map<String, Object> errorAttributes = super.getErrorAttributes(request, false);
        if ((Integer) errorAttributes.get(STATUS) == 500) {
            Object messageObj = errorAttributes.get("message");
            boolean isClientError;
            if (messageObj == null) { // For NullPointerException
                isClientError = true;
            } else {
                String message = messageObj.toString();
                isClientError = message.contains("WebFlux") || message.contains("cast");
            }
            if (isClientError) {
                errorAttributes.put(STATUS, 400);
                errorAttributes.remove("error");
            }
        }
        return errorAttributes;
    }
}
