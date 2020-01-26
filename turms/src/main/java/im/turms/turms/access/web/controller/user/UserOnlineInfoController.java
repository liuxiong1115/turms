package im.turms.turms.access.web.controller.user;

import im.turms.turms.access.web.util.ResponseFactory;
import im.turms.turms.annotation.web.RequiredPermission;
import im.turms.turms.cluster.TurmsClusterManager;
import im.turms.turms.common.PageUtil;
import im.turms.turms.constant.DeviceType;
import im.turms.turms.constant.UserStatus;
import im.turms.turms.pojo.bo.UserOnlineInfo;
import im.turms.turms.pojo.domain.User;
import im.turms.turms.pojo.domain.UserLocation;
import im.turms.turms.pojo.dto.AcknowledgedDTO;
import im.turms.turms.pojo.dto.ResponseDTO;
import im.turms.turms.pojo.dto.TotalDTO;
import im.turms.turms.pojo.dto.UpdateOnlineStatusDTO;
import im.turms.turms.service.user.UserService;
import im.turms.turms.service.user.onlineuser.OnlineUserService;
import im.turms.turms.service.user.onlineuser.UsersNearbyService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.socket.CloseStatus;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.*;

import static im.turms.turms.constant.AdminPermission.*;

@RestController
@RequestMapping("/users/online-infos")
public class UserOnlineInfoController {
    private final UserService userService;
    private final OnlineUserService onlineUserService;
    private final UsersNearbyService usersNearbyService;
    private final TurmsClusterManager turmsClusterManager;
    private final PageUtil pageUtil;

    public UserOnlineInfoController(UserService userService, OnlineUserService onlineUserService, UsersNearbyService usersNearbyService, TurmsClusterManager turmsClusterManager, PageUtil pageUtil) {
        this.userService = userService;
        this.onlineUserService = onlineUserService;
        this.usersNearbyService = usersNearbyService;
        this.turmsClusterManager = turmsClusterManager;
        this.pageUtil = pageUtil;
    }

    @GetMapping("/count")
    @RequiredPermission(STATISTICS_USER_QUERY)
    public Mono<ResponseEntity<ResponseDTO<TotalDTO>>> countOnlineUsers() {
        return ResponseFactory.total(onlineUserService.countOnlineUsers());
    }

    /**
     * @param size only works when ids is null or empty
     */
    @GetMapping("/statuses")
    @RequiredPermission(USER_ONLINE_INFO_QUERY)
    public Mono<ResponseEntity<ResponseDTO<Collection<UserOnlineInfo>>>> queryOnlineUsersStatus(
            @RequestParam(required = false) Set<Long> ids,
            @RequestParam(required = false) Integer size,
            @RequestParam(defaultValue = "true") Boolean shouldCheckIfExists) {
        if (ids != null && !ids.isEmpty()) {
            List<Mono<UserOnlineInfo>> userOnlineInfoMonos = new ArrayList<>(ids.size());
            for (Long userId : ids) {
                Mono<UserOnlineInfo> userOnlineInfoMno = onlineUserService.queryUserOnlineInfo(userId);
                userOnlineInfoMno = userOnlineInfoMno.flatMap(info -> {
                    if (info.getUserStatus(false) == UserStatus.OFFLINE && shouldCheckIfExists) {
                        return userService.userExists(userId, false)
                                .flatMap(exists -> {
                                    if (exists) {
                                        return Mono.just(info);
                                    } else {
                                        return Mono.empty();
                                    }
                                });
                    } else {
                        return Mono.just(info);
                    }
                });
                userOnlineInfoMonos.add(userOnlineInfoMno);
            }
            return ResponseFactory.okIfTruthy(Flux.merge(userOnlineInfoMonos));
        } else {
            size = pageUtil.getSize(size);
            if (size > turmsClusterManager.getTurmsProperties().getSecurity()
                    .getMaxAvailableOnlineUsersStatusPerRequest()) {
                throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS);
            }
            Flux<UserOnlineInfo> userOnlineInfoFlux = onlineUserService.queryUserOnlineInfos(size);
            return ResponseFactory.okIfTruthy(userOnlineInfoFlux);
        }
    }

    @GetMapping("/users-nearby")
    @RequiredPermission(USER_ONLINE_INFO_QUERY)
    public Mono<ResponseEntity<ResponseDTO<Collection<User>>>> queryUsersNearby(
            @RequestParam Long userId,
            @RequestParam(required = false) DeviceType deviceType,
            @RequestParam(required = false) Integer maxPeopleNumber,
            @RequestParam(required = false) Double maxDistance) {
        Flux<User> usersNearby = usersNearbyService.queryUsersProfilesNearby(userId, deviceType, maxPeopleNumber, maxDistance);
        return ResponseFactory.okIfTruthy(usersNearby);
    }

    @GetMapping("/locations")
    @RequiredPermission(USER_ONLINE_INFO_QUERY)
    public ResponseEntity<ResponseDTO<Collection<UserLocation>>> queryUserLocations(@RequestParam Long userId) {
        SortedSet<UserLocation> userLocations = onlineUserService.getUserLocations(userId);
        return ResponseFactory.okIfTruthy(userLocations);
    }

    @PutMapping("/statuses")
    @RequiredPermission(USER_ONLINE_INFO_UPDATE)
    public Mono<ResponseEntity<ResponseDTO<AcknowledgedDTO>>> updateUserOnlineStatus(
            @RequestParam Set<Long> userIds,
            @RequestParam(required = false) Set<DeviceType> deviceTypes,
            @RequestBody UpdateOnlineStatusDTO updateOnlineStatusDTO) {
        Mono<Boolean> updated;
        UserStatus onlineStatus = updateOnlineStatusDTO.getOnlineStatus();
        if (onlineStatus == UserStatus.OFFLINE) {
            if (deviceTypes != null) {
                updated = onlineUserService.setUsersDevicesOffline(userIds, deviceTypes, CloseStatus.NORMAL);
            } else {
                updated = onlineUserService.setUsersOffline(userIds, CloseStatus.NORMAL);
            }
        } else {
            updated = onlineUserService.updateOnlineUsersStatus(userIds, onlineStatus);
        }
        return ResponseFactory.acknowledged(updated);
    }
}
