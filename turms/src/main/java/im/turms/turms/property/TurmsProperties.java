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
import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.databind.*;
import com.hazelcast.nio.ObjectDataInput;
import com.hazelcast.nio.ObjectDataOutput;
import com.hazelcast.nio.serialization.IdentifiedDataSerializable;
import im.turms.turms.common.Constants;
import im.turms.turms.common.MapUtil;
import im.turms.turms.config.hazelcast.IdentifiedDataFactory;
import im.turms.turms.property.business.Group;
import im.turms.turms.property.business.Message;
import im.turms.turms.property.business.Notification;
import im.turms.turms.property.business.User;
import im.turms.turms.property.env.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.springframework.boot.context.properties.ConfigurationProperties;

import javax.validation.constraints.NotNull;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ConfigurationProperties(prefix = "turms")
@Data
@NoArgsConstructor
public class TurmsProperties implements IdentifiedDataSerializable {
    public static final ObjectWriter MUTABLE_PROPERTIES_WRITER = new ObjectMapper()
            .setSerializationInclusion(JsonInclude.Include.NON_EMPTY)
            .disable(MapperFeature.DEFAULT_VIEW_INCLUSION)
            .writerWithView(MutablePropertiesView.class);

    @JsonView(MutablePropertiesView.class)
    private Cache cache = new Cache();
    @JsonView(MutablePropertiesView.class)
    private Cluster cluster = new Cluster();
    @JsonView(MutablePropertiesView.class)
    private Database database = new Database();
    @JsonView(MutablePropertiesView.class)
    private Log log = new Log();
    @JsonView(MutablePropertiesView.class)
    private Session session = new Session();
    @JsonView(MutablePropertiesView.class)
    private Security security = new Security();
    @JsonView(MutablePropertiesView.class)
    private Plugin plugin = new Plugin();

    @JsonView(MutablePropertiesView.class)
    private Message message = new Message();
    @JsonView(MutablePropertiesView.class)
    private Group group = new Group();
    @JsonView(MutablePropertiesView.class)
    private User user = new User();
    @JsonView(MutablePropertiesView.class)
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

    @Override
    public void writeData(ObjectDataOutput out) throws IOException {
        cache.writeData(out);
        cluster.writeData(out);
        database.writeData(out);
        log.writeData(out);
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
        log.readData(in);
        session.readData(in);
        security.readData(in);
        plugin.readData(in);

        message.readData(in);
        group.readData(in);
        user.readData(in);
        notification.readData(in);
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

    public TurmsProperties reset() throws IOException {
        return merge(this, new TurmsProperties());
    }

    public static Map<String, Object> mergePropertiesWithMetadata(
            @NotNull Map<String, Object> properties,
            @NotNull Map<String, Object> metadata) {
        properties = MapUtil.addValueKeyToAllLeaves(properties);
        return MapUtil.deepMerge(properties, metadata);
    }

    public static Map<String, Object> getPropertiesMap(TurmsProperties turmsProperties, boolean mutable) throws IOException {
        if (mutable) {
            return Constants.MAPPER.readValue(MUTABLE_PROPERTIES_WRITER.writeValueAsBytes(turmsProperties), Constants.TYPE_REF_MAP);
        } else {
            return Constants.MAPPER.readValue(Constants.MAPPER.writeValueAsBytes(turmsProperties), Constants.TYPE_REF_MAP);
        }
    }

    public static Map<String, Object> getMetadata(Map<String, Object> map, Class<?> clazz, boolean onlyMutable, boolean withMutableFlag) {
        String packageName = TurmsProperties.class.getPackageName();
        List<Field> fieldList;
        if (onlyMutable) {
            fieldList = FieldUtils.getFieldsListWithAnnotation(clazz, JsonView.class);
        } else {
            fieldList = FieldUtils.getAllFieldsList(clazz);
        }
        for (Field field : fieldList) {
            if (field.getType().getTypeName().startsWith(packageName)) {
                if (field.getType().isEnum()) {
                    if (withMutableFlag) {
                        map.put(field.getName(), Map.of("type", "enum",
                                "options", field.getType().getEnumConstants(),
                                "mutable", field.isAnnotationPresent(JsonView.class)));
                    } else {
                        map.put(field.getName(), Map.of("type", "enum",
                                "options", field.getType().getEnumConstants()));
                    }
                } else {
                    Object any = getMetadata(new HashMap<>(), field.getType(), onlyMutable, withMutableFlag);
                    map.put(field.getName(), any);
                }
            } else if (!Modifier.isStatic(field.getModifiers())) {
                String typeName = field.getType().getTypeName();
                if (typeName.equals(String.class.getTypeName())) {
                    typeName = "string";
                }
                if (withMutableFlag) {
                    map.put(field.getName(), Map.of("type", typeName,
                            "mutable", field.isAnnotationPresent(JsonView.class)));
                } else {
                    map.put(field.getName(), Map.of("type", typeName));
                }
            }
        }
        return map;
    }
}