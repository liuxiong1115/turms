package im.turms.turms.discovery.mongo;

import com.hazelcast.cluster.Address;
import com.hazelcast.spi.discovery.DiscoveryNode;
import com.mongodb.MongoClientSettings;
import com.mongodb.ReadPreference;
import com.mongodb.reactivestreams.client.MongoClient;
import im.turms.turms.pojo.domain.config.discovery.ServiceRecord;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import org.springframework.boot.autoconfigure.mongo.MongoProperties;
import org.springframework.boot.autoconfigure.mongo.ReactiveMongoClientFactory;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.SimpleReactiveMongoDatabaseFactory;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.index.PartialIndexFilter;
import org.springframework.data.mongodb.core.index.ReactiveIndexOperations;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import java.time.Duration;
import java.util.Collections;
import java.util.Date;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Log4j2
public class MongoDiscoveryClient {

    public static final int MAX_ALLOWABLE_TTL = 300;

    @Getter
    private final ReactiveMongoTemplate mongoTemplate;
    private final ScheduledExecutorService scheduler;

    private ServiceRecord localServiceRecord;

    public MongoDiscoveryClient(String uri) {
        mongoTemplate = getMongoTemplate(uri);
        scheduler = Executors.newSingleThreadScheduledExecutor();
        initIndices();
    }

    private ReactiveMongoTemplate getMongoTemplate(String uri) {
        MongoProperties properties = new MongoProperties();
        properties.setUri(uri);
        properties.setAutoIndexCreation(false);
        ReactiveMongoClientFactory factory = new ReactiveMongoClientFactory(properties, null, null);
        MongoClientSettings clientSettings = MongoClientSettings.builder().build();
        MongoClient mongoClient = factory.createMongoClient(clientSettings);
        SimpleReactiveMongoDatabaseFactory databaseFactory = new SimpleReactiveMongoDatabaseFactory(mongoClient, properties.getMongoClientDatabase());
        ReactiveMongoTemplate template = new ReactiveMongoTemplate(databaseFactory);
        template.setReadPreference(ReadPreference.primaryPreferred());
        return template;
    }

    private void initIndices() {
        PartialIndexFilter filter = PartialIndexFilter
                .of(Criteria.where(ServiceRecord.Fields.isSeed).is(false));
        Index myIndex = new Index()
                .background()
                .named(ServiceRecord.Fields.lastHeartbeatDate)
                .on(ServiceRecord.Fields.lastHeartbeatDate, Sort.Direction.ASC)
                .expire(MAX_ALLOWABLE_TTL)
                .partial(filter);
        ReactiveIndexOperations indexOps = mongoTemplate.indexOps(ServiceRecord.class);
        indexOps.ensureIndex(myIndex).block(Duration.ofSeconds(10));
    }

    public boolean hasRegistered() {
        return localServiceRecord != null;
    }

    public Set<ServiceRecord> queryHealthyNodes(int ttl) {
        long now = System.currentTimeMillis();
        Optional<Set<ServiceRecord>> serviceRecords = mongoTemplate.find(new Query(), ServiceRecord.class)
                .filter(serviceRecord -> (now - serviceRecord.getLastHeartbeatDate().getTime()) / 1000 <= ttl)
                .collect(Collectors.toSet())
                .blockOptional();
        return serviceRecords.orElse(Collections.emptySet());
    }

    public void register(DiscoveryNode localDiscoveryNode) {
        Address privateAddress = localDiscoveryNode.getPrivateAddress();
        Address publicAddress = localDiscoveryNode.getPublicAddress();
        Date now = new Date();
        ServiceRecord serviceRecord = new ServiceRecord(
                UUID.randomUUID().toString(),
                false,
                now,
                privateAddress.getHost(),
                privateAddress.getPort(),
                publicAddress.getHost(),
                publicAddress.getPort());
        mongoTemplate.insert(serviceRecord).block(Duration.ofSeconds(10));
        localServiceRecord = serviceRecord;
    }

    public void unregister() {
        try {
            stopHeartbeat();
        } catch (Exception ignored) {

        }
        if (localServiceRecord != null) {
            String id = localServiceRecord.getId();
            Query query = new Query().addCriteria(Criteria.where("_id").is(id));
            try {
                log.info(String.format("Unregistering %s", localServiceRecord.getId()));
                mongoTemplate.remove(query, ServiceRecord.class).block(Duration.ofSeconds(10));
                log.info(String.format("Unregistered %s", localServiceRecord.getId()));
            } catch (Exception e) {
                log.error("Failed to unregister", e);
            }
        }
    }

    public void startHeartbeat(long interval) {
        scheduler.scheduleWithFixedDelay(() -> {
            String id = localServiceRecord.getId();
            Query query = new Query().addCriteria(Criteria.where("_id").is(id));
            Date now = new Date();
            Update update = new Update().set(ServiceRecord.Fields.lastHeartbeatDate, now);
            long duration = Math.max(interval / 3, 3);
            mongoTemplate.updateFirst(query, update, ServiceRecord.class).block(Duration.ofSeconds(duration));
            localServiceRecord.setLastHeartbeatDate(now);
        }, interval, interval, TimeUnit.SECONDS);
    }

    public void stopHeartbeat() {
        scheduler.shutdownNow();
    }
}
