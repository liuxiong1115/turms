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

package im.turms.turms.exception;

import im.turms.turms.common.TurmsStatusCode;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotNull;
import java.util.EnumMap;
import java.util.Map;

@EqualsAndHashCode(callSuper = false)
@Data
public class TurmsBusinessException extends NoStackTraceException {
    private static final Map<TurmsStatusCode, TurmsBusinessException> EXCEPTION_POOL = new EnumMap<>(TurmsStatusCode.class);
    private final TurmsStatusCode code;

    private TurmsBusinessException(@NotNull TurmsStatusCode code) {
        this.code = code;
    }

    public static TurmsBusinessException get(@NotNull TurmsStatusCode code) {
        return EXCEPTION_POOL.computeIfAbsent(code, key -> new TurmsBusinessException(code));
    }
}
