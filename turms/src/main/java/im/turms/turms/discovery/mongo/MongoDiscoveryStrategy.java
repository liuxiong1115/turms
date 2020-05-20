package im.turms.turms.discovery.mongo;

import com.hazelcast.cluster.Address;
import com.hazelcast.logging.ILogger;
import com.hazelcast.spi.discovery.AbstractDiscoveryStrategy;
import com.hazelcast.spi.discovery.DiscoveryNode;
import com.hazelcast.spi.discovery.SimpleDiscoveryNode;
import im.turms.turms.pojo.domain.config.discovery.ServiceRecord;

import java.net.UnknownHostException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Note that the hazelcast discovery mechanism is designed to find seed servers.
 * That is different from the service discovery mechanism for the traditional microservices architecture.
 * TODO: call com.hazelcast.internal.cluster.impl.DiscoveryJoiner#getPossibleAddresses() manually by reflection
 */
public class MongoDiscoveryStrategy extends AbstractDiscoveryStrategy {

    private final MongoDiscoveryClient discoveryClient;
    private final int heartbeatTimeout;

    /**
     * @param localDiscoveryNode passed from com.hazelcast.instance.impl.Node#createDiscoveryService(com.hazelcast.config.DiscoveryConfig, java.util.List, com.hazelcast.cluster.Member)
     */
    public MongoDiscoveryStrategy(DiscoveryNode localDiscoveryNode, ILogger logger, Map<String, Comparable> properties) {
        super(logger, properties);
        String uri = getOrDefault(MongoDiscoveryConfiguration.Key.URI, MongoDiscoveryConfiguration.URI, "mongodb://localhost:27017/turms-config");
        long heartbeatInterval = getOrDefault(MongoDiscoveryConfiguration.Key.HEARTBEAT_INTERVAL, MongoDiscoveryConfiguration.HEARTBEAT_INTERVAL, 25);
        heartbeatTimeout = getOrDefault(MongoDiscoveryConfiguration.Key.HEARTBEAT_TIMEOUT, MongoDiscoveryConfiguration.HEARTBEAT_TIMEOUT, 60);
        discoveryClient = new MongoDiscoveryClient(uri);
        discoveryClient.register(localDiscoveryNode);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (discoveryClient.hasRegistered()) {
                discoveryClient.unregister();
            }
        }));

        discoveryClient.startHeartbeat(heartbeatInterval);
    }

    @Override
    public Iterable<DiscoveryNode> discoverNodes() {
        Set<ServiceRecord> serviceRecords = discoveryClient.queryHealthyNodes(heartbeatTimeout);
        if (serviceRecords.isEmpty()) {
            return Collections.emptySet();
        }
        Set<DiscoveryNode> discoveryNodes = new HashSet<>(serviceRecords.size());
        for (ServiceRecord serviceRecord : serviceRecords) {
            try {
                Address privateAddress = new Address(serviceRecord.getPrivateAddressHost(), serviceRecord.getPrivateAddressPort());
                Address publicAddress = new Address(serviceRecord.getPublicAddressHost(), serviceRecord.getPublicAddressPort());
                SimpleDiscoveryNode discoveryNode = new SimpleDiscoveryNode(privateAddress, publicAddress);
                discoveryNodes.add(discoveryNode);
            } catch (UnknownHostException e) {
                getLogger().severe(e);
            }
        }
        return discoveryNodes;
    }
}
