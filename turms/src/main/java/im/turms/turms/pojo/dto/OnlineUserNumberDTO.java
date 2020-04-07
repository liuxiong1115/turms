package im.turms.turms.pojo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Map;
import java.util.UUID;

@Data
@AllArgsConstructor
public final class OnlineUserNumberDTO {
    private final Integer total;
    private final Map<UUID, Integer> numberById;
}
