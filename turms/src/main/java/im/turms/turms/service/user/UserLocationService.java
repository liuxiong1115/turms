package im.turms.turms.service.user;

import im.turms.common.TurmsStatusCode;
import im.turms.common.constant.DeviceType;
import im.turms.common.exception.TurmsBusinessException;
import im.turms.turms.manager.TurmsClusterManager;
import im.turms.turms.pojo.domain.UserLocation;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;
import reactor.core.publisher.Mono;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.PastOrPresent;
import java.util.Date;

@Service
@Validated
public class UserLocationService {
    private final ReactiveMongoTemplate mongoTemplate;
    private final TurmsClusterManager turmsClusterManager;

    public UserLocationService(
            @Qualifier("userMongoTemplate") ReactiveMongoTemplate mongoTemplate,
            TurmsClusterManager turmsClusterManager) {
        this.mongoTemplate = mongoTemplate;
        this.turmsClusterManager = turmsClusterManager;
    }

    public Mono<UserLocation> saveUserLocation(@NotNull UserLocation userLocation) {
        if (turmsClusterManager.getTurmsProperties().getUser().getLocation().isPersistent()) {
            return mongoTemplate.insert(userLocation);
        } else {
            return Mono.error(TurmsBusinessException.get(TurmsStatusCode.DISABLED_FUNCTION));
        }
    }

    public Mono<UserLocation> saveUserLocation(
            @Nullable Long id,
            @NotNull Long userId,
            @NotNull DeviceType deviceType,
            float longitude,
            float latitude,
            @NotNull @PastOrPresent Date timestamp) {
        if (turmsClusterManager.getTurmsProperties().getUser().getLocation().isPersistent()) {
            if (id == null) {
                id = turmsClusterManager.generateRandomId();
            }
            UserLocation location = new UserLocation(id, userId, deviceType, longitude, latitude, null, null, timestamp);
            return mongoTemplate.save(location);
        } else {
            return Mono.error(TurmsBusinessException.get(TurmsStatusCode.DISABLED_FUNCTION));
        }
    }
}