package im.turms.turms.annotation.domain;


import org.springframework.data.mongodb.core.index.Indexed;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;


/**
 * Indexed by default because the index is usually widely used for both users and admins
 * and it's unnecessary to avoid.
 */
@Indexed
// TODO: We should allow developers to customize the RetentionPolicy after
// the ticket https://github.com/turms-im/turms/issues/348 has been finished
@Retention(RetentionPolicy.RUNTIME)
public @interface OptionalIndexedForBasicFeature {
}
