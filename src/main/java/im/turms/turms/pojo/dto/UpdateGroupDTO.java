package im.turms.turms.pojo.dto;

import lombok.Data;

import java.util.Date;

@Data
public class UpdateGroupDTO {
    Date muteEndDate;
    String name;
    String url;
    String intro;
    String announcement;
    Integer minimumScore;
    Long typeId;
    Long successorId;
    Boolean quitAfterTransfer;
}
