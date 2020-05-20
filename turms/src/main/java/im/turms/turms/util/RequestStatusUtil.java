package im.turms.turms.util;

import im.turms.common.constant.RequestStatus;
import im.turms.turms.pojo.domain.GroupInvitation;
import org.springframework.data.mongodb.core.query.Update;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;
import java.util.Date;
import java.util.Objects;

public class RequestStatusUtil {
    private RequestStatusUtil() {
    }

    private static boolean isProcessedByResponder(@Nullable RequestStatus status) {
        return status == RequestStatus.ACCEPTED
                || status == RequestStatus.DECLINED
                || status == RequestStatus.IGNORED;
    }

    public static Date getResponseDateBasedOnStatus(
            @Nullable RequestStatus status,
            @Nullable Date responseDate,
            @Nullable Date now) {
        if (RequestStatusUtil.isProcessedByResponder(status)) {
            if (responseDate == null) {
                responseDate = Objects.requireNonNullElseGet(now, Date::new);
            }
            return responseDate;
        } else {
            return null;
        }
    }

    public static Update updateResponseDateBasedOnStatus(@NotNull Update update, @Nullable RequestStatus status, @Nullable Date responseDate) {
        if (status != null) {
            if (RequestStatusUtil.isProcessedByResponder(status)) {
                if (responseDate == null) {
                    responseDate = new Date();
                }
                update.set(GroupInvitation.Fields.RESPONSE_DATE, responseDate);
            } else {
                update.unset(GroupInvitation.Fields.RESPONSE_DATE);
            }
        }
        return update;
    }
}