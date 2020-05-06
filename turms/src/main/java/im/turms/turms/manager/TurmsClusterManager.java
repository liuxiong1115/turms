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

package im.turms.turms.manager;

import com.hazelcast.cluster.Member;
import com.hazelcast.cluster.MembershipAdapter;
import com.hazelcast.cluster.MembershipEvent;
import com.hazelcast.collection.ISet;
import com.hazelcast.config.Config;
import com.hazelcast.config.EntryListenerConfig;
import com.hazelcast.config.ListenerConfig;
import com.hazelcast.config.ReplicatedMapConfig;
import com.hazelcast.core.EntryAdapter;
import com.hazelcast.core.EntryEvent;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IExecutorService;
import com.hazelcast.flakeidgen.FlakeIdGenerator;
import com.hazelcast.replicatedmap.ReplicatedMap;
import im.turms.turms.annotation.cluster.HazelcastConfig;
import im.turms.turms.annotation.cluster.PostHazelcastJoined;
import im.turms.turms.property.TurmsProperties;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Log4j2
@Component
public class TurmsClusterManager {

    public static final int HASH_SLOTS_NUMBER = 127;
    public static final String ATTRIBUTE_ADDRESS = "ADDR";
    private static final String HAZELCAST_KEY_SHARED_PROPERTIES = "properties";
    private static final String HAZELCAST_KEY_MEMBER_ADDRESSES = "addresses";
    private static final String HAZELCAST_KEY_DEFAULT = "default";
    private static final String HAZELCAST_KEY_INACTIVE_MEMBERS = "inactiveMembers";

    private static final String CLUSTER_STATE = "clusterState";
    private static final String CLUSTER_TIME = "clusterTime";
    private static final String CLUSTER_VERSION = "clusterVersion";
    private static final String MEMBERS = "members";
    private final ApplicationContext context;
    private TurmsProperties sharedTurmsProperties;
    @Getter
    @Setter
    private HazelcastInstance hazelcastInstance;
    private ReplicatedMap<SharedPropertiesKey, Object> sharedProperties;
    private ReplicatedMap<UUID, String> memberAddresses; // Don't just use MemberAttributeConfig because it only supports immutable attributes
    private ISet<UUID> inactiveMembers;

    @Getter
    private List<Member> membersSnapshot = Collections.emptyList();
    @Getter
    private List<Member> otherMembersSnapshot = Collections.emptyList();
    private Map<UUID, Member> membersSnapshotMap = Collections.emptyMap();
    private Member localMembersSnapshot;

    private boolean isMaster = false;
    private boolean hasJoinedCluster = false;
    private FlakeIdGenerator idGenerator;
    private final List<Function<MembershipEvent, Void>> onMembersChangeListeners;
    private volatile String pendingMemberAddress;

    public TurmsClusterManager(
            TurmsProperties localTurmsProperties,
            @Qualifier("hazelcastInstance") @Lazy HazelcastInstance hazelcastInstance,
            ApplicationContext context) {
        sharedTurmsProperties = localTurmsProperties;
        onMembersChangeListeners = new LinkedList<>();
        this.hazelcastInstance = hazelcastInstance;
        this.context = context;
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
                    initCurrentNodeStatusAfterMemberChange(membershipEvent);
                }

