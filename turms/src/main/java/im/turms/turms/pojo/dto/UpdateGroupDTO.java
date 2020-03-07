package im.turms.turms.pojo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Date;

@Data
@AllArgsConstructor
public final class UpdateGroupDTO {
    private final Long typeId;
    private final Long creatorId;
    private final Long ownerId;
    private final String name;
    private final String intro;
    private final String announcement;
    private final String profilePictureUrl;
    private final Integer minimumScore;
    private final Boolean isActive;
    private final Date creationDate;
    private final Date deletionDate;
    private final Date muteEndDate;
    private final Long successorId;
    private final Boolean quitAfterTransfer;
}
