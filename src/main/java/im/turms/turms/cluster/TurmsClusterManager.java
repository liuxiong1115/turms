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

package im.turms.turms.cluster;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.hazelcast.config.Config;
import com.hazelcast.config.EntryListenerConfig;
import com.hazelcast.config.ListenerConfig;
import com.hazelcast.config.ReplicatedMapConfig;
import com.hazelcast.core.*;
import com.hazelcast.flakeidgen.FlakeIdGenerator;
import com.hazelcast.scheduledexecutor.IScheduledExecutorService;
import im.turms.turms.annotation.cluster.HazelcastConfig;
import im.turms.turms.common.TurmsLogger;
import im.turms.turms.property.TurmsProperties;
import im.turms.turms.task.QueryResponsibleTurmsServerAddressTask;
import im.turms.turms.task.TurmsTaskExecutor;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.*;
import java.util.function.Function;

@Component
public class TurmsClusterManager {
    @Value("${server.port}")
    Integer port;

    public static final int HASH_SLOTS_NUMBER = 127;
    private static final String HAZELCAST_KEY_SHARED_PROPERTIES = "sharedProperties";
    private static final String HAZELCAST_KEY_DEFAULT = "default";
    private static final String CLUSTER_STATE = "clusterState";
    private static final String CLUSTER_TIME = "clusterTime";
    private static final String CLUSTER_VERSION = "clusterVersion";
    private static final String MEMBERS = "members";
    @Getter
    @Setter
    private HazelcastInstance hazelcastInstance;
    private TurmsProperties sharedTurmsProperties;
    private ReplicatedMap<SharedPropertiesKey, Object> sharedProperties;
    private List<Member> membersSnapshot = Collections.emptyList();
    private Member localMembersSnapshot;
    private boolean isMaster = false;
    private boolean hasJoinedCluster = false;
    private List<Function<MembershipEvent, Void>> listenersOnMembersChange;
    private FlakeIdGenerator idGenerator;
    private Cache<String, String> memberAddressCache;
    private final TurmsTaskExecutor turmsTaskExecutor;

    public TurmsClusterManager(
            TurmsProperties localTurmsProperties,
            @Lazy HazelcastInstance hazelcastInstance,
            @Lazy TurmsTaskExecutor turmsTaskExecutor) {
        sharedTurmsProperties = localTurmsProperties;
        listenersOnMembersChange = new LinkedList<>();
        this.hazelcastInstance = hazelcastInstance;
        memberAddressCache = Caffeine
                .newBuilder()
                .maximumSize(HASH_SLOTS_NUMBER)
                .build();
        this.turmsTaskExecutor = turmsTaskExecutor;
    }

    @HazelcastConfig
    public Function<Config, Void> clusterListenerConfig() {
        return config -> {
            ReplicatedMapConfig replicatedMapConfig = config.getReplicatedMapConfig(HAZELCAST_KEY_SHARED_PROPERTIES);
            replicatedMapConfig.addEntryListenerConfig(
                    new EntryListenerConfig().setImplementation(sharedPropertiesEntryListener()));
            config.addListenerConfig(new ListenerConfig(new MembershipAdapter() {
                @Override
                public void memberAdded(MembershipEvent membershipEvent) {
                    membersSnapshot = new ArrayList<>(hazelcastInstance.getCluster().getMembers());
                    memberAddressCache.invalidateAll();
                    if (membersSnapshot.size() > HASH_SLOTS_NUMBER) {
                        hazelcastInstance.shutdown();
                        throw new RuntimeException("The members of cluster should be not more than " + HASH_SLOTS_NUMBER);
                    }
                    localMembersSnapshot = hazelcastInstance.getCluster().getLocalMember();
                    if (!hasJoinedCluster) {
                        getEnvAfterJoinedCluster();
                    }
                    hasJoinedCluster = true;
                    if (isCurrentMemberMaster()) {
                        if (!isMaster && membersSnapshot.size() > 1) {
                            uploadPropertiesToAllMembers();
                        }
                        isMaster = true;
                    } else {
                        isMaster = false;
                    }
                    logWorkingRanges(
                            membershipEvent.getCluster().getMembers(),
                            membershipEvent.getCluster().getLocalMember());
                    notifyMembersChangeListeners(membershipEvent);
                }

                @Override
                public void memberRemoved(MembershipEvent membershipEvent) {
                    membersSnapshot = new ArrayList<>(hazelcastInstance.getCluster().getMembers());
                    memberAddressCache.invalidateAll();
                    localMembersSnapshot = hazelcastInstance.getCluster().getLocalMember();
                    logWorkingRanges(
                            membershipEvent.getCluster().getMembers(),
                            membershipEvent.getCluster().getLocalMember());
                    hasJoinedCluster = hasJoinedCluster();
                    notifyMembersChangeListeners(membershipEvent);
                }
            }));
            return null;
        };
    }

