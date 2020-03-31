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

package im.turms.turms.access.web.controller;

import im.turms.turms.access.web.util.ResponseFactory;
import im.turms.turms.annotation.web.RequiredPermission;
import im.turms.turms.manager.TurmsClusterManager;
import im.turms.turms.pojo.dto.AddressDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static im.turms.turms.constant.AdminPermission.NONE;

/**
 * Used to inform clients of which servers they should connect.
 * Note: This is a service degradation because JavaScript in any browsers
 * is not allowed to get the response information in the handshake of WebSocket.
 * i.e. Turms server will tell to which server they should connect in the response of handshake
 * but the clients in browsers cannot get the response.
 */
@RestController
@RequestMapping("/router")
public class RouterController {
    private final TurmsClusterManager turmsClusterManager;

    public RouterController(TurmsClusterManager turmsClusterManager) {
        this.turmsClusterManager = turmsClusterManager;
    }

    @GetMapping
    @RequiredPermission(NONE)
    public ResponseEntity<AddressDTO> queryResponsibleServerAddress(@RequestParam Long userId) {
        String address = turmsClusterManager.getResponsibleTurmsServerAddress(userId);
        return ResponseFactory.raw(new AddressDTO(address));
    }
}
