package im.turms.turms.annotation.domain;


import org.springframework.data.mongodb.core.index.GeoSpatialIndexed;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;


/**
 * Indexed by default because the index is usually widely used for both users and admins
 * and it's unnecessary to avoid.
 * <p>
 * Use 8 bits with the error ± 19m for a better performance and if you want to keep
 * the error lower, set the bits to 9 with the error ± 2.4m.
 * <p>
 * Note that those errors come from a location near the equator.
 */
@GeoSpatialIndexed(bits = 8)
// TODO: We should allow developers to customize the RetentionPolicy after
// the ticket https://github.com/turms-im/turms/issues/348 has been finished
@Retention(RetentionPolicy.RUNTIME)
public @interface OptionalGeoSpatialIndexedForBasicFeature {
}
