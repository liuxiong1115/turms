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

package im.turms.turms.access.web.util;

import com.fasterxml.jackson.annotation.JsonIgnore;
import im.turms.turms.common.TurmsStatusCode;
import im.turms.turms.exception.TurmsBusinessException;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.http.ResponseEntity;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.validation.constraints.NotNull;
import java.util.*;

import static im.turms.turms.common.Constants.*;

@Data
public class ResponseFactory {
    private static final Object ERROR_OBJECT = EMPTY_OBJECT;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Response {
        private Integer code;
        private String reason;
        private Date timestamp;
        private Object data;

        @JsonIgnore
        private Integer httpCode;

        /**
         * Note: To keep consistent, the data field should be always a map structure object.
         */
        public Response(TurmsStatusCode turmsStatusCode, Object data) {
            this.code = turmsStatusCode.getBusinessCode();
            this.reason = turmsStatusCode.getReason();
            this.timestamp = new Date();
            this.data = data;
            this.httpCode = turmsStatusCode.getHttpStatusCode();
        }
    }

    public static Mono<ResponseEntity> withKey(String key, Mono data) {
        return okWhenTruthy(
                data.map(value -> Collections.singletonMap(key, value)),
                true);
    }

    public static Mono<ResponseEntity> dataWithCodeAnyway(
            @NotNull TurmsStatusCode turmsStatusCode,
            @NotNull Mono data,
            @NotNull Object dataWhenNull) {
        return data
                .switchIfEmpty(Mono.just(
                        ResponseEntity
                                .status(turmsStatusCode.getHttpStatusCode())
                                .body(new Response(turmsStatusCode, dataWhenNull)))
                        .map(a -> ResponseEntity
                                .status(turmsStatusCode.getHttpStatusCode())
                                .body(new Response(turmsStatusCode, a))))
                .onErrorResume(TurmsBusinessException.class, e -> handleException((TurmsBusinessException) e));
    }

    public static Mono<ResponseEntity> dataWithCodeAnyway(TurmsStatusCode turmsStatusCode, Flux data, Object dataWhenNull) {
        return dataWithCodeAnyway(turmsStatusCode, data.collectList(), dataWhenNull);
    }

    public static Mono<ResponseEntity> code(TurmsStatusCode turmsStatusCode) {
        return Mono.just(ResponseEntity
                .status(turmsStatusCode.getHttpStatusCode())
                .body(new Response(turmsStatusCode, null)));
    }

    public static Mono<ResponseEntity> codeInMono(TurmsStatusCode turmsStatusCode) {
        return Mono.just(ResponseEntity
                .status(turmsStatusCode.getHttpStatusCode())
                .body(new Response(turmsStatusCode, null)));
    }

    public static ResponseEntity entity(TurmsStatusCode turmsStatusCode) {
        return ResponseEntity
                .status(turmsStatusCode.getHttpStatusCode())
                .body(new Response(turmsStatusCode, null));
    }

    public static ResponseEntity fail() {
        return entity(TurmsStatusCode.FAILED);
    }

    public static Mono<ResponseEntity> okWhenTruthy(Flux data) {
        return okWhenTruthy(data, true);
    }

    public static Mono<ResponseEntity> okWhenTruthy(Flux data, boolean passData) {
        return data
                .collectList()
                .map(list -> {
                    if (((Collection) list).size() != 0) {
                        return okWhenTruthy(list, passData, TurmsStatusCode.OK, TurmsStatusCode.FAILED);
                    } else {
                        return entity(TurmsStatusCode.NOT_FOUND);
                    }
                })
                .onErrorResume(TurmsBusinessException.class, e -> handleException((TurmsBusinessException) e));
    }

    public static Mono<ResponseEntity> okWhenTruthy(Flux data, TurmsStatusCode failed) {
        return data
                .collectList()
                .map(list -> {
                    if (((Collection) list).size() != 0) {
                        return okWhenTruthy(list, true, TurmsStatusCode.OK, failed);
                    } else {
                        return entity(TurmsStatusCode.NOT_FOUND);
                    }
                })
                .onErrorResume(TurmsBusinessException.class, e -> handleException((TurmsBusinessException) e));
    }

    public static Mono<ResponseEntity> okWhenTruthy(Mono data) {
        return okWhenTruthy(data, true);
    }

    public static Mono<ResponseEntity> okWhenTruthy(Mono data, boolean passData) {
        return data
                .map(item -> okWhenTruthy(item, passData, TurmsStatusCode.OK, TurmsStatusCode.FAILED))
                .switchIfEmpty(code(TurmsStatusCode.NOT_FOUND))
                .onErrorResume(TurmsBusinessException.class, e -> handleException((TurmsBusinessException) e));
    }

