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

package im.turms.turms.access.web.controller.user;

import im.turms.turms.access.web.util.ResponseFactory;
import im.turms.turms.annotation.web.RequiredPermission;
import im.turms.turms.common.TurmsStatusCode;
import im.turms.turms.constant.AdminPermission;
import im.turms.turms.pojo.domain.UserRelationship;
import im.turms.turms.pojo.dto.AddRelationshipDTO;
import im.turms.turms.pojo.dto.UpdateRelationshipDTO;
import im.turms.turms.service.user.relationship.UserRelationshipService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Set;

import static im.turms.turms.common.Constants.DEFAULT_RELATIONSHIP_GROUP_INDEX;

@RestController
@RequestMapping("/users/relationships")
public class UserRelationshipController {
    private final UserRelationshipService userRelationshipService;

    public UserRelationshipController(UserRelationshipService userRelationshipService) {
        this.userRelationshipService = userRelationshipService;
    }

    @PostMapping
    @RequiredPermission(AdminPermission.USER_RELATIONSHIP_CREATE)
    public Mono<ResponseEntity> addRelationship(@RequestBody AddRelationshipDTO addRelationshipDTO) {
        Mono<Boolean> upsert = userRelationshipService.upsertOneSidedRelationship(
                addRelationshipDTO.getOwnerId(),
                addRelationshipDTO.getRelatedUserId(),
                addRelationshipDTO.getIsBlocked(),
                DEFAULT_RELATIONSHIP_GROUP_INDEX,
                null,
                addRelationshipDTO.getEstablishmentDate(),
                false,
                null);
        return ResponseFactory.acknowledged(upsert);
    }

    @DeleteMapping
    @RequiredPermission(AdminPermission.USER_RELATIONSHIP_DELETE)
    public Mono<ResponseEntity> deleteRelationships(
            @RequestParam Long ownerId,
            @RequestParam(required = false) Set<Long> relatedUsersIds) {
        Mono<Boolean> deleted = userRelationshipService.deleteOneSidedRelationships(ownerId, relatedUsersIds);
        return ResponseFactory.acknowledged(deleted);
    }

    @PutMapping
    @RequiredPermission(AdminPermission.USER_RELATIONSHIP_UPDATE)
    public Mono<ResponseEntity> updateRelationships(
            @RequestParam Long ownerId,
            @RequestParam(required = false) Set<Long> relatedUsersIds,
            @RequestBody UpdateRelationshipDTO updateRelationshipDTO) {
        if (updateRelationshipDTO.getIsBlocked() != null && updateRelationshipDTO.getEstablishmentDate() != null) {
            Mono<Boolean> updated = userRelationshipService.updateUserOneSidedRelationships(
                    ownerId,
                    relatedUsersIds,
                    updateRelationshipDTO.getIsBlocked(),
                    updateRelationshipDTO.getEstablishmentDate());
            return ResponseFactory.acknowledged(updated);
        } else {
            return ResponseFactory.code(TurmsStatusCode.ILLEGAL_ARGUMENTS);
        }
    }

    //TODO: pagify
    @GetMapping
    @RequiredPermission(AdminPermission.USER_RELATIONSHIP_QUERY)
    public Mono<ResponseEntity> getRelationships(
            @RequestParam Long ownerId,
            @RequestParam(required = false) Set<Long> relatedUsersIds,
            @RequestParam(required = false) Integer groupIndex,
            @RequestParam(required = false) Boolean blocked
//            @RequestParam(defaultValue = "0") Integer page,
//            @RequestParam(defaultValue = "0") Integer size
    ) {
        Flux<UserRelationship> relationships = userRelationshipService.queryRelationships(
                ownerId, relatedUsersIds, groupIndex, blocked);
        return ResponseFactory.okWhenTruthy(relationships, true);
    }
}
