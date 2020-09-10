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
        this(null, null, null, null);
    }

    public TurmsClient(@Nullable String turmsServerUrl) {
        this(turmsServerUrl, null, null, null);
    }

    public TurmsClient(
            @Nullable String turmsServerUrl,
            @Nullable Duration connectTimeout,
            @Nullable Duration minRequestsInterval) {
        this(turmsServerUrl, connectTimeout, minRequestsInterval, null);
    }

    public TurmsClient(ClientOptions options) {
        this(options.url(),
                options.connectTimeout(),
                options.minRequestsInterval(),
                options.storageServerUrl());
    }

    // Base constructor
    public TurmsClient(
            @Nullable String url,
            @Nullable Duration connectTimeout,
            @Nullable Duration minRequestsInterval,
            @Nullable String storageServerUrl) {
        driver = new TurmsDriver(url, connectTimeout, minRequestsInterval);
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