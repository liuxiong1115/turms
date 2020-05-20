package im.turms.turms.annotation.domain;


import org.springframework.data.mongodb.core.index.Indexed;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;


/**
 * Not indexed by default because the domain marked as OptionalIndexedForDifferentAmount
 * usually has only a few (or some) documents and has a low index selectivity.
 * <p>
 * No need to add an index unless your application really has a lot of records
 * and you are sure that it has a medium or high index selectivity.
 */
@Indexed
// TODO: We should allow developers to customize the RetentionPolicy after
// the ticket https://github.com/turms-im/turms/issues/348 has been finished
@Retention(RetentionPolicy.SOURCE)
public @interface OptionalIndexedForDifferentAmount {
}
