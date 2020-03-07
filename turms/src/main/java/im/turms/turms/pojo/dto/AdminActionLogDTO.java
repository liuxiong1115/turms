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

package im.turms.turms.pojo.dto;

import com.google.common.net.InetAddresses;
import com.mongodb.DBObject;
import im.turms.turms.pojo.domain.AdminActionLog;
import lombok.Data;

import java.util.Date;

@Data
public final class AdminActionLogDTO {
    private final Long id;
    private final String account;
    private final Date logDate;
    private final String ip;
    private final String action;
    private final DBObject params;
    private final DBObject body;

    public static AdminActionLogDTO from(AdminActionLog log) {
        return new AdminActionLogDTO(
                log.getId(),
                log.getAccount(),
                log.getLogDate(),
                log.getIp() != null ? InetAddresses.fromInteger(log.getIp()).getHostAddress() : null,
                log.getAction(),
                log.getParams(),
                log.getBody());
    }
}