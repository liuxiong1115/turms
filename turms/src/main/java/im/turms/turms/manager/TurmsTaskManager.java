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

package im.turms.turms.manager;

import com.google.common.annotations.Beta;
import com.hazelcast.cluster.Member;
import com.hazelcast.core.IExecutorService;
import com.hazelcast.core.MultiExecutionCallback;
import im.turms.turms.util.ReactorUtil;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.validation.constraints.NotNull;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

@Component
@Beta
public class TurmsTaskManager {
    private final IExecutorService executor;
    private final TurmsClusterManager turmsClusterManager;

    public TurmsTaskManager(TurmsClusterManager turmsClusterManager) {
        executor = turmsClusterManager.getExecutor();
        this.turmsClusterManager = turmsClusterManager;
    }

    public <T> Mono<T> call(@NotNull Member member, @NotNull Callable<T> task, @NotNull Duration duration) {
        Future<T> future = executor.submitToMember(task, member);
        return ReactorUtil.future2Mono(future, duration);
    }

    public <T> Flux<T> callAllOthers(@NotNull Callable<T> task, @NotNull Duration duration) {
        Map<Member, Future<T>> futureMap = executor.submitToMembers(task, turmsClusterManager.getOtherMembersSnapshot());
        return ReactorUtil.futures2Flux(futureMap.values(), duration);
    }

    public <T> Mono<Map<Member, T>> callAllOthersAsMap(@NotNull Callable<T> task, @NotNull Duration duration) {
        Mono<Map<Member, T>> map = Mono.create(sink -> executor.submitToMembers(task, turmsClusterManager.getOtherMembersSnapshot(), new MultiExecutionCallback() {
            @Override
            public void onResponse(Member member, Object value) {

            }

            @Override
            public void onComplete(Map<Member, Object> values) {
                sink.success((Map<Member, T>) values);
            }
        }));
        return map.timeout(duration);
    }
}
