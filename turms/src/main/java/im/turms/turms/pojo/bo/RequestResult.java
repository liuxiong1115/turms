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

import im.turms.common.TurmsStatusCode;
import im.turms.common.model.bo.common.Int64Values;
import im.turms.common.model.dto.notification.TurmsNotification;
import im.turms.common.model.dto.request.TurmsRequest;
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
public final class RequestResult {
    public static final RequestResult NO_CONTENT = new RequestResult(
            null,
            false,
            null,
            null,
            TurmsStatusCode.NO_CONTENT,
            null);

    private final TurmsNotification.Data dataForRequester;
    public final boolean relayDataToOtherSenderOnlineDevices;
    private final Set<Long> recipients;
    private final TurmsRequest dataForRecipients;
    private final TurmsStatusCode code;
    private final String reason;

    public static RequestResult fail() {
        return create(TurmsStatusCode.FAILED);
    }

    public static RequestResult create(@NotNull TurmsStatusCode code) {
        return new RequestResult(null, false, Collections.emptySet(), null, code, null);
    }

    public static RequestResult create(@NotNull TurmsStatusCode code, @NotNull String reason) {
        return new RequestResult(null, false, Collections.emptySet(), null, code, reason);
    }

    public static RequestResult create(@NotNull Long id) {
        TurmsNotification.Data data = TurmsNotification.Data
                .newBuilder()
                .setIds(Int64Values.newBuilder().addValues(id).build())
                .build();
        return new RequestResult(
                data,
                false,
                Collections.emptySet(),
                null,
                TurmsStatusCode.OK,
                null);
    }

    public static RequestResult create(
            @NotNull Long id,
            @NotNull Long recipientId,
            @NotNull TurmsRequest dataForRecipient) {
        TurmsNotification.Data data = TurmsNotification.Data
                .newBuilder()
                .setIds(Int64Values.newBuilder().addValues(id).build())
                .build();
        return new RequestResult(data, false, Collections.singleton(recipientId), dataForRecipient, TurmsStatusCode.OK, null);
    }

    public static RequestResult create(
            @NotNull Long id,
            @NotEmpty Set<Long> recipients,
            boolean relayDataToOtherSenderOnlineDevices,
            TurmsRequest dataForRecipients) {
        TurmsNotification.Data data = TurmsNotification.Data
                .newBuilder()
                .setIds(Int64Values.newBuilder().addValues(id).build())
                .build();
        return new RequestResult(
                data,
                relayDataToOtherSenderOnlineDevices,
                recipients,
                dataForRecipients,
                TurmsStatusCode.OK,
                null);
    }

    public static RequestResult create(@NotNull TurmsNotification.Data data) {
        return new RequestResult(data, false, Collections.emptySet(), null, TurmsStatusCode.OK, null);
    }

    public static RequestResult create(
            @NotNull Long recipientId,
            @NotNull TurmsRequest dataForRecipient) {
        return new RequestResult(null, false, Collections.singleton(recipientId), dataForRecipient, TurmsStatusCode.OK, null);
    }

    public static RequestResult create(
            @NotEmpty Set<Long> recipientsIds,
            @NotNull TurmsRequest dataForRecipient) {
        return new RequestResult(null, false, recipientsIds, dataForRecipient, TurmsStatusCode.OK, null);
    }

    public static RequestResult create(
            @NotNull Long recipientId,
            @NotNull TurmsRequest dataForRecipient,
            @NotNull TurmsStatusCode code) {
        return new RequestResult(null, false, Collections.singleton(recipientId), dataForRecipient, code, null);
    }

    public static RequestResult okIfTrue(@Nullable Boolean acknowledged) {
        return acknowledged != null && acknowledged ? ok() : fail();
    }

    public static RequestResult ok() {
        return create(TurmsStatusCode.OK);
    }
}