    public static Mono<ResponseEntity> okWhenTruthy(Mono data, boolean passData, TurmsStatusCode failed) {
        return data
                .map(item -> okWhenTruthy(item, passData, TurmsStatusCode.OK, failed))
                .switchIfEmpty(code(TurmsStatusCode.NOT_FOUND))
                .onErrorResume(TurmsBusinessException.class, e -> handleException((TurmsBusinessException) e));
    }

    public static Mono<ResponseEntity> okWhenTruthy(Mono data, TurmsStatusCode failed) {
        return data
                .map(item -> okWhenTruthy(item, true, TurmsStatusCode.OK, failed))
                .switchIfEmpty(code(TurmsStatusCode.NOT_FOUND))
                .onErrorResume(TurmsBusinessException.class, e -> handleException((TurmsBusinessException) e));
    }

    public static ResponseEntity okWhenTruthy(Object data) {
        return okWhenTruthy(data, true, TurmsStatusCode.OK, TurmsStatusCode.FAILED);
    }

    public static ResponseEntity okWhenTruthy(Object data, boolean passData) {
        return okWhenTruthy(data, passData, TurmsStatusCode.OK, TurmsStatusCode.FAILED);
    }

    public static ResponseEntity okWhenTruthy(Object data, boolean returnData, TurmsStatusCode ok, TurmsStatusCode failed) {
        if (data != null) {
            if (data instanceof Boolean) {
                if ((boolean) data) {
                    return ResponseEntity.ok(new Response(ok, returnData ? true : null));
                } else {
                    return ResponseEntity.status(failed.getHttpStatusCode())
                            .body(new Response(failed, null));
                }
            } else if (data instanceof Map) {
                Map map = (Map) data;
                if (!map.isEmpty()) {
                    Object total = map.get("total");
                    if (total instanceof Long && total.equals(0L)) {
                        return ResponseEntity.status(TurmsStatusCode.NOT_FOUND.getHttpStatusCode())
                                .body(new Response(failed, null));
                    }
                    return ResponseEntity.ok(new Response(ok, returnData ? data : null));
                } else {
                    return ResponseEntity.status(TurmsStatusCode.NOT_FOUND.getHttpStatusCode())
                            .body(new Response(failed, null));
                }
            } else if (data instanceof Collection) {
                Collection<?> collection = (Collection<?>) data;
                if (!collection.isEmpty()) {
                    return ResponseEntity.ok(new Response(ok, returnData ? data : null));
                } else {
                    return ResponseEntity.status(TurmsStatusCode.NOT_FOUND.getHttpStatusCode())
                            .body(new Response(failed, null));
                }
            } else {
                return ResponseEntity.ok(new Response(ok, returnData ? data : null));
            }
        } else {
            return ResponseEntity.status(failed.getHttpStatusCode())
                    .body(new Response(failed, null));
        }
    }

    private static Mono<ResponseEntity<Response>> handleException(TurmsBusinessException exception) {
        TurmsStatusCode code = exception.getCode();
        return Mono.just(ResponseEntity
                .status(code.getHttpStatusCode())
                .body(new Response(code, null)));
    }

    public static Mono<ResponseEntity> acknowledged(Mono<Boolean> data) {
        return okWhenTruthy(
                data.map(acknowledged -> Collections.singletonMap(ACKNOWLEDGED, acknowledged)),
                true);
    }

    public static ResponseEntity acknowledged(Boolean data) {
        return okWhenTruthy(Collections.singletonMap(ACKNOWLEDGED, data), true);
    }

    public static Mono<ResponseEntity> authenticated(Mono<Boolean> data) {
        return data.map(authenticated -> {
            if (authenticated) {
                return ResponseEntity.ok().build();
            } else {
                return ResponseEntity.notFound().build();
            }
        });
    }

    public static ResponseEntity authenticated(Boolean data) {
        return okWhenTruthy(Collections.singletonMap(AUTHENTICATED, data), true);
    }

    public static <T> Mono<ResponseEntity> collectCountResults(List<Mono<Pair<String, T>>> counts) {
        Mono<Map<String, T>> resultMono = Flux.merge(counts)
                .collectList()
                .map(pairs -> {
                    Map<String, T> resultMap = new HashMap<>(counts.size());
                    for (Pair<String, T> pair : pairs) {
                        resultMap.put(pair.getLeft(), pair.getRight());
                    }
                    return resultMap;
                });
        return okWhenTruthy(resultMono);
    }
}
