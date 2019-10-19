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

package im.turms.turms.pojo.bo;

import im.turms.turms.common.TurmsStatusCode;
import im.turms.turms.pojo.request.TurmsRequest;
import im.turms.turms.pojo.response.Int64Values;
import im.turms.turms.pojo.response.TurmsResponse;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;

import javax.annotation.Nullable;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.Collections;
import java.util.Set;

@Data
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class RequestResult {
    public static final RequestResult NOT_FOUND = new RequestResult(
            null,
            null,
            null,
            TurmsStatusCode.NOT_FOUND);

    private TurmsResponse.Data dataForRequester;
    private Set<Long> recipients;
    private TurmsRequest dataForRecipients;
    private TurmsStatusCode code;

    public static RequestResult fail() {
        return status(TurmsStatusCode.FAILED);
    }

    public static RequestResult status(@NotNull TurmsStatusCode code) {
        return new RequestResult(null, Collections.emptySet(), null, code);
    }

    public static RequestResult responseIdIfTrue(@NotNull Long id, @Nullable Boolean acknowledged) {
        if (acknowledged != null && acknowledged) {
            return responseId(id);
        } else {
            return fail();
        }
    }

    public static RequestResult responseId(@NotNull Long id) {
        TurmsResponse.Data data = TurmsResponse.Data
                .newBuilder()
                .setIds(Int64Values.newBuilder().addValues(id).build())
                .build();
        return new RequestResult(
                data,
                Collections.emptySet(),
                null,
                TurmsStatusCode.OK);
    }

    public static RequestResult responseIds(@NotEmpty Set<Long> ids) {
        TurmsResponse.Data data = TurmsResponse.Data
                .newBuilder()
                .setIds(Int64Values.newBuilder().addAllValues(ids).build())
                .build();
        return new RequestResult(
                data,
                Collections.emptySet(),
                null,
                TurmsStatusCode.OK);
    }

    public static RequestResult responseIdAndRecipientData(
            @NotNull Long id,
            @NotNull Long recipientId,
            @NotNull TurmsRequest dataForRecipient) {
        TurmsResponse.Data data = TurmsResponse.Data
                .newBuilder()
                .setIds(Int64Values.newBuilder().addValues(id).build())
                .build();
        return new RequestResult(data, Collections.singleton(recipientId), dataForRecipient, TurmsStatusCode.OK);
    }

    public static RequestResult responseIdAndRecipientData(
            @NotNull Long id,
            @Nullable Set<Long> recipients,
            TurmsRequest dataForRecipients) {
        if (recipients == null || recipients.isEmpty()) {
            return responseId(id);
        } else {
            TurmsResponse.Data data = TurmsResponse.Data
                    .newBuilder()
                    .setIds(Int64Values.newBuilder().addValues(id).build())
                    .build();
            return new RequestResult(
                    data,
                    recipients,
                    dataForRecipients,
                    TurmsStatusCode.OK);
        }
    }

    public static RequestResult responseData(@NotNull TurmsResponse.Data data) {
        return new RequestResult(data, Collections.emptySet(), null, TurmsStatusCode.OK);
    }

    public static RequestResult recipientData(
            @NotNull Long recipientId,
            @NotNull TurmsRequest dataForRecipient) {
        return new RequestResult(null, Collections.singleton(recipientId), dataForRecipient, TurmsStatusCode.OK);
    }

    public static RequestResult recipientData(
            @NotEmpty Set<Long> recipientsIds,
            @NotNull TurmsRequest dataForRecipient) {
        return new RequestResult(null, recipientsIds, dataForRecipient, TurmsStatusCode.OK);
    }

    public static RequestResult recipientData(
            @NotNull Long recipientId,
            @NotNull TurmsRequest dataForRecipient,
            @NotNull TurmsStatusCode code) {
        return new RequestResult(null, Collections.singleton(recipientId), dataForRecipient, code);
    }

    public static RequestResult recipientData(
            @NotEmpty Set<Long> recipientsIds,
            @NotNull TurmsRequest dataForRecipient,
            @NotNull TurmsStatusCode code) {
        return new RequestResult(null, recipientsIds, dataForRecipient, code);
    }

    public static RequestResult okIfTrue(@Nullable Boolean acknowledged) {
        return acknowledged != null && acknowledged ? ok() : fail();
    }

    public static RequestResult ok() {
        return status(TurmsStatusCode.OK);
    }
}
