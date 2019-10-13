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

import com.hazelcast.config.Config;
import com.hazelcast.config.MapConfig;
import com.hazelcast.config.NearCacheConfig;
import im.turms.turms.annotation.cluster.HazelcastConfig;
import im.turms.turms.common.QueryBuilder;
import im.turms.turms.pojo.domain.UserMaxDailyOnlineUserNumber;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import javax.annotation.Nullable;
import java.util.Date;
import java.util.function.Function;

@Service
public class UserMaxDailyOnlineUserNumberService {
    private static final String MAX_USER_NUMBER_EVERY_DAY_MAP = "MAX_USER_NUMBER_EVERY_DAY_MAP";
    private final ReactiveMongoTemplate mongoTemplate;

    public UserMaxDailyOnlineUserNumberService(ReactiveMongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    public Mono<Integer> countMaxUserNumber(
            @Nullable Date maxUserNumberDateStart,
            @Nullable Date maxUserNumberDateEnd) {
        Query query = QueryBuilder.newBuilder()
                .addBetweenIfNotNull(UserMaxDailyOnlineUserNumber.Fields.date, maxUserNumberDateStart, maxUserNumberDateEnd)
                .max("number")
                .buildQuery();
        return mongoTemplate.findOne(query, UserMaxDailyOnlineUserNumber.class)
                .map(UserMaxDailyOnlineUserNumber::getNumber);
    }

    //TODO: move to yaml
    @HazelcastConfig
    public static Function<Config, Void> maxUserNumberCacheConfig() {
        return config -> {
            MapConfig mapConfig = config.getMapConfig(MAX_USER_NUMBER_EVERY_DAY_MAP);
            NearCacheConfig nearCacheConfig = new NearCacheConfig();
            nearCacheConfig.getEvictionConfig().setSize(128);
            mapConfig.setNearCacheConfig(nearCacheConfig);
            return null;
        };
    }

//    public Flux<Pair<String, Integer>> countMaxDailyUserNumber(String maxUserNumberStartDate, String maxUserNumberEndDate) throws ParseException {
//        List<String> dates = DateTimeUtil.getDatesBetweenTwoDates(maxUserNumberStartDate, maxUserNumberEndDate, true);
//        if (dates != null) {
//            List<Pair<String, Integer>> list = new ArrayList<>(dates.size());
//            for (String date : dates) {
//                Integer number = countMaxUserNumber(date);
//                list.add(new ImmutablePair<>(date, number));
//            }
//            return Flux.fromIterable(list);
//        }
//        return Flux.empty();
//    }
}
