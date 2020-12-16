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

package im.turms.turms.workflow.access.http.controller.cluster;

import im.turms.server.common.cluster.node.Node;
import im.turms.server.common.cluster.node.NodeVersion;
import im.turms.server.common.cluster.service.config.domain.discovery.Member;
import im.turms.turms.workflow.access.http.dto.request.cluster.AddMemberDTO;
import im.turms.turms.workflow.access.http.dto.request.cluster.UpdateMemberDTO;
import im.turms.turms.workflow.access.http.dto.response.ResponseDTO;
import im.turms.turms.workflow.access.http.dto.response.ResponseFactory;
import im.turms.turms.workflow.access.http.permission.RequiredPermission;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import static im.turms.turms.workflow.access.http.permission.AdminPermission.*;


/**
 * @author James Chen
 */
@RestController
@RequestMapping("/cluster/members")
public class MemberController {

    private final Node node;

    public MemberController(Node node) {
        this.node = node;
    }

    @GetMapping
    @RequiredPermission(CLUSTER_MEMBERS_QUERY)
    public ResponseEntity<ResponseDTO<Collection<Member>>> queryMembers() {
        return ResponseFactory.okIfTruthy(node.getDiscoveryService().getAllKnownMembers().values());
    }

    @DeleteMapping
    @RequiredPermission(CLUSTER_MEMBERS_DELETE)
    public Mono<ResponseEntity<ResponseDTO<Void>>> removeMembers(@RequestParam List<String> ids) {
        Mono<Void> unregisterMembers = node.getDiscoveryService().unregisterMembers(new HashSet<>(ids));
        return unregisterMembers.thenReturn(ResponseFactory.OK);
    }

    @PostMapping
    @RequiredPermission(CLUSTER_MEMBERS_CREATE)
    public Mono<ResponseEntity<ResponseDTO<Void>>> addMember(@RequestBody AddMemberDTO addMemberDTO) {
        String clusterId = node.getDiscoveryService().getLocalMember().getClusterId();
        Member member = new Member(
                clusterId,
                addMemberDTO.getNodeId(),
                addMemberDTO.getNodeType(),
                NodeVersion.parse(addMemberDTO.getVersion()),
                addMemberDTO.isSeed(),
                addMemberDTO.getRegistrationDate(),
                addMemberDTO.getPriority(),
                addMemberDTO.getMemberHost(),
                addMemberDTO.getMemberPort(),
                addMemberDTO.getServiceAddress(),
                false,
                addMemberDTO.isActive());
        return node.getDiscoveryService()
                .registerMember(member)
                .thenReturn(ResponseFactory.OK);
    }

    @PutMapping
    @RequiredPermission(CLUSTER_MEMBERS_UPDATE)
    public Mono<ResponseEntity<ResponseDTO<Void>>> updateMember(
            @RequestParam String id,
            @RequestBody UpdateMemberDTO updateMemberDTO) {
        Mono<Void> addMemberMono = node.getDiscoveryService().updateMemberInfo(
                id,
                updateMemberDTO.getIsSeed(),
                updateMemberDTO.getIsActive());
        return addMemberMono.thenReturn(ResponseFactory.OK);
    }

    @GetMapping("/master")
    @RequiredPermission(CLUSTER_MEMBERS_QUERY)
    public ResponseEntity<ResponseDTO<Member>> queryMaster() {
        String nodeId = node.getDiscoveryService().getLeader().getNodeId();
        Member master = node.getDiscoveryService().getAllKnownMembers().get(nodeId);
        return ResponseFactory.okIfTruthy(master);
    }

}