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

package im.turms.turms.access.web.filter;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import im.turms.turms.annotation.web.RequiredPermission;
import im.turms.turms.compiler.CompilerOptions;
import im.turms.turms.constant.AdminPermission;
import im.turms.turms.manager.TurmsClusterManager;
import im.turms.turms.manager.TurmsPluginManager;
import im.turms.turms.service.admin.AdminActionLogService;
import im.turms.turms.service.admin.AdminService;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.core.MethodParameter;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.reactive.result.method.annotation.RequestMappingHandlerMapping;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.validation.constraints.NotNull;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Objects;

import static im.turms.turms.constant.Common.ACCOUNT;
import static im.turms.turms.constant.Common.PASSWORD;

@Component
public class ControllerFilter implements WebFilter {
    private static final BasicDBObject EMPTY_DBOJBECT = new BasicDBObject();
    private static final String ATTR_BODY = "BODY";
    private final RequestMappingHandlerMapping requestMappingHandlerMapping;
    private final AdminService adminService;
    private final AdminActionLogService adminActionLogService;
    private final TurmsClusterManager turmsClusterManager;
    private final TurmsPluginManager turmsPluginManager;
    private final boolean pluginEnabled;
    private final boolean enableAdminApi;

    public ControllerFilter(RequestMappingHandlerMapping requestMappingHandlerMapping, AdminService adminService, AdminActionLogService adminActionLogService, TurmsClusterManager turmsClusterManager, TurmsPluginManager turmsPluginManager) {
        this.requestMappingHandlerMapping = requestMappingHandlerMapping;
        this.adminService = adminService;
        this.adminActionLogService = adminActionLogService;
        this.turmsClusterManager = turmsClusterManager;
        this.turmsPluginManager = turmsPluginManager;
        pluginEnabled = turmsClusterManager.getTurmsProperties().getPlugin().isEnabled();
        enableAdminApi = turmsClusterManager.getTurmsProperties().getAdmin().isEnabled();
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        Object handlerMethodObject = requestMappingHandlerMapping.getHandler(exchange)
                .toProcessor()
                .peek();
        if (isDevAndSwaggerRequest(exchange)) {
            return chain.filter(exchange);
        }
        if (handlerMethodObject instanceof HandlerMethod) {
            return filterHandlerMethod(handlerMethodObject, exchange, chain);
        } else {
            return filterUnhandledRequest(exchange, chain);
        }
    }

    private Mono<Void> filterHandlerMethod(Object handlerMethodObject, ServerWebExchange exchange, WebFilterChain chain) {
        HandlerMethod handlerMethod = (HandlerMethod) handlerMethodObject;
        Pair<String, String> pair = parseAccountAndPassword(exchange);
        String account = pair.getLeft();
        String password = pair.getRight();
        RequiredPermission requiredPermission = handlerMethod.getMethodAnnotation(RequiredPermission.class);
        if (requiredPermission != null && requiredPermission.value().equals(AdminPermission.NONE)) {
            return chain.filter(exchange);
        } else {
            if (account != null && password != null) {
                return adminService.authenticate(account, password)
                        .flatMap(authenticated -> {
                            if (authenticated != null && authenticated) {
                                if (requiredPermission != null) {
                                    return adminService.isAdminAuthorized(exchange, account, requiredPermission.value())
                                            .flatMap(authorized -> {
                                                if (authorized != null && authorized) {
                                                    return tryPersistingAndPass(account, exchange, chain, handlerMethod);
                                                } else {
                                                    exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                                                    return Mono.empty();
                                                }
                                            });
                                } else {
                                    return tryPersistingAndPass(account, exchange, chain, handlerMethod);
                                }
                            } else {
                                exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                                return Mono.empty();
                            }
                        });
            } else {
                exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                return Mono.empty();
            }
        }
    }

    private Mono<Void> filterUnhandledRequest(ServerWebExchange exchange, WebFilterChain chain) {
        if (isHandshakeRequest(exchange) || isCorsPreflightRequest(exchange)) {
            return chain.filter(exchange);
        } else if (enableAdminApi) {
            Pair<String, String> pair = parseAccountAndPassword(exchange);
            String account = pair.getLeft();
            String password = pair.getRight();
            if (account != null && password != null) {
                return adminService.authenticate(account, password)
                        .flatMap(authenticated -> {
                            if (authenticated) {
                                return chain.filter(exchange);
                            } else {
                                exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                                return Mono.empty();
                            }
                        });
            } else {
                exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                return Mono.empty();
            }
        } else {
            exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
            return Mono.empty();
        }
    }

    private Pair<String, String> parseAccountAndPassword(@NotNull ServerWebExchange exchange) {
        ServerHttpRequest request = exchange.getRequest();
        return Pair.of(request.getHeaders().getFirst(ACCOUNT),
                request.getHeaders().getFirst(PASSWORD));
    }

