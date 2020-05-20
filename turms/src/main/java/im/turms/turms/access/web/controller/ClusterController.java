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

import im.turms.common.TurmsStatusCode;
import im.turms.common.exception.TurmsBusinessException;
import im.turms.turms.access.web.util.ResponseFactory;
import im.turms.turms.annotation.web.RequiredPermission;
import im.turms.turms.manager.TurmsClusterManager;
import im.turms.turms.pojo.dto.AcknowledgedDTO;
import im.turms.turms.pojo.dto.ResponseDTO;
import im.turms.turms.property.TurmsProperties;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static im.turms.turms.constant.AdminPermission.*;

@RestController
@RequestMapping("/cluster")
public class ClusterController {
    private final TurmsClusterManager turmsClusterManager;

    public ClusterController(TurmsClusterManager turmsClusterManager) {
        this.turmsClusterManager = turmsClusterManager;
    }

    @GetMapping("/hazelcast-info")
    @RequiredPermission(CLUSTER_SERVER_INFO_QUERY)
    public ResponseEntity<ResponseDTO<Map>> queryHazelcastInfo(@RequestParam(defaultValue = "false") boolean withConfigs) {
        Map<String, ?> hazelcastInfo = turmsClusterManager.getHazelcastInfo(withConfigs);
        return ResponseFactory.okIfTruthy(hazelcastInfo);
    }

    // Config

    @GetMapping("/config")
    @RequiredPermission(CLUSTER_SERVER_INFO_QUERY)
    public ResponseEntity<ResponseDTO<Map<String, Object>>> queryClusterConfig(@RequestParam(defaultValue = "false") Boolean onlyMutable) {
        try {
            return ResponseFactory.okIfTruthy(TurmsProperties.getPropertyValueMap(turmsClusterManager.getTurmsProperties(), onlyMutable));
        } catch (IOException e) {
            throw TurmsBusinessException.get(TurmsStatusCode.SERVER_INTERNAL_ERROR);
        }
    }

    @GetMapping("/config/metadata")
    @RequiredPermission(CLUSTER_SERVER_INFO_QUERY)
    public ResponseEntity<ResponseDTO<Map<String, Object>>> queryClusterConfigMetadata(
            @RequestParam(defaultValue = "false") Boolean onlyMutable,
            @RequestParam(defaultValue = "false") Boolean withValue,
            @RequestParam(defaultValue = "false") Boolean withMutableFlag) {
        Map<String, Object> metadata = TurmsProperties.getMetadata(new HashMap<>(), TurmsProperties.class, onlyMutable, withMutableFlag);
        if (withValue) {
            try {
                Map<String, Object> propertyValueMap = TurmsProperties.getPropertyValueMap(turmsClusterManager.getTurmsProperties(), onlyMutable)
                        .entrySet()
                        .stream()
                        .filter(entry -> metadata.containsKey(entry.getKey()))
                        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
                Map<String, Object> propertiesWithMetadata = TurmsProperties.mergePropertiesWithMetadata(propertyValueMap, metadata);
                return ResponseFactory.okIfTruthy(propertiesWithMetadata);
            } catch (IOException e) {
                throw TurmsBusinessException.get(TurmsStatusCode.SERVER_INTERNAL_ERROR);
            }
        } else {
            return ResponseFactory.okIfTruthy(metadata);
        }
    }

    /**
     * Do not call this method frequently because it costs a lot of resources
     */
    @PutMapping("/config")
    @RequiredPermission(CLUSTER_CONFIG_UPDATE)
    public ResponseEntity<ResponseDTO<TurmsProperties>> updateClusterConfig(
            @RequestParam(defaultValue = "false") Boolean shouldReset,
            @RequestBody(required = false) TurmsProperties turmsProperties) throws IOException {
        if (shouldReset) {
            return ResponseFactory.okIfTruthy(turmsClusterManager.getTurmsProperties().reset());
        } else {
            if (turmsProperties != null) {
                String propertiesForUpdating = TurmsProperties.getMutablePropertiesString(turmsProperties);
                TurmsProperties mergedProperties = TurmsProperties.merge(
                        turmsClusterManager.getTurmsProperties(),
                        propertiesForUpdating);
                turmsClusterManager.updatePropertiesAndNotify(mergedProperties);
                mergedProperties.persist(propertiesForUpdating);
                return ResponseFactory.okIfTruthy(mergedProperties);
            } else {
                throw TurmsBusinessException.get(TurmsStatusCode.ILLEGAL_ARGUMENTS, "The new properties must not be null if shouldReset is false");
            }
        }
    }

    // Server

    /**
     * Note that the master node of turms cluster is always the first active node in the response
     */
    @GetMapping("/servers")
    @RequiredPermission(CLUSTER_SERVER_INFO_QUERY)
    public ResponseEntity<ResponseDTO<Collection<String>>> queryServers(
            @RequestParam(defaultValue = "false") boolean onlyActiveServers,
            @RequestParam(defaultValue = "false") boolean onlyInactiveServers) {
        return ResponseFactory.okIfTruthy(turmsClusterManager.getServersAddress(onlyActiveServers, onlyInactiveServers));
    }

    @DeleteMapping("/servers")
    @RequiredPermission(CLUSTER_SERVER_ACTIVATION_UPDATE)
    public ResponseEntity<ResponseDTO<AcknowledgedDTO>> deactivateServers(@RequestParam List<String> ids) {
        Set<UUID> uuids = new HashSet<>(ids.size());
        for (String id : ids) {
            uuids.add(UUID.fromString(id));
        }
        turmsClusterManager.deactivateServers(uuids);
        return ResponseFactory.acknowledged(true);
    }

    @PostMapping("/servers")
    @RequiredPermission(CLUSTER_SERVER_ACTIVATION_UPDATE)
    public ResponseEntity<ResponseDTO<AcknowledgedDTO>> activateServers(@RequestParam List<String> ids) {
        Set<UUID> uuids = new HashSet<>(ids.size());
        for (String id : ids) {
            uuids.add(UUID.fromString(id));
        }
        turmsClusterManager.activateServers(uuids);
        return ResponseFactory.acknowledged(true);
    }

    @GetMapping("/servers/ids")
    @RequiredPermission(CLUSTER_SERVER_INFO_QUERY)
    public ResponseEntity<ResponseDTO<Map<UUID, String>>> queryServersId(
            @RequestParam(defaultValue = "false") boolean onlyActiveServers,
            @RequestParam(defaultValue = "false") boolean onlyInactiveServers) {
        return ResponseFactory.okIfTruthy(turmsClusterManager.getServersIdMap(onlyActiveServers, onlyInactiveServers));
    }
}
