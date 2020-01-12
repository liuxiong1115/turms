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
public class AdminActionLogDTO {
    private Long id;
    private String account;
    private Date logDate;
    private String ip;
    private String action;
    private DBObject params;
    private DBObject body;

    public static AdminActionLogDTO from(AdminActionLog log) {
        AdminActionLogDTO dto = new AdminActionLogDTO();
        dto.id = log.getId();
        dto.account = log.getAccount();
        dto.logDate = log.getLogDate();
        if (log.getIp() != null) {
            dto.ip = InetAddresses.fromInteger(log.getIp()).getHostAddress();
        }
        dto.action = log.getAction();
        dto.params = log.getParams();
        dto.body = log.getBody();
        return dto;
    }
}