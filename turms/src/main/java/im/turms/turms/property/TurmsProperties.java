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
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@ConfigurationProperties(prefix = "turms", ignoreUnknownFields = false)
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
    private static Path latestConfigFilePath;

    // Env

    @JsonView(MutablePropertiesView.class)
    @Valid
    @NestedConfigurationProperty
    private Admin admin = new Admin();

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
    private Address address = new Address();

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
    private Mock mock = new Mock();

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

    @Autowired
    public TurmsProperties(Environment environment) {
        String[] activeProfiles = environment.getActiveProfiles();
        String activeProfile = null;
        for (String profile : activeProfiles) {
            if (!profile.contains("latest")) {
                activeProfile = profile;
                break;
            }
        }
        // The property should be passed from turms.cmd or turms.sh
        String configDir = System.getProperty("spring.config.location");
        if (configDir == null || configDir.isBlank()) {
            configDir = "./config";
        }
        String latestConfigFileName = activeProfile != null
                ? String.format("application-%s-latest.yaml", activeProfile)
                : "application-latest.yaml";
        latestConfigFilePath = Path.of(String.format("%s/%s", configDir, latestConfigFileName));
    }

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
        admin.writeData(out);
        cache.writeData(out);
        cluster.writeData(out);
        database.writeData(out);
        address.writeData(out);
        log.writeData(out);
        session.writeData(out);
        security.writeData(out);
        storage.writeData(out);
        mock.writeData(out);
        plugin.writeData(out);
        rpc.writeData(out);

        message.writeData(out);
        group.writeData(out);
        user.writeData(out);
        notification.writeData(out);
    }

    @Override
    public void readData(ObjectDataInput in) throws IOException {
        admin.readData(in);
        cache.readData(in);
        cluster.readData(in);
        database.readData(in);
        address.readData(in);
        log.readData(in);
        session.readData(in);
        security.readData(in);
        storage.readData(in);
        mock.readData(in);
        plugin.readData(in);
        rpc.readData(in);

        message.readData(in);
        group.readData(in);
        user.readData(in);
        notification.readData(in);
    }

    public void persist(String propertiesJson) throws IOException {
        ObjectNode tree = getNotEmptyPropertiesTree(propertiesJson);
        Yaml yaml = getYaml();
        String configYaml = yaml.dump(yaml.load(MUTABLE_PROPERTIES_WRITER.writeValueAsString(tree)));
        Path dir = latestConfigFilePath.getParent();
        if (dir != null) {
            Files.createDirectories(dir);
        }
        Files.writeString(latestConfigFilePath, configYaml, StandardCharsets.UTF_8,
                StandardOpenOption.WRITE,
                StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.CREATE);
    }

    public static TurmsProperties merge(
            @NotNull TurmsProperties propertiesToUpdate,
            @NotNull String propertiesForUpdating) throws IOException {
        ObjectReader objectReader = new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .setSerializationInclusion(JsonInclude.Include.NON_EMPTY)
                .readerForUpdating(propertiesToUpdate)
                .forType(TurmsProperties.class);
        return objectReader.readValue(propertiesForUpdating);
    }

    public static TurmsProperties merge(
            @NotNull TurmsProperties propertiesToUpdate,
            @NotNull TurmsProperties propertiesForUpdating) throws IOException {
        return merge(propertiesToUpdate, getMutablePropertiesString(propertiesForUpdating));
    }

    public static String getMutablePropertiesString(TurmsProperties propertiesForUpdating) throws JsonProcessingException {
        return MUTABLE_PROPERTIES_WRITER.writeValueAsString(propertiesForUpdating);
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
        fieldList = fieldList
                .stream()
                .filter(field -> !field.isAnnotationPresent(JsonIgnore.class))
                .collect(Collectors.toList());
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

    @JsonIgnore
    private Yaml getYaml() {
        DumperOptions options = new DumperOptions();
        options.setIndent(2);
        options.setPrettyFlow(true);
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        return new Yaml(options);
    }

    private ObjectNode getNotEmptyPropertiesTree(String propertiesJson) throws JsonProcessingException {
        ObjectNode jsonNodeTree = (ObjectNode) new ObjectMapper().readTree(propertiesJson);
        List<String> emptyFieldNames = new LinkedList<>();
        Iterator<Map.Entry<String, JsonNode>> fields = jsonNodeTree.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> entry = fields.next();
            if (entry.getValue().size() == 0) {
                emptyFieldNames.add(entry.getKey());
            }
        }
        for (String name : emptyFieldNames) {
            jsonNodeTree.remove(name);
        }
        ObjectNode tree = JsonNodeFactory.instance.objectNode();
        tree.set("turms", jsonNodeTree);
        return tree;
    }
}