package im.turms.turms.discovery.mongo;

import com.hazelcast.config.properties.PropertyDefinition;
import com.hazelcast.config.properties.PropertyTypeConverter;
import com.hazelcast.config.properties.SimplePropertyDefinition;
import com.hazelcast.config.properties.ValidationException;

public class MongoDiscoveryConfiguration {

    public static final class Key {
        public static final String URI = "uri";
        public static final String HEARTBEAT_INTERVAL = "heartbeat-interval";
        public static final String HEARTBEAT_TIMEOUT = "heartbeat-timeout";

        private Key() {}
    }

    public static final PropertyDefinition URI =
            new SimplePropertyDefinition(Key.URI, true, PropertyTypeConverter.STRING);

    public static final PropertyDefinition HEARTBEAT_INTERVAL =
            new SimplePropertyDefinition(Key.HEARTBEAT_INTERVAL, true, PropertyTypeConverter.INTEGER, value -> {
                int heartbeatInterval = (int) value;
                if (heartbeatInterval <= 0 || heartbeatInterval >= MongoDiscoveryClient.MAX_ALLOWABLE_TTL) {
                    String message = String.format("%s must be greater or equal to 0 and less than %d", Key.HEARTBEAT_INTERVAL, MongoDiscoveryClient.MAX_ALLOWABLE_TTL);
                    throw new ValidationException(message);
                }
            });

    public static final PropertyDefinition HEARTBEAT_TIMEOUT =
            new SimplePropertyDefinition(Key.HEARTBEAT_TIMEOUT, true, PropertyTypeConverter.INTEGER, value -> {
                int heartbeatTimeout = (int) value;
                if (heartbeatTimeout <= 0 || heartbeatTimeout >= MongoDiscoveryClient.MAX_ALLOWABLE_TTL) {
                    String message = String.format("%s must be greater or equal to 0 and less than %d", Key.HEARTBEAT_TIMEOUT, MongoDiscoveryClient.MAX_ALLOWABLE_TTL);
                    throw new ValidationException(message);
                }
            });

    private MongoDiscoveryConfiguration() {
    }
}
