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

package im.turms.turms.service.user.onlineuser;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.davidmoten.rtree2.geometry.internal.PointFloat;
import com.hazelcast.cluster.Member;
import im.turms.common.TurmsCloseStatus;
import im.turms.common.TurmsStatusCode;
import im.turms.common.constant.DeviceType;
import im.turms.common.constant.UserStatus;
import im.turms.common.exception.TurmsBusinessException;
import im.turms.turms.annotation.constraint.DeviceTypeConstraint;
import im.turms.turms.constant.CloseStatusFactory;
import im.turms.turms.manager.*;
import im.turms.turms.constant.Common;
import im.turms.turms.plugin.UserOnlineStatusChangeHandler;
import im.turms.turms.pojo.bo.UserOnlineInfo;
import im.turms.turms.pojo.domain.UserLocation;
import im.turms.turms.pojo.domain.UserLoginLog;
import im.turms.turms.pojo.domain.UserOnlineUserNumber;
import im.turms.turms.property.TurmsProperties;
import im.turms.turms.service.user.UserLocationService;
import im.turms.turms.service.user.UserLoginLogService;
import im.turms.turms.service.user.onlineuser.manager.OnlineUserManager;
import im.turms.turms.task.*;
import im.turms.turms.util.ReactorUtil;
import io.netty.util.HashedWheelTimer;
import io.netty.util.Timeout;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.reactive.socket.CloseStatus;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.Mono;
import reactor.math.MathFlux;

import javax.annotation.Nullable;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import static im.turms.turms.constant.Common.ALL_DEVICE_TYPES;
import static im.turms.turms.constant.Common.EMPTY_USER_LOCATION;
import static im.turms.turms.manager.TurmsClusterManager.HASH_SLOTS_NUMBER;

@Service
@Validated
public class OnlineUserService {
    private static final int DEFAULT_ONLINE_USERS_MANAGER_CAPACITY = 1024;
    private static final String LOG_ONLINE_USERS_NUMBER_TASK = "loun";
    private final ReactiveMongoTemplate mongoTemplate;
    private final TurmsClusterManager turmsClusterManager;
    private final TurmsPluginManager turmsPluginManager;
    private final ReasonCacheManager reasonCacheManager;
    private final UsersNearbyService usersNearbyService;
    private final IrresponsibleUserService irresponsibleUserService;
    private final UserLoginLogService userLoginLogService;
    private final UserLocationService userLocationService;
    private final HashedWheelTimer heartbeatTimer;
    private final TrivialTaskManager onlineUsersNumberPersisterTimer;
    private final TurmsTaskManager turmsTaskManager;
    private final Queue<Timeout> disconnectionTasks;
    private final ReentrantLock disconnectionLock;
    // user ID -> Dummy Value.
    // Note that only the online status will be cached and the offline status will not be cached
    // because it's unacceptable if a user is indeed online but considered as offline
    private final Cache<Long, Object> remoteOnlineUsersCache;
    // user ID -> Online information.
    // Note that only the online information will be cached and the offline information will not be cached
    // because it's unacceptable if a user is indeed online but considered as offline
    private final Cache<Long, UserOnlineInfo> remoteUsersOnlineInfoCache;
    private final boolean locationEnabled;
    private final boolean pluginEnabled;
    /**
     * Integer(Slot) -> Long(userId) -> OnlineUserManager
     */
    private final List<Map<Long, OnlineUserManager>> onlineUsersManagerAtSlots;

