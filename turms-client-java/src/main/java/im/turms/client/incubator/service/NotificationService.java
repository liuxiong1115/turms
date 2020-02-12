package im.turms.client.incubator.service;

import im.turms.client.incubator.TurmsClient;
import im.turms.turms.pojo.request.TurmsRequest;

import java.util.function.BiFunction;

public class NotificationService {
    private TurmsClient turmsClient;
    public BiFunction<TurmsRequest, Long, Void> onNotification;

    public NotificationService(TurmsClient turmsClient) {
        this.turmsClient = turmsClient;
        this.turmsClient.getDriver()
                .getOnNotificationListeners()
                .add(notification -> {
                    if (onNotification != null && notification.hasRelayedRequest()) {
                        Long requesterId = notification.hasRequesterId() ?
                                notification.getRequesterId().getValue() : null;
                        onNotification.apply(notification.getRelayedRequest(), requesterId);
                    }
                    return null;
                });
    }
}
