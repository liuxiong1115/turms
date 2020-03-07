package im.turms.turms.pojo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@FieldNameConstants
public final class MessageStatisticsDTO {
    private Long sentMessagesOnAverage;
    private Long acknowledgedMessages;
    private Long acknowledgedMessagesOnAverage;
    private Long sentMessages;

    private List<StatisticsRecordDTO> sentMessagesOnAverageRecords;
    private List<StatisticsRecordDTO> acknowledgedMessagesRecords;
    private List<StatisticsRecordDTO> acknowledgedMessagesOnAverageRecords;
    private List<StatisticsRecordDTO> sentMessagesRecords;
}
