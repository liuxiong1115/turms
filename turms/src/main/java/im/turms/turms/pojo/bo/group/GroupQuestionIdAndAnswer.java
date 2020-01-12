package im.turms.turms.pojo.bo.group;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class GroupQuestionIdAndAnswer {
    private Long id;
    private String answer;
}
