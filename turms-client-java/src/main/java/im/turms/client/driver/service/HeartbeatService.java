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

import im.turms.client.driver.StateStore;
import im.turms.common.constant.statuscode.TurmsStatusCode;
import im.turms.common.exception.TurmsBusinessException;
import java8.util.concurrent.CompletableFuture;
import okio.ByteString;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.*;

/**
 * @author James Chen
 */
public class HeartbeatService {

    private static final int DEFAULT_HEARTBEAT_INTERVAL = 120 * 1000;

    private final StateStore stateStore;

    private final int heartbeatInterval;
    private final int minRequestInterval;

    private final ScheduledExecutorService heartbeatTimer = Executors.newSingleThreadScheduledExecutor();
    private ScheduledFuture<?> heartbeatTimerFuture;

    private final ConcurrentLinkedQueue<CompletableFuture<Void>> heartbeatFutures = new ConcurrentLinkedQueue<>();

    public HeartbeatService(@NotNull StateStore stateStore, @Nullable Integer minRequestInterval, @Nullable Integer heartbeatInterval) {
        this.stateStore = stateStore;
        this.minRequestInterval = minRequestInterval != null ? minRequestInterval : 0;
        this.heartbeatInterval = heartbeatInterval != null ? heartbeatInterval : DEFAULT_HEARTBEAT_INTERVAL;
    }

    public synchronized void start() {
        if (heartbeatTimerFuture == null || heartbeatTimerFuture.isDone()) {
            heartbeatTimerFuture = heartbeatTimer.scheduleAtFixedRate(
                    (this::checkAndSendHeartbeatTask),
                    heartbeatInterval,
                    heartbeatInterval,
                    TimeUnit.MILLISECONDS);
        }
    }

    public void stop() {
        if (heartbeatTimerFuture != null) {
            heartbeatTimerFuture.cancel(true);
        }
    }

    public void reset() {
        stop();
        start();
    }

    public CompletableFuture<Void> send() {
        CompletableFuture<Void> future = new CompletableFuture<>();
        if (stateStore.isConnected()) {
            boolean wasEnqueued = stateStore.getWebSocket().send(ByteString.EMPTY);
            if (wasEnqueued) {
                heartbeatFutures.offer(future);
            } else {
                future.completeExceptionally(TurmsBusinessException.get(TurmsStatusCode.MESSAGE_IS_REJECTED));
            }
        } else {
            future.completeExceptionally(TurmsBusinessException.get(TurmsStatusCode.CLIENT_SESSION_HAS_BEEN_CLOSED));
        }
        return future;
    }


    public void completeHeartbeatFutures() {
        while (true) {
            CompletableFuture<Void> future = heartbeatFutures.poll();
            if (future != null) {
                future.complete(null);
            } else {
                return;
            }
        }
    }

    public void rejectHeartbeatFutures(Throwable throwable) {
        while (true) {
            CompletableFuture<Void> future = heartbeatFutures.poll();
            if (future != null) {
                future.completeExceptionally(throwable);
            } else {
                return;
            }
        }
    }

    private void checkAndSendHeartbeatTask() {
        long difference = System.currentTimeMillis() - stateStore.getLastRequestDate();
        if (difference > minRequestInterval) {
            send();
        }
    }

}
