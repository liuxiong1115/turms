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
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.concurrent.Callable;

@SpringAware
public class QueryNearestUsersTask implements Callable<Iterable<Entry<Long, PointFloat>>>, Serializable, ApplicationContextAware {
    private final PointFloat pointFloat;
    private final Double maxDistance;
    private final Integer maxNumber;
    private transient ApplicationContext context;
    private transient UsersNearbyService usersNearbyService;

    public QueryNearestUsersTask(
            @NotNull PointFloat pointFloat,
            @NotNull Double maxDistance,
            @NotNull Integer maxNumber) {
        this.pointFloat = pointFloat;
        this.maxDistance = maxDistance;
        this.maxNumber = maxNumber;
    }

    @Override
    public Iterable<Entry<Long, PointFloat>> call() {
        return usersNearbyService.getNearestPoints(pointFloat, maxDistance, maxNumber);
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
