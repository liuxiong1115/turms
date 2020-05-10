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

package im.turms.turms.config.mongo;

import com.mongodb.MongoClientSettings;
import com.mongodb.reactivestreams.client.MongoClient;
import im.turms.turms.config.mongo.convert.EnumToIntegerConverter;
import im.turms.turms.config.mongo.convert.IntegerToEnumConverter;
import im.turms.turms.config.mongo.convert.IntegerToEnumConverterFactory;
import im.turms.turms.pojo.domain.*;
import im.turms.turms.property.TurmsProperties;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.mongo.MongoClientSettingsBuilderCustomizer;
import org.springframework.boot.autoconfigure.mongo.MongoProperties;
import org.springframework.boot.autoconfigure.mongo.ReactiveMongoClientFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.support.GenericConversionService;
import org.springframework.data.convert.CustomConversions;
import org.springframework.data.mapping.model.CamelCaseAbbreviatingFieldNamingStrategy;
import org.springframework.data.mapping.model.FieldNamingStrategy;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.SimpleReactiveMongoDatabaseFactory;
import org.springframework.data.mongodb.core.WriteConcernResolver;
import org.springframework.data.mongodb.core.WriteResultChecking;
import org.springframework.data.mongodb.core.convert.DefaultMongoTypeMapper;
import org.springframework.data.mongodb.core.convert.MappingMongoConverter;
import org.springframework.data.mongodb.core.convert.NoOpDbRefResolver;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @see org.springframework.boot.autoconfigure.data.mongo.MongoReactiveDataAutoConfiguration
 */
@Configuration
public class MongoConfig {
    private final TurmsProperties turmsProperties;
    // hash code of MongoProperties -> ReactiveMongoTemplate
    // because MongoProperties doesn't have a custom hashcode implementation but a native implementation
    private static final Map<Integer, ReactiveMongoTemplate> TEMPLATE_MAP = new HashMap<>(5);

    public MongoConfig(TurmsProperties turmsProperties) {
        this.turmsProperties = turmsProperties;
    }

    @Bean
    public WriteConcernResolver writeConcernResolver() {
        return action -> {
            Class<?> entityType = action.getEntityType();
            if (entityType == Admin.class) return turmsProperties.getDatabase().getWriteConcern().getAdmin();
            if (entityType == AdminActionLog.class)
                return turmsProperties.getDatabase().getWriteConcern().getAdminActionLog();
            if (entityType == AdminRole.class) return turmsProperties.getDatabase().getWriteConcern().getAdminRole();
            if (entityType == Group.class) return turmsProperties.getDatabase().getWriteConcern().getGroup();
            if (entityType == GroupBlacklistedUser.class)
                return turmsProperties.getDatabase().getWriteConcern().getGroupBlacklistedUser();
            if (entityType == GroupInvitation.class)
                return turmsProperties.getDatabase().getWriteConcern().getGroupInvitation();
            if (entityType == GroupJoinQuestion.class)
                return turmsProperties.getDatabase().getWriteConcern().getGroupJoinQuestion();
            if (entityType == GroupJoinRequest.class)
                return turmsProperties.getDatabase().getWriteConcern().getGroupJoinRequest();
            if (entityType == GroupMember.class)
                return turmsProperties.getDatabase().getWriteConcern().getGroupMember();
            if (entityType == GroupType.class) return turmsProperties.getDatabase().getWriteConcern().getGroupType();
            if (entityType == GroupVersion.class)
                return turmsProperties.getDatabase().getWriteConcern().getGroupVersion();
            if (entityType == Message.class) return turmsProperties.getDatabase().getWriteConcern().getMessage();
            if (entityType == MessageStatus.class)
                return turmsProperties.getDatabase().getWriteConcern().getMessageStatus();
            if (entityType == User.class) return turmsProperties.getDatabase().getWriteConcern().getUser();
            if (entityType == UserActionLog.class)
                return turmsProperties.getDatabase().getWriteConcern().getUserActionLog();
            if (entityType == UserFriendRequest.class)
                return turmsProperties.getDatabase().getWriteConcern().getUserFriendRequest();
            if (entityType == UserLocation.class)
                return turmsProperties.getDatabase().getWriteConcern().getUserLocation();
            if (entityType == UserLoginLog.class)
                return turmsProperties.getDatabase().getWriteConcern().getUserLoginLog();
            if (entityType == UserOnlineUserNumber.class)
                return turmsProperties.getDatabase().getWriteConcern().getUserOnlineUserNumber();
            if (entityType == UserPermissionGroup.class)
                return turmsProperties.getDatabase().getWriteConcern().getUserPermissionGroup();
            if (entityType == UserRelationship.class)
                return turmsProperties.getDatabase().getWriteConcern().getUserRelationship();
            if (entityType == UserRelationshipGroup.class)
                return turmsProperties.getDatabase().getWriteConcern().getUserRelationshipGroup();
            if (entityType == UserRelationshipGroupMember.class)
                return turmsProperties.getDatabase().getWriteConcern().getUserRelationshipGroupMember();
            if (entityType == UserVersion.class)
                return turmsProperties.getDatabase().getWriteConcern().getUserVersion();
            return action.getDefaultWriteConcern();
        };
    }

