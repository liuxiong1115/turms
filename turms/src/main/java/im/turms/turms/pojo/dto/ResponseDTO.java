package im.turms.turms.pojo.dto;


import com.fasterxml.jackson.annotation.JsonIgnore;
import im.turms.common.TurmsStatusCode;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.experimental.FieldNameConstants;

import java.util.Date;

@Data
@AllArgsConstructor
@FieldNameConstants
public final class ResponseDTO<T> {

    private final Integer code;
    private final String reason;
    private final Date timestamp;
    private final T data;

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