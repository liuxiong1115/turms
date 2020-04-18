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
import im.turms.turms.config.hazelcast.IdentifiedDataFactory;
import im.turms.turms.constant.Common;
import im.turms.turms.property.business.Group;
import im.turms.turms.property.business.Message;
import im.turms.turms.property.business.Notification;
import im.turms.turms.property.business.User;
import im.turms.turms.property.env.*;
import im.turms.turms.util.MapUtil;
import jdk.jfr.Description;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@ConfigurationProperties(prefix = "turms")
@Component
@Data
@NoArgsConstructor
@Validated
public class TurmsProperties implements IdentifiedDataSerializable {
    public static final ObjectWriter MUTABLE_PROPERTIES_WRITER = new ObjectMapper()
            .setSerializationInclusion(JsonInclude.Include.NON_EMPTY)
            .disable(MapperFeature.DEFAULT_VIEW_INCLUSION)
            .writerWithView(MutablePropertiesView.class);
    public static final List<Function<TurmsProperties, Void>> propertiesChangeListeners = new LinkedList<>();

    // Env

    @JsonView(MutablePropertiesView.class)
    @Valid
    @NestedConfigurationProperty
    private Cache cache = new Cache();

    @JsonView(MutablePropertiesView.class)
    @Valid
    @NestedConfigurationProperty
    private Cluster cluster = new Cluster();

    @JsonView(MutablePropertiesView.class)
    @Valid
    @NestedConfigurationProperty
    private Database database = new Database();

    @JsonView(MutablePropertiesView.class)
    @Valid
    @NestedConfigurationProperty
    private Ip ip = new Ip();

    @JsonView(MutablePropertiesView.class)
    @Valid
    @NestedConfigurationProperty
    private Log log = new Log();

    @JsonView(MutablePropertiesView.class)
    @Valid
    @NestedConfigurationProperty
    private Session session = new Session();

    @JsonView(MutablePropertiesView.class)
    @Valid
    @NestedConfigurationProperty
    private Security security = new Security();

    @JsonView(MutablePropertiesView.class)
    @Valid
    @NestedConfigurationProperty
    private Storage storage = new Storage();

    @JsonView(MutablePropertiesView.class)
    @Valid
    @NestedConfigurationProperty
    private Plugin plugin = new Plugin();

    @JsonView(MutablePropertiesView.class)
    @Valid
    @NestedConfigurationProperty
    private Rpc rpc = new Rpc();

    // Business

    @JsonView(MutablePropertiesView.class)
    @Valid
    @NestedConfigurationProperty
    private Message message = new Message();

    @JsonView(MutablePropertiesView.class)
    @Valid
    @NestedConfigurationProperty
    private Group group = new Group();

    @JsonView(MutablePropertiesView.class)
    @Valid
    @NestedConfigurationProperty
    private User user = new User();

    @JsonView(MutablePropertiesView.class)
    @Valid
    @NestedConfigurationProperty
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
        ip.writeData(out);
        log.writeData(out);
        session.writeData(out);
        security.writeData(out);
        storage.writeData(out);
        plugin.writeData(out);
        rpc.writeData(out);

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
        ip.readData(in);
        log.readData(in);
        session.readData(in);
        security.readData(in);
        storage.readData(in);
        plugin.readData(in);
        rpc.readData(in);

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

    public static void addListeners(Function<TurmsProperties, Void> listener) {
        propertiesChangeListeners.add(listener);
    }

    public static void notifyListeners(TurmsProperties properties) {
        for (Function<TurmsProperties, Void> listener : propertiesChangeListeners) {
            listener.apply(properties);
        }
    }

    public static Map<String, Object> getPropertyValueMap(TurmsProperties turmsProperties, boolean mutable) throws IOException {
        if (mutable) {
            return Common.MAPPER.readValue(MUTABLE_PROPERTIES_WRITER.writeValueAsBytes(turmsProperties), Common.TYPE_REF_MAP);
        } else {
            return Common.MAPPER.readValue(Common.MAPPER.writeValueAsBytes(turmsProperties), Common.TYPE_REF_MAP);
        }
    }

    public static Map<String, Object> getMetadata(Map<String, Object> map, Class<?> clazz, boolean onlyMutable, boolean withMutableFlag) {
        String packageName = TurmsProperties.class.getPackageName();
        List<Field> fieldList;
        if (onlyMutable) {
            fieldList = FieldUtils.getFieldsListWithAnnotation(clazz, JsonView.class)
                    .stream()
                    .filter(TurmsProperties::isMutableProperty)
                    .collect(Collectors.toList());
        } else {
            fieldList = FieldUtils.getAllFieldsList(clazz);
        }
        for (Field field : fieldList) {
            if (field.getType().getTypeName().startsWith(packageName)) {
                if (field.getType().isEnum()) {
                    HashMap<Object, Object> fieldMap = new HashMap<>(5);
                    fieldMap.put("type", "enum");
                    fieldMap.put("options", field.getType().getEnumConstants());
                    fieldMap.put("deprecated", field.isAnnotationPresent(Deprecated.class));
                    if (field.isAnnotationPresent(Description.class)) {
                        fieldMap.put("desc", field.getDeclaredAnnotation(Description.class).value());
                    }
                    if (withMutableFlag) {
                        fieldMap.put("mutable", TurmsProperties.isMutableProperty(field));
                    }
                    map.put(field.getName(), fieldMap);
                } else {
                    Object any = getMetadata(new HashMap<>(), field.getType(), onlyMutable, withMutableFlag);
                    map.put(field.getName(), any);
                }
            } else if (!Modifier.isStatic(field.getModifiers())) {
                String typeName = field.getType().getTypeName();
                if (typeName.equals(String.class.getTypeName())) {
                    typeName = "string";
                }
                HashMap<Object, Object> fieldMap = new HashMap<>(4);
                fieldMap.put("type", typeName);
                fieldMap.put("deprecated", field.isAnnotationPresent(Deprecated.class));
                if (field.isAnnotationPresent(Description.class)) {
                    fieldMap.put("desc", field.getDeclaredAnnotation(Description.class).value());
                }
                if (withMutableFlag) {
                    fieldMap.put("mutable", TurmsProperties.isMutableProperty(field));
                }
                map.put(field.getName(), fieldMap);
            }
        }
        return map;
    }

    private static boolean isMutableProperty(Field field) {
        if (field.isAnnotationPresent(JsonView.class)) {
            JsonView jsonView = field.getDeclaredAnnotation(JsonView.class);
            for (Class<?> clazz : jsonView.value()) {
                if (clazz == MutablePropertiesView.class) {
                    return true;
                }
            }
        }
        return false;
    }
}