    public OnlineUserService(
            TurmsClusterManager turmsClusterManager,
            UsersNearbyService usersNearbyService,
            ReactiveMongoTemplate mongoTemplate,
            UserLoginLogService userLoginLogService,
            UserLocationService userLocationService,
            TurmsTaskManager turmsTaskManager,
            TurmsPluginManager turmsPluginManager,
            TrivialTaskManager trivialTaskManager,
            IrresponsibleUserService irresponsibleUserService,
            ReasonCacheManager reasonCacheManager) {
        this.turmsClusterManager = turmsClusterManager;
        this.usersNearbyService = usersNearbyService;
        turmsClusterManager.addListenerOnMembersChange(
                membershipEvent -> {
                    onClusterMembersChange();
                    return null;
                });
        this.mongoTemplate = mongoTemplate;
        this.userLoginLogService = userLoginLogService;
        this.userLocationService = userLocationService;
        this.turmsTaskManager = turmsTaskManager;
        this.turmsPluginManager = turmsPluginManager;
        this.heartbeatTimer = new HashedWheelTimer();
        this.onlineUsersNumberPersisterTimer = trivialTaskManager;
        TurmsProperties turmsProperties = turmsClusterManager.getTurmsProperties();
        if (turmsProperties.getCache().getRemoteUserOnlineStatusCacheMaxSize() > 0
                && turmsProperties.getCache().getRemoteUserOnlineStatusExpireAfter() > 0) {
            remoteOnlineUsersCache = Caffeine.newBuilder()
                    .maximumSize(turmsProperties.getCache().getRemoteUserOnlineStatusCacheMaxSize())
                    .expireAfterWrite(Duration.ofSeconds(turmsProperties.getCache().getRemoteUserOnlineStatusExpireAfter()))
                    .build();
        } else {
            remoteOnlineUsersCache = null;
        }
        if (turmsProperties.getCache().getRemoteUserOnlineInfoCacheMaxSize() > 0
                && turmsProperties.getCache().getRemoteUserOnlineInfoExpireAfter() > 0) {
            remoteUsersOnlineInfoCache = Caffeine.newBuilder()
                    .maximumSize(turmsProperties.getCache().getRemoteUserOnlineInfoCacheMaxSize())
                    .expireAfterWrite(Duration.ofSeconds(turmsProperties.getCache().getRemoteUserOnlineInfoExpireAfter()))
                    .build();
        } else {
            remoteUsersOnlineInfoCache = null;
        }
        onlineUsersManagerAtSlots = new ArrayList(Arrays.asList(new HashMap[HASH_SLOTS_NUMBER]));
        locationEnabled = turmsProperties.getUser().getLocation().isEnabled();
        rescheduleOnlineUsersNumberPersister();
        TurmsProperties.addListeners(properties -> {
            rescheduleOnlineUsersNumberPersister();
            return null;
        });
        pluginEnabled = turmsProperties.getPlugin().isEnabled();
        this.irresponsibleUserService = irresponsibleUserService;
        if (irresponsibleUserService.isAllowIrresponsibleUsersAfterResponsibilityChanged() ||
                irresponsibleUserService.isAllowIrresponsibleUsersWhenConnecting()) {
            disconnectionTasks = new LinkedList<>();
            disconnectionLock = new ReentrantLock(true);
        } else {
            disconnectionTasks = null;
            disconnectionLock = null;
        }
        this.reasonCacheManager = reasonCacheManager;
    }

    private void rescheduleOnlineUsersNumberPersister() {
        onlineUsersNumberPersisterTimer.reschedule(LOG_ONLINE_USERS_NUMBER_TASK,
                turmsClusterManager.getTurmsProperties().getUser().getOnlineUsersNumberPersisterCron(),
                this::checkAndSaveOnlineUsersNumber);
    }

    public void setIrresponsibleUsersOffline(boolean isServerClosing) {
        Pair<Integer, Integer> slotIndexRange = turmsClusterManager.getResponsibleSlotIndexRange();
        for (int index = 0; index < HASH_SLOTS_NUMBER; index++) {
            boolean isIrresponsible = slotIndexRange.getLeft() > index || index >= slotIndexRange.getRight();
            if (isIrresponsible) {
                Member member = turmsClusterManager.getClusterMemberBySlotIndex(index);
                if (member != null) {
                    TurmsCloseStatus status = isServerClosing ? TurmsCloseStatus.SERVER_CLOSED : TurmsCloseStatus.REDIRECT;
                    String address = turmsClusterManager.getAddress(member);
                    setUsersOfflineBySlotIndex(index, CloseStatusFactory.get(status, address));
                } else {
                    // This should never happen
                    setUsersOfflineBySlotIndex(index, CloseStatusFactory.get(TurmsCloseStatus.SERVER_ERROR, "Cannot find a server responsible for the user"));
                }
            }
        }
    }

