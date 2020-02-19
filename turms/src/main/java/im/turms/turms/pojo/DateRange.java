package im.turms.turms.pojo;

import im.turms.common.TurmsStatusCode;
import im.turms.common.exception.TurmsBusinessException;
import lombok.Data;

import java.util.Date;

@Data
public class DateRange {
    private Date start;
    private Date end;

    public DateRange(Date start, Date end) {
        if (start != null && end != null && end.before(start)) {
            throw TurmsBusinessException.get(TurmsStatusCode.ILLEGAL_ARGUMENTS);
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
