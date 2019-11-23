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
import im.turms.turms.pojo.domain.Admin;
import im.turms.turms.pojo.dto.AddAdminDTO;
import im.turms.turms.pojo.dto.UpdateAdminDTO;
import im.turms.turms.service.admin.AdminService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Date;
import java.util.Set;

import static im.turms.turms.constant.AdminPermission.*;

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
    @RequiredPermission(NONE)
    public Mono<Void> checkAccountAndPassword(
            @RequestHeader String account,
            @RequestHeader String password) {
        if (!account.isBlank() && !password.isBlank()) {
            return adminService.authenticate(account, password)
                    .map(authenticated -> {
                        if (authenticated != null && authenticated) {
                            throw new ResponseStatusException(HttpStatus.OK);
                        } else {
                            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
                        }
                    });
        } else {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }
    }

    @PostMapping
    @RequiredPermission(ADMIN_CREATE)
    public Mono<ResponseEntity> addAdmin(
            @RequestHeader String account,
            @RequestBody AddAdminDTO addAdminDTO) {
        if (addAdminDTO.getRoleId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }
        Mono<Admin> generatedAdmin = adminService.authAndAddAdmin(
                account,
                addAdminDTO.getAccount(),
                addAdminDTO.getPassword(),
                addAdminDTO.getRoleId(),
                addAdminDTO.getName(),
                new Date(),
                false);
        return ResponseFactory.okWhenTruthy(generatedAdmin);
    }

    @DeleteMapping
    @RequiredPermission(ADMIN_DELETE)
    public Mono<ResponseEntity> deleteAdmins(
            @RequestHeader String account,
            @RequestParam Set<String> accounts) {
        Mono<Boolean> deleted = adminService.authAndDeleteAdmins(account, accounts);
        return ResponseFactory.acknowledged(deleted);
    }

    @PutMapping
    @RequiredPermission(ADMIN_UPDATE)
    public Mono<ResponseEntity> updateAdmins(
            @RequestHeader String account,
            @RequestParam Set<String> accounts,
            @RequestBody UpdateAdminDTO updateAdminDTO) {
        Mono<Boolean> updated = adminService.authAndUpdateAdmins(
                account,
                accounts,
                updateAdminDTO.getPassword(),
                updateAdminDTO.getName(),
                updateAdminDTO.getRoleId());
        return ResponseFactory.acknowledged(updated);
    }

    @GetMapping
    @RequiredPermission(ADMIN_QUERY)
    public Mono<ResponseEntity> queryAdmins(
            @RequestParam(required = false) Set<String> accounts,
            @RequestParam(required = false) String account,
            @RequestParam(required = false) Long roleId,
            @RequestParam(defaultValue = "false") boolean withPassword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "0") int size) {
        if (StringUtils.hasText(account)) {
            Mono<Admin> getAdmin = adminService.queryAdmin(account, withPassword);
            return ResponseFactory.okWhenTruthy(getAdmin);
        } else {
            size = pageUtil.getSize(size);
            Flux<Admin> admins = adminService.queryAdmins(accounts, roleId, withPassword, page, size);
            Mono<Long> total = adminService.countAdmins(accounts, roleId);
            return ResponseFactory.page(total, admins);
        }
    }
}
