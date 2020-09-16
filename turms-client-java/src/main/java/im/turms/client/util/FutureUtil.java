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

package im.turms.client.util;

import im.turms.common.constant.statuscode.TurmsStatusCode;
import im.turms.common.exception.TurmsBusinessException;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * @author James Chen
 */
public class FutureUtil {

    private FutureUtil() {
    }

    public static <V> CompletableFuture<V> timeout(CompletableFuture<V> future, Duration timeout, ScheduledExecutorService executorService, Consumer<Exception> onTimeout) {
        executorService.schedule(() -> {
            if (!future.isDone()) {
                TurmsBusinessException exception = TurmsBusinessException.get(TurmsStatusCode.TIMEOUT);
                future.completeExceptionally(exception);
                if (future.isCompletedExceptionally()) {
                    onTimeout.accept(exception);
                }
            }
        }, timeout.toMillis(), TimeUnit.MILLISECONDS);
        return future;
    }

}