    public void setIrresponsibleUsersOffline(boolean isServerClosing, int jitter) {
        Pair<Integer, Integer> slotIndexRange = turmsClusterManager.getResponsibleSlotIndexRange();
        int irresponsibleSlotNumber = TurmsClusterManager.HASH_SLOTS_NUMBER + slotIndexRange.getRight() - slotIndexRange.getLeft();
        int step = Math.max(jitter * 2 / irresponsibleSlotNumber, 1);
        int start = Math.max(irresponsibleUserService.getClearUpIrresponsibleUsersAfter() - jitter, 0);
        UUID localMemberId = turmsClusterManager.getLocalMember().getUuid();
        for (int index = 0; index < HASH_SLOTS_NUMBER; index++) {
            boolean isIrresponsible = slotIndexRange.getLeft() > index || index >= slotIndexRange.getRight();
            if (isIrresponsible) {
                Member member = turmsClusterManager.getClusterMemberBySlotIndex(index);
                if (member != null) {
                    TurmsCloseStatus status = isServerClosing ? TurmsCloseStatus.SERVER_CLOSED : TurmsCloseStatus.REDIRECT;
                    String address = turmsClusterManager.getAddress(member);
                    irresponsibleUserService.putAll(getUsersBySlotIndex(index), localMemberId);
                    int finalIndex = index;
                    Timeout timeout = irresponsibleUserService.getIrresponsibleUsersCleanerTimer().newTimeout(ignored -> {
                        setUsersOfflineBySlotIndex(finalIndex, CloseStatusFactory.get(status, address));
                    }, start + index * step, TimeUnit.SECONDS);
                    disconnectionTasks.offer(timeout);
                } else {
                    // This should never happen
                    setUsersOfflineBySlotIndex(index, CloseStatusFactory.get(TurmsCloseStatus.SERVER_ERROR, "Cannot find a server responsible for the user"));
                }
            }
        }
    }

    public void setUsersOfflineBySlotIndex(@NotNull Integer slotIndex, @NotNull CloseStatus closeStatus) {
        if (slotIndex >= 0 && slotIndex < HASH_SLOTS_NUMBER) {
            Map<Long, OnlineUserManager> managerMap = getOnlineUsersManager(slotIndex);
            if (managerMap != null) {
                setManagersOffline(closeStatus, managerMap.values());
            }
        }
    }

    /**
     * Use LinkedList instead of Set for better performance
     */
    public List<Long> getUsersBySlotIndex(@NotNull Integer slotIndex) {
        List<Long> userIds = new LinkedList<>();
        if (slotIndex >= 0 && slotIndex < HASH_SLOTS_NUMBER) {
            Map<Long, OnlineUserManager> managerMap = getOnlineUsersManager(slotIndex);
            if (managerMap != null) {
                for (OnlineUserManager manager : managerMap.values()) {
                    if (manager != null) {
                        userIds.add(manager.getOnlineUserInfo().getUserId());
                    }
                }
            }
        }
        return userIds;
    }

    public boolean setLocalUserDevicesOffline(
            @NotNull Long userId,
            @NotEmpty Set<@DeviceTypeConstraint DeviceType> deviceTypes,
            @NotNull CloseStatus closeStatus) {
        OnlineUserManager manager = getLocalOnlineUserManager(userId);
        if (manager != null) {
            Date now = new Date();
            for (DeviceType deviceType : deviceTypes) {
                OnlineUserManager.Session session = manager.getSession(deviceType);
                if (session != null) {
                    clearSession(userId, manager, session, now, closeStatus);
                }
            }
            clearOnlineUserManagerAndTriggerPlugins(closeStatus, manager, userId);
            return true;
        } else {
            return false;
        }
    }

    private void setManagersOffline(@NotNull CloseStatus closeStatus, @NotNull Collection<OnlineUserManager> managers) {
        for (OnlineUserManager manager : managers) {
            if (manager != null) {
                ConcurrentMap<DeviceType, OnlineUserManager.Session> sessionMap = manager.getOnlineUserInfo().getSessionMap();
                Date now = new Date();
                Long userId = manager.getOnlineUserInfo().getUserId();
                for (OnlineUserManager.Session session : sessionMap.values()) {
                    clearSession(userId, manager, session, now, closeStatus);
                }
                clearOnlineUserManagerAndTriggerPlugins(closeStatus, manager, userId);
            }
        }
    }

    private void clearSession(Long userId, OnlineUserManager manager, OnlineUserManager.Session session, Date date, CloseStatus closeStatus) {
        Long logId = session.getLogId();
        if (logId != null) {
            userLoginLogService
                    .updateLogoutDate(logId, date)
                    .subscribe();
        }
        DeviceType deviceType = session.getDeviceType();
        manager.setDeviceOffline(deviceType, closeStatus);
        usersNearbyService.removeUserLocation(userId, deviceType);
        irresponsibleUserService.remove(userId);
        String sessionId = session.getWebSocketSession().getId();
        if (reasonCacheManager.shouldCacheDisconnectionReason(userId, deviceType, sessionId)) {
            reasonCacheManager.cacheDisconnectionReason(userId, deviceType, sessionId, closeStatus.getCode());
        }
    }

    public void clearOnlineUserManager(@NotNull Long userId) {
        int slotIndex = turmsClusterManager.getSlotIndexByUserId(userId);
        onlineUsersManagerAtSlots.get(slotIndex).remove(userId);
    }