                @Override
                public void memberRemoved(MembershipEvent membershipEvent) {
                    initCurrentNodeStatusAfterMemberChange(membershipEvent);
                }
            }));
            return null;
        };
    }

    private synchronized void initCurrentNodeStatusAfterMemberChange(MembershipEvent membershipEvent) {
        membersSnapshot = hazelcastInstance.getCluster().getMembers()
                .stream()
                .sorted(Comparator.comparing(Member::getUuid))
                .collect(Collectors.toList());
        localMembersSnapshot = hazelcastInstance.getCluster().getLocalMember();
        otherMembersSnapshot = membersSnapshot
                .stream()
                .filter(member -> !member.getUuid().equals(localMembersSnapshot.getUuid()))
                .collect(Collectors.toList());
        Map<UUID, Member> memberMap = new HashMap<>(membersSnapshot.size());
        for (Member member : membersSnapshot) {
            memberMap.put(member.getUuid(), member);
        }
        membersSnapshotMap = memberMap;
        if (membershipEvent.getEventType() == MembershipEvent.MEMBER_ADDED) {
            if (membersSnapshot.size() > HASH_SLOTS_NUMBER) {
                shutdown();
                throw new RuntimeException("The members of cluster should be not more than " + HASH_SLOTS_NUMBER);
            }
            if (!hasJoinedCluster) {
                initEnvAfterJoinedCluster();
            }
            hasJoinedCluster = true;
            if (membersSnapshot.get(0).getUuid().equals(localMembersSnapshot.getUuid())) {
                if (!isMaster && membersSnapshot.size() > 1) {
                    uploadPropertiesToAllMembers(sharedTurmsProperties);
                }
                isMaster = true;
            } else {
                isMaster = false;
            }
        } else {
            hasJoinedCluster = membersSnapshot.contains(localMembersSnapshot);
            memberAddresses.remove(membershipEvent.getMember().getUuid());
        }
        logWorkingRanges(
                membershipEvent.getCluster().getMembers(),
                membershipEvent.getCluster().getLocalMember());
        notifyMembersChangeListeners(membershipEvent);
    }

    public boolean isActive() {
        return hazelcastInstance.getLifecycleService().isRunning()
                && hasJoinedCluster
                && sharedTurmsProperties.getCluster().getMinimumQuorumToServe() <= membersSnapshot.size()
                && !inactiveMembers.contains(localMembersSnapshot.getUuid());
    }

    public boolean isCurrentMemberMaster() {
        return isMaster;
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
            String range = String.format("[%d,%d)", start, end);
            boolean isCurrentNodeRange = localMember == clusterMembers[index];
            result.put(index, range + (isCurrentNodeRange ? "*" : ""));
        }
        log.info("Working Ranges for Slot Indexes: {}", result);
    }

    private void initEnvAfterJoinedCluster() {
        if (hazelcastInstance != null) {
            idGenerator = hazelcastInstance.getFlakeIdGenerator(HAZELCAST_KEY_DEFAULT);
            sharedProperties = hazelcastInstance.getReplicatedMap(HAZELCAST_KEY_SHARED_PROPERTIES);
            memberAddresses = hazelcastInstance.getReplicatedMap(HAZELCAST_KEY_MEMBER_ADDRESSES);
            inactiveMembers = hazelcastInstance.getSet(HAZELCAST_KEY_INACTIVE_MEMBERS);

            Map<String, Object> beans = context.getBeansWithAnnotation(PostHazelcastJoined.class);
            for (Object value : beans.values()) {
                if (value instanceof Function) {
                    Function<HazelcastInstance, Void> function = (Function<HazelcastInstance, Void>) value;
                    function.apply(hazelcastInstance);
                }
            }
            fetchSharedPropertiesFromCluster();
            if (pendingMemberAddress != null) {
                updateAddress(pendingMemberAddress);
                pendingMemberAddress = null;
            }
        }
    }

    public IExecutorService getExecutor() {
        return hazelcastInstance.getExecutorService(HAZELCAST_KEY_DEFAULT);
    }

    public void fetchSharedPropertiesFromCluster() {
        Object turmsPropertiesObject = sharedProperties.get(SharedPropertiesKey.TURMS_PROPERTIES);
        if (turmsPropertiesObject instanceof TurmsProperties) {
            sharedTurmsProperties = (TurmsProperties) turmsPropertiesObject;
        }
    }

    public boolean uploadPropertiesToAllMembers(@NotNull TurmsProperties properties) {
        log.info("uploading turms properties to all members");
        try {
            sharedProperties.put(SharedPropertiesKey.TURMS_PROPERTIES, properties);
        } catch (Exception e) {
            log.error("failed to upload turms properties to all members", e);
            return false;
        }
        log.info("turms properties have been uploaded to all members");
        return true;
    }

    public void updatePropertiesAndNotify(@NotNull TurmsProperties properties) {
        boolean isUploaded = uploadPropertiesToAllMembers(properties);
        if (isUploaded) {
            TurmsProperties.notifyListeners(properties);
        }
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
        onMembersChangeListeners.add(listener);
    }

    private void notifyMembersChangeListeners(MembershipEvent membershipEvent) {
        for (Function<MembershipEvent, Void> function : onMembersChangeListeners) {
            function.apply(membershipEvent);
        }
    }

    public Map<String, Object> getHazelcastInfo(boolean withConfigs) {
        Map<String, Object> map = new HashMap<>(withConfigs ? 5 : 4);
        map.put(CLUSTER_STATE, hazelcastInstance.getCluster().getClusterState());
        map.put(CLUSTER_TIME, hazelcastInstance.getCluster().getClusterTime());
        map.put(CLUSTER_VERSION, hazelcastInstance.getCluster().getClusterVersion());
        map.put(MEMBERS, hazelcastInstance.getCluster().getMembers());
        if (withConfigs) {
            map.put("configs", hazelcastInstance.getConfig());
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

    public String getAddressIfCurrentNodeIrresponsibleByUserId(@NotNull Long userId) {
        Member member = getMemberByUserId(userId);
        if (member == null || member == localMembersSnapshot) {
            return null;
        } else {
            return getAddress(member);
        }
    }

    public Member getMemberIfCurrentNodeIrresponsibleByUserId(@NotNull Long userId) {
        Member member = getMemberByUserId(userId);
        if (member == null || member == localMembersSnapshot) {
            return null;
        } else {
            return member;
        }
    }

    public Member getLocalMember() {
        return localMembersSnapshot;
    }

    public Integer getLocalMemberIndex() {
        Member localMember = getLocalMember();
        int index = membersSnapshot.indexOf(localMember);
        return index != -1 ? index : null;
    }

    public Set<Member> getMembers() {
        return hazelcastInstance.getCluster().getMembers();
    }

    public Map<UUID, Member> getMembersMap() {
        return membersSnapshotMap;
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
    public Pair<Integer, Integer> getResponsibleSlotIndexRange() {
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

    public Member getMemberById(@NotNull UUID memberId) {
        return membersSnapshotMap.get(memberId);
    }

    public int getSlotIndexByUserId(@NotNull Long userId) {
        return (int) (userId % HASH_SLOTS_NUMBER);
    }

    public Integer getSlotIndexByUserIdForCurrentNode(@NotNull Long userId) {
        int slotIndex = getSlotIndexByUserId(userId);
        Member member = getClusterMemberBySlotIndex(slotIndex);
        return member != null && member == getLocalMember() ? slotIndex : null;
    }

    public String getResponsibleTurmsServerAddress(@NotNull Long userId) {
        Member member = getMemberByUserId(userId);
        if (member == null) {
            return null;
        } else {
            return getAddress(member);
        }
    }

    public void shutdown() {
        if (hazelcastInstance != null && hazelcastInstance.getLifecycleService().isRunning()) {
            hazelcastInstance.shutdown();
        }
    }

    public boolean isSingleton() {
        return membersSnapshot.size() == 1;
    }

    public void updateAddress(String address) {
        if (memberAddresses != null) {
            memberAddresses.put(localMembersSnapshot.getUuid(), address);
        } else {
            pendingMemberAddress = address;
        }
    }

    public String getAddress(Member member) {
        String address = memberAddresses.get(member.getUuid());
        if (address != null) {
            return address;
        } else {
            return member.getAttribute(ATTRIBUTE_ADDRESS);
        }
    }

    public String getLocalServerAddress() {
        return getAddress(localMembersSnapshot);
    }

    public List<String> getServersAddress(boolean onlyActiveServers, boolean onlyInactiveServers) {
        List<String> addresses = new ArrayList<>(membersSnapshot.size());
        for (Member member : membersSnapshot) {
            if (onlyActiveServers) {
                if (!inactiveMembers.contains(member.getUuid())) {
                    addresses.add(getAddress(member));
                }
            } else if (onlyInactiveServers) {
                if (inactiveMembers.contains(member.getUuid())) {
                    addresses.add(getAddress(member));
                }
            } else {
                addresses.add(getAddress(member));
            }
        }
        return addresses;
    }

    public Map<UUID, String> getServersIdMap(boolean onlyActiveServers, boolean onlyInactiveServers) {
        Map<UUID, String> map = new HashMap<>(membersSnapshot.size());
        for (Map.Entry<UUID, Member> entry : membersSnapshotMap.entrySet()) {
            UUID uuid = entry.getKey();
            Member member = entry.getValue();
            if (onlyActiveServers) {
                if (!inactiveMembers.contains(uuid)) {
                    map.put(uuid, getAddress(member));
                }
            } else if (onlyInactiveServers) {
                if (inactiveMembers.contains(uuid)) {
                    map.put(uuid, getAddress(member));
                }
            } else {
                map.put(uuid, getAddress(member));
            }
        }
        return map;
    }

    public void activateServers(Set<UUID> ids) {
        inactiveMembers.removeAll(ids);
    }

    public void deactivateServers(Set<UUID> ids) {
        inactiveMembers.addAll(ids);
    }

    private enum SharedPropertiesKey {
        TURMS_PROPERTIES
    }
}