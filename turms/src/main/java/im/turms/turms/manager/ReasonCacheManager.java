package im.turms.turms.manager;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import im.turms.common.TurmsStatusCode;
import im.turms.common.constant.DeviceType;
import im.turms.common.exception.TurmsBusinessException;
import im.turms.turms.property.TurmsProperties;
import org.apache.commons.lang3.tuple.Triple;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.NotNull;
import java.time.Duration;
import java.util.Set;

@Service
@Validated
public class ReasonCacheManager {
    private final Set<DeviceType> degradedDeviceTypes;
    private final boolean enableQueryLoginFailedReason;
    private final boolean enableQueryDisconnectionReason;
    /**
     * Triple<user ID, device type, login request ID> -> reason
     * 1. Integer: http status code
     * 2. String: redirect address
     * <p>
     * Note:
     * 1. The reason to cache the request ID (as a token) is to
     * prevent others from querying others' login failed reason.
     * 2. To keep it simple, use Object to avoid defining/using a new model
     */
    private final Cache<Triple<Long, DeviceType, Long>, Object> loginFailedReasonCache;
    /**
     * Triple<user ID, device type, session ID> -> Custom CloseStatus (TurmsCloseStatus)
     */
    private final Cache<Triple<Long, DeviceType, String>, Integer> disconnectionReasonCache;

    @Autowired
    public ReasonCacheManager(TurmsProperties turmsProperties) {
        this.loginFailedReasonCache = Caffeine
                .newBuilder()
                .maximumSize(turmsProperties.getCache().getLoginFailedReasonCacheMaxSize())
                .expireAfterWrite(Duration.ofSeconds(turmsProperties.getCache().getLoginFailedReasonExpireAfter()))
                .build();
        this.disconnectionReasonCache = Caffeine
                .newBuilder()
                .maximumSize(turmsProperties.getCache().getDisconnectionReasonCacheMaxSize())
                .expireAfterWrite(Duration.ofSeconds(turmsProperties.getCache().getDisconnectionReasonExpireAfter()))
                .build();
        enableQueryLoginFailedReason = turmsProperties.getSession().isEnableQueryLoginFailedReason();
        enableQueryDisconnectionReason = turmsProperties.getSession().isEnableQueryDisconnectionReason();
        degradedDeviceTypes = turmsProperties.getSession().getDegradedDeviceTypesForLoginFailedReason();
    }

    public Object getLoginFailedReason(@NotNull Long userId, @NotNull DeviceType deviceType, @NotNull Long requestId) {
        if (!enableQueryLoginFailedReason) {
            throw TurmsBusinessException.get(TurmsStatusCode.DISABLED_FUNCTION);
        } else if (!degradedDeviceTypes.contains(deviceType)) {
            String reason = String.format("The device type %s is not allowed to query login-failed reason", deviceType);
            throw TurmsBusinessException.get(TurmsStatusCode.ILLEGAL_ARGUMENTS, reason);
        } else {
            return loginFailedReasonCache.getIfPresent(Triple.of(userId, deviceType, requestId));
        }
    }

    public boolean shouldCacheLoginFailedReason(Long userId, DeviceType deviceType, Long requestId) {
        return enableQueryLoginFailedReason &&
                deviceType != null &&
                degradedDeviceTypes.contains(deviceType) &&
                userId != null &&
                requestId != null;
    }

    public void cacheLoginFailedReason(Long userId, DeviceType deviceType, Long requestId, Object value) {
        loginFailedReasonCache.put(Triple.of(userId, deviceType, requestId), value);
    }

    public boolean shouldCacheDisconnectionReason(Long userId, DeviceType deviceType, String sessionId) {
        return enableQueryDisconnectionReason &&
                deviceType != null &&
                degradedDeviceTypes.contains(deviceType) &&
                userId != null &&
                sessionId != null;
    }

    public void cacheDisconnectionReason(Long userId, DeviceType deviceType, String sessionId, Integer value) {
        disconnectionReasonCache.put(Triple.of(userId, deviceType, sessionId), value);
    }

    public Integer getDisconnectionReason(@NotNull Long userId, @NotNull DeviceType deviceType, @NotNull String sessionId) {
        if (!enableQueryDisconnectionReason) {
            throw TurmsBusinessException.get(TurmsStatusCode.DISABLED_FUNCTION);
        } else if (!degradedDeviceTypes.contains(deviceType)) {
            String reason = String.format("The device type %s is not allowed to query disconnection reason", deviceType);
            throw TurmsBusinessException.get(TurmsStatusCode.ILLEGAL_ARGUMENTS, reason);
        } else {
            return disconnectionReasonCache.getIfPresent(Triple.of(userId, deviceType, sessionId));
        }
    }
}
