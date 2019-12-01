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
public class UserStatisticsDTO {
    private Long deletedUsers;
    private Long usersWhoSentMessages;
    private Long loggedInUsers;
    private Long maxOnlineUsers;
    private Long registeredUsers;

    private List<StatisticsRecordDTO> deletedUsersRecords;
    private List<StatisticsRecordDTO> usersWhoSentMessagesRecords;
    private List<StatisticsRecordDTO> loggedInUsersRecords;
    private List<StatisticsRecordDTO> maxOnlineUsersRecords;
    private List<StatisticsRecordDTO> registeredUsersRecords;
}
