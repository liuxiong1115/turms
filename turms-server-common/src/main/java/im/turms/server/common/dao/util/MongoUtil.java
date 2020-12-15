package im.turms.server.common.dao.util;

import im.turms.server.common.dao.context.TurmsMongoMappingContext;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.index.ReactiveMongoPersistentEntityIndexCreator;
import org.springframework.data.mongodb.core.mapping.BasicMongoPersistentEntity;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * @author James Chen
 */
public class MongoUtil {
    private MongoUtil() {
    }

    public static void createIndexes(ReactiveMongoTemplate template, Set<Class<?>> classes) {
        TurmsMongoMappingContext context = (TurmsMongoMappingContext) template.getConverter().getMappingContext();
        ReactiveMongoPersistentEntityIndexCreator indexCreator = new ReactiveMongoPersistentEntityIndexCreator(context, template::indexOps);
        List<Mono<Void>> monos = new ArrayList<>(classes.size());
        for (Class<?> clazz : classes) {
            BasicMongoPersistentEntity<?> entity = context.getPersistentEntity(clazz);
            monos.add(indexCreator.checkForIndexes(entity));
        }
        Flux.merge(monos).blockLast(Duration.ofMinutes(5));
    }

}