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

package im.turms.turms.pojo.dto;

import lombok.Data;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
public class PageResult {
    Mono<Long> total;
    Flux data;

    private PageResult() {
    }

    public static Mono<Map> getResult(Mono<Long> total, Flux data) {
        Mono<List> list = data.collectList();
        return Mono.zip(total, list).flatMap(tuple -> {
            Map<String, Object> map = new HashMap<>(2);
            map.put("total", tuple.getT1());
            map.put("records", tuple.getT2());
            return Mono.just(map);
        });
    }
}