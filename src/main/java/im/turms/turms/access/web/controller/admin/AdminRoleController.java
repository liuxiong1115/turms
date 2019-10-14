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
import im.turms.turms.common.TurmsStatusCode;
import im.turms.turms.constant.AdminPermission;
import im.turms.turms.pojo.domain.AdminRole;
import im.turms.turms.service.admin.AdminRoleService;
import org.springframework.http.ResponseEntity;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.Set;

@RestController
@RequestMapping("/admins/roles")
public class AdminRoleController {
    private final AdminRoleService adminRoleService;

    public AdminRoleController(AdminRoleService adminRoleService) {
        this.adminRoleService = adminRoleService;
    }

    @PostMapping
    @RequiredPermission(AdminPermission.ADMIN_ROLE_CREATE)
    public Mono<ResponseEntity> addAdminRole(@RequestBody AdminRole adminRole) {
        if (adminRole.getRoleId() != null
                && !CollectionUtils.isEmpty(adminRole.getPermissions())
                && StringUtils.hasText(adminRole.getName())) {
            return ResponseFactory.okWhenTruthy(adminRoleService.addAdminRole(adminRole));
        } else {
            return ResponseFactory.code(TurmsStatusCode.ILLEGAL_ARGUMENTS);
        }
    }

    @DeleteMapping
    @RequiredPermission(AdminPermission.ADMIN_ROLE_DELETE)
    public Mono<ResponseEntity> deleteAdminRoles(@RequestParam Set<Long> ids) {
        Mono<Boolean> deleted = adminRoleService.deleteAdminRoles(ids);
        return ResponseFactory.acknowledged(deleted);
    }

    @PutMapping
    @RequiredPermission(AdminPermission.ADMIN_ROLE_UPDATE)
    public Mono<ResponseEntity> updateAdminRole(
            @RequestParam Long id,
            @RequestBody AdminRole role) {
        if (StringUtils.hasText(role.getName()) || !CollectionUtils.isEmpty(role.getPermissions())) {
            Mono<Boolean> updated = adminRoleService.updateAdminRole(id, role.getName(), role.getPermissions());
            return ResponseFactory.acknowledged(updated);
        } else {
            return ResponseFactory.code(TurmsStatusCode.ILLEGAL_ARGUMENTS);
        }
    }

    @GetMapping
    @RequiredPermission(AdminPermission.ADMIN_ROLE_QUERY)
    public Mono<ResponseEntity> getAdminRoles() {
        //TODO: provide filters
        return ResponseFactory.okWhenTruthy(adminRoleService.queryAllAdminRoles());
    }
}