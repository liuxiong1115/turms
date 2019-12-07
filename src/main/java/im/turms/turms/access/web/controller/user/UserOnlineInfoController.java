package im.turms.turms.access.web.controller.user;

import im.turms.turms.access.web.util.ResponseFactory;
import im.turms.turms.annotation.web.RequiredPermission;
import im.turms.turms.cluster.TurmsClusterManager;
import im.turms.turms.constant.AdminPermission;
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

@RestController
@RequestMapping("/users/online-infos")
public class UserOnlineInfoController {
    private final UserService userService;
    private final OnlineUserService onlineUserService;
    private final UsersNearbyService usersNearbyService;
    private final TurmsClusterManager turmsClusterManager;

    public UserOnlineInfoController(UserService userService, OnlineUserService onlineUserService, UsersNearbyService usersNearbyService, TurmsClusterManager turmsClusterManager) {
        this.userService = userService;
        this.onlineUserService = onlineUserService;
        this.usersNearbyService = usersNearbyService;
        this.turmsClusterManager = turmsClusterManager;
    }

    @GetMapping("/count")
    @RequiredPermission(AdminPermission.USER_QUERY)
    public Mono<ResponseEntity<ResponseDTO<TotalDTO>>> countOnlineUsers() {
        return ResponseFactory.total(onlineUserService.countOnlineUsers());
    }

    /**
     * @param number only works when userIds is null or empty
     */
    @GetMapping("/statuses")
    @RequiredPermission(AdminPermission.USER_QUERY)
    public Mono<ResponseEntity<ResponseDTO<Collection<UserOnlineInfo>>>> queryOnlineUsersStatus(
            @RequestParam(required = false) Set<Long> userIds,
            @RequestParam(defaultValue = "20") Integer number,
            @RequestParam(defaultValue = "true") Boolean checkIfExists) {
        if (userIds != null && !userIds.isEmpty()) {
            List<Mono<UserOnlineInfo>> userOnlineInfoMonos = new ArrayList<>(userIds.size());
            for (Long userId : userIds) {
                Mono<UserOnlineInfo> userOnlineInfoMno = onlineUserService.queryUserOnlineInfo(userId);
                userOnlineInfoMno = userOnlineInfoMno.flatMap(info -> {
                    if (info.getUserStatus() == UserStatus.OFFLINE && checkIfExists) {
                        return userService.userExists(userId)
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
            if (number > turmsClusterManager.getTurmsProperties().getSecurity()
                    .getMaxQueryOnlineUsersStatusPerRequest()) {
                throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS);
            }
            Flux<UserOnlineInfo> userOnlineInfoFlux = onlineUserService.queryUserOnlineInfos(number);
            return ResponseFactory.okIfTruthy(userOnlineInfoFlux);
        }
    }

    @PutMapping("/statuses")
    @RequiredPermission(AdminPermission.USER_UPDATE)
    public Mono<ResponseEntity<ResponseDTO<AcknowledgedDTO>>> updateUserOnlineStatus(
            @RequestParam Long userId,
            @RequestParam(required = false) Set<DeviceType> deviceTypes,
            @RequestBody UpdateOnlineStatusDTO updateOnlineStatusDTO) {
        Mono<Boolean> updated;
        UserStatus onlineStatus = updateOnlineStatusDTO.getOnlineStatus();
        if (onlineStatus == UserStatus.OFFLINE) {
            if (deviceTypes != null) {
                updated = onlineUserService.setUserDevicesOffline(userId, deviceTypes, CloseStatus.NORMAL);
            } else {
                updated = onlineUserService.setUserOffline(userId, CloseStatus.NORMAL);
            }
        } else {
            updated = onlineUserService.updateOnlineUserStatus(userId, onlineStatus);
        }
        return ResponseFactory.acknowledged(updated);
    }

    @GetMapping("/users-nearby")
    @RequiredPermission(AdminPermission.USER_QUERY)
    public Mono<ResponseEntity<ResponseDTO<Collection<User>>>> queryUsersNearby(
            @RequestParam Long userId,
            @RequestParam(required = false) DeviceType deviceType,
            @RequestParam(required = false) Integer maxPeopleNumber,
            @RequestParam(required = false) Double maxDistance) {
        Flux<User> usersNearby = usersNearbyService.queryUsersProfilesNearby(userId, deviceType, maxPeopleNumber, maxDistance);
        return ResponseFactory.okIfTruthy(usersNearby);
    }

    @GetMapping("/locations")
    @RequiredPermission(AdminPermission.USER_QUERY)
    public ResponseEntity<ResponseDTO<Collection<UserLocation>>> queryUserLocations(@RequestParam Long userId) {
        SortedSet<UserLocation> userLocations = onlineUserService.getUserLocations(userId);
        return ResponseFactory.okIfTruthy(userLocations);
    }
}
