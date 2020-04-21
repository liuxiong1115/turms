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
import im.turms.turms.pojo.bo.DateRange;
import im.turms.turms.pojo.dto.AcknowledgedDTO;
import im.turms.turms.pojo.dto.AdminActionLogDTO;
import im.turms.turms.pojo.dto.PaginationDTO;
import im.turms.turms.pojo.dto.ResponseDTO;
import im.turms.turms.service.admin.AdminActionLogService;
import im.turms.turms.util.PageUtil;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collection;
import java.util.Date;
import java.util.Set;

import static im.turms.turms.constant.AdminPermission.ADMIN_ACTION_LOG_DELETE;
import static im.turms.turms.constant.AdminPermission.ADMIN_ACTION_LOG_QUERY;

@RestController
@RequestMapping("/admins/action-logs")
public class AdminActionLogController {
    private final AdminActionLogService adminActionLogService;
    private final PageUtil pageUtil;

    public AdminActionLogController(AdminActionLogService adminActionLogService, PageUtil pageUtil) {
        this.adminActionLogService = adminActionLogService;
        this.pageUtil = pageUtil;
    }

    @GetMapping
    @RequiredPermission(ADMIN_ACTION_LOG_QUERY)
    public Mono<ResponseEntity<ResponseDTO<Collection<AdminActionLogDTO>>>> queryAdminActionLogs(
            @RequestParam(required = false) Set<Long> ids,
            @RequestParam(required = false) Set<String> accounts,
            @RequestParam(required = false) Date logDateStart,
            @RequestParam(required = false) Date logDateEnd,
            @RequestParam(required = false) Integer size) {
        size = pageUtil.getSize(size);
        Flux<AdminActionLogDTO> adminActionLogsFlux = adminActionLogService
                .queryAdminActionLogs(ids, accounts, DateRange.of(logDateStart, logDateEnd), 0, size)
                .map(AdminActionLogDTO::from);
        return ResponseFactory.okIfTruthy(adminActionLogsFlux);
    }

    @GetMapping("/page")
    @RequiredPermission(ADMIN_ACTION_LOG_QUERY)
    public Mono<ResponseEntity<ResponseDTO<PaginationDTO<AdminActionLogDTO>>>> queryAdminActionLogs(
            @RequestParam(required = false) Set<Long> ids,
            @RequestParam(required = false) Set<String> accounts,
            @RequestParam(required = false) Date logDateStart,
            @RequestParam(required = false) Date logDateEnd,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(required = false) Integer size) {
        size = pageUtil.getSize(size);
        Mono<Long> count = adminActionLogService
                .countAdminActionLogs(ids, accounts, DateRange.of(logDateStart, logDateEnd));
        Flux<AdminActionLogDTO> adminActionLogsFlux = adminActionLogService
                .queryAdminActionLogs(ids, accounts, DateRange.of(logDateStart, logDateEnd), page, size)
                .map(AdminActionLogDTO::from);
        return ResponseFactory.page(count, adminActionLogsFlux);
    }

    @DeleteMapping
    @RequiredPermission(ADMIN_ACTION_LOG_DELETE)
    public Mono<ResponseEntity<ResponseDTO<AcknowledgedDTO>>> deleteAdminActionLog(
            @RequestParam(required = false) Set<Long> ids) {
        Mono<Boolean> deleteMono = adminActionLogService.deleteAdminActionLogs(ids);
        return ResponseFactory.acknowledged(deleteMono);
    }
}