    @Bean
    public ReactiveMongoTemplate logMongoTemplate(
            TurmsProperties turmsProperties,
            ObjectProvider<MongoClientSettingsBuilderCustomizer> builderCustomizers,
            ObjectProvider<MongoClientSettings> settings,
            WriteConcernResolver writeConcernResolver) {
        return getMongoTemplate(turmsProperties.getDatabase().getMongoProperties().getLog(), builderCustomizers, settings, writeConcernResolver);
    }

    @Bean
    public ReactiveMongoTemplate adminMongoTemplate(
            TurmsProperties turmsProperties,
            ObjectProvider<MongoClientSettingsBuilderCustomizer> builderCustomizers,
            ObjectProvider<MongoClientSettings> settings,
            WriteConcernResolver writeConcernResolver) {
        return getMongoTemplate(turmsProperties.getDatabase().getMongoProperties().getAdmin(), builderCustomizers, settings, writeConcernResolver);
    }

    @Bean
    public ReactiveMongoTemplate groupMongoTemplate(
            TurmsProperties turmsProperties,
            ObjectProvider<MongoClientSettingsBuilderCustomizer> builderCustomizers,
            ObjectProvider<MongoClientSettings> settings,
            WriteConcernResolver writeConcernResolver) {
        return getMongoTemplate(turmsProperties.getDatabase().getMongoProperties().getGroup(), builderCustomizers, settings, writeConcernResolver);
    }

    @Bean
    public ReactiveMongoTemplate messageMongoTemplate(
            TurmsProperties turmsProperties,
            ObjectProvider<MongoClientSettingsBuilderCustomizer> builderCustomizers,
            ObjectProvider<MongoClientSettings> settings,
            WriteConcernResolver writeConcernResolver) {
        return getMongoTemplate(turmsProperties.getDatabase().getMongoProperties().getMessage(), builderCustomizers, settings, writeConcernResolver);
    }

    @Bean
    public ReactiveMongoTemplate userMongoTemplate(
            TurmsProperties turmsProperties,
            ObjectProvider<MongoClientSettingsBuilderCustomizer> builderCustomizers,
            ObjectProvider<MongoClientSettings> settings,
            WriteConcernResolver writeConcernResolver) {
        return getMongoTemplate(turmsProperties.getDatabase().getMongoProperties().getUser(), builderCustomizers, settings, writeConcernResolver);
    }

    public MappingMongoConverter newMongoConverter(MongoMappingContext mongoMappingContext) {
        List<Converter<?, ?>> converters = new ArrayList<>();
        converters.add(new EnumToIntegerConverter());
        converters.add(new IntegerToEnumConverter(null));

        CustomConversions customConversions = new CustomConversions(CustomConversions.StoreConversions.NONE, converters);
        MappingMongoConverter converter = new MappingMongoConverter(NoOpDbRefResolver.INSTANCE, mongoMappingContext);
        converter.setTypeMapper(new DefaultMongoTypeMapper(null));
        converter.setCustomConversions(customConversions);

        ConversionService conversionService = converter.getConversionService();
        ((GenericConversionService) conversionService)
                .addConverterFactory(new IntegerToEnumConverterFactory());
        converter.afterPropertiesSet();
        return converter;
    }

    private ReactiveMongoTemplate getMongoTemplate(
            MongoProperties properties,
            ObjectProvider<MongoClientSettingsBuilderCustomizer> builderCustomizers,
            ObjectProvider<MongoClientSettings> settings,
            WriteConcernResolver writeConcernResolver) {
        return TEMPLATE_MAP.computeIfAbsent(getPropertiesHashCode(properties), key -> {
            // ReactiveMongoClientFactory
            ReactiveMongoClientFactory factory = new ReactiveMongoClientFactory(properties, null,
                    builderCustomizers.orderedStream().collect(Collectors.toList()));
            MongoClient mongoClient = factory.createMongoClient(settings.getIfAvailable());
            SimpleReactiveMongoDatabaseFactory databaseFactory = new SimpleReactiveMongoDatabaseFactory(mongoClient, properties.getMongoClientDatabase());

            // MongoMappingContext
            boolean autoIndexCreation = properties.isAutoIndexCreation() != null
                    ? properties.isAutoIndexCreation()
                    : true;
            // Note that we don't use the field naming strategy specified by developer
            MongoMappingContext context = new MongoMappingContext();
            context.setAutoIndexCreation(autoIndexCreation);

            // MappingMongoConverter
            MappingMongoConverter converter = newMongoConverter(context);

            // ReactiveMongoTemplate
            ReactiveMongoTemplate mongoTemplate = new ReactiveMongoTemplate(databaseFactory, converter);
            mongoTemplate.setWriteConcernResolver(writeConcernResolver);
            mongoTemplate.setWriteResultChecking(WriteResultChecking.EXCEPTION);

            return mongoTemplate;
        });
    }

    private int getPropertiesHashCode(MongoProperties properties) {
        int result = Objects.hash(properties.getHost(), properties.getPort(), properties.getUri(),
                properties.getDatabase(), properties.getAuthenticationDatabase(),
                properties.getGridFsDatabase(), properties.getUsername(),
                properties.getReplicaSetName(), properties.getFieldNamingStrategy(),
                properties.getUuidRepresentation(), properties.isAutoIndexCreation());
        result = 31 * result + Arrays.hashCode(properties.getPassword());
        return result;
    }
}