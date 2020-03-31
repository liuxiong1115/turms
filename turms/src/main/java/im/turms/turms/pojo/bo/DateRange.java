package im.turms.turms.pojo.bo;

import im.turms.common.TurmsStatusCode;
import im.turms.common.exception.TurmsBusinessException;
import lombok.Data;

import java.util.Date;

@Data
public final class DateRange {
    private final Date start;
    private final Date end;

    public DateRange(Date start, Date end) {
        if (start != null && end != null && end.before(start)) {
            throw TurmsBusinessException.get(TurmsStatusCode.ILLEGAL_ARGUMENTS, "The end date must not be before the start date");
        }
        this.start = start;
        this.end = end;
    }

    public static DateRange of(Date start, Date end) {
        if (start != null || end != null) {
            return new DateRange(start, end);
        } else {
            return null;
        }
    }
}
