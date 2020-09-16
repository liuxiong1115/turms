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

package im.turms.client;

import im.turms.client.driver.TurmsDriver;
import im.turms.client.service.*;

import javax.annotation.Nullable;
import java.time.Duration;

/**
 * @author James Chen
 */
public class TurmsClient {

    private final TurmsDriver driver;
    private final UserService userService;
    private final GroupService groupService;
    private final MessageService messageService;
    private final StorageService storageService;

    private final NotificationService notificationService;

    public TurmsClient() {
        this(null, null, null, null, null, null);
    }

    public TurmsClient(@Nullable String url) {
        this(url, null, null);
    }

    public TurmsClient(
            @Nullable String url,
            @Nullable Duration connectTimeout) {
        this(url, connectTimeout, null);
    }

    public TurmsClient(
            @Nullable String url,
            @Nullable Duration connectTimeout,
            @Nullable Duration requestTimeout) {
        this(url, connectTimeout, requestTimeout, null);
    }

    public TurmsClient(
            @Nullable String url,
            @Nullable Duration connectTimeout,
            @Nullable Duration requestTimeout,
            @Nullable Duration minRequestInterval) {
        this(url, connectTimeout, requestTimeout, minRequestInterval, null);
    }

    public TurmsClient(
            @Nullable String url,
            @Nullable Duration connectTimeout,
            @Nullable Duration requestTimeout,
            @Nullable Duration minRequestInterval,
            @Nullable Duration heartbeatInterval) {
        this(url, connectTimeout, requestTimeout, minRequestInterval, heartbeatInterval, null);
    }

    public TurmsClient(
            @Nullable String url,
            @Nullable String storageServerUrl) {
        this(url, null, null, null, null, storageServerUrl);
    }

    public TurmsClient(ClientOptions options) {
        this(options.url(),
                options.connectTimeout(),
                options.heartbeatInterval(),
                options.minRequestInterval(),
                options.heartbeatInterval(),
                options.storageServerUrl());
    }

    /**
     * Base constructor
     */
    public TurmsClient(
            @Nullable String url,
            @Nullable Duration connectTimeout,
            @Nullable Duration requestTimeout,
            @Nullable Duration minRequestInterval,
            @Nullable Duration heartbeatInterval,
            @Nullable String storageServerUrl) {
        driver = new TurmsDriver(url, connectTimeout, requestTimeout, minRequestInterval, heartbeatInterval);
        userService = new UserService(this);
        groupService = new GroupService(this);
        messageService = new MessageService(this);
        notificationService = new NotificationService(this);
        storageService = new StorageService(this, storageServerUrl);
    }

    public TurmsDriver getDriver() {
        return driver;
    }

    public UserService getUserService() {
        return userService;
    }

    public GroupService getGroupService() {
        return groupService;
    }

    public MessageService getMessageService() {
        return messageService;
    }

    public StorageService getStorageService() {
        return storageService;
    }

    public NotificationService getNotificationService() {
        return notificationService;
    }

}