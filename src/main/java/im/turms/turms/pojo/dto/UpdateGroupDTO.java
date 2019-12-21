package im.turms.turms.pojo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UpdateGroupDTO {
    private Long typeId;
    private Long creatorId;
    private Long ownerId;
    private String name;
    private String intro;
    private String announcement;
    private String profilePictureUrl;
    private Integer minimumScore;
    private Boolean isActive;
    private Date creationDate;
    private Date deletionDate;
    private Date muteEndDate;
    private Long successorId;
    private Boolean quitAfterTransfer;
}
