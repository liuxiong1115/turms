package im.turms.turms.annotation.domain;


import org.springframework.data.mongodb.core.index.Indexed;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;


/**
 * Indexed by default because the index is used to support the widely used and advanced features of turms.
 * e.g.
 * 1. physically delete the data with a "deleted" flag,
 * <p>
 * 2. expiration date.
 * <p>
 * Remove the index if you don't need these features.
 */
@Indexed
// TODO: We should allow developers to customize the RetentionPolicy after
// the ticket https://github.com/turms-im/turms/issues/348 has been finished
@Retention(RetentionPolicy.RUNTIME)
public @interface OptionalIndexedForAdvancedFeature {
}
