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
import im.turms.turms.pojo.domain.Admin;
import im.turms.turms.pojo.dto.PageResult;
import im.turms.turms.service.admin.AdminService;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Set;

@RestController
@RequestMapping("/admins")
public class AdminController {
    private final AdminService adminService;
    private final PageUtil pageUtil;

    public AdminController(AdminService adminService, PageUtil pageUtil) {
        this.adminService = adminService;
        this.pageUtil = pageUtil;
    }

    @RequestMapping(method = RequestMethod.HEAD)
    @RequiredPermission(AdminPermission.CUSTOM)
    public Mono<ResponseEntity> checkAccountAndPassword(
            @RequestParam String account,
            @RequestParam String password) {
        if (!account.isBlank() && !password.isBlank()) {
            Mono<Boolean> authenticated = adminService.authenticate(account, password);
            return ResponseFactory.authenticated(authenticated);
        } else {
            return ResponseFactory.code(TurmsStatusCode.ILLEGAL_ARGUMENTS);
        }
    }

    @PostMapping
    @RequiredPermission(AdminPermission.ADMIN_CREATE)
    public Mono<ResponseEntity> addAdmin(@RequestBody Admin admin) {
        Mono<Admin> generatedAdmin = adminService.addAdmin(
                admin.getAccount(),
                admin.getPassword(),
                admin.getRoleId(),
                admin.getName(),
                admin.getRegistrationDate(),
                false);
        return ResponseFactory.okWhenTruthy(generatedAdmin);
    }

    @DeleteMapping
    @RequiredPermission(AdminPermission.ADMIN_DELETE)
    public Mono<ResponseEntity> deleteAdmins(@RequestParam Set<String> accounts) {
        Mono<Boolean> deleted = adminService.deleteAdmins(accounts);
        return ResponseFactory.acknowledged(deleted);
    }

    @PutMapping
    @RequiredPermission(AdminPermission.ADMIN_UPDATE)
    public Mono<ResponseEntity> updateAdmins(
            @RequestParam Set<String> accounts,
            @RequestBody Admin admin) {
        Mono<Boolean> updated = adminService.updateAdmins(
                accounts,
                admin.getPassword(),
                admin.getName(),
                admin.getRoleId());
        return ResponseFactory.acknowledged(updated);
    }

    @GetMapping
    @RequiredPermission(AdminPermission.ADMIN_QUERY)
    public Mono<ResponseEntity> getAdmins(
            @RequestParam(required = false) Set<String> accounts,
            @RequestParam(required = false) String account,
            @RequestParam(required = false) String role,
            @RequestParam(defaultValue = "false") boolean withPassword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "0") int size) {
        if (StringUtils.hasText(account)) {
            Mono<Admin> getAdmin = adminService.queryAdmin(account, withPassword);
            return ResponseFactory.okWhenTruthy(getAdmin);
        } else {
            size = pageUtil.getSize(size);
            Flux<Admin> admins = adminService.queryAdmins(accounts, role, withPassword, page, size);
            Mono<Long> total = adminService.countAdmins(accounts, role);
            return ResponseFactory.okWhenTruthy(PageResult.getResult(total, admins));
        }
    }
}
