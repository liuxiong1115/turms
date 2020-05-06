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

import com.github.davidmoten.rtree2.Entry;
import com.github.davidmoten.rtree2.RTree;
import com.github.davidmoten.rtree2.geometry.internal.PointFloat;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimaps;
import com.google.common.collect.Sets;
import com.google.common.collect.SortedSetMultimap;
import im.turms.common.TurmsStatusCode;
import im.turms.common.constant.DeviceType;
import im.turms.common.exception.TurmsBusinessException;
import im.turms.turms.annotation.constraint.DeviceTypeConstraint;
import im.turms.turms.manager.TurmsClusterManager;
import im.turms.turms.manager.TurmsTaskManager;
import im.turms.turms.pojo.domain.User;
import im.turms.turms.pojo.domain.UserLocation;
import im.turms.turms.service.user.UserService;
import im.turms.turms.service.user.onlineuser.manager.OnlineUserManager;
import im.turms.turms.task.QueryNearestUserIdsTask;
import im.turms.turms.task.QueryNearestUserSessionsIdsTask;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.annotation.Nullable;
import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.*;
import java.util.stream.Collectors;

/**
 * The accuracy is within 1.7m
 */
@Service
@Validated
public class UsersNearbyService {
    private final TurmsClusterManager turmsClusterManager;
    private final TurmsTaskManager turmsTaskManager;
    private final UserService userService;
    private final OnlineUserService onlineUserService;

    private final boolean locationEnabled;
    private final boolean treatUserIdAndDeviceTypeAsUniqueUser;
    /**
     * search is O(log(n)) on average
     * insert, delete are O(n) worst case
     * Only one of the trees will be used according to "isTreatUserIdAndDeviceTypeAsUniqueUser"
     */
    private RTree<Pair<Long, DeviceType>, PointFloat> sessionIdTree;
    private RTree<Long, PointFloat> userIdTree;
    // userId -> location, order by time asc
    private SortedSetMultimap<Pair<Long, DeviceType>, UserLocation> userSessionLocations;
    private SortedSetMultimap<Long, UserLocation> userLocations;

    public UsersNearbyService(@Lazy OnlineUserService onlineUserService, TurmsClusterManager turmsClusterManager, TurmsTaskManager turmsTaskManager, UserService userService) {
        this.onlineUserService = onlineUserService;
        this.turmsClusterManager = turmsClusterManager;
        this.turmsTaskManager = turmsTaskManager;
        this.userService = userService;
        locationEnabled = turmsClusterManager.getTurmsProperties().getUser().getLocation().isEnabled();
        treatUserIdAndDeviceTypeAsUniqueUser = turmsClusterManager.getTurmsProperties().getUser().getLocation().isTreatUserIdAndDeviceTypeAsUniqueUser();
        if (locationEnabled) {
            if (treatUserIdAndDeviceTypeAsUniqueUser) {
                sessionIdTree = RTree.create();
                userSessionLocations = newLocationMap();
            } else {
                userIdTree = RTree.create();
                userLocations = newLocationMap();
            }
        }
    }

    private <T> SortedSetMultimap<T, UserLocation> newLocationMap() {
        return Multimaps.newSortedSetMultimap(
                Maps.newHashMapWithExpectedSize(10240), () -> Sets.newTreeSet((location1, location2) -> {
                    if (location1.getTimestamp().getTime() == location2.getTimestamp().getTime()) {
                        return 0;
                    } else {
                        return location1.getTimestamp().getTime() < location2.getTimestamp().getTime() ? -1 : 1;
                    }
                }));
    }

    public boolean isTreatUserIdAndDeviceTypeAsUniqueUser() {
        return treatUserIdAndDeviceTypeAsUniqueUser;
    }

    public SortedSetMultimap<Pair<Long, DeviceType>, UserLocation> getUserSessionLocations() {
        if (locationEnabled) {
            return userSessionLocations;
        } else {
            throw TurmsBusinessException.get(TurmsStatusCode.DISABLED_FUNCTION);
        }
    }

    public SortedSetMultimap<Long, UserLocation> getUserLocations() {
        if (locationEnabled) {
            return userLocations;
        } else {
            throw TurmsBusinessException.get(TurmsStatusCode.DISABLED_FUNCTION);
        }
    }

