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
import java8.util.concurrent.CompletableFuture;
import org.jetbrains.annotations.Nullable;

import static im.turms.common.exception.TurmsBusinessException.get;

/**
 * @author James Chen
 */
public class TurmsBusinessExceptionUtil {

    private TurmsBusinessExceptionUtil() {
    }

    public static <T> CompletableFuture<T> getFuture(TurmsStatusCode statusCode) {
        CompletableFuture<T> future = new CompletableFuture<>();
        future.completeExceptionally(get(statusCode));
        return future;
    }

    public static <T> CompletableFuture<T> getFuture(TurmsStatusCode statusCode, @Nullable String reason) {
        CompletableFuture<T> future = new CompletableFuture<>();
        future.completeExceptionally(get(statusCode, reason));
        return future;
    }

    public static <T> CompletableFuture<T> getFuture(TurmsStatusCode statusCode, @Nullable Throwable cause) {
        CompletableFuture<T> future = new CompletableFuture<>();
        future.completeExceptionally(get(statusCode, cause));
        return future;
    }

}