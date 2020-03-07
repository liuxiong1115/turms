package im.turms.turms.pojo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Date;

@Data
@AllArgsConstructor
public final class StatisticsRecordDTO {
    private final Date date;
    private final Long total;
}