    /**
     * Usually used when a user is just online.
     */
    public void upsertUserLocation(
            @NotNull Long userId,
            @NotNull DeviceType deviceType,
            @NotNull Float longitude,
            @NotNull Float latitude,
            @NotNull Date date) {
        if (!locationEnabled) {
            throw TurmsBusinessException.get(TurmsStatusCode.DISABLED_FUNCTION);
        }
        PointFloat point = PointFloat.create(longitude, latitude);
        if (treatUserIdAndDeviceTypeAsUniqueUser) {
            Pair<Long, DeviceType> key = Pair.of(userId, deviceType);
            SortedSet<UserLocation> locations = this.userSessionLocations.get(key);
            if (locations != null && !locations.isEmpty()) {
                UserLocation location = locations.last();
                PointFloat deletePoint = PointFloat.create(location.getLongitude(), location.getLatitude());
                sessionIdTree.delete(key, deletePoint);
            }
            sessionIdTree = sessionIdTree.add(key, point);
            this.userSessionLocations.put(key, new UserLocation(turmsClusterManager.generateRandomId(), userId, deviceType, longitude, latitude, null, null, date));
        } else {
            SortedSet<UserLocation> locations = this.userLocations.get(userId);
            if (locations != null && !locations.isEmpty()) {
                UserLocation location = locations.last();
                PointFloat deletePoint = PointFloat.create(location.getLongitude(), location.getLatitude());
                userIdTree.delete(userId, deletePoint);
            }
            userIdTree = userIdTree.add(userId, point);
            this.userLocations.put(userId, new UserLocation(turmsClusterManager.generateRandomId(), userId, deviceType, longitude, latitude, null, null, date));
        }
    }

    public void removeUserLocation(@NotNull Long userId, @NotNull @DeviceTypeConstraint DeviceType deviceType) {
        if (locationEnabled) {
            if (treatUserIdAndDeviceTypeAsUniqueUser) {
                Pair<Long, DeviceType> key = Pair.of(userId, deviceType);
                SortedSet<UserLocation> deletedUserLocations = userSessionLocations.removeAll(key);
                for (UserLocation deletedUserLocation : deletedUserLocations) {
                    PointFloat point = PointFloat.create(deletedUserLocation.getLongitude(), deletedUserLocation.getLatitude());
                    sessionIdTree.delete(key, point);
                }
            } else {
                SortedSet<UserLocation> deletedUserLocations = userLocations.removeAll(userId);
                for (UserLocation deletedUserLocation : deletedUserLocations) {
                    PointFloat point = PointFloat.create(deletedUserLocation.getLongitude(), deletedUserLocation.getLatitude());
                    userIdTree.delete(userId, point);
                }
            }
        }
    }

    public Flux<Long> queryNearestUserIds(
            @NotNull Long userId,
            @Nullable DeviceType deviceType,
            @Nullable Short maxPeopleNumber,
            @Nullable Double maxDistance) {
        if (!locationEnabled) {
            throw TurmsBusinessException.get(TurmsStatusCode.DISABLED_FUNCTION);
        }
        OnlineUserManager onlineUserManager = onlineUserService.getLocalOnlineUserManager(userId);
        if (onlineUserManager != null) {
            OnlineUserManager.Session session;
            if (deviceType == null) {
                Set<DeviceType> usingDeviceTypes = onlineUserManager.getUsingDeviceTypes();
                if (usingDeviceTypes != null && !usingDeviceTypes.isEmpty()) {
                    deviceType = usingDeviceTypes.iterator().next();
                }
            }
            if (deviceType != null) {
                session = onlineUserManager.getSession(deviceType);
                if (session != null) {
                    UserLocation location = onlineUserManager.getSession(deviceType).getLocation();
                    if (location != null) {
                        return queryNearestUserIds(
                                location.getLongitude(),
                                location.getLatitude(),
                                maxPeopleNumber,
                                maxDistance);
                    }
                }
            }
        }
        return Flux.empty();
    }

