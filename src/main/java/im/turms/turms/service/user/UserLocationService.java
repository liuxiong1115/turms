package im.turms.turms.service.user;

import im.turms.turms.cluster.TurmsClusterManager;
import im.turms.turms.pojo.domain.UserLocation;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;
import java.util.Date;

@Service
public class UserLocationService {
    private final ReactiveMongoTemplate mongoTemplate;
    private final TurmsClusterManager turmsClusterManager;

    public UserLocationService(ReactiveMongoTemplate mongoTemplate, TurmsClusterManager turmsClusterManager) {
        this.mongoTemplate = mongoTemplate;
        this.turmsClusterManager = turmsClusterManager;
    }

    public Mono<UserLocation> saveUserLocation(UserLocation userLocation) {
        return mongoTemplate.insert(userLocation);
    }

    public Mono<UserLocation> saveUserLocation(
            @Nullable Long id,
            @NotNull Long userId,
            float longitude,
            float latitude,
            Date timestamp) {
        if (id == null) {
            id = turmsClusterManager.generateRandomId();
        }
        UserLocation location = new UserLocation();
        location.setId(id);
        location.setUserId(userId);
        location.setLongitude(longitude);
        location.setLatitude(latitude);
        location.setTimestamp(timestamp);
        return mongoTemplate.save(location);
    }
}
