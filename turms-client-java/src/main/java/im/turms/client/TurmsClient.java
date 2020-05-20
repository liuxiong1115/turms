package im.turms.client;

import im.turms.client.driver.TurmsDriver;
import im.turms.client.service.*;

import javax.annotation.Nullable;

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
            @Nullable Integer connectionTimeout,
            @Nullable Integer minRequestsInterval) {
        this(turmsServerUrl, connectionTimeout, minRequestsInterval, null);
    }

    public TurmsClient(ClientOptions options) {
        this(options.turmsServerUrl(),
                options.connectionTimeout(),
                options.minRequestsInterval(),
                options.storageServerUrl());
    }

    // Base constructor
    public TurmsClient(
            @Nullable String turmsServerUrl,
            @Nullable Integer connectionTimeout,
            @Nullable Integer minRequestsInterval,
            @Nullable String storageServerUrl) {
        driver = new TurmsDriver(this, turmsServerUrl, connectionTimeout, minRequestsInterval);
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