    private void clearOnlineUserManagerAndTriggerPlugins(@NotNull CloseStatus closeStatus, OnlineUserManager manager, Long userId) {
        if (manager.getSessionsNumber() == 0) {
            clearOnlineUserManager(userId);
        }
        if (pluginEnabled) {
            List<UserOnlineStatusChangeHandler> handlerList = turmsPluginManager.getUserOnlineStatusChangeHandlerList();
            if (!handlerList.isEmpty()) {
                for (UserOnlineStatusChangeHandler handler : handlerList) {
                    handler.goOffline(manager, closeStatus).subscribe();
                }
            }
        }
    }

    public boolean setLocalUserOffline(
            @NotNull Long userId,
            @NotNull CloseStatus closeStatus) {
        return setLocalUserDevicesOffline(userId, ALL_DEVICE_TYPES, closeStatus);
    }

    public boolean setLocalUserDeviceOffline(
            @NotNull Long userId,
            @NotNull @DeviceTypeConstraint DeviceType deviceType,
            @NotNull CloseStatus closeStatus) {
        return setLocalUserDevicesOffline(userId, Collections.singleton(deviceType), closeStatus);
    }

    public Mono<Boolean> setUsersOffline(
            @NotEmpty Set<Long> userIds,
            @NotNull CloseStatus closeStatus) {
        List<Mono<Boolean>> list = new ArrayList<>(userIds.size());
        for (Long userId : userIds) {
            list.add(setUserOffline(userId, closeStatus));
        }
        return Flux.merge(list).all(value -> value);
    }

    public Mono<Boolean> setUserOffline(
            @NotNull Long userId,
            @NotNull CloseStatus closeStatus) {
        boolean responsible = turmsClusterManager.isCurrentNodeResponsibleByUserId(userId);
        if (responsible) {
            setLocalUserOffline(userId, closeStatus);
            return Mono.just(true);
        } else {
            Member member;
            UUID memberId = irresponsibleUserService.getMemberIdIfExists(userId);
            if (memberId != null) {
                member = turmsClusterManager.getMemberById(memberId);
            } else {
                member = turmsClusterManager.getMemberByUserId(userId);
            }
            if (member != null) {
                Future<Boolean> future = turmsClusterManager
                        .getExecutor()
                        .submitToMember(new SetUserOfflineTask(userId, null, closeStatus.getCode()), member);
                return ReactorUtil.future2Mono(future, turmsClusterManager.getTurmsProperties().getRpc().getTimeoutDuration());
            } else {
                return Mono.just(false);
            }
        }
    }

    public Mono<Boolean> setUsersDevicesOffline(
            @NotEmpty Set<Long> userIds,
            @NotEmpty Set<@DeviceTypeConstraint DeviceType> deviceTypes,
            @NotNull CloseStatus closeStatus) {
        List<Mono<Boolean>> monos = new ArrayList<>(userIds.size());
        for (Long userId : userIds) {
            monos.add(setUserDevicesOffline(userId, deviceTypes, closeStatus));
        }
        return Flux.merge(monos).all(value -> value);
    }

    public Mono<Boolean> setUserDevicesOffline(
            @NotNull Long userId,
            @NotEmpty Set<@DeviceTypeConstraint DeviceType> deviceTypes,
            @NotNull CloseStatus closeStatus) {
        boolean responsible = turmsClusterManager.isCurrentNodeResponsibleByUserId(userId);
        if (responsible) {
            return Mono.just(setLocalUserDevicesOffline(userId, deviceTypes, closeStatus));
        } else {
            Member member;
            UUID memberId = irresponsibleUserService.getMemberIdIfExists(userId);
            if (memberId != null) {
                member = turmsClusterManager.getMemberById(memberId);
            } else {
                member = turmsClusterManager.getMemberByUserId(userId);
            }
            if (member != null) {
                Set<Integer> types = new HashSet<>(deviceTypes.size());
                for (DeviceType deviceType : deviceTypes) {
                    types.add(deviceType.ordinal());
                }
                Future<Boolean> future = turmsClusterManager
                        .getExecutor()
                        .submitToMember(new SetUserOfflineTask(userId, types, closeStatus.getCode()), member);
                return ReactorUtil.future2Mono(future, turmsClusterManager.getTurmsProperties().getRpc().getTimeoutDuration());
            } else {
                return Mono.just(false);
            }
        }
    }

    public int countLocalOnlineUsers() {
        int number = 0;
        for (Map<Long, OnlineUserManager> managersMap : onlineUsersManagerAtSlots) {
            if (managersMap != null) {
                number += managersMap.size();
            }
        }
        return number;
    }

