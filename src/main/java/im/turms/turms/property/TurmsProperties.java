/*
 * Copyright (C) 2019 The Turms Project
 * https://github.com/turms-im/turms
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package im.turms.turms.property;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.hazelcast.nio.ObjectDataInput;
import com.hazelcast.nio.ObjectDataOutput;
import com.hazelcast.nio.serialization.IdentifiedDataSerializable;
import im.turms.turms.config.hazelcast.IdentifiedDataFactory;
import im.turms.turms.property.business.Group;
import im.turms.turms.property.business.Message;
import im.turms.turms.property.business.Notification;
import im.turms.turms.property.business.User;
import im.turms.turms.property.env.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;

import javax.validation.constraints.NotNull;
import java.io.IOException;

@ConfigurationProperties(prefix = "turms")
@Data
@NoArgsConstructor
public class TurmsProperties implements IdentifiedDataSerializable {
    public static final ObjectWriter MUTABLE_PROPERTIES_WRITER = new ObjectMapper()
            .setSerializationInclusion(JsonInclude.Include.NON_EMPTY)
            .writerWithView(MutablePropertiesView.class);
    public static final ObjectReader MUTABLE_PROPERTIES_READER = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .readerWithView(MutablePropertiesView.class)
            .forType(TurmsProperties.class);
    private Cache cache = new Cache();
    private Cluster cluster = new Cluster();
    private Database database = new Database();
    private Session session = new Session();
    private Security security = new Security();
    private Plugin plugin = new Plugin();

    private Message message = new Message();
    private Group group = new Group();
    private User user = new User();
    private Notification notification = new Notification();

    @JsonIgnore
    @Override
    public int getFactoryId() {
        return IdentifiedDataFactory.FACTORY_ID;
    }

    @JsonIgnore
    @Override
    public int getClassId() {
        return IdentifiedDataFactory.Type.PROPERTIES.getValue();
    }

    public static TurmsProperties merge(
            @NotNull TurmsProperties propertiesToUpdate,
            @NotNull TurmsProperties propertiesForUpdating) throws IOException {
        ObjectReader objectReader = new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .setSerializationInclusion(JsonInclude.Include.NON_EMPTY)
                .readerForUpdating(propertiesToUpdate)
                .forType(TurmsProperties.class);
        return objectReader.readValue(MUTABLE_PROPERTIES_WRITER.writeValueAsBytes(propertiesForUpdating));
    }

    public static TurmsProperties getMutableProperties(TurmsProperties turmsProperties) throws IOException {
        return MUTABLE_PROPERTIES_READER.readValue(MUTABLE_PROPERTIES_WRITER.writeValueAsBytes(turmsProperties));
    }

    @Override
    public void writeData(ObjectDataOutput out) throws IOException {
        cache.writeData(out);
        cluster.writeData(out);
        database.writeData(out);
        session.writeData(out);
        security.writeData(out);
        plugin.writeData(out);

        message.writeData(out);
        group.writeData(out);
        user.writeData(out);
        notification.writeData(out);
    }

    @Override
    public void readData(ObjectDataInput in) throws IOException {
        cache.readData(in);
        cluster.readData(in);
        database.readData(in);
        session.readData(in);
        security.readData(in);
        plugin.readData(in);

        message.readData(in);
        group.readData(in);
        user.readData(in);
        notification.readData(in);
    }
}