    public Flux<Pair<Long, DeviceType>> queryNearestUserSessionIds(
            @NotNull Long userId,
            @Nullable DeviceType deviceType,
            @Nullable Short maxPeopleNumber,
            @Nullable Double maxDistance) {
        if (!locationEnabled) {
            throw TurmsBusinessException.get(TurmsStatusCode.DISABLED_FUNCTION);
        }
        OnlineUserManager onlineUserManager = onlineUserService.getLocalOnlineUserManager(userId);
        if (onlineUserManager != null) {
            OnlineUserManager.Session session;
            if (deviceType == null) {
                Set<DeviceType> usingDeviceTypes = onlineUserManager.getUsingDeviceTypes();
                if (usingDeviceTypes != null && !usingDeviceTypes.isEmpty()) {
                    deviceType = usingDeviceTypes.iterator().next();
                }
            }
            if (deviceType != null) {
                session = onlineUserManager.getSession(deviceType);
                if (session != null) {
                    UserLocation location = onlineUserManager.getSession(deviceType).getLocation();
                    if (location != null) {
                        return queryUserSessionIdsNearby(
                                location.getLongitude(),
                                location.getLatitude(),
                                maxPeopleNumber,
                                maxDistance);
                    }
                }
            }
        }
        return Flux.empty();
    }

    public Flux<User> queryUsersProfilesNearby(
            @NotNull Long userId,
            @Nullable DeviceType deviceType,
            @Nullable Short maxPeopleNumber,
            @Nullable Double maxDistance) {
        if (!locationEnabled) {
            throw TurmsBusinessException.get(TurmsStatusCode.DISABLED_FUNCTION);
        }
        if (treatUserIdAndDeviceTypeAsUniqueUser) {
            return queryNearestUserSessionIds(userId, deviceType, maxPeopleNumber, maxDistance)
                    .collect(Collectors.toSet())
                    .flatMapMany(ids -> {
                        if (ids.isEmpty()) {
                            return Mono.empty();
                        } else {
                            Set<Long> userIds = new HashSet<>(ids.size());
                            for (Pair<Long, DeviceType> id : ids) {
                                userIds.add(id.getKey());
                            }
                            return userService.queryUsersProfiles(userIds, false);
                        }
                    });
        } else {
            return queryNearestUserIds(userId, deviceType, maxPeopleNumber, maxDistance)
                    .collect(Collectors.toSet())
                    .flatMapMany(ids -> {
                        if (ids.isEmpty()) {
                            return Mono.empty();
                        } else {
                            return userService.queryUsersProfiles(ids, false);
                        }
                    });
        }
    }

    public Flux<Long> queryNearestUserIds(
            @NotNull Float longitude,
            @NotNull Float latitude,
            @Nullable Short maxNumber,
            @Nullable Double maxDistance) {
        if (!locationEnabled) {
            throw TurmsBusinessException.get(TurmsStatusCode.DISABLED_FUNCTION);
        }
        if (userIdTree.size() > 0 || !turmsClusterManager.isSingleton()) {
            im.turms.turms.property.business.User.@Valid Location location = turmsClusterManager
                    .getTurmsProperties()
                    .getUser()
                    .getLocation();
            if (maxNumber == null) {
                maxNumber = location.getDefaultMaxAvailableUsersNearbyNumberPerQuery();
            }
            if (maxDistance == null) {
                maxDistance = location.getDefaultMaxDistancePerQuery();
            }
            if (maxNumber > location.getMaxAvailableUsersNearbyNumberLimitPerQuery()) {
                maxNumber = location.getMaxAvailableUsersNearbyNumberLimitPerQuery();
            }
            if (maxDistance > location.getMaxDistanceLimitPerQuery()) {
                maxDistance = location.getMaxDistanceLimitPerQuery();
            }
            Double finalMaxDistance = maxDistance;
            Short finalMaxPeopleNumber = maxNumber;
            PointFloat point = PointFloat.create(longitude, latitude);
            return turmsTaskManager.callAllOthers(new QueryNearestUserIdsTask(
                    longitude,
                    latitude,
                    maxDistance,
                    maxNumber), turmsClusterManager.getTurmsProperties().getRpc().getTimeoutDuration())
                    .concatWith(s -> s.onNext(getNearestUserIds(point, finalMaxDistance, finalMaxPeopleNumber)))
                    .collectList()
                    .flatMapMany(entries -> Flux.fromIterable(getNearestUserIds(
                            point,
                            entries,
                            finalMaxDistance,
                            finalMaxPeopleNumber)))
                    .map(Entry::value);
        } else {
            return Flux.empty();
        }
    }

