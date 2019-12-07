package im.turms.turms.access.web.controller.user;

import im.turms.turms.access.web.util.ResponseFactory;
import im.turms.turms.annotation.web.RequiredPermission;
import im.turms.turms.common.PageUtil;
import im.turms.turms.pojo.domain.Group;
import im.turms.turms.pojo.dto.PaginationDTO;
import im.turms.turms.pojo.dto.ResponseDTO;
import im.turms.turms.service.group.GroupMemberService;
import im.turms.turms.service.group.GroupService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collection;

import static im.turms.turms.constant.AdminPermission.USER_JOINED_GROUP_QUERY;

@RestController
@RequestMapping("/users/groups")
public class UserGroupController {
    private final GroupService groupService;
    private final GroupMemberService groupMemberService;
    private final PageUtil pageUtil;

    public UserGroupController(GroupService groupService, GroupMemberService groupMemberService, PageUtil pageUtil) {
        this.groupService = groupService;
        this.groupMemberService = groupMemberService;
        this.pageUtil = pageUtil;
    }

    @GetMapping
    @RequiredPermission(USER_JOINED_GROUP_QUERY)
    public Mono<ResponseEntity<ResponseDTO<Collection<Group>>>> queryUserJoinedGroups(
            @RequestParam Long userId,
            @RequestParam(required = false) Integer size) {
        size = pageUtil.getSize(size);
        Flux<Group> groupsFlux = groupService.queryUserJoinedGroups(userId, 0, size);
        return ResponseFactory.okIfTruthy(groupsFlux);
    }

    @GetMapping("/page")
    @RequiredPermission(USER_JOINED_GROUP_QUERY)
    public Mono<ResponseEntity<ResponseDTO<PaginationDTO<Group>>>> queryUserJoinedGroups(
            @RequestParam Long userId,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(required = false) Integer size) {
        size = pageUtil.getSize(size);
        Mono<Long> count = groupMemberService.countUserJoinedGroupsIds(userId);
        Flux<Group> groupsFlux = groupService.queryUserJoinedGroups(userId, page, size);
        return ResponseFactory.page(count, groupsFlux);
    }
}
