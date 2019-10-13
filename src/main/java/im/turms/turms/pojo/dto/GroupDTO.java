package im.turms.turms.pojo.dto;

import lombok.Data;

import java.util.Date;

@Data
public class GroupDTO {
    Date muteEndDate;
    String name;
    String url;
    String intro;
    String announcement;
    Long typeId;
    Long successorId;
    Boolean quitAfterTransfer;
}
