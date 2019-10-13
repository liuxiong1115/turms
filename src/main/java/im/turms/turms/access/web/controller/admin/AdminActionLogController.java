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

package im.turms.turms.access.web.controller.admin;

import im.turms.turms.access.web.util.ResponseFactory;
import im.turms.turms.annotation.web.RequiredPermission;
import im.turms.turms.common.PageUtil;
import im.turms.turms.common.TurmsStatusCode;
import im.turms.turms.constant.AdminPermission;
import im.turms.turms.pojo.domain.AdminActionLog;
import im.turms.turms.service.admin.AdminActionLogService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Date;
import java.util.Set;

@RestController
@RequestMapping("/admins/action-logs")
public class AdminActionLogController {
    private final AdminActionLogService adminActionLogService;
    private final PageUtil pageUtil;

    public AdminActionLogController(AdminActionLogService adminActionLogService, PageUtil pageUtil) {
        this.adminActionLogService = adminActionLogService;
        this.pageUtil = pageUtil;
    }

    @DeleteMapping
    @RequiredPermission(AdminPermission.ADMIN_ACTION_LOG_DELETE)
    public Mono<ResponseEntity> deleteAdminActionLog(
            @RequestParam(required = false) Set<Long> ids,
            @RequestParam(required = false) Set<String> accounts,
            @RequestParam(required = false) Date startDate,
            @RequestParam(required = false) Date endDate) {
        if (ids != null || startDate != null || endDate != null) {
            Mono<Boolean> deleted = adminActionLogService
                    .deleteAdminActionLogs(ids, accounts, startDate, endDate);
            return ResponseFactory.acknowledged(deleted);
        } else {
            return Mono.just(ResponseFactory.entity(TurmsStatusCode.ILLEGAL_ARGUMENTS));
        }
    }

    @GetMapping
    @RequiredPermission(AdminPermission.ADMIN_ACTION_LOG_QUERY)
    public Mono<ResponseEntity> getAdminActionLogs(
            @RequestParam(required = false) Set<Long> ids,
            @RequestParam(required = false) Set<String> accounts,
            @RequestParam(required = false) Date startDate,
            @RequestParam(required = false) Date endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "0") int size) {
        size = pageUtil.getSize(size);
        Flux<AdminActionLog> adminActionLogs = adminActionLogService
                .getAdminActionLogs(ids, accounts, startDate, endDate, page, size);
        return ResponseFactory.okWhenTruthy(adminActionLogs);
    }
}
