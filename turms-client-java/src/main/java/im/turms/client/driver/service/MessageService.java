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
import im.turms.common.constant.statuscode.TurmsStatusCode;
import im.turms.common.exception.TurmsBusinessException;
import im.turms.common.model.dto.notification.TurmsNotification;
import im.turms.common.model.dto.request.TurmsRequest;
import okio.ByteString;

import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author James Chen
 */
public class MessageService {

    private static final Logger LOGGER = Logger.getLogger(MessageService.class.getName());

    private final StateStore stateStore;

    private final Duration minRequestsInterval;
    private final List<Consumer<TurmsNotification>> onNotificationListeners = new LinkedList<>();
    private final HashMap<Long, AbstractMap.SimpleEntry<TurmsRequest, CompletableFuture<TurmsNotification>>> requestMap = new HashMap<>(256);

    public MessageService(StateStore stateStore, Duration minRequestsInterval) {
        this.stateStore = stateStore;
        this.minRequestsInterval = minRequestsInterval;
    }

    // Listeners

    public void addOnNotificationListener(Consumer<TurmsNotification> listener) {
        onNotificationListeners.add(listener);
    }

    private void notifyOnNotificationListener(TurmsNotification notification) {
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
            Date now = new Date();
            boolean isFrequent = minRequestsInterval != null && now.getTime() - stateStore.getLastRequestDate() <= minRequestsInterval.toMillis();
            if (isFrequent) {
                future.completeExceptionally(TurmsBusinessException.get(TurmsStatusCode.CLIENT_REQUESTS_TOO_FREQUENT));
            } else {
                stateStore.setLastRequestDate(now.getTime());
                long requestId = generateRandomId();
                requestBuilder.setRequestId(Int64Value.newBuilder().setValue(requestId).build());
                TurmsRequest request = requestBuilder.build();
                ByteBuffer data = ByteBuffer.wrap(request.toByteArray());
                requestMap.put(requestId, new AbstractMap.SimpleEntry<>(request, future));
                boolean wasEnqueued = stateStore.getWebSocket().send(ByteString.of(data));
                if (!wasEnqueued) {
                    future.completeExceptionally(TurmsBusinessException.get(TurmsStatusCode.MESSAGE_IS_REJECTED));
                }
            }
        } else {
            future.completeExceptionally(TurmsBusinessException.get(TurmsStatusCode.CLIENT_SESSION_HAS_BEEN_CLOSED));
        }
        return future;
    }

    private long generateRandomId() {
        long id;
        do {
            id = ThreadLocalRandom.current().nextLong(1, 16384);
        } while (requestMap.containsKey(id));
        return id;
    }

    public void triggerOnNotificationReceived(TurmsNotification notification) {
        boolean isResponse = !notification.hasRelayedRequest() && notification.hasRequestId();
        if (isResponse) {
            long requestId = notification.getRequestId().getValue();
            AbstractMap.SimpleEntry<TurmsRequest, CompletableFuture<TurmsNotification>> pair = requestMap.remove(requestId);
            if (pair != null) {
                CompletableFuture<TurmsNotification> future = pair.getValue();
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
                    future.completeExceptionally(TurmsBusinessException.get(TurmsStatusCode.FAILED, "Invalid notification: the code is missing"));
                }
            }
        }
        notifyOnNotificationListener(notification);
    }

}