    public Mono<Integer> countOnlineUsers() {
        Flux<Integer> futures = turmsTaskManager.callAll(new CountOnlineUsersTask(),
                turmsClusterManager.getTurmsProperties().getRpc().getTimeoutDuration());
        return MathFlux.sumInt(futures);
    }

    public Mono<Map<UUID, Integer>> countOnlineUsersByNodes() {
        Mono<Map<Member, Integer>> mono = turmsTaskManager.callAllAsMap(new CountOnlineUsersTask(),
                turmsClusterManager.getTurmsProperties().getRpc().getTimeoutDuration());
        return mono.map(map -> {
            Map<UUID, Integer> idNumberMap = new HashMap<>(map.size());
            for (Map.Entry<Member, Integer> entry : map.entrySet()) {
                idNumberMap.put(entry.getKey().getUuid(), entry.getValue());
            }
            return idNumberMap;
        });
    }

    public void updateHeartbeatTimestamp(
            @NotNull Long userId,
            @NotNull @DeviceTypeConstraint DeviceType deviceType) {
        OnlineUserManager onlineUserManager = getLocalOnlineUserManager(userId);
        if (onlineUserManager != null) {
            OnlineUserManager.Session session = onlineUserManager.getSession(deviceType);
            if (session != null) {
                session.setLastHeartbeatTimestamp(System.currentTimeMillis());
            }
        }
    }

    private Timeout newHeartbeatTimeout(@NotNull Long userId, @NotNull @DeviceTypeConstraint DeviceType deviceType, @NotNull OnlineUserManager.Session session) {
        int heartbeatTimeoutSeconds = turmsClusterManager.getTurmsProperties().getSession().getHeartbeatTimeoutSeconds();
        return heartbeatTimer.newTimeout(timeout -> {
                    long now = System.currentTimeMillis();
                    int elapsedTime = (int) ((now - session.getLastHeartbeatTimestamp()) / 1000);
                    if (elapsedTime > heartbeatTimeoutSeconds) {
                        setLocalUserDeviceOffline(userId, deviceType, CloseStatusFactory.get(TurmsCloseStatus.HEARTBEAT_TIMEOUT));
                    }
                },
                Math.max(heartbeatTimeoutSeconds / 3, 1),
                TimeUnit.SECONDS);
    }

    public void checkAndSaveOnlineUsersNumber() {
        if (turmsClusterManager.isCurrentMemberMaster()) {
            countOnlineUsers()
                    .flatMap(this::saveOnlineUsersNumber)
                    .subscribe();
        }
    }

    public Mono<UserOnlineUserNumber> saveOnlineUsersNumber(@NotNull Integer onlineUsersNumber) {
        UserOnlineUserNumber userOnlineUserNumber = new UserOnlineUserNumber(new Date(), onlineUsersNumber);
        return mongoTemplate.save(userOnlineUserNumber);
    }

    public Mono<TurmsStatusCode> addOnlineUser(
            @NotNull Long userId,
            @NotNull UserStatus userStatus,
            @NotNull DeviceType loggingInDeviceType,
            @Nullable Map<String, String> deviceDetails,
            @NotNull Integer ip,
            @Nullable PointFloat userLocation,
            @NotNull WebSocketSession webSocketSession,
            @NotNull FluxSink<WebSocketMessage> notificationSink) {
        Mono<UserLocation> locationIdMono = userLocation != null
                ? userLocationService.saveUserLocation(null, userId, loggingInDeviceType, userLocation.xFloat(), userLocation.yFloat(), new Date())
                : Mono.empty();
        return locationIdMono
                .onErrorReturn(EMPTY_USER_LOCATION)
                .defaultIfEmpty(EMPTY_USER_LOCATION)
                .flatMap(location -> {
                    Long locationId = EMPTY_USER_LOCATION != location ? location.getId() : null;
                    boolean logUserLogin = turmsClusterManager.getTurmsProperties().getLog().isLogUserLogin();
                    boolean triggerHandlers = pluginEnabled && !turmsPluginManager.getLogHandlerList().isEmpty();
                    if (logUserLogin || triggerHandlers) {
                        UserLoginLog log = new UserLoginLog(turmsClusterManager.generateRandomId(), userId,
                                new Date(), null, locationId, ip, loggingInDeviceType, deviceDetails);
                        Mono<TurmsStatusCode> codeMono = logUserLogin
                                ? userLoginLogService.save(log).onErrorReturn(log)
                                .thenReturn(setUpOnlineUserManager(userId, loggingInDeviceType, userStatus, location, webSocketSession, notificationSink, log.getId()))
                                : Mono.just(setUpOnlineUserManager(userId, loggingInDeviceType, userStatus, location, webSocketSession, notificationSink, log.getId()));
                        if (triggerHandlers) {
                            codeMono = codeMono.doOnSuccess(turmsStatusCode -> {
                                try {
                                    userLoginLogService.triggerLogHandlers(log).subscribe();
                                } catch (Exception ignored) {
                                }
                            });
                        }
                        return codeMono;
                    } else {
                        Mono<TurmsStatusCode> mono = Mono.just(setUpOnlineUserManager(userId, loggingInDeviceType, userStatus, location, webSocketSession, notificationSink, null));
                        return pluginEnabled
                                ? mono.flatMap(statusCode -> userLoginLogService.triggerLogHandlers(userId, ip, loggingInDeviceType, deviceDetails, locationId).thenReturn(statusCode))
                                : mono;
                    }
                });
    }