    private Mono<Void> tryPersistingAndPass(
            @NotNull String account,
            @NotNull ServerWebExchange exchange,
            @NotNull WebFilterChain chain,
            @NotNull HandlerMethod handlerMethod) {
        boolean logAdminAction = turmsClusterManager.getTurmsProperties().getLog().isLogAdminAction();
        boolean invokeHandlers = pluginEnabled && !turmsPluginManager.getLogHandlerList().isEmpty();
        Mono<Void> additionalMono;
        if (logAdminAction || invokeHandlers) {
            ServerHttpRequest request = exchange.getRequest();
            String action = handlerMethod.getMethod().getName();
            String host = Objects.requireNonNull(request.getRemoteAddress()).getHostString();
            DBObject params = null;
            Mono<BasicDBObject> bodyMono;
            if (turmsClusterManager.getTurmsProperties().getLog().isLogRequestParams()) {
                params = parseValidParams(request, handlerMethod);
            }
            if (turmsClusterManager.getTurmsProperties().getLog().isLogRequestBody()) {
                bodyMono = parseValidBody(exchange);
                exchange = replaceRequestBody(exchange);
            } else {
                bodyMono = Mono.empty();
            }
            DBObject finalParams = params;
            ServerWebExchange finalExchange = exchange;
            additionalMono = bodyMono.defaultIfEmpty(EMPTY_DBOJBECT).flatMap(dbObject -> {
                DBObject body = dbObject != EMPTY_DBOJBECT ? dbObject : null;
                if (logAdminAction) {
                    return adminActionLogService.saveAdminActionLog(
                            account,
                            new Date(),
                            host,
                            action,
                            finalParams,
                            body)
                            .doOnSuccess(log -> {
                                if (invokeHandlers) {
                                    adminActionLogService.triggerLogHandlers(finalExchange, log);
                                }
                            });
                } else {
                    adminActionLogService.triggerLogHandlers(
                            finalExchange,
                            null,
                            account,
                            new Date(),
                            host,
                            action,
                            finalParams,
                            body);
                    return Mono.empty();
                }
            }).then();
        } else {
            additionalMono = Mono.empty();
        }
        ServerWebExchange finalExchange = exchange;
        return additionalMono.then(chain.filter(exchange))
                .doFinally(signalType -> finalExchange.getAttributes().remove(ATTR_BODY));
    }

    private DBObject parseValidParams(ServerHttpRequest request, HandlerMethod handlerMethod) {
        MethodParameter[] methodParameters = handlerMethod.getMethodParameters();
        MultiValueMap<String, String> queryParams = request.getQueryParams();
        BasicDBObject params = null;
        if (methodParameters.length > 0 && !queryParams.isEmpty()) {
            params = new BasicDBObject(queryParams.size());
            for (MethodParameter methodParameter : methodParameters) {
                String parameterName = methodParameter.getParameterName();
                if (parameterName != null) {
                    String value = queryParams.getFirst(parameterName);
                    if (value != null) {
                        params.put(parameterName, value);
                    }
                }
            }
            if (params.isEmpty()) {
                params = EMPTY_DBOJBECT;
            }
        }
        return params;
    }

    private Mono<BasicDBObject> parseValidBody(@NotNull ServerWebExchange exchange) {
        return DataBufferUtils.join(exchange.getRequest().getBody())
                .map(dataBuffer -> {
                    exchange.getAttributes().put(ATTR_BODY, dataBuffer);
                    String json = dataBuffer.toString(StandardCharsets.UTF_8);
                    BasicDBObject dbObject = BasicDBObject.parse(json);
                    if (dbObject.isEmpty()) {
                        return EMPTY_DBOJBECT;
                    } else {
                        return dbObject;
                    }
                });
    }

    /**
     * Build a new request with a new body to pass down to RequestBodyMethodArgumentResolver.resolveArgument
     */
    private ServerWebExchange replaceRequestBody(ServerWebExchange exchange) {
        ServerHttpRequest mutatedRequest = new ServerHttpRequestDecorator(exchange.getRequest()) {
            @Override
            public Flux<DataBuffer> getBody() {
                DataBuffer dataBuffer = exchange.getAttribute(ATTR_BODY);
                if (dataBuffer != null) {
                    return Flux.just(dataBuffer);
                } else {
                    return getDelegate().getBody();
                }
            }
        };
        return exchange.mutate().request(mutatedRequest).build();
    }

    private boolean isHandshakeRequest(@NotNull ServerWebExchange exchange) {
        String upgrade = exchange.getRequest().getHeaders().getFirst("Upgrade");
        if (upgrade != null && upgrade.equals("websocket")) {
            String path = exchange.getRequest().getURI().getPath();
            return path.equals("") || path.equals("/");
        }
        return false;
    }

    private boolean isCorsPreflightRequest(@NotNull ServerWebExchange exchange) {
        ServerHttpRequest request = exchange.getRequest();
        return request.getMethodValue().equals(HttpMethod.OPTIONS.name())
                && request.getHeaders().containsKey(HttpHeaders.ORIGIN)
                && request.getHeaders().containsKey(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD);
    }

    private boolean isDevAndSwaggerRequest(@NotNull ServerWebExchange exchange) {
        if (CompilerOptions.ENV == CompilerOptions.Env.DEV) {
            String path = exchange.getRequest().getURI().getPath();
            return path.startsWith("/v2/api-docs")
                    || path.startsWith("/swagger-resources")
                    || path.startsWith("/swagger-ui.html")
                    || path.startsWith("/webjars/springfox-swagger-ui");
        } else {
            return false;
        }
    }
}
