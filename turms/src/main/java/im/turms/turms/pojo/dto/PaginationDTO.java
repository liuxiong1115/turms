package im.turms.turms.pojo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Collection;

@Data
@AllArgsConstructor
public final class PaginationDTO<T> {
    private final Long total;
    private final Collection<T> records;
}
