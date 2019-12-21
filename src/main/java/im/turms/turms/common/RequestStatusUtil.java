package im.turms.turms.common;

import im.turms.turms.constant.RequestStatus;

public class RequestStatusUtil {
    private RequestStatusUtil() {}

    public static boolean isProcessedByResponder(RequestStatus status) {
        return status == RequestStatus.ACCEPTED
                || status == RequestStatus.DECLINED
                || status == RequestStatus.IGNORED;
    }
}