    public boolean isWorkable() {
        if (hazelcastInstance != null) {
            return hasJoinedCluster
                    && sharedTurmsProperties.getCluster().getMinimumQuorumToRun()
                    <= membersSnapshot.size();
        } else {
            return false;
        }
    }

    public boolean isCurrentMemberMaster() {
        for (Member member : hazelcastInstance.getCluster().getMembers()) {
            return member.getUuid()
                    .equals(hazelcastInstance.getCluster().getLocalMember().getUuid());
        }
        return false;
    }

    private void logWorkingRanges(@NotEmpty Set<Member> members, @NotNull Member localMember) {
        int step = HASH_SLOTS_NUMBER / members.size();
        Map<Object, Object> result = new HashMap<>();
        Member[] clusterMembers = members.toArray(new Member[0]);
        for (int index = 0; index < clusterMembers.length; index++) {
            int start = index * step;
            int end = index == clusterMembers.length - 1
                    ? HASH_SLOTS_NUMBER
                    : (index + 1) * step;
            String range = "[" + start + "," + end + ")";
            boolean isCurrentNodeRange = localMember == clusterMembers[index];
            result.put(index, range + (isCurrentNodeRange ? "*" : ""));
        }
        TurmsLogger.logJson("Working Ranges for Slot Indexes", result);
    }

    private void getEnvAfterJoinedCluster() {
        if (hazelcastInstance != null) {
            idGenerator = hazelcastInstance.getFlakeIdGenerator(HAZELCAST_KEY_DEFAULT);
            sharedProperties = hazelcastInstance.getReplicatedMap(HAZELCAST_KEY_SHARED_PROPERTIES);
            getTurmsPropertiesFromCluster();
        }
    }

    private boolean hasJoinedCluster() {
        Set<Member> members = hazelcastInstance.getCluster().getMembers();
        for (Member member : members) {
            if (hazelcastInstance.getCluster().getLocalMember().getUuid().equals(member.getUuid())) {
                return true;
            }
        }
        return false;
    }

    public IScheduledExecutorService getScheduledExecutor() {
        return hazelcastInstance.getScheduledExecutorService(HAZELCAST_KEY_DEFAULT);
    }

    public IExecutorService getExecutor() {
        return hazelcastInstance.getExecutorService(HAZELCAST_KEY_DEFAULT);
    }

    public void getTurmsPropertiesFromCluster() {
        Object turmsPropertiesObject = sharedProperties.get(SharedPropertiesKey.TURMS_PROPERTIES);
        if (turmsPropertiesObject instanceof TurmsProperties) {
            sharedTurmsProperties = (TurmsProperties) turmsPropertiesObject;
        }
    }

    public void uploadPropertiesToAllMembers() {
        sharedProperties.put(SharedPropertiesKey.TURMS_PROPERTIES, sharedTurmsProperties);
    }

    public void updateProperties(@NotNull TurmsProperties properties) {
        sharedProperties.put(SharedPropertiesKey.TURMS_PROPERTIES, properties);
    }

    private EntryAdapter<SharedPropertiesKey, Object> sharedPropertiesEntryListener() {
        return new EntryAdapter<>() {
            @Override
            public void entryUpdated(EntryEvent<SharedPropertiesKey, Object> event) {
                onSharedPropertiesAddedOrUpdated(event);
            }

            @Override
            public void entryAdded(EntryEvent<SharedPropertiesKey, Object> event) {
                onSharedPropertiesAddedOrUpdated(event);
            }
        };
    }

    private void onSharedPropertiesAddedOrUpdated(EntryEvent<SharedPropertiesKey, Object> event) {
        switch (event.getKey()) {
            case TURMS_PROPERTIES:
                sharedTurmsProperties = (TurmsProperties) event.getValue();
                break;
            default:
                break;
        }
    }

    public void addListenerOnMembersChange(Function<MembershipEvent, Void> listener) {
        listenersOnMembersChange.add(listener);
    }

    private void notifyMembersChangeListeners(MembershipEvent membershipEvent) {
        for (Function<MembershipEvent, Void> function : listenersOnMembersChange) {
            function.apply(membershipEvent);
        }
    }

