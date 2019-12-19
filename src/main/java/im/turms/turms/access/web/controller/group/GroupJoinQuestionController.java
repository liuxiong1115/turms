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

package im.turms.turms.access.web.controller.group;

import im.turms.turms.access.web.util.ResponseFactory;
import im.turms.turms.annotation.web.RequiredPermission;
import im.turms.turms.common.PageUtil;
import im.turms.turms.pojo.domain.GroupJoinQuestion;
import im.turms.turms.pojo.dto.*;
import im.turms.turms.service.group.GroupJoinQuestionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collection;
import java.util.Set;

import static im.turms.turms.constant.AdminPermission.*;

@RestController
@RequestMapping("/groups/join-questions")
public class GroupJoinQuestionController {
    private final GroupJoinQuestionService groupJoinQuestionService;
    private final PageUtil pageUtil;

    public GroupJoinQuestionController(PageUtil pageUtil, GroupJoinQuestionService groupJoinQuestionService) {
        this.pageUtil = pageUtil;
        this.groupJoinQuestionService = groupJoinQuestionService;
    }

    @GetMapping
    @RequiredPermission(GROUP_JOIN_QUESTION_QUERY)
    public Mono<ResponseEntity<ResponseDTO<Collection<GroupJoinQuestion>>>> queryGroupJoinQuestions(
            @RequestParam(required = false) Set<Long> ids,
            @RequestParam(required = false) Long groupId,
            @RequestParam(required = false) Integer size) {
        size = pageUtil.getSize(size);
        Flux<GroupJoinQuestion> groupJoinQuestionFlux = groupJoinQuestionService
                .queryGroupJoinQuestions(ids, groupId, 0, size, true);
        return ResponseFactory.okIfTruthy(groupJoinQuestionFlux);
    }

    @GetMapping("/page")
    @RequiredPermission(GROUP_JOIN_QUESTION_QUERY)
    public Mono<ResponseEntity<ResponseDTO<PaginationDTO<GroupJoinQuestion>>>> queryGroupsInformation(
            @RequestParam(required = false) Set<Long> ids,
            @RequestParam(required = false) Long groupId,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(required = false) Integer size) {
        size = pageUtil.getSize(size);
        Mono<Long> count = groupJoinQuestionService.countGroupJoinQuestions(ids, groupId);
        Flux<GroupJoinQuestion> groupJoinQuestionFlux = groupJoinQuestionService
                .queryGroupJoinQuestions(ids, groupId, page, size, true);
        return ResponseFactory.page(count, groupJoinQuestionFlux);
    }

    @PostMapping
    @RequiredPermission(GROUP_JOIN_QUESTION_CREATE)
    public Mono<ResponseEntity<ResponseDTO<GroupJoinQuestion>>> addGroupJoinQuestion(@RequestBody AddGroupJoinQuestionDTO dto) {
        Mono<GroupJoinQuestion> createMono = groupJoinQuestionService
                .createGroupJoinQuestion(
                        dto.getGroupId(),
                        dto.getQuestion(),
                        dto.getAnswers(),
                        dto.getScore());
        return ResponseFactory.okIfTruthy(createMono);
    }

    @PutMapping
    @RequiredPermission(GROUP_JOIN_QUESTION_UPDATE)
    public Mono<ResponseEntity<ResponseDTO<AcknowledgedDTO>>> updateGroupJoinQuestions(
            @RequestParam Set<Long> ids,
            @RequestBody UpdateGroupJoinQuestionDTO dto) {
        Mono<Boolean> updateMono = groupJoinQuestionService.updateGroupJoinQuestions(
                ids,
                dto.getGroupId(),
                dto.getQuestion(),
                dto.getAnswers(),
                dto.getScore());
        return ResponseFactory.acknowledged(updateMono);
    }

    @DeleteMapping
    @RequiredPermission(GROUP_JOIN_QUESTION_DELETE)
    public Mono<ResponseEntity<ResponseDTO<AcknowledgedDTO>>> deleteGroupJoinQuestions(
            @RequestParam(required = false) Set<Long> ids,
            @RequestParam(required = false) Long groupId) {
        Mono<Boolean> deleteMono = groupJoinQuestionService.deleteGroupJoinQuestion(ids, groupId);
        return ResponseFactory.acknowledged(deleteMono);
    }
}
