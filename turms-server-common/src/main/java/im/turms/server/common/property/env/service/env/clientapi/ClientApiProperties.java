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

package im.turms.server.common.property.env.service.env.clientapi;

import im.turms.common.model.dto.request.TurmsRequest;
import im.turms.server.common.property.env.common.ClientApiLoggingProperties;
import im.turms.server.common.property.metadata.annotation.Description;
import lombok.Data;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

import java.util.Collections;
import java.util.Set;

/**
 * @author James Chen
 */
@Data
public class ClientApiProperties {

    @Description("The disabled endpoints for client requests. Return ILLEGAL_ARGUMENT if a client tries to access them")
    private Set<TurmsRequest.KindCase> disabledEndpoints = Collections.emptySet();

    @NestedConfigurationProperty
    private ClientApiLoggingProperties logging = new ClientApiLoggingProperties();

}