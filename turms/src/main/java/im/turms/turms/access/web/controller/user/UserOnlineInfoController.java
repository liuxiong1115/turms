package im.turms.turms.access.web.controller.user;

import im.turms.common.TurmsCloseStatus;
import im.turms.common.constant.DeviceType;
import im.turms.common.constant.UserStatus;
import im.turms.turms.access.web.util.ResponseFactory;
import im.turms.turms.annotation.web.RequiredPermission;
import im.turms.turms.constant.CloseStatusFactory;
import im.turms.turms.manager.TurmsClusterManager;
import im.turms.turms.pojo.bo.UserOnlineInfo;
import im.turms.turms.pojo.domain.User;
import im.turms.turms.pojo.domain.UserLocation;
import im.turms.turms.pojo.dto.AcknowledgedDTO;
import im.turms.turms.pojo.dto.OnlineUserNumberDTO;
import im.turms.turms.pojo.dto.ResponseDTO;
import im.turms.turms.pojo.dto.UpdateOnlineStatusDTO;
import im.turms.turms.service.user.UserService;
import im.turms.turms.service.user.onlineuser.OnlineUserService;
import im.turms.turms.service.user.onlineuser.UsersNearbyService;
import im.turms.turms.util.PageUtil;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
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
    public Mono<ResponseEntity<ResponseDTO<OnlineUserNumberDTO>>> countOnlineUsers(@RequestParam(required = false, defaultValue = "false") Boolean countByNodes) {
        if (countByNodes != null && countByNodes) {
            return ResponseFactory.okIfTruthy(onlineUserService.countOnlineUsersByNodes()
                    .map(idNumberMap -> {
                        int sum = 0;
                        for (Integer number : idNumberMap.values()) {
                            sum += number;
                        }
                        return new OnlineUserNumberDTO(sum, idNumberMap);
                    }));
        } else {
            return ResponseFactory.okIfTruthy(onlineUserService.countOnlineUsers()
                    .map(total -> new OnlineUserNumberDTO(total, null)));
        }
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
    public ResponseEntity<ResponseDTO<Collection<UserLocation>>> queryUserLocations(@RequestParam Long userId, @RequestParam(required = false) DeviceType deviceType) {
        SortedSet<UserLocation> userLocations = onlineUserService.getUserLocations(userId, deviceType);
        return ResponseFactory.okIfTruthy(userLocations);
    }

    @PutMapping("/statuses")
    @RequiredPermission(USER_ONLINE_INFO_UPDATE)
    public Mono<ResponseEntity<ResponseDTO<AcknowledgedDTO>>> updateUserOnlineStatus(
            @RequestParam Set<Long> userIds,
            @RequestParam(required = false) Set<DeviceType> deviceTypes,
            @RequestBody UpdateOnlineStatusDTO updateOnlineStatusDTO) {
        Mono<Boolean> updateMono;
        UserStatus onlineStatus = updateOnlineStatusDTO.getOnlineStatus();
        if (onlineStatus == UserStatus.OFFLINE) {
            updateMono = deviceTypes != null
                    ? onlineUserService.setUsersDevicesOffline(userIds, deviceTypes, CloseStatusFactory.get(TurmsCloseStatus.DISCONNECTED_BY_ADMIN))
                    : onlineUserService.setUsersOffline(userIds, CloseStatusFactory.get(TurmsCloseStatus.DISCONNECTED_BY_ADMIN));
        } else {
            updateMono = onlineUserService.updateOnlineUsersStatus(userIds, onlineStatus);
        }
        return ResponseFactory.acknowledged(updateMono);
    }
}
