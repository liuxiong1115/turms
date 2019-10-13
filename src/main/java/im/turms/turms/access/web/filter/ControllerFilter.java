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
import im.turms.turms.common.Constants;
import im.turms.turms.constant.AdminPermission;
import im.turms.turms.service.admin.AdminService;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.reactive.result.method.annotation.RequestMappingHandlerMapping;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import org.springframework.web.server.adapter.DefaultServerWebExchange;
import reactor.core.publisher.Mono;

import static im.turms.turms.common.Constants.ACCOUNT;
import static im.turms.turms.common.Constants.PASSWORD;

@Component
public class ControllerFilter implements WebFilter {
    private final RequestMappingHandlerMapping requestMappingHandlerMapping;
    private final AdminService adminService;

    public ControllerFilter(RequestMappingHandlerMapping requestMappingHandlerMapping, AdminService adminService) {
        this.requestMappingHandlerMapping = requestMappingHandlerMapping;
        this.adminService = adminService;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        if (Constants.DEV_MODE) {
            return chain.filter(exchange);
        }
        Object handlerMethodObject = requestMappingHandlerMapping.getHandler(exchange)
                .toProcessor()
                .peek();
        if (handlerMethodObject instanceof HandlerMethod) {
            RequiredPermission requiredPermission = ((HandlerMethod) handlerMethodObject).getMethodAnnotation(RequiredPermission.class);
            if (requiredPermission != null && requiredPermission.value().equals(AdminPermission.CUSTOM)) {
                return chain.filter(exchange);
            } else {
                ServerHttpRequest request = exchange.getRequest();
                HttpCookie account = request.getCookies().getFirst(ACCOUNT);
                HttpCookie password = request.getCookies().getFirst(PASSWORD);
                if (account != null && password != null) {
                    String accountValue = account.getValue();
                    String passwordValue = password.getValue();
                    return adminService.authenticate(accountValue, passwordValue)
                            .flatMap(authenticated -> {
                                if (authenticated != null && authenticated) {
                                    if (requiredPermission != null) {
                                        return adminService.isAdminAuthorized(accountValue, requiredPermission.value())
                                                .flatMap(authorized -> {
                                                    if (authorized != null && authorized) {
                                                        return chain.filter(exchange);
                                                    } else {
                                                        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                                                        return Mono.empty();
                                                    }
                                                });
                                    } else {
                                        return chain.filter(exchange);
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
}