    public Map<String, Object> getClusterInfo(boolean withConfigs) {
        Map<String, Object> map = new HashMap<>(4);
        map.put(CLUSTER_STATE, hazelcastInstance.getCluster().getClusterState());
        map.put(CLUSTER_TIME, hazelcastInstance.getCluster().getClusterTime());
        map.put(CLUSTER_VERSION, hazelcastInstance.getCluster().getClusterVersion());
        map.put(MEMBERS, hazelcastInstance.getCluster().getMembers());
        // Do not just pass hazelcastInstance.getConfig() which will cause JsonMappingException: Infinite recursion
        // TODO: optimize structure
        if (withConfigs) {
            map.put("configs", hazelcastInstance.getConfig().toString());
        }
        return map;
    }

    public TurmsProperties getTurmsProperties() {
        return sharedTurmsProperties;
    }

    /**
     * Note: It's unnecessary to check if the ID is 0L because of its mechanism
     */
    public Long generateRandomId() {
        return idGenerator.newId();
    }

    public boolean isCurrentNodeResponsibleByUserId(@NotNull Long userId) {
        int index = getSlotIndexByUserId(userId);
        Member member = getClusterMemberBySlotIndex(index);
        return member != null && member.getUuid().equals(localMembersSnapshot.getUuid());
    }

    public boolean isCurrentNodeResponsibleBySlotIndex(@NotNull Integer slotIndex) {
        Member member = getClusterMemberBySlotIndex(slotIndex);
        return member != null && member.getUuid().equals(localMembersSnapshot.getUuid());
    }

    public Member getLocalMember() {
        return localMembersSnapshot;
    }

    public Integer getLocalMemberIndex() {
        Member localMember = getLocalMember();
        Set<Member> members = getMembers();
        int index = -1;
        for (Member member : members) {
            index++;
            if (member == localMember) {
                return index;
            }
        }
        return null;
    }

    public Set<Member> getMembers() {
        return hazelcastInstance.getCluster().getMembers();
    }

    public Member getClusterMemberBySlotIndex(@NotNull Integer slotIndex) {
        if (slotIndex >= 0 && slotIndex < HASH_SLOTS_NUMBER) {
            int count = membersSnapshot.size();
            if (count == 0) {
                return null;
            }
            int memberIndex = slotIndex / (HASH_SLOTS_NUMBER / count);
            if (memberIndex < count) {
                return membersSnapshot.get(memberIndex);
            } else {
                return null;
            }
        } else {
            return null;
        }
    }


    /**
     * [start, end)
     */
    public Pair<Integer, Integer> getWorkingRange() {
        int size = membersSnapshot.size();
        if (size != 0) {
            Integer localMemberIndex = getLocalMemberIndex();
            if (localMemberIndex != null) {
                int step = HASH_SLOTS_NUMBER / size;
                return Pair.of(localMemberIndex * step,
                        (localMemberIndex + 1) * step);
            }
        }
        return null;
    }

    public Member getMemberByUserId(@NotNull Long userId) {
        int index = getSlotIndexByUserId(userId);
        return getClusterMemberBySlotIndex(index);
    }

    private int getSlotIndexByUserId(@NotNull Long userId) {
        return (int) (userId % HASH_SLOTS_NUMBER);
    }

    public Integer getSlotIndexByUserIdForCurrentNode(@NotNull Long userId) {
        int slotIndex = getSlotIndexByUserId(userId);
        Member member = getClusterMemberBySlotIndex(slotIndex);
        return member != null && member == getLocalMember() ? slotIndex : null;
    }

    public String getLocalTurmsServerAddress() {
        //TODO: cache
        return String.format("%s:%d",
                getLocalMember().getAddress().getHost(),
                port);
    }

    public Mono<String> getResponsibleTurmsServerAddress(@NotNull Long userId) {
        Member member = getMemberByUserId(userId);
        if (member == null) {
            return Mono.empty();
        } else {
            if (member.getUuid().equals(getLocalMember().getUuid())) {
                return Mono.just(getLocalTurmsServerAddress());
            } else {
                String address = memberAddressCache.getIfPresent(member.getUuid());
                if (address != null) {
                    return Mono.just(address);
            } else {
                return turmsTaskExecutor.call(member,
                            new QueryResponsibleTurmsServerAddressTask())
                            .doOnSuccess(addr -> memberAddressCache.put(
                                    member.getUuid(), addr));
                }
            }
        }
    }

    private enum SharedPropertiesKey {
        TURMS_PROPERTIES,
    }
}