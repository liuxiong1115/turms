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

package helper;

import im.turms.common.constant.statuscode.TurmsStatusCode;
import im.turms.common.exception.TurmsBusinessException;

import java.util.concurrent.ExecutionException;

public class ExceptionUtil {
    private ExceptionUtil() {
    }

    public static boolean isTurmsStatusCode(ExecutionException throwable, TurmsStatusCode code) {
        Throwable cause = throwable.getCause();
        return isTurmsStatusCode(cause, code);
    }

    public static boolean isTurmsStatusCode(Throwable throwable, TurmsStatusCode code) {
        if (throwable instanceof TurmsBusinessException) {
            return ((TurmsBusinessException) throwable).getCode() == code;
        }
        return false;
    }
}
