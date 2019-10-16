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
import im.turms.turms.cluster.TurmsClusterManager;
import im.turms.turms.constant.DeviceType;
import im.turms.turms.pojo.domain.User;
import im.turms.turms.pojo.domain.UserLocation;
import im.turms.turms.service.user.UserService;
import im.turms.turms.task.QueryNearestUsersTask;
import im.turms.turms.task.TurmsTaskExecutor;
import jdk.jfr.Experimental;
import org.springframework.context.annotation.Lazy;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import javax.annotation.Nullable;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.time.Duration;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.stream.Collectors;

/**
 * The accuracy is within 1.7m
 */
// TODO: benchmark
// Main Check point: Pay attention to the memory usage
@Experimental
@Service
public class UsersNearbyService {
    private final TurmsClusterManager turmsClusterManager;
    private final TurmsTaskExecutor turmsTaskExecutor;
    private final UserService userService;
    private final OnlineUserService onlineUserService;
    private RTree<Long, PointFloat> tree;
    // userId -> location, order by time asc
    private SortedSetMultimap<Long, UserLocation> userLocations; //TODO: max capacity

    //TODO:
    public UsersNearbyService(@Lazy OnlineUserService onlineUserService, TurmsClusterManager turmsClusterManager, TurmsTaskExecutor turmsTaskExecutor, UserService userService) {
        this.tree = RTree.star().create();
        userLocations = Multimaps.newSortedSetMultimap(
                Maps.newHashMap(), () -> Sets.newTreeSet((location1, location2) -> {
                    if (location1.getTimestamp().getTime() == location2.getTimestamp().getTime()) {
                        return 0;
                    } else {
                        return location1.getTimestamp().getTime() < location2.getTimestamp().getTime() ? -1 : 1;
                    }
                })
        );
        this.onlineUserService = onlineUserService;
        this.turmsClusterManager = turmsClusterManager;
        this.turmsTaskExecutor = turmsTaskExecutor;
        this.userService = userService;
    }

    public SortedSetMultimap<Long, UserLocation> getUserLocations() {
        return userLocations;
    }

    /**
     * Usually used when a user is just online.
     */
    public void upsertUserLocation(
            @NonNull Long userId,
            @NonNull Float longitude,
            @NonNull Float latitude,
            @NotNull Date date) {
        PointFloat point = PointFloat.create(longitude, latitude);
        SortedSet<UserLocation> locations = this.userLocations.get(userId);
        if (locations != null && !locations.isEmpty()) {
            UserLocation location = locations.last();
            PointFloat deletePoint = PointFloat.create(location.getLongitude(), location.getLatitude());
            tree.delete(userId, deletePoint);
        }
        tree = tree.add(userId, point);
        this.userLocations.put(userId, new UserLocation(userId, longitude, latitude, date));
    }

    /**
     * Because of time complexity, better remove locations at scheduled times
     * rather than when a user is just offline.
     * TODO
     */
    public void removeUserLocation(@NotNull Long userId) {
        SortedSet<UserLocation> deletedUserLocations = userLocations.removeAll(userId);
        for (UserLocation deletedUserLocation : deletedUserLocations) {
            tree.delete(userId, deletedUserLocation.getPoint());
        }
    }

    public Flux<Long> queryUsersIdsNearby(
            @NotNull Long userId,
            @Nullable DeviceType deviceType,
            @Nullable Integer maxPeopleNumber,
            @Nullable Double maxDistance) {
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
                        return queryUsersIdsNearby(
                                location.getPoint().xFloat(),
                                location.getPoint().yFloat(),
                                maxPeopleNumber,
                                maxDistance);
                    }
                }
            }
        }
        return Flux.empty();
    }

    public Flux<User> queryUsersProfilesNearby(
            @NonNull Long userId,
            @Nullable DeviceType deviceType,
            @Nullable Integer maxPeopleNumber,
            @Nullable Double maxDistance) {
        return queryUsersIdsNearby(userId, deviceType, maxPeopleNumber, maxDistance)
                .collect(Collectors.toSet())
                .flatMapMany(userService::queryUsersProfiles);
    }

    public Flux<Long> queryUsersIdsNearby(
            @NotNull Float longitude,
            @NotNull Float latitude,
            @Nullable Integer maxNumber,
            @Nullable Double maxDistance) {
        if (tree.size() > 0) {
            if (maxNumber == null) {
                maxNumber = turmsClusterManager
                        .getTurmsProperties()
                        .getUser()
                        .getLocation()
                        .getMaxQueryUsersNearbyNumber();
            }
            if (maxDistance == null) {
                maxDistance = turmsClusterManager
                        .getTurmsProperties()
                        .getUser()
                        .getLocation()
                        .getMaxDistance();
            }
            Double finalMaxDistance = maxDistance;
            Integer finalMaxPeopleNumber = maxNumber;
            PointFloat currentUserPosition = PointFloat.create(longitude, latitude);
            return turmsTaskExecutor.callAll(new QueryNearestUsersTask(
                    currentUserPosition,
                    maxDistance,
                    maxNumber), Duration.ofSeconds(30))
                    .collectList()
                    .flatMapMany(entries -> {
                        Iterable<Entry<Long, PointFloat>> result = getNearestPoints(
                                currentUserPosition,
                                entries,
                                finalMaxDistance,
                                finalMaxPeopleNumber);
                        return Flux.fromIterable(result);
                    })
                    .map(Entry::value);
        } else {
            return Flux.empty();
        }
    }

    public Iterable<Entry<Long, PointFloat>> getNearestPoints(
            @NotNull PointFloat point,
            @NotNull Double maxDistance,
            @NotNull Integer maxNumber) {
        return tree.nearest(point, maxDistance, maxNumber);
    }

    private Iterable<Entry<Long, PointFloat>> getNearestPoints(
            @NotNull PointFloat point,
            @NotEmpty List<Iterable<Entry<Long, PointFloat>>> entries,
            @NotNull Double maxDistance,
            @NotNull Integer maxNumber) {
        RTree<Long, PointFloat> disposableTree = RTree.star().create();
        for (Iterable<Entry<Long, PointFloat>> entryList : entries) {
            for (Entry<Long, PointFloat> locationEntry : entryList) {
                disposableTree.add(locationEntry);
            }
        }
        return disposableTree.nearest(point, maxDistance, maxNumber);
    }
}
