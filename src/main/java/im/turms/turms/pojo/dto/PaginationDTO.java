package im.turms.turms.pojo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Collection;

@Data
@AllArgsConstructor
public class PaginationDTO<T> {
    private Long total;
    private Collection<T> records;
}
