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
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.SimpleReactiveMongoDatabaseFactory;
import org.springframework.data.mongodb.core.WriteConcernResolver;
import org.springframework.data.mongodb.core.convert.*;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @see org.springframework.boot.autoconfigure.data.mongo.MongoReactiveDataAutoConfiguration
 */
@Configuration
public class MongoConfig {
    private final TurmsProperties turmsProperties;
    private final MongoMappingContext mongoMappingContext;
    private static final Map<MongoProperties, ReactiveMongoTemplate> map = new HashMap<>();

    public MongoConfig(
            TurmsProperties turmsProperties,
            MongoMappingContext mongoMappingContext) {
        this.mongoMappingContext = mongoMappingContext;
        this.turmsProperties = turmsProperties;
        this.mongoMappingContext.setAutoIndexCreation(true);
    }

    @Bean
    public MappingMongoConverter mappingMongoConverter() {
        List<Converter<?, ?>> converters = new ArrayList<>();
        converters.add(new EnumToIntegerConverter());
        converters.add(new IntegerToEnumConverter(null));
        CustomConversions customConversions = new CustomConversions(CustomConversions.StoreConversions.NONE, converters);

        DbRefResolver dbRefResolver = NoOpDbRefResolver.INSTANCE;
        MappingMongoConverter converter = new MappingMongoConverter(dbRefResolver, mongoMappingContext);
        converter.setTypeMapper(new DefaultMongoTypeMapper(null));
        converter.setCustomConversions(customConversions);
        ConversionService conversionService = converter.getConversionService();
        ((GenericConversionService) conversionService)
                .addConverterFactory(new IntegerToEnumConverterFactory());
        converter.afterPropertiesSet();
        return converter;
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
            MongoConverter mappingMongoConverter,
            ObjectProvider<MongoClientSettingsBuilderCustomizer> builderCustomizers,
            ObjectProvider<MongoClientSettings> settings,
            WriteConcernResolver writeConcernResolver) {
        return getMongoTemplate(turmsProperties.getDatabase().getMongoProperties().getLog(), builderCustomizers, settings, mappingMongoConverter, writeConcernResolver);
    }

    @Bean
    public ReactiveMongoTemplate adminMongoTemplate(
            TurmsProperties turmsProperties,
            MongoConverter mappingMongoConverter,
            ObjectProvider<MongoClientSettingsBuilderCustomizer> builderCustomizers,
            ObjectProvider<MongoClientSettings> settings,
            WriteConcernResolver writeConcernResolver) {
        return getMongoTemplate(turmsProperties.getDatabase().getMongoProperties().getAdmin(), builderCustomizers, settings, mappingMongoConverter, writeConcernResolver);
    }

    @Bean
    public ReactiveMongoTemplate groupMongoTemplate(
            TurmsProperties turmsProperties,
            MongoConverter mappingMongoConverter,
            ObjectProvider<MongoClientSettingsBuilderCustomizer> builderCustomizers,
            ObjectProvider<MongoClientSettings> settings,
            WriteConcernResolver writeConcernResolver) {
        return getMongoTemplate(turmsProperties.getDatabase().getMongoProperties().getGroup(), builderCustomizers, settings, mappingMongoConverter, writeConcernResolver);
    }

    @Bean
    public ReactiveMongoTemplate messageMongoTemplate(
            TurmsProperties turmsProperties,
            MongoConverter mappingMongoConverter,
            ObjectProvider<MongoClientSettingsBuilderCustomizer> builderCustomizers,
            ObjectProvider<MongoClientSettings> settings,
            WriteConcernResolver writeConcernResolver) {
        return getMongoTemplate(turmsProperties.getDatabase().getMongoProperties().getMessage(), builderCustomizers, settings, mappingMongoConverter, writeConcernResolver);
    }

    @Bean
    public ReactiveMongoTemplate userMongoTemplate(
            TurmsProperties turmsProperties,
            MongoConverter mappingMongoConverter,
            ObjectProvider<MongoClientSettingsBuilderCustomizer> builderCustomizers,
            ObjectProvider<MongoClientSettings> settings,
            WriteConcernResolver writeConcernResolver) {
        return getMongoTemplate(turmsProperties.getDatabase().getMongoProperties().getUser(), builderCustomizers, settings, mappingMongoConverter, writeConcernResolver);
    }

    private ReactiveMongoTemplate getMongoTemplate(
            MongoProperties properties,
            ObjectProvider<MongoClientSettingsBuilderCustomizer> builderCustomizers,
            ObjectProvider<MongoClientSettings> settings,
            MongoConverter converter,
            WriteConcernResolver writeConcernResolver) {
        return map.computeIfAbsent(getDefaultIfNotUpdated(properties), mongoProperties -> {
            ReactiveMongoClientFactory factory = new ReactiveMongoClientFactory(mongoProperties, null,
                    builderCustomizers.orderedStream().collect(Collectors.toList()));
            MongoClient mongoClient = factory.createMongoClient(settings.getIfAvailable());
            SimpleReactiveMongoDatabaseFactory databaseFactory = new SimpleReactiveMongoDatabaseFactory(mongoClient, mongoProperties.getMongoClientDatabase());
            ReactiveMongoTemplate mongoTemplate = new ReactiveMongoTemplate(databaseFactory, converter);
            mongoTemplate.setWriteConcernResolver(writeConcernResolver);
            return mongoTemplate;
        });
    }

    private MongoProperties getDefaultIfNotUpdated(MongoProperties properties) {
        MongoProperties defaultProperties = turmsProperties.getDatabase().getMongoProperties().getDefaultProperties();
        if (properties.getHost() != null && !properties.getHost().equals(defaultProperties.getHost()))
            return properties;
        if (properties.getPort() != null && !properties.getPort().equals(defaultProperties.getPort()))
            return properties;
        if (properties.getUri() != null && !properties.getUri().equals(defaultProperties.getUri()))
            return properties;
        if (properties.getDatabase() != null && !properties.getDatabase().equals(defaultProperties.getDatabase()))
            return properties;
        if (properties.getAuthenticationDatabase() != null && !properties.getAuthenticationDatabase().equals(defaultProperties.getAuthenticationDatabase()))
            return properties;
        if (properties.getGridFsDatabase() != null && !properties.getGridFsDatabase().equals(defaultProperties.getGridFsDatabase()))
            return properties;
        if (properties.getUsername() != null && !properties.getUsername().equals(defaultProperties.getUsername()))
            return properties;
        if (properties.getPassword() != null && !Arrays.equals(properties.getPassword(), defaultProperties.getPassword()))
            return properties;
        if (properties.getReplicaSetName() != null && !properties.getReplicaSetName().equals(defaultProperties.getReplicaSetName()))
            return properties;
        if (properties.getFieldNamingStrategy() != null && !properties.getFieldNamingStrategy().equals(defaultProperties.getFieldNamingStrategy()))
            return properties;
        if (properties.isAutoIndexCreation() != null && !properties.isAutoIndexCreation().equals(defaultProperties.isAutoIndexCreation()))
            return properties;
        return defaultProperties;
    }
}