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

package im.turms.gateway.access.websocket.config;

import im.turms.common.constant.DeviceType;
import im.turms.common.constant.UserStatus;
import im.turms.common.constant.statuscode.TurmsStatusCode;
import im.turms.common.exception.TurmsBusinessException;
import im.turms.gateway.access.websocket.util.HandshakeRequestUtil;
import im.turms.gateway.service.mediator.WorkflowMediator;
import im.turms.server.common.cluster.node.Node;
import im.turms.server.common.property.TurmsPropertiesManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.geo.Point;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.socket.HandshakeInfo;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.server.support.HandshakeWebSocketService;
import org.springframework.web.server.MethodNotAllowedException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.ServerWebInputException;
import org.springframework.web.server.handler.ExceptionHandlingWebHandler;
import reactor.core.publisher.Mono;

import javax.validation.constraints.NotNull;
import java.net.InetSocketAddress;
import java.net.URI;
import java.security.Principal;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * @author James Chen
 */
@Component
public class TurmsHandshakeWebSocketService extends HandshakeWebSocketService {

    /**
     * Use the "X-" prefix as the prefix of custom headers.
     * Though the prefix is deprecated as mentioned in https://tools.ietf.org/html/rfc6648,
     * we still think it's a good practice
     */
    private static final String RESPONSE_HEADER_NODE_IDENTITY = "X-API-Identity";
    private static final String RESPONSE_HEADER_TURMS_STATUS_CODE = "X-API-Code";
    private static final Map<TurmsStatusCode, String> CODE_STRING_POOL = new EnumMap<>(TurmsStatusCode.class);

    private final Node node;
    private final TurmsPropertiesManager turmsPropertiesManager;
    private final WorkflowMediator workflowMediator;
    private final boolean useOperatingSystemClassAsDefaultDeviceType;
    private final boolean locationEnabled;

    private List<String> identityList;

    @Autowired
    public TurmsHandshakeWebSocketService(
            Node node,
            TurmsPropertiesManager turmsPropertiesManager,
            WorkflowMediator workflowMediator) {
        this.node = node;
        this.turmsPropertiesManager = turmsPropertiesManager;
        this.workflowMediator = workflowMediator;
        useOperatingSystemClassAsDefaultDeviceType = turmsPropertiesManager.getLocalProperties().getGateway().getSession().isUseOperatingSystemClassAsDefaultDeviceType();
        locationEnabled = turmsPropertiesManager.getLocalProperties().getLocation().isEnabled();
        identityList = getNewIdentityList();
        turmsPropertiesManager.addListeners(turmsProperties -> identityList = getNewIdentityList());
    }

