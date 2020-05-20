package im.turms.turms.annotation.domain;


import org.springframework.data.mongodb.core.index.Indexed;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;


/**
 * Not indexed by default because if your application really has some features that
 * needs to be implemented by these queries that uses the fields marked as OptionalIndexedForCustomFeature
 * (in most cases these queries should be able to eliminate and should be eliminated
 * especially in the sharded collections), you may need to index these fields.
 * <p>
 * By the way, to use the targeted queries in sharded collections, you may need to create a new auxiliary collection.
 */
@Indexed
// TODO: We should allow developers to customize the RetentionPolicy after
// the ticket https://github.com/turms-im/turms/issues/348 has been finished
@Retention(RetentionPolicy.RUNTIME)
public @interface OptionalIndexedForDynamicStatistics {
}
