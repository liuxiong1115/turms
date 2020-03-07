package im.turms.turms.pojo.bo;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public final class GroupQuestionIdAndAnswer {
    private final Long id;
    private final String answer;
}
