package im.turms.turms.annotation.domain;

import im.turms.turms.pojo.domain.AdminActionLog;
import org.springframework.data.mongodb.core.mapping.Sharded;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Disabled by default
 */
@Retention(RetentionPolicy.SOURCE)
@Sharded(immutableKey = true, shardKey = {AdminActionLog.Fields.LOG_DATE, AdminActionLog.Fields.ACTION})
public @interface AdminActionLogShardKey {
}
