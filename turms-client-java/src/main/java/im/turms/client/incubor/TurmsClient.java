package im.turms.client.incubor;

import im.turms.client.incubor.driver.TurmsDriver;
import im.turms.client.incubor.service.GroupService;
import im.turms.client.incubor.service.MessageService;
import im.turms.client.incubor.service.UserService;
import lombok.Getter;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;

@Getter
public class TurmsClient {
    private TurmsDriver driver;
    private UserService userService;
    private GroupService groupService;
    private MessageService messageService;

    TurmsClient(
            @NotNull String url,
            @Nullable Integer connectionTimeout,
            @Nullable Integer minRequestsInterval,
            @Nullable String httpUrl,
            @Nullable Boolean queryReasonWhenLoginFailed,
            @Nullable Boolean queryReasonWhenDisconnected) {
        driver = new TurmsDriver(
                this,
                url,
                connectionTimeout,
                minRequestsInterval,
                httpUrl,
                queryReasonWhenLoginFailed,
                queryReasonWhenDisconnected);
        userService = new UserService(this);
        groupService = new GroupService(this);
        messageService = new MessageService(this);
    }
}