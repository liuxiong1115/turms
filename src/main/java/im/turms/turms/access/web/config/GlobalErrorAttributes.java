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

import im.turms.turms.common.TurmsStatusCode;
import im.turms.turms.exception.TurmsBusinessException;
import im.turms.turms.pojo.dto.ResponseDTO;
import org.springframework.boot.web.reactive.error.DefaultErrorAttributes;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;

import javax.validation.ConstraintViolationException;
import java.util.Map;

import static im.turms.turms.common.Constants.STATUS;

@Component
public class GlobalErrorAttributes extends DefaultErrorAttributes {
    @Override
    public Map<String, Object> getErrorAttributes(
            ServerRequest request,
            boolean includeStackTrace) {
        Map<String, Object> errorAttributes = super.getErrorAttributes(request, false);
        Throwable throwable = super.getError(request);
        throwable = translate(throwable);
        if (throwable instanceof TurmsBusinessException) {
            TurmsStatusCode code = ((TurmsBusinessException) throwable).getCode();
            errorAttributes.put(STATUS, code.getHttpStatusCode());
            errorAttributes.put(ResponseDTO.Fields.code, code.getBusinessCode());
            errorAttributes.put(ResponseDTO.Fields.reason, code.getReason());
        } else {
            if (isClientError(errorAttributes)) {
                errorAttributes.put(STATUS, 400);
            }
            errorAttributes.computeIfAbsent(
                    ResponseDTO.Fields.code,
                    key -> TurmsStatusCode.FAILED.getBusinessCode());
            errorAttributes.computeIfAbsent(
                    ResponseDTO.Fields.reason,
                    key -> TurmsStatusCode.FAILED.getReason());
        }
        errorAttributes.remove("error");
        errorAttributes.remove("message");
        return errorAttributes;
    }

    private Throwable translate(Throwable throwable) {
        if (throwable instanceof ConstraintViolationException
                || throwable instanceof NullPointerException) {
            return TurmsBusinessException.get(TurmsStatusCode.ILLEGAL_ARGUMENTS);
        } else if (throwable instanceof DuplicateKeyException) {
            return TurmsBusinessException.get(TurmsStatusCode.DUPLICATE_KEY);
        } else {
            return throwable;
        }
    }

    private boolean isClientError(Map<String, Object> errorAttributes) {
        Integer status = (Integer) errorAttributes.get(STATUS);
        if (status == HttpStatus.INTERNAL_SERVER_ERROR.value()) {
            String message = errorAttributes.get("message").toString();
            return message.contains("WebFlux") || message.contains("cast");
        } else {
            return false;
        }
    }
}