    /**
     * @return Posible values: OK, NOT_RESPONSIBLE, SESSION_SIMULTANEOUS_CONFLICTS_OFFLINE
     */
    private TurmsStatusCode setUpOnlineUserManager(
            @NotNull Long userId,
            @NotNull DeviceType loggingInDeviceType,
            @NotNull UserStatus userStatus,
            @NotNull UserLocation location,
            @NotNull WebSocketSession webSocketSession,
            @NotNull FluxSink<WebSocketMessage> notificationSink,
            @Nullable Long logId) {
        Integer slotIndex = turmsClusterManager.getSlotIndexByUserIdForCurrentNode(userId);
        if (slotIndex == null) {
            return TurmsStatusCode.NOT_RESPONSIBLE;
        } else {
            OnlineUserManager onlineUserManager = getLocalOnlineUserManager(userId);
            // If the user's devices are already online
            if (onlineUserManager != null) {
                onlineUserManager.setUserOnlineStatus(
                        userStatus == UserStatus.OFFLINE || userStatus == UserStatus.UNRECOGNIZED ?
                                UserStatus.AVAILABLE : userStatus);
                try {
                    onlineUserManager.addOnlineDeviceType(
                            loggingInDeviceType,
                            location == EMPTY_USER_LOCATION ? null : location,
                            webSocketSession,
                            notificationSink,
                            null,
                            logId);
                } catch (TurmsBusinessException e) {
                    return e.getCode();
                }
            } else {
                onlineUserManager = new OnlineUserManager(
                        userId,
                        userStatus,
                        loggingInDeviceType,
                        location == EMPTY_USER_LOCATION ? null : location,
                        webSocketSession,
                        notificationSink,
                        null,
                        logId);
            }
            if (turmsClusterManager.getTurmsProperties().getSession().getHeartbeatTimeoutSeconds() > 0) {
                Timeout heartbeatTimeout = newHeartbeatTimeout(
                        userId,
                        loggingInDeviceType,
                        onlineUserManager.getSession(loggingInDeviceType));
                onlineUserManager.getSession(loggingInDeviceType).setHeartbeatTimeout(heartbeatTimeout);
            }
            getOrAddOnlineUsersManager(slotIndex).put(userId, onlineUserManager);
            if (pluginEnabled) {
                List<UserOnlineStatusChangeHandler> handlerList = turmsPluginManager.getUserOnlineStatusChangeHandlerList();
                if (!handlerList.isEmpty()) {
                    for (UserOnlineStatusChangeHandler handler : handlerList) {
                        try {
                            handler.goOnline(onlineUserManager, loggingInDeviceType).subscribe();
                        } catch (Exception ignored) {
                        }
                    }
                }
            }
            return TurmsStatusCode.OK;
        }
    }

    private Map<Long, OnlineUserManager> getOrAddOnlineUsersManager(@NotNull Integer slotIndex) {
        if (slotIndex < 0 || slotIndex >= HASH_SLOTS_NUMBER) {
            throw new IllegalArgumentException();
        }
        Map<Long, OnlineUserManager> userManagerMap = onlineUsersManagerAtSlots.get(slotIndex);
        if (userManagerMap == null) {
            Map<Long, OnlineUserManager> map = new ConcurrentHashMap<>(DEFAULT_ONLINE_USERS_MANAGER_CAPACITY);
            onlineUsersManagerAtSlots.set(slotIndex, map);
            return map;
        } else {
            return userManagerMap;
        }
    }

    private Map<Long, OnlineUserManager> getOnlineUsersManager(@NotNull Integer slotIndex) {
        if (slotIndex >= 0 && slotIndex < onlineUsersManagerAtSlots.size()) {
            return onlineUsersManagerAtSlots.get(slotIndex);
        } else {
            return null;
        }
    }

