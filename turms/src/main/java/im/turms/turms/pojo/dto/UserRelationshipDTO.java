package im.turms.turms.pojo.dto;

import im.turms.turms.pojo.domain.UserRelationship;
import lombok.AllArgsConstructor;
import lombok.Data;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;
import java.util.Date;
import java.util.Set;

@Data
public final class UserRelationshipDTO {
    private final Key key;
    private final Boolean isBlocked;
    private final Date establishmentDate;
    private final Set<Integer> groupIndexes;

    public UserRelationshipDTO(Long ownerId, Long relatedUserId, Boolean isBlocked, Date establishmentDate, Set<Integer> groupIndexes) {
        this.key = new Key(ownerId, relatedUserId);
        this.isBlocked = isBlocked;
        this.establishmentDate = establishmentDate;
        this.groupIndexes = groupIndexes;
    }

    public static UserRelationshipDTO fromDomain(UserRelationship relationship) {
        return fromDomain(relationship, null);
    }

    public static UserRelationshipDTO fromDomain(@NotNull UserRelationship relationship, @Nullable Set<Integer> groupIndexes) {
        return new UserRelationshipDTO(
                relationship.getKey().getOwnerId(),
                relationship.getKey().getRelatedUserId(),
                relationship.getIsBlocked(),
                relationship.getEstablishmentDate(),
                groupIndexes);
    }

    @Data
    @AllArgsConstructor
    public static final class Key {
        private final Long ownerId;
        private final Long relatedUserId;
    }
}