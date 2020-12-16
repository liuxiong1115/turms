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

package im.turms.server.common.access.http.config;

import im.turms.common.constant.statuscode.TurmsStatusCode;
import im.turms.common.exception.TurmsBusinessException;
import lombok.Data;
import org.springframework.core.io.buffer.DataBufferLimitException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import javax.validation.ConstraintViolationException;
import java.util.Date;

/**
 * @author James Chen
 */
@Data
public class ErrorAttributes {

    private final int status;
    private final int code;
    private final String reason;
    private final Date timestamp;
    private final String exception;

    public ErrorAttributes(Throwable throwable) {
        SimpleErrorAttributes attributes;
        if (throwable instanceof ResponseStatusException) {
            attributes = SimpleErrorAttributes.fromResponseStatusException((ResponseStatusException) throwable);
        } else if (throwable instanceof TurmsBusinessException) {
            attributes = SimpleErrorAttributes.fromTurmsBusinessException((TurmsBusinessException) throwable);
        } else {
            attributes = SimpleErrorAttributes.fromTrivialException(throwable);
        }
        status = attributes.httpStatus;
        code = attributes.statusCode.getBusinessCode();
        reason = attributes.reason;
        timestamp = new Date();
        exception = throwable.getClass().getName();
    }

    @Data
    private static class SimpleErrorAttributes {

        private final int httpStatus;
        private final TurmsStatusCode statusCode;
        private final String reason;

        private static SimpleErrorAttributes fromTurmsBusinessException(TurmsBusinessException exception) {
            TurmsStatusCode statusCode = exception.getCode();
            HttpStatus httpStatus = HttpStatus.valueOf(statusCode.getHttpStatusCode());
            String reason = statusCode.getReason();
            return new SimpleErrorAttributes(httpStatus.value(), statusCode, reason);
        }

        private static SimpleErrorAttributes fromResponseStatusException(ResponseStatusException exception) {
            HttpStatus httpStatus = exception.getStatus();
            String reason = exception.getReason();
            TurmsStatusCode statusCode;
            switch (httpStatus.series()) {
                case INFORMATIONAL:
                case SUCCESSFUL:
                case REDIRECTION:
                    statusCode = TurmsStatusCode.OK;
                    break;
                case CLIENT_ERROR:
                    //TODO
                    statusCode = TurmsStatusCode.SERVER_INTERNAL_ERROR;
                    break;
                case SERVER_ERROR:
                    statusCode = TurmsStatusCode.OK;
                    break;
                default:
                    throw new IllegalStateException("Unexpected value: " + httpStatus.series());
            }
            return new SimpleErrorAttributes(httpStatus.value(), statusCode, reason);
        }

        private static SimpleErrorAttributes fromTrivialException(Throwable throwable) {
            TurmsStatusCode statusCode;
            if (throwable instanceof ConstraintViolationException) {
                statusCode = TurmsStatusCode.ILLEGAL_ARGUMENTS;
            } else if (throwable instanceof DuplicateKeyException) {
                statusCode = TurmsStatusCode.DUPLICATE_KEY;
            } else if (throwable instanceof DataBufferLimitException) {
                statusCode = TurmsStatusCode.FILE_TOO_LARGE;
            } else {
                statusCode = TurmsStatusCode.SERVER_INTERNAL_ERROR;
            }
            String reason = throwable.getMessage();
            return new SimpleErrorAttributes(statusCode.getHttpStatusCode(), statusCode, reason);
        }
    }

}