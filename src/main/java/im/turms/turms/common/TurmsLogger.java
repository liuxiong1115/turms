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

package im.turms.turms.common;

import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.validation.constraints.NotNull;
import java.net.InetAddress;

public class TurmsLogger {
    private static final Logger logger = LoggerFactory.getLogger(TurmsLogger.class);

    public static Logger getLogger() {
        return logger;
    }

    public static void logCurrentServerRunningAt(@NonNull InetAddress inetAddress, int port) {
        logger.info("The current node is running at {}:{}", inetAddress.getHostAddress(), port);
    }

    public static void logCurrentNodeLoggingInToCluster(@NonNull String address) {
        logger.info("The current node running at {} is trying to log in to the cluster_info.", address);
    }

    public static void logCurrentNodeFailedToLogInToCluster(@NonNull String address) {
        logger.error("The current node running at {} failed to log in to the cluster_info.", address);
    }

    public static void logCurrentNodeLoggedInToCluster(@NonNull String address) {
        logger.info("The current node running at {} logged in to the cluster_info.", address);
    }

    public static void logCurrentNodeLoggingOutOfCluster(@NonNull String address) {
        logger.info("The current node running at {} is logging out of the cluster_info.", address);
    }

    public static void logCurrentNodeFailedToLogOutOfCluster(@NonNull String address) {
        logger.error("The current node running at {} failed to log out of the cluster_info.", address);
    }

    public static void logCurrentNodeLoggedOutOfCluster(@NonNull String address) {
        logger.info("The current node running at {} logged out of the cluster_info.", address);
    }

    public static void logConnectingPeer(@NonNull String peerAddress) {
        logger.info("Trying to connect to the peer running at {}", peerAddress);
    }

    public static void logThrowable(Throwable t) {
        if (t != null) {
            logger.error(t.toString());
        }
    }

    public static void logServerErrorUserIdInSessionRetrievalFailure() {
        logger.error("Cannot retrieve userId from the session. Check if developers forgot to addOnlineUser userId into \"uid\" in the session.");
    }

    public static void logServerErrorDeviceTypeInSessionRetrievalFailure() {
        logger.error("Cannot retrieve deviceType from the session. Check if developers forgot to addOnlineUser deviceType into \"uid\" in the session.");
    }

    public static void logServerErrorUserOnlineStatusInSessionRetrievalFailure() {
        logger.error("Cannot get the default user online status from the session. Check if developers forgot to addOnlineUser userOnlineStatus into \"uid\" in the session.");
    }

    public static void logJson(@NotNull String title, @NotNull Object map) {
        try {
            String json = Constants.MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(map);
            logger.info("{}: {}", title, json);
        } catch (JsonProcessingException ignored) {

        }
    }

    public static void logObject(@NotNull String title, @NotNull Object data) {
        logger.info("{}: {}", title, data);
    }

    public static void log(String text) {
        logger.info(text);
    }
}
