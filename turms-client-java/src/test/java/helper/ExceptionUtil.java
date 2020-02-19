package helper;

import im.turms.common.TurmsStatusCode;
import im.turms.common.exception.TurmsBusinessException;

import java.util.concurrent.ExecutionException;

public class ExceptionUtil {
    private ExceptionUtil() {
    }

    public static boolean isTurmsStatusCode(ExecutionException throwable, TurmsStatusCode code) {
        Throwable cause = throwable.getCause();
        return isTurmsStatusCode(cause, code);
    }

    public static boolean isTurmsStatusCode(Throwable throwable, TurmsStatusCode code) {
        if (throwable instanceof TurmsBusinessException) {
            return ((TurmsBusinessException) throwable).getCode() == code;
        }
        return false;
    }
}
