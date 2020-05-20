package im.turms.turms.discovery.mongo;

import com.hazelcast.config.properties.PropertyDefinition;
import com.hazelcast.logging.ILogger;
import com.hazelcast.spi.discovery.DiscoveryNode;
import com.hazelcast.spi.discovery.DiscoveryStrategy;
import com.hazelcast.spi.discovery.DiscoveryStrategyFactory;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

public class MongoDiscoveryStrategyFactory implements DiscoveryStrategyFactory {

    @Override
    public Class<? extends DiscoveryStrategy> getDiscoveryStrategyType() {
        return MongoDiscoveryStrategy.class;
    }

    @Override
    public DiscoveryStrategy newDiscoveryStrategy(DiscoveryNode discoveryNode, ILogger logger, Map<String, Comparable> properties) {
        return new MongoDiscoveryStrategy(discoveryNode, logger, properties);
    }

    @Override
    public Collection<PropertyDefinition> getConfigurationProperties() {
        return Arrays.asList(
                MongoDiscoveryConfiguration.URI,
                MongoDiscoveryConfiguration.HEARTBEAT_INTERVAL,
                MongoDiscoveryConfiguration.HEARTBEAT_TIMEOUT);
    }
}
