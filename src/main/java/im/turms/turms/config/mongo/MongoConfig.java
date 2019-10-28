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

import im.turms.turms.config.mongo.convert.EnumToIntegerConverter;
import im.turms.turms.config.mongo.convert.IntegerToEnumConverter;
import im.turms.turms.config.mongo.convert.IntegerToEnumConverterFactory;
import im.turms.turms.pojo.domain.*;
import im.turms.turms.property.TurmsProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.support.GenericConversionService;
import org.springframework.data.convert.CustomConversions;
import org.springframework.data.mongodb.MongoDbFactory;
import org.springframework.data.mongodb.ReactiveMongoDatabaseFactory;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.WriteConcernResolver;
import org.springframework.data.mongodb.core.convert.*;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;

import java.util.ArrayList;
import java.util.List;

// Reference: org/springframework/boot/autoconfigure/data/mongo/MongoReactiveDataAutoConfiguration.java
@Configuration
public class MongoConfig {
    // Note: Remember changing to turmsClusterManager.getTurmsProperties() when needing to support updating
    // config dynamically.
    private final TurmsProperties turmsProperties;
    private final MongoDbFactory mongoDbFactory;
    private final MongoMappingContext mongoMappingContext;

    public MongoConfig(MongoDbFactory mongoDbFactory, MongoMappingContext mongoMappingContext, TurmsProperties turmsProperties) {
        this.mongoDbFactory = mongoDbFactory;
        this.mongoMappingContext = mongoMappingContext;
        this.turmsProperties = turmsProperties;
        this.mongoMappingContext.setAutoIndexCreation(true);
    }

    @Bean
    public CustomConversions customConversions() {
        List<Converter<?, ?>> converters = new ArrayList<>();
        converters.add(new EnumToIntegerConverter());
        converters.add(new IntegerToEnumConverter(null));
        return new CustomConversions(CustomConversions.StoreConversions.NONE, converters);
    }

    @Bean
    public ReactiveMongoTemplate reactiveMongoTemplate(
            ReactiveMongoDatabaseFactory reactiveMongoDatabaseFactory,
            MongoConverter converter,
            WriteConcernResolver writeConcernResolver) {
        ReactiveMongoTemplate template = new ReactiveMongoTemplate(reactiveMongoDatabaseFactory, converter);
        template.setWriteConcernResolver(writeConcernResolver);
        return template;
    }

    @Bean
    public MappingMongoConverter mappingMongoConverter() {
        DbRefResolver dbRefResolver = new DefaultDbRefResolver(mongoDbFactory);
        MappingMongoConverter converter = new MappingMongoConverter(dbRefResolver, mongoMappingContext);
        converter.setTypeMapper(new DefaultMongoTypeMapper(null));
        converter.setCustomConversions(customConversions());
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
            if (entityType == AdminActionLog.class) return turmsProperties.getDatabase().getWriteConcern().getAdminActionLog();
            if (entityType == AdminRole.class) return turmsProperties.getDatabase().getWriteConcern().getAdminRole();
            if (entityType == Group.class) return turmsProperties.getDatabase().getWriteConcern().getGroup();
            if (entityType == GroupBlacklistedUser.class) return turmsProperties.getDatabase().getWriteConcern().getGroupBlacklistedUser();
            if (entityType == GroupInvitation.class) return turmsProperties.getDatabase().getWriteConcern().getGroupInvitation();
            if (entityType == GroupJoinQuestion.class) return turmsProperties.getDatabase().getWriteConcern().getGroupJoinQuestion();
            if (entityType == GroupJoinRequest.class) return turmsProperties.getDatabase().getWriteConcern().getGroupJoinRequest();
            if (entityType == GroupMember.class) return turmsProperties.getDatabase().getWriteConcern().getGroupMember();
            if (entityType == GroupType.class) return turmsProperties.getDatabase().getWriteConcern().getGroupType();
            if (entityType == GroupVersion.class) return turmsProperties.getDatabase().getWriteConcern().getGroupVersion();
            if (entityType == Message.class) return turmsProperties.getDatabase().getWriteConcern().getMessage();
            if (entityType == MessageStatus.class) return turmsProperties.getDatabase().getWriteConcern().getMessageStatus();
            if (entityType == User.class) return turmsProperties.getDatabase().getWriteConcern().getUser();
            if (entityType == UserFriendRequest.class) return turmsProperties.getDatabase().getWriteConcern().getUserFriendRequest();
            if (entityType == UserLocation.class) return turmsProperties.getDatabase().getWriteConcern().getUserLocation();
            if (entityType == UserLoginLog.class) return turmsProperties.getDatabase().getWriteConcern().getUserLoginLog();
            if (entityType == UserOnlineUserNumber.class) return turmsProperties.getDatabase().getWriteConcern().getUserOnlineUserNumber();
            if (entityType == UserPermissionGroup.class) return turmsProperties.getDatabase().getWriteConcern().getUserPermissionGroup();
            if (entityType == UserPermissionGroupMember.class) return turmsProperties.getDatabase().getWriteConcern().getUserPermissionGroupMember();
            if (entityType == UserRelationship.class) return turmsProperties.getDatabase().getWriteConcern().getUserRelationship();
            if (entityType == UserRelationshipGroup.class) return turmsProperties.getDatabase().getWriteConcern().getUserRelationshipGroup();
            if (entityType == UserRelationshipGroupMember.class) return turmsProperties.getDatabase().getWriteConcern().getUserRelationshipGroupMember();
            if (entityType == UserVersion.class) return turmsProperties.getDatabase().getWriteConcern().getUserVersion();
            return action.getDefaultWriteConcern();
        };
    }
}