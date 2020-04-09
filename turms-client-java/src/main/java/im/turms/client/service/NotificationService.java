package im.turms.client.service;

import im.turms.client.TurmsClient;
import im.turms.common.model.dto.request.TurmsRequest;

import java.util.function.Function;

public class NotificationService {
    private Function<TurmsRequest, Void> onNotification;

    public NotificationService(TurmsClient turmsClient) {
        turmsClient.getDriver()
                .getOnNotificationListeners()
                .add(notification -> {
                    if (onNotification != null && notification.hasRelayedRequest()) {
                        TurmsRequest request = notification.getRelayedRequest();
                        if (!request.hasCreateMessageRequest()) {
                            onNotification.apply(request);
                        }
                    }
                    return null;
                });
    }

    public void setOnNotification(Function<TurmsRequest, Void> onNotification) {
        this.onNotification = onNotification;
    }
}
