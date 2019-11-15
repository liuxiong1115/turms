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

import im.turms.turms.annotation.web.RequiredPermission;
import im.turms.turms.cluster.TurmsClusterManager;
import im.turms.turms.common.Constants;
import im.turms.turms.constant.AdminPermission;
import im.turms.turms.service.admin.AdminActionLogService;
import im.turms.turms.service.admin.AdminService;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.reactive.result.method.annotation.RequestMappingHandlerMapping;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import javax.validation.constraints.NotNull;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static im.turms.turms.common.Constants.ACCOUNT;
import static im.turms.turms.common.Constants.PASSWORD;

@Component
public class ControllerFilter implements WebFilter {
    private final RequestMappingHandlerMapping requestMappingHandlerMapping;
    private final AdminService adminService;
    private final AdminActionLogService adminActionLogService;
    private final TurmsClusterManager turmsClusterManager;

    public ControllerFilter(RequestMappingHandlerMapping requestMappingHandlerMapping, AdminService adminService, AdminActionLogService adminActionLogService, TurmsClusterManager turmsClusterManager) {
        this.requestMappingHandlerMapping = requestMappingHandlerMapping;
        this.adminService = adminService;
        this.adminActionLogService = adminActionLogService;
        this.turmsClusterManager = turmsClusterManager;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        Object handlerMethodObject = requestMappingHandlerMapping.getHandler(exchange)
                .toProcessor()
                .peek();
        if (handlerMethodObject instanceof HandlerMethod) {
            HandlerMethod handlerMethod = (HandlerMethod) handlerMethodObject;
            ServerHttpRequest request = exchange.getRequest();
            String account = request.getHeaders().getFirst(ACCOUNT);
            String password = request.getHeaders().getFirst(PASSWORD);
            if (Constants.DEV_MODE) {
                if (account != null && password != null) {
                    return tryPersistingAndPass(account, exchange, chain, handlerMethod);
                }
                return chain.filter(exchange);
            }
            RequiredPermission requiredPermission = handlerMethod.getMethodAnnotation(RequiredPermission.class);
            if (requiredPermission != null && requiredPermission.value().equals(AdminPermission.CUSTOM)) {
                return chain.filter(exchange);
            } else {
                if (account != null && password != null) {
                    return adminService.authenticate(account, password)
                            .flatMap(authenticated -> {
                                if (authenticated != null && authenticated) {
                                    if (requiredPermission != null) {
                                        return adminService.isAdminAuthorized(account, requiredPermission.value())
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
        } else {
            String upgrade = exchange.getRequest().getHeaders().getFirst("Upgrade");
            if (upgrade != null && upgrade.equals("websocket")) {
                return chain.filter(exchange);
            } else {
                exchange.getResponse().setStatusCode(HttpStatus.METHOD_NOT_ALLOWED);
                return Mono.empty();
            }
        }
    }

    /**
     * TODO: Persist the resolved params when Spring
     * 1. Provides a "filter"(or something like this) after getMethodArgumentValues() and before invoking custom handlers
     * 2. (Best) Or allows developers to catch resolved params and body in exchange.getResponse().beforeCommit()
     * https://github.com/spring-projects/spring-framework/issues/24004
     */
    @Deprecated
    private Mono<Void> tryPersistingAndPass(
            @NotNull String account,
            @NotNull ServerWebExchange exchange,
            @NotNull WebFilterChain chain,
            @NotNull HandlerMethod handlerMethod) {
        if (turmsClusterManager.getTurmsProperties().getLog().isLogAdminAction()) {
            String action = handlerMethod.getMethod().getName();
            MethodParameter[] methodParameters = handlerMethod.getMethodParameters();
            ServerHttpRequest request = exchange.getRequest();
            MultiValueMap<String, String> queryParams = request.getQueryParams();
            Map<String, String> attributes = null;
            if (methodParameters.length > 0 && !queryParams.isEmpty()) {
                attributes = new HashMap<>(methodParameters.length);
                for (MethodParameter methodParameter : methodParameters) {
                    String parameterName = methodParameter.getParameterName();
                    if (parameterName != null) {
                        String value = queryParams.getFirst(parameterName);
                        if (value != null) {
                            attributes.put(parameterName, value);
                        }
                    }
                }
            }
            String host = Objects.requireNonNull(request.getRemoteAddress()).getHostString();
            return chain.filter(exchange)
                    .mergeWith(adminActionLogService.saveAdminActionLog(
                            account,
                            new Date(),
                            host,
                            action,
                            attributes,
                            null)
                            .then())
                    .single();
        } else {
            return chain.filter(exchange);
        }
    }
}
