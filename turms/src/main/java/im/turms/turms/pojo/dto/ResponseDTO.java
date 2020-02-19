package im.turms.turms.pojo.dto;


import com.fasterxml.jackson.annotation.JsonIgnore;
import im.turms.common.TurmsStatusCode;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;

import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
@FieldNameConstants
public class ResponseDTO<T> {
    /**
     * For now, "code" and "reason" are 
     */
    private Integer code;
    private String reason;
    private Date timestamp;
    private T data;

    @JsonIgnore
    private Integer httpCode;

    public ResponseDTO(TurmsStatusCode turmsStatusCode, T data) {
        this.code = turmsStatusCode.getBusinessCode();
        this.reason = turmsStatusCode.getReason();
        this.timestamp = new Date();
        this.data = data;
        this.httpCode = turmsStatusCode.getHttpStatusCode();
    }
}