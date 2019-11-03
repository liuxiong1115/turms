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

package im.turms.turms.plugin;

import im.turms.turms.pojo.bo.RequestResult;
import im.turms.turms.pojo.dto.TurmsRequestWrapper;
import org.pf4j.ExtensionPoint;
import reactor.core.publisher.Mono;

import javax.validation.constraints.NotNull;

public interface ClientRequestHandler extends ExtensionPoint {
    /**
     * @return the returned {@link TurmsRequestWrapper} will be passed to downstream.
     */
    Mono<TurmsRequestWrapper> transform(@NotNull TurmsRequestWrapper turmsRequestWrapper);

    /**
     * @return returning Mono.empty() will pass the request to downstream to handle.
     */
    Mono<RequestResult> handleTurmsRequest(@NotNull TurmsRequestWrapper turmsRequestWrapper);
}