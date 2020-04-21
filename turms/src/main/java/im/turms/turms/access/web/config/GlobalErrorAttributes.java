/*
 * Copyright (C) 2019 The Turms Project
 * https://github.com/turms-im/turms
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package im.turms.turms.access.web.config;

import im.turms.common.TurmsStatusCode;
import im.turms.common.exception.TurmsBusinessException;
import im.turms.turms.pojo.dto.ResponseDTO;
import im.turms.turms.property.TurmsProperties;
import org.springframework.boot.web.reactive.error.DefaultErrorAttributes;
import org.springframework.core.io.buffer.DataBufferLimitException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;

import javax.validation.ConstraintViolationException;
import java.util.Map;

import static im.turms.turms.constant.Common.STATUS;

@Component
public class GlobalErrorAttributes extends DefaultErrorAttributes {
    private static final String ATTR_MESSAGE = "message";
    private static final String ATTR_ERROR = "error";
    private final boolean respondStackTraceIfException;

    public GlobalErrorAttributes(TurmsProperties turmsProperties) {
        respondStackTraceIfException = turmsProperties.getSecurity().isRespondStackTraceIfException();
    }

    @Override
    public Map<String, Object> getErrorAttributes(
            ServerRequest request,
            boolean includeStackTrace) {
        Map<String, Object> errorAttributes = super.getErrorAttributes(request, respondStackTraceIfException);
        Throwable throwable = translate(super.getError(request), errorAttributes);
        if (throwable instanceof TurmsBusinessException) {
            TurmsStatusCode code = ((TurmsBusinessException) throwable).getCode();
            errorAttributes.put(STATUS, code.getHttpStatusCode());
            errorAttributes.put(ResponseDTO.Fields.code, code.getBusinessCode());
            errorAttributes.put(ResponseDTO.Fields.reason, code.getReason());
        } else {
            if (isClientError(errorAttributes)) {
                errorAttributes.put(STATUS, 400);
            }
            errorAttributes.putIfAbsent(ResponseDTO.Fields.code, TurmsStatusCode.FAILED.getBusinessCode());
            errorAttributes.putIfAbsent(ResponseDTO.Fields.reason, errorAttributes.get(ATTR_MESSAGE));
        }
        errorAttributes.remove(ATTR_ERROR);
        errorAttributes.remove(ATTR_MESSAGE);
        return errorAttributes;
    }

    private Throwable translate(Throwable throwable, Map<String, Object> errorAttributes) {
        if (throwable instanceof ConstraintViolationException
                || throwable instanceof NullPointerException) {
            return TurmsBusinessException.get(TurmsStatusCode.ILLEGAL_ARGUMENTS, errorAttributes.get(ATTR_MESSAGE).toString());
        } else if (throwable instanceof DuplicateKeyException) {
            return TurmsBusinessException.get(TurmsStatusCode.DUPLICATE_KEY);
        } else if (throwable instanceof DataBufferLimitException) {
            return TurmsBusinessException.get(TurmsStatusCode.FILE_TOO_LARGE);
        } else {
            return throwable;
        }
    }

    private boolean isClientError(Map<String, Object> errorAttributes) {
        Integer status = (Integer) errorAttributes.get(STATUS);
        if (status == HttpStatus.INTERNAL_SERVER_ERROR.value()) {
            String message = errorAttributes.get(ATTR_MESSAGE).toString();
            return message.contains("WebFlux") || message.contains("cast");
        } else {
            return false;
        }
    }
}
