package im.turms.turms.service.user.onlineuser;

import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.Multimap;
import com.hazelcast.cluster.Member;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.replicatedmap.ReplicatedMap;
import im.turms.turms.annotation.cluster.PostHazelcastJoined;
import im.turms.turms.manager.TurmsClusterManager;
import im.turms.turms.property.TurmsProperties;
import io.netty.util.HashedWheelTimer;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.*;
import java.util.function.Function;

import static im.turms.turms.constant.Common.HAZELCAST_IRRESPONSIBLE_USERS_MAP;

/**
 * Note that the users' status are not strongly consistent and inconsistency is allowed.
 * clearUpIrresponsibleUsersAfter is used to make the statuses consistent eventually.
 * So don't waste system resources to rely on synchronization mechanism to try to keep it more consistent.
 */
@Log4j2
@Service
@Validated
public class IrresponsibleUserService {
    private final TurmsClusterManager turmsClusterManager;
    @Getter
    private final HashedWheelTimer irresponsibleUsersCleanerTimer;
    @Getter
    private final boolean allowIrresponsibleUsersWhenConnecting;
    @Getter
    private final boolean allowIrresponsibleUsersAfterResponsibilityChanged;
    @Getter
    private final boolean isEnabled;
    @Getter
    private final int clearUpIrresponsibleUsersAfter;
    @Getter
    private final int clearUpIrresponsibleUsersJitter;
    private ReplicatedMap<Long, UUID> irresponsibleUserMap;

    public IrresponsibleUserService(TurmsProperties turmsProperties, TurmsClusterManager turmsClusterManager) {
        this.turmsClusterManager = turmsClusterManager;
        allowIrresponsibleUsersWhenConnecting = turmsProperties.getSession().isAllowIrresponsibleUsersWhenConnecting();
        allowIrresponsibleUsersAfterResponsibilityChanged = turmsProperties.getSession().isAllowIrresponsibleUsersAfterResponsibilityChanged();
        clearUpIrresponsibleUsersAfter = turmsProperties.getSession().getClearUpIrresponsibleUsersAfter();
        clearUpIrresponsibleUsersJitter = turmsProperties.getSession().getClearUpIrresponsibleUsersJitter();
        isEnabled = allowIrresponsibleUsersWhenConnecting || allowIrresponsibleUsersAfterResponsibilityChanged;
        if (isEnabled) {
            irresponsibleUsersCleanerTimer = new HashedWheelTimer();
        } else {
            irresponsibleUsersCleanerTimer = null;
        }
    }

    @PostHazelcastJoined
    public Function<HazelcastInstance, Void> initIrresponsibleUsersMap() {
        return hazelcastInstance -> {
            irresponsibleUserMap = hazelcastInstance.getReplicatedMap(HAZELCAST_IRRESPONSIBLE_USERS_MAP);
            return null;
        };
    }

    public void put(@NotNull Long userId, @NotNull UUID memberId) {
        if (isEnabled && clearUpIrresponsibleUsersAfter > 0) {
            irresponsibleUserMap.put(userId, memberId);
        }
    }

    public void putAll(@NotNull List<Long> userIds, @NotNull UUID memberId) {
        if (isEnabled && clearUpIrresponsibleUsersAfter > 0) {
            Map<Long, UUID> map = new HashMap<>(userIds.size());
            for (Long userId : userIds) {
                map.put(userId, memberId);
            }
            irresponsibleUserMap.putAll(map);
        }
    }

    public boolean exists(@NotNull Long userId) {
        if (isEnabled) {
            UUID uuid = irresponsibleUserMap.get(userId);
            if (uuid != null) {
                if (turmsClusterManager.getMembersMap().containsKey(uuid)) {
                    return true;
                } else {
                    irresponsibleUserMap.remove(userId);
                }
            }
        }
        return false;
    }

    public UUID getMemberIdIfExists(@NotNull Long userId) {
        if (isEnabled) {
            UUID uuid = irresponsibleUserMap.get(userId);
            if (uuid != null) {
                if (turmsClusterManager.getMembersMap().containsKey(uuid)) {
                    return uuid;
                } else {
                    irresponsibleUserMap.remove(userId);
                }
            }
        }
        return null;
    }

    public Multimap<Member, Long> getMembersIfExists(@NotEmpty Set<Long> userIds) {
        Multimap<Member, Long> map = LinkedListMultimap.create();
        if (isEnabled) {
            for (Long userId : userIds) {
                UUID uuid = getMemberIdIfExists(userId);
                if (uuid != null) {
                    Member member = turmsClusterManager.getMemberById(uuid);
                    if (member != null) {
                        map.put(member, userId);
                    }
                }
            }
        }
        return map;
    }

    public boolean isIrresponsibleUserServedByCurrentNode(@NotNull Long userId) {
        UUID memberId = getMemberIdIfExists(userId);
        if (memberId != null) {
            return memberId == turmsClusterManager.getLocalMember().getUuid();
        } else {
            return false;
        }
    }

    public void remove(@NotNull Long userId) {
        if (isEnabled) {
            try {
                irresponsibleUserMap.remove(userId);
            } catch (Exception e) {
                log.error(e);
            }
        }
    }
}