    /**
     * Authenticate during the handshake to avoid wasting resources.
     *
     * @see HandshakeWebSocketService#handleRequest(org.springframework.web.server.ServerWebExchange, org.springframework.web.reactive.socket.WebSocketHandler)
     * @see ExceptionHandlingWebHandler
     */
    @Override
    public Mono<Void> handleRequest(ServerWebExchange exchange, WebSocketHandler handler) {
        // 1. Validate request
        ServerHttpRequest request = exchange.getRequest();
        HttpMethod method = request.getMethod();
        HttpHeaders headers = request.getHeaders();
        if (HttpMethod.GET != method) {
            return Mono.error(new MethodNotAllowedException(
                    request.getMethodValue(), Collections.singleton(HttpMethod.GET)));
        }
        if (!"WebSocket".equalsIgnoreCase(headers.getUpgrade())) {
            return handleBadRequest(exchange, "Invalid 'Upgrade' header: " + headers);
        }
        List<String> connectionValue = headers.getConnection();
        if (!connectionValue.contains("Upgrade") && !connectionValue.contains("upgrade")) {
            return handleBadRequest(exchange, "Invalid 'Connection' header: " + headers);
        }
        String key = headers.getFirst(com.google.common.net.HttpHeaders.SEC_WEBSOCKET_KEY);
        if (key == null) {
            return handleBadRequest(exchange, "Missing \"Sec-WebSocket-Key\" header");
        }

        // 2. Prepare and validate business data
        Long requestId = HandshakeRequestUtil.parseRequestIdFromHeadersOrCookies(request);
        Long userId = HandshakeRequestUtil.parseUserIdFromHeadersOrCookies(request);
        Map<String, String> deviceDetails = HandshakeRequestUtil.parseDeviceDetailsFromHeaders(request);
        DeviceType loggingInDeviceType = HandshakeRequestUtil.parseDeviceTypeFromHeadersOrCookies(
                request,
                deviceDetails,
                useOperatingSystemClassAsDefaultDeviceType);
        if (userId == null) {
            return rejectUpgradeRequest(exchange, TurmsStatusCode.UNAUTHORIZED, requestId, userId, loggingInDeviceType);
        }
        if (!node.isActive()) {
            return rejectUpgradeRequest(exchange, TurmsStatusCode.UNAVAILABLE, requestId, userId, loggingInDeviceType);
        }
        if (identityList != null) {
            exchange.getResponse().getHeaders().put(RESPONSE_HEADER_NODE_IDENTITY, identityList);
        }
        String password = HandshakeRequestUtil.parsePasswordFromHeadersOrCookies(request);
        UserStatus userStatus = HandshakeRequestUtil.parseUserStatusFromHeadersOrCookies(request);
        Point position = locationEnabled
                ? HandshakeRequestUtil.parseLocationFromHeadersOrCookies(request)
                : null;
        String ip = HandshakeRequestUtil.parseIp(request);

        // 3. Try to login
        return workflowMediator.processLoginRequest(userId, password, loggingInDeviceType, userStatus, position, ip, deviceDetails)
                .then(acceptUpgradeRequest(exchange, handler, userId, loggingInDeviceType))
                .onErrorResume(TurmsBusinessException.class, e -> rejectUpgradeRequest(exchange, e.getCode(), requestId, userId, loggingInDeviceType));
    }

    private Mono<Void> acceptUpgradeRequest(
            @NotNull ServerWebExchange exchange,
            @NotNull WebSocketHandler handler,
            @NotNull Long userId,
            @NotNull DeviceType loggingInDeviceType) {
        // Store the identity for im.turms.gateway.access.websocket.config.TurmsWebSocketHandler.handle
        Map<String, Object> attributes = Map.of(HandshakeRequestUtil.USER_ID_FIELD, userId,
                HandshakeRequestUtil.DEVICE_TYPE_FIELD, loggingInDeviceType);
        return getUpgradeStrategy().upgrade(exchange, handler, null,
                () -> createHandshakeInfo(exchange, attributes));
    }

    private Mono<Void> rejectUpgradeRequest(
            @NotNull ServerWebExchange exchange,
            @NotNull TurmsStatusCode statusCode,
            @Nullable Long requestId,
            @Nullable Long userId,
            @Nullable DeviceType loggingInDeviceType) {
        HttpHeaders headers = exchange.getResponse().getHeaders();
        String headerCodeValue = CODE_STRING_POOL.computeIfAbsent(statusCode, key -> Integer.toString(key.getBusinessCode()));
        headers.set(RESPONSE_HEADER_TURMS_STATUS_CODE, headerCodeValue);
        ResponseStatusException exception = new ResponseStatusException(HttpStatus.valueOf(statusCode.getHttpStatusCode()));
        return workflowMediator.rejectLoginRequest(statusCode, userId, loggingInDeviceType, requestId)
                .then(Mono.error(exception));
    }

    private List<String> getNewIdentityList() {
        String identity = turmsPropertiesManager.getLocalProperties().getGateway().getDiscovery().getIdentity();
        return StringUtils.isEmpty(identity) ? null : List.of(identity);
    }

    private HandshakeInfo createHandshakeInfo(ServerWebExchange exchange, Map<String, Object> attributes) {
        ServerHttpRequest request = exchange.getRequest();
        URI uri = request.getURI();
        Mono<Principal> principal = exchange.getPrincipal();
        String logPrefix = exchange.getLogPrefix();
        InetSocketAddress remoteAddress = request.getRemoteAddress();
        return new HandshakeInfo(uri, HttpHeaders.EMPTY, principal, null, remoteAddress, attributes, logPrefix);
    }

    private Mono<Void> handleBadRequest(ServerWebExchange exchange, String reason) {
        if (logger.isDebugEnabled()) {
            logger.debug(exchange.getLogPrefix() + reason);
        }
        return Mono.error(new ServerWebInputException(reason));
    }

}
