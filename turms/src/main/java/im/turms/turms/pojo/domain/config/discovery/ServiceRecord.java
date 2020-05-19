package im.turms.turms.pojo.domain.config.discovery;

import io.github.classgraph.json.Id;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;

@Getter
@Document
@FieldNameConstants
public final class ServiceRecord {

    @Id
    private final String id;

    private final boolean isSeed;
    private final Date registrationDate;
    @Setter
    private Date lastHeartbeatDate;

    private final String privateAddressHost;
    private final int privateAddressPort;
    private final String publicAddressHost;
    private final int publicAddressPort;

    public ServiceRecord(
            String id,
            boolean isSeed,
            Date registrationDate,
            String privateAddressHost,
            Integer privateAddressPort,
            String publicAddressHost,
            Integer publicAddressPort) {
        this.id = id;
        this.isSeed = isSeed;
        this.registrationDate = registrationDate;
        this.lastHeartbeatDate = registrationDate;
        this.privateAddressHost = privateAddressHost;
        this.privateAddressPort = privateAddressPort;
        this.publicAddressHost = publicAddressHost;
        this.publicAddressPort = publicAddressPort;
    }
}