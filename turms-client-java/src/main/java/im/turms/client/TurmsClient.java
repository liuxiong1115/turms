package im.turms.client;

import im.turms.client.driver.TurmsDriver;
import im.turms.client.service.*;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;

public class TurmsClient {
    private TurmsDriver driver;
    private UserService userService;
    private GroupService groupService;
    private MessageService messageService;
    private StorageService storageService;

    private NotificationService notificationService;

    public TurmsClient(
            @NotNull String turmsServerUrl,
            @Nullable Integer connectionTimeout,
            @Nullable Integer minRequestsInterval,
            @Nullable String storageServerUrl) {
        driver = new TurmsDriver(turmsServerUrl, connectionTimeout, minRequestsInterval);
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