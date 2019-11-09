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

package im.turms.turms.common;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.validation.constraints.NotEmpty;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

public class ReactorUtil {

    private ReactorUtil() {
    }

    public static <T> Mono<Map<String, T>> objectToMap(String title, Mono<T> object) {
        return object.map(obj -> Collections.singletonMap(title, obj));
    }

    public static <T> Flux<T> futures2Flux(@NotEmpty Collection<Future<T>> futures) {
        List<Mono<T>> list = new ArrayList<>(futures.size());
        for (Future<T> future : futures) {
            list.add(future2Mono(future));
        }
        return Flux.merge(list);
    }

    public static <T> Mono<T> future2Mono(Future<T> future) {
        return Mono.fromCompletionStage((CompletableFuture<T>)future);
    }
}
