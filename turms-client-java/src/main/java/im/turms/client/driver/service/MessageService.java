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

package im.turms.client.driver.service;

import com.google.protobuf.Int64Value;
import im.turms.client.driver.StateStore;
import im.turms.client.util.FutureUtil;
import im.turms.common.constant.statuscode.TurmsStatusCode;
import im.turms.common.exception.TurmsBusinessException;
import im.turms.common.model.dto.notification.TurmsNotification;
import im.turms.common.model.dto.request.TurmsRequest;
import java8.util.concurrent.CompletableFuture;
import java8.util.concurrent.ThreadLocalRandom;
import java8.util.function.Consumer;
import okio.ByteString;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.ByteBuffer;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import static im.turms.client.driver.TurmsDriver.scheduledService;

/**
 * @author James Chen
 */
public class MessageService {

    private static final Logger LOGGER = Logger.getLogger(MessageService.class.getName());
    private static final int DEFAULT_REQUEST_TIMEOUT = 30 * 1000;

    private final StateStore stateStore;

    private final int requestTimeout;
    private final Integer minRequestInterval;
    private final List<Consumer<TurmsNotification>> onNotificationListeners = new LinkedList<>();
    private final ConcurrentHashMap<Long, RequestFuturePair> requestMap = new ConcurrentHashMap<>(256);

    public MessageService(@NotNull StateStore stateStore, @Nullable Integer requestTimeout, @Nullable Integer minRequestInterval) {
        this.stateStore = stateStore;
        this.requestTimeout = requestTimeout != null ? requestTimeout : DEFAULT_REQUEST_TIMEOUT;
        this.minRequestInterval = minRequestInterval;
    }

    // Listeners

    public void addOnNotificationListener(Consumer<TurmsNotification> listener) {
        onNotificationListeners.add(listener);
    }

    private void notifyOnNotificationListeners(TurmsNotification notification) {
        for (Consumer<TurmsNotification> listener : onNotificationListeners) {
            try {
                listener.accept(notification);
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "", e);
            }
        }
    }

    // Request and notification

    public CompletableFuture<TurmsNotification> sendRequest(TurmsRequest.Builder requestBuilder) {
        CompletableFuture<TurmsNotification> future = new CompletableFuture<>();
        if (stateStore.isConnected()) {
            boolean isSignalRequest = requestBuilder.getKindCase() == TurmsRequest.KindCase.ACK_REQUEST;
            if (!isSignalRequest) {
                Date now = new Date();
                boolean isFrequent = minRequestInterval != null && now.getTime() - stateStore.getLastRequestDate() <= minRequestInterval;
                if (isFrequent) {
                    future.completeExceptionally(TurmsBusinessException.get(TurmsStatusCode.CLIENT_REQUESTS_TOO_FREQUENT));
                    return future;
                } else {
                    stateStore.setLastRequestDate(now.getTime());
                }
            }
            while (true) {
                long requestId = generateRandomId();
                TurmsRequest request = requestBuilder
                        .setRequestId(Int64Value.newBuilder().setValue(requestId).build())
                        .build();
                RequestFuturePair newRequest = new RequestFuturePair(request, future);
                RequestFuturePair currentRequest = requestMap.putIfAbsent(requestId, newRequest);
                boolean wasRequestAbsent = newRequest == currentRequest;
                if (wasRequestAbsent) {
                    ByteBuffer data = ByteBuffer.wrap(request.toByteArray());
                    boolean wasEnqueued = stateStore.getWebSocket().send(ByteString.of(data));
                    if (wasEnqueued) {
                        if (requestTimeout > 0) {
                            return FutureUtil.timeout(future, requestTimeout, scheduledService, e -> requestMap.remove(requestId));
                        }
                    } else {
                        future.completeExceptionally(TurmsBusinessException.get(TurmsStatusCode.MESSAGE_IS_REJECTED));
                    }
                    return future;
                }
            }
        } else {
            future.completeExceptionally(TurmsBusinessException.get(TurmsStatusCode.CLIENT_SESSION_HAS_BEEN_CLOSED));
            return future;
        }
    }

    public void didReceiveNotification(TurmsNotification notification) {
        boolean isResponse = !notification.hasRelayedRequest() && notification.hasRequestId();
        if (isResponse) {
            long requestId = notification.getRequestId().getValue();
            RequestFuturePair pair = requestMap.remove(requestId);
            if (pair != null) {
                CompletableFuture<TurmsNotification> future = pair.future;
                if (notification.hasCode()) {
                    int code = notification.getCode().getValue();
                    if (TurmsStatusCode.isSuccessCode(code)) {
                        future.complete(notification);
                    } else {
                        TurmsBusinessException exception = TurmsBusinessException.get(notification);
                        if (exception != null) {
                            future.completeExceptionally(exception);
                        } else {
                            LOGGER.log(Level.WARNING, "Unknown status code");
                        }
                    }
                } else {
                    future.completeExceptionally(TurmsBusinessException.get(TurmsStatusCode.INVALID_DATA, "Invalid notification: the code is missing"));
                }
            }
        }
        notifyOnNotificationListeners(notification);
    }

    private long generateRandomId() {
        long id;
        do {
            id = ThreadLocalRandom.current().nextLong(1, 16384);
        } while (requestMap.containsKey(id));
        return id;
    }

    private static class RequestFuturePair {
        TurmsRequest request;
        CompletableFuture<TurmsNotification> future;

        public RequestFuturePair(TurmsRequest request, CompletableFuture<TurmsNotification> future) {
            this.request = request;
            this.future = future;
        }
    }

}
