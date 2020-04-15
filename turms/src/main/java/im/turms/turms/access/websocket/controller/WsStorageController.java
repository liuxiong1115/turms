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

package im.turms.turms.access.websocket.controller;

import com.google.protobuf.StringValue;
import im.turms.common.TurmsStatusCode;
import im.turms.common.constant.ContentType;
import im.turms.common.exception.TurmsBusinessException;
import im.turms.common.model.dto.notification.TurmsNotification;
import im.turms.common.model.dto.request.TurmsRequest;
import im.turms.common.model.dto.request.storage.DeleteResourceRequest;
import im.turms.common.model.dto.request.storage.QuerySignedGetUrlRequest;
import im.turms.common.model.dto.request.storage.QuerySignedPutUrlRequest;
import im.turms.turms.annotation.websocket.TurmsRequestMapping;
import im.turms.turms.pojo.bo.RequestResult;
import im.turms.turms.pojo.bo.TurmsRequestWrapper;
import im.turms.turms.service.storage.StorageService;
import org.springframework.stereotype.Controller;
import reactor.core.publisher.Mono;

import java.util.function.Function;

@Controller
public class WsStorageController {
    private final StorageService storageService;

    public WsStorageController(StorageService storageService) {
        this.storageService = storageService;
    }

    @TurmsRequestMapping(TurmsRequest.KindCase.QUERY_SIGNED_GET_URL_REQUEST)
    public Function<TurmsRequestWrapper, Mono<RequestResult>> handleQuerySignedGetUrlRequest() {
        return turmsRequestWrapper -> {
            QuerySignedGetUrlRequest querySignedGetUrlRequest = turmsRequestWrapper.getTurmsRequest().getQuerySignedGetUrlRequest();
            ContentType contentType = querySignedGetUrlRequest.getContentType();
            if (contentType != ContentType.UNRECOGNIZED) {
                String keyStr = querySignedGetUrlRequest.hasKeyStr() ? querySignedGetUrlRequest.getKeyStr().getValue() : null;
                Long keyNum = querySignedGetUrlRequest.hasKeyNum() ? querySignedGetUrlRequest.getKeyNum().getValue() : null;
                return storageService.queryPresignedGetUrl(turmsRequestWrapper.getUserId(), contentType, keyStr, keyNum)
                        .map(url -> RequestResult.create(TurmsNotification.Data.newBuilder()
                                .setUrl(StringValue.newBuilder().setValue(url).build())
                                .build()));
            } else {
                throw TurmsBusinessException.get(TurmsStatusCode.ILLEGAL_ARGUMENTS, "The content type must not be UNRECOGNIZED");
            }
        };
    }

    @TurmsRequestMapping(TurmsRequest.KindCase.QUERY_SIGNED_PUT_URL_REQUEST)
    public Function<TurmsRequestWrapper, Mono<RequestResult>> handleQuerySignedPutUrlRequest() {
        return turmsRequestWrapper -> {
            QuerySignedPutUrlRequest querySignedPutUrlRequest = turmsRequestWrapper.getTurmsRequest().getQuerySignedPutUrlRequest();
            ContentType contentType = querySignedPutUrlRequest.getContentType();
            if (contentType != ContentType.UNRECOGNIZED) {
                long contentLength = querySignedPutUrlRequest.getContentLength();
                String keyStr = querySignedPutUrlRequest.hasKeyStr() ? querySignedPutUrlRequest.getKeyStr().getValue() : null;
                Long keyNum = querySignedPutUrlRequest.hasKeyNum() ? querySignedPutUrlRequest.getKeyNum().getValue() : null;
                return storageService.queryPresignedPutUrl(turmsRequestWrapper.getUserId(), contentType, keyStr, keyNum, contentLength)
                        .map(url -> RequestResult.create(TurmsNotification.Data.newBuilder()
                                .setUrl(StringValue.newBuilder().setValue(url).build())
                                .build()));
            } else {
                throw TurmsBusinessException.get(TurmsStatusCode.ILLEGAL_ARGUMENTS, "The content type must not be UNRECOGNIZED");
            }
        };
    }

    @TurmsRequestMapping(TurmsRequest.KindCase.DELETE_RESOURCE_REQUEST)
    public Function<TurmsRequestWrapper, Mono<RequestResult>> handleDeleteResourceRequest() {
        return turmsRequestWrapper -> {
            DeleteResourceRequest deleteResourceRequest = turmsRequestWrapper.getTurmsRequest().getDeleteResourceRequest();
            ContentType contentType = deleteResourceRequest.getContentType();
            if (contentType != ContentType.UNRECOGNIZED) {
                String keyStr = deleteResourceRequest.hasKeyStr() ? deleteResourceRequest.getKeyStr().getValue() : null;
                Long keyNum = deleteResourceRequest.hasKeyNum() ? deleteResourceRequest.getKeyNum().getValue() : null;
                return storageService.deleteResource(turmsRequestWrapper.getUserId(), contentType, keyStr, keyNum)
                        .thenReturn(RequestResult.ok());
            } else {
                throw TurmsBusinessException.get(TurmsStatusCode.ILLEGAL_ARGUMENTS, "The content type must not be UNRECOGNIZED");
            }
        };
    }
}
