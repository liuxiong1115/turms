package im.turms.turms.common;

import im.turms.turms.exception.TurmsBusinessException;

import javax.annotation.Nullable;
import javax.validation.constraints.NotEmpty;
import java.util.Collection;
import java.util.Date;
import java.util.Map;

public class Validator {
    private Validator() {
    }

    public static void throwIfAllNull(Object... array) {
        if (areAllNull(array)) {
            throw TurmsBusinessException.get(TurmsStatusCode.ILLEGAL_ARGUMENTS);
        }
    }

    public static void throwIfAfterWhenNotNull(@Nullable Date startDate, @Nullable Date endDate) {
        if (startDate == null || endDate == null) {
            return;
        }
        if (endDate.before(startDate)) {
            throw TurmsBusinessException.get(TurmsStatusCode.ILLEGAL_ARGUMENTS);
        }
    }

    public static void throwIfAnyFalsy(@NotEmpty Object... array) {
        for (Object o : array) {
            if (o == null) {
                throw TurmsBusinessException.get(TurmsStatusCode.ILLEGAL_ARGUMENTS);
            } else {
                if (o instanceof String) {
                    if (((String) o).isBlank()) {
                        throw TurmsBusinessException.get(TurmsStatusCode.ILLEGAL_ARGUMENTS);
                    }
                } else if (o instanceof Collection) {
                    if (((Collection) o).isEmpty()) {
                        throw TurmsBusinessException.get(TurmsStatusCode.ILLEGAL_ARGUMENTS);
                    }
                }
            }
        }
    }

    public static void throwIfAnyNull(@NotEmpty Object... array) {
        for (Object o : array) {
            if (o == null) {
                throw TurmsBusinessException.get(TurmsStatusCode.ILLEGAL_ARGUMENTS);
            }
        }
    }

    public static void throwIfAllFalsy(@NotEmpty Object... array) {
        for (Object o : array) {
            if (o != null) {
                if (o instanceof String) {
                    if (!((String) o).isBlank()) {
                        return;
                    }
                } else if (o instanceof Collection) {
                    if (!((Collection) o).isEmpty()) {
                        return;
                    }
                } else {
                    return;
                }
            }
        }
        throw TurmsBusinessException.get(TurmsStatusCode.ILLEGAL_ARGUMENTS);
    }

    public static boolean areAllNull(Object... array) {
        if (array == null) {
            return true;
        } else {
            for (Object o : array) {
                if (o != null) {
                    return false;
                }
            }
        }
        return true;
    }

    public static void throwIfFalseAndAllNull(boolean bool, Object... array) {
        if (!bool && areAllNull(array)) {
            throw TurmsBusinessException.get(TurmsStatusCode.ILLEGAL_ARGUMENTS);
        }
    }

    public static void throwIfEmpty(Object object) {
        if (object instanceof Map) {
            if (((Map) object).isEmpty()) {
                throw TurmsBusinessException.get(TurmsStatusCode.ILLEGAL_ARGUMENTS);
            }
        } else if (object instanceof Collection) {
            if (((Collection) object).isEmpty()) {
                throw TurmsBusinessException.get(TurmsStatusCode.ILLEGAL_ARGUMENTS);
            }
        }
    }
}