    public OnlineUserManager getLocalOnlineUserManager(@NotNull Long userId) {
        Integer slotIndex = turmsClusterManager.getSlotIndexByUserIdForCurrentNode(userId);
        if (slotIndex != null) {
            Map<Long, OnlineUserManager> managersMap = getOrAddOnlineUsersManager(slotIndex);
            return managersMap.get(userId);
        } else {
            return null;
        }
    }

    public Mono<Boolean> updateOnlineUsersStatus(@NotEmpty Set<Long> userIds, @NotNull UserStatus userStatus) {
        List<Mono<Boolean>> monos = new ArrayList<>(userIds.size());
        for (Long userId : userIds) {
            monos.add(updateOnlineUserStatus(userId, userStatus));
        }
        return Flux.merge(monos).all(value -> value);
    }

    public Mono<Boolean> updateOnlineUserStatus(@NotNull Long userId, @NotNull UserStatus userStatus) {
        if (userStatus == UserStatus.UNRECOGNIZED || userStatus == UserStatus.OFFLINE) {
            String failedReason = userStatus == UserStatus.UNRECOGNIZED ?
                    "The user status must not be UNRECOGNIZED" :
                    "The online user status must not be OFFLINE";
            throw TurmsBusinessException.get(TurmsStatusCode.ILLEGAL_ARGUMENTS, failedReason);
        }
        if (turmsClusterManager.isCurrentNodeResponsibleByUserId(userId)) {
            OnlineUserManager manager = getLocalOnlineUserManager(userId);
            if (manager != null) {
                return Mono.just(manager.setUserOnlineStatus(userStatus));
            } else {
                return Mono.just(false);
            }
        } else {
            Member member;
            UUID memberId = irresponsibleUserService.getMemberIdIfExists(userId);
            if (memberId != null) {
                member = turmsClusterManager.getMemberById(memberId);
            } else {
                member = turmsClusterManager.getMemberByUserId(userId);
            }
            UpdateOnlineUserStatusTask task = new UpdateOnlineUserStatusTask(userId, userStatus.getNumber());
            Future<Boolean> future = turmsClusterManager.getExecutor()
                    .submitToMember(task, member);
            return ReactorUtil.future2Mono(future, turmsClusterManager.getTurmsProperties().getRpc().getTimeoutDuration());
        }
    }

    public Mono<UserOnlineInfo> queryUserOnlineInfo(@NotNull Long userId) {
        boolean onlyRespondOnlineStatus = turmsClusterManager.getTurmsProperties().getUser().isOnlyRespondOnlineStatusWhenQueryOnlineInfo();
        if (turmsClusterManager.isCurrentNodeResponsibleByUserId(userId)) {
            OnlineUserManager localOnlineUserManager = getLocalOnlineUserManager(userId);
            if (localOnlineUserManager != null) {
                UserOnlineInfo onlineUserInfo = localOnlineUserManager.getOnlineUserInfo();
                if (onlyRespondOnlineStatus) {
                    return Mono.just(UserOnlineInfo.builder()
                            .userId(userId)
                            // Reduce the possibilities of OnlineStatus to unify
                            // the possibilities when querying a remote user's status
                            .userStatus(onlineUserInfo.getUserStatus() != UserStatus.OFFLINE ? UserStatus.AVAILABLE : UserStatus.OFFLINE)
                            .build());
                } else {
                    return Mono.just(localOnlineUserManager.getOnlineUserInfo());
                }
            } else {
                return Mono.just(UserOnlineInfo.builder()
                        .userId(userId)
                        .userStatus(UserStatus.OFFLINE)
                        .build());
            }
        } else {
            UUID memberId = irresponsibleUserService.getMemberIdIfExists(userId);
            Member member = memberId != null
                    ? turmsClusterManager.getMemberById(memberId)
                    : turmsClusterManager.getMemberByUserId(userId);
            if (onlyRespondOnlineStatus) {
                return checkIfRemoteUserOffline(member, userId)
                        .map(isOnline -> UserOnlineInfo.builder()
                                .userId(userId)
                                .userStatus(isOnline ? UserStatus.AVAILABLE : UserStatus.OFFLINE)
                                .build());
            } else {
                if (remoteUsersOnlineInfoCache != null) {
                    UserOnlineInfo onlineInfo = remoteUsersOnlineInfoCache.getIfPresent(userId);
                    if (onlineInfo != null) {
                        return Mono.just(onlineInfo);
                    }
                }
                QueryUserOnlineInfoTask task = new QueryUserOnlineInfoTask(userId);
                Future<UserOnlineInfo> future = turmsClusterManager.getExecutor()
                        .submitToMember(task, member);
                Mono<UserOnlineInfo> mono = ReactorUtil.future2Mono(future, turmsClusterManager.getTurmsProperties().getRpc().getTimeoutDuration());
                return remoteUsersOnlineInfoCache != null ?
                        mono.doOnNext(userOnlineInfo -> {
                            if (userOnlineInfo.getUserStatus(false) != UserStatus.OFFLINE) {
                                remoteUsersOnlineInfoCache.put(userId, userOnlineInfo);
                            }
                        })
                        : mono;
            }
        }
    }

