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

package im.turms.turms.task;

import com.github.davidmoten.rtree2.Entry;
import com.github.davidmoten.rtree2.geometry.internal.PointFloat;
import com.hazelcast.spring.context.SpringAware;
import im.turms.turms.service.user.onlineuser.UsersNearbyService;
import lombok.Getter;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import javax.validation.constraints.NotNull;
import java.util.concurrent.Callable;

@SpringAware
public class QueryNearestUserIdsTask implements Callable<Iterable<Entry<Long, PointFloat>>>, ApplicationContextAware {

    @Getter
    private final Float longitude;

    @Getter
    private final Float latitude;

    @Getter
    private final Double maxDistance;

    @Getter
    private final Short maxNumber;

    private transient ApplicationContext context;
    private transient UsersNearbyService usersNearbyService;

    public QueryNearestUserIdsTask(
            @NotNull Float longitude,
            @NotNull Float latitude,
            @NotNull Double maxDistance,
            @NotNull Short maxNumber) {
        this.longitude = longitude;
        this.latitude = latitude;
        this.maxDistance = maxDistance;
        this.maxNumber = maxNumber;
    }

    @Override
    public Iterable<Entry<Long, PointFloat>> call() {
        return usersNearbyService.getNearestUserIds(PointFloat.create(longitude, latitude), maxDistance, maxNumber);
    }

    @Override
    public void setApplicationContext(final ApplicationContext applicationContext) throws BeansException {
        context = applicationContext;
    }

    @Autowired
    public void setOnlineUserService(final UsersNearbyService usersNearbyService) {
        this.usersNearbyService = usersNearbyService;
    }
}