    public Flux<Pair<Long, DeviceType>> queryUserSessionIdsNearby(
            @NotNull Float longitude,
            @NotNull Float latitude,
            @Nullable Short maxNumber,
            @Nullable Double maxDistance) {
        if (!locationEnabled) {
            throw TurmsBusinessException.get(TurmsStatusCode.DISABLED_FUNCTION);
        }
        if (sessionIdTree.size() > 0 || !turmsClusterManager.isSingleton()) {
            if (maxNumber == null) {
                maxNumber = turmsClusterManager
                        .getTurmsProperties()
                        .getUser()
                        .getLocation()
                        .getDefaultMaxAvailableUsersNearbyNumberPerQuery();
            }
            if (maxDistance == null) {
                maxDistance = turmsClusterManager
                        .getTurmsProperties()
                        .getUser()
                        .getLocation()
                        .getDefaultMaxDistancePerQuery();
            }
            Double finalMaxDistance = maxDistance;
            Short finalMaxPeopleNumber = maxNumber;
            PointFloat point = PointFloat.create(longitude, latitude);
            return turmsTaskManager.callAllOthers(new QueryNearestUserSessionsIdsTask(
                    longitude,
                    latitude,
                    maxDistance,
                    maxNumber), turmsClusterManager.getTurmsProperties().getRpc().getTimeoutDuration())
                    .concatWith(s -> s.onNext(getNearestUserSessionIds(point, finalMaxDistance, finalMaxPeopleNumber)))
                    .collectList()
                    .flatMapMany(entries -> Flux.fromIterable(getNearestUserSessionIds(
                            point,
                            entries,
                            finalMaxDistance,
                            finalMaxPeopleNumber)))
                    .map(Entry::value);
        } else {
            return Flux.empty();
        }
    }

    public Iterable<Entry<Long, PointFloat>> getNearestUserIds(
            @NotNull PointFloat point,
            @NotNull Double maxDistance,
            @NotNull Short maxNumber) {
        if (!locationEnabled) {
            throw TurmsBusinessException.get(TurmsStatusCode.DISABLED_FUNCTION);
        }
        return userIdTree.nearest(point, maxDistance, maxNumber);
    }

    private Iterable<Entry<Long, PointFloat>> getNearestUserIds(
            @NotNull PointFloat point,
            @NotEmpty List<Iterable<Entry<Long, PointFloat>>> entries,
            @NotNull Double maxDistance,
            @NotNull Short maxNumber) {
        if (!locationEnabled) {
            throw TurmsBusinessException.get(TurmsStatusCode.DISABLED_FUNCTION);
        }
        RTree<Long, PointFloat> disposableTree = RTree.create();
        for (Iterable<Entry<Long, PointFloat>> entryList : entries) {
            for (Entry<Long, PointFloat> locationEntry : entryList) {
                disposableTree.add(locationEntry);
            }
        }
        return disposableTree.nearest(point, maxDistance, maxNumber);
    }

    public Iterable<Entry<Pair<Long, DeviceType>, PointFloat>> getNearestUserSessionIds(
            @NotNull PointFloat point,
            @NotNull Double maxDistance,
            @NotNull Short maxNumber) {
        if (!locationEnabled) {
            throw TurmsBusinessException.get(TurmsStatusCode.DISABLED_FUNCTION);
        }
        return sessionIdTree.nearest(point, maxDistance, maxNumber);
    }

    private Iterable<Entry<Pair<Long, DeviceType>, PointFloat>> getNearestUserSessionIds(
            @NotNull PointFloat point,
            @NotEmpty List<Iterable<Entry<Pair<Long, DeviceType>, PointFloat>>> entries,
            @NotNull Double maxDistance,
            @NotNull Short maxNumber) {
        if (!locationEnabled) {
            throw TurmsBusinessException.get(TurmsStatusCode.DISABLED_FUNCTION);
        }
        RTree<Pair<Long, DeviceType>, PointFloat> disposableTree = RTree.create();
        for (Iterable<Entry<Pair<Long, DeviceType>, PointFloat>> entryList : entries) {
            for (Entry<Pair<Long, DeviceType>, PointFloat> locationEntry : entryList) {
                disposableTree.add(locationEntry);
            }
        }
        return disposableTree.nearest(point, maxDistance, maxNumber);
    }
}