    public Flux<UserOnlineInfo> queryUserOnlineInfos(@NotNull Integer number) {
        Pair<Integer, Integer> workingRange = turmsClusterManager.getResponsibleSlotIndexRange();
        if (workingRange == null) {
            return Flux.empty();
        } else {
            Integer start = workingRange.getLeft();
            Integer end = workingRange.getRight();
            return Flux.create(sink -> {
                // Do not use Flux.take()
                int count = 0;
                for (int i = start; i < end; i++) {
                    if (count >= number) {
                        break;
                    }
                    Map<Long, OnlineUserManager> map = onlineUsersManagerAtSlots.get(i);
                    if (map != null && !map.isEmpty()) {
                        for (OnlineUserManager manager : map.values()) {
                            if (count >= number) {
                                break;
                            }
                            sink.next(manager.getOnlineUserInfo());
                            count++;
                        }
                    }
                }
                sink.complete();
            });
        }
    }

    public Mono<Boolean> checkIfRemoteUserOffline(@NotNull Member member, @NotNull Long userId) {
        if (remoteOnlineUsersCache != null && remoteOnlineUsersCache.getIfPresent(userId) != null) {
            return Mono.just(true);
        }
        return turmsTaskManager.call(member, new CheckIfUserOnlineTask(userId),
                turmsClusterManager.getTurmsProperties().getRpc().getTimeoutDuration());
    }

    private void onClusterMembersChange() {
        if (irresponsibleUserService.isAllowIrresponsibleUsersAfterResponsibilityChanged()) {
            // Make sure to drain disconnectionTasks and fill it up again in a lock
            // So it won't get in trouble when onClusterMembersChange are triggered several times at one moment
            disconnectionLock.lock();
            while (!disconnectionTasks.isEmpty()) {
                disconnectionTasks.poll().cancel();
            }
            try {
                setIrresponsibleUsersOffline(false, irresponsibleUserService.getClearUpIrresponsibleUsersJitter());
            } finally {
                disconnectionLock.unlock();
            }
        } else {
            setIrresponsibleUsersOffline(false);
        }
    }

    public SortedSet<UserLocation> getUserLocations(@NotNull Long userId, @DeviceTypeConstraint DeviceType deviceType) {
        if (locationEnabled) {
            if (usersNearbyService.isTreatUserIdAndDeviceTypeAsUniqueUser()) {
                if (deviceType != null) {
                    return usersNearbyService.getUserSessionLocations().get(Pair.of(userId, deviceType));
                } else {
                    throw new IllegalArgumentException("deviceType must not be null if treatUserIdAndDeviceTypeAsUniqueUser is true");
                }
            } else {
                return usersNearbyService.getUserLocations().get(userId);
            }
        } else {
            throw TurmsBusinessException.get(TurmsStatusCode.DISABLED_FUNCTION);
        }
    }

    public boolean updateUserLocation(
            @NotNull Long userId,
            @NotNull DeviceType deviceType,
            float latitude,
            float longitude,
            @Nullable String name,
            @Nullable String address) {
        OnlineUserManager onlineUserManager = getLocalOnlineUserManager(userId);
        if (onlineUserManager != null) {
            Date now = new Date();
            UserLocation location = new UserLocation(
                    turmsClusterManager.generateRandomId(),
                    userId,
                    deviceType,
                    longitude,
                    latitude,
                    name,
                    address,
                    now);
            OnlineUserManager.Session session = onlineUserManager.getSession(deviceType);
            if (session != null) {
                session.setLocation(location);
                userLocationService.saveUserLocation(location)
                        .onErrorReturn(EMPTY_USER_LOCATION)
                        .subscribe();
                return true;
            }
        }
        return false;
    }

    public boolean shouldCacheRemoteUserOnlineStatus() {
        return remoteOnlineUsersCache != null;
    }

    public void cacheRemoteUserOnlineStatus(@NotNull Long userId) {
        if (remoteOnlineUsersCache != null) {
            remoteOnlineUsersCache.put(userId, Common.EMPTY_OBJECT);
        }
    }
}
