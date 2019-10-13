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
import im.turms.turms.cluster.TurmsClusterManager;
import im.turms.turms.common.TurmsStatusCode;
import im.turms.turms.constant.AdminPermission;
import im.turms.turms.property.TurmsProperties;
import im.turms.turms.service.user.UserSimultaneousLoginService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/cluster")
public class ClusterController {
    private final TurmsClusterManager turmsClusterManager;
    private final UserSimultaneousLoginService userSimultaneousLoginService;

    public ClusterController(TurmsClusterManager turmsClusterManager, UserSimultaneousLoginService userSimultaneousLoginService) {
        this.turmsClusterManager = turmsClusterManager;
        this.userSimultaneousLoginService = userSimultaneousLoginService;
    }

    @GetMapping
    public ResponseEntity getClusterInfo(@RequestParam(defaultValue = "false") boolean withConfigs) {
        Map<String, Object> clusterInfo = turmsClusterManager.getClusterInfo(withConfigs);
        return ResponseFactory.okWhenTruthy(clusterInfo, true);
    }

    @GetMapping("/server")
    public ResponseEntity getServerHost(@RequestParam Long userId) {
        String host = turmsClusterManager.getMemberByUserId(userId).getAddress().getHost();
        return ResponseFactory.okWhenTruthy(host);
    }

    @GetMapping("/config")
    public ResponseEntity getClusterConfig(@RequestParam(defaultValue = "false") boolean mutable) {
        TurmsProperties properties = turmsClusterManager.getTurmsProperties();
        if (mutable) {
            try {
                return ResponseFactory.okWhenTruthy(TurmsProperties.getMutableProperties(turmsClusterManager.getTurmsProperties()));
            } catch (IOException e) {
                return ResponseFactory.entity(TurmsStatusCode.SERVER_INTERNAL_ERROR);
            }
        } else {
            return ResponseFactory.okWhenTruthy(properties);
        }
    }

    /**
     * Do not call this method frequently because it will cost a lot of resources
     */
    @PutMapping("/config")
    @RequiredPermission(AdminPermission.CLUSTER_CONFIG_UPDATE)
    public ResponseEntity updateClusterConfig(@RequestBody TurmsProperties turmsProperties) throws IOException {
        TurmsProperties mergedProperties = TurmsProperties.merge(
                turmsClusterManager.getTurmsProperties(),
                turmsProperties);
        turmsClusterManager.updateProperties(mergedProperties);
        userSimultaneousLoginService.applyStrategy(
                mergedProperties.getUser().getSimultaneousLogin().getStrategy());
        return ResponseFactory.okWhenTruthy(mergedProperties);
    }
}
