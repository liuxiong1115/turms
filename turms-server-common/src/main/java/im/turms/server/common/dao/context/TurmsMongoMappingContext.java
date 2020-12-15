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

package im.turms.server.common.dao.context;

import org.springframework.data.mapping.PersistentPropertyPath;
import org.springframework.data.mapping.PersistentPropertyPaths;
import org.springframework.data.mapping.PropertyPath;
import org.springframework.data.mapping.context.InvalidPersistentPropertyPath;
import org.springframework.data.mongodb.core.mapping.BasicMongoPersistentEntity;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;
import org.springframework.data.mongodb.core.mapping.MongoPersistentProperty;
import org.springframework.data.util.ClassTypeInformation;
import org.springframework.data.util.TypeInformation;
import org.springframework.util.Assert;

import java.util.*;
import java.util.function.Predicate;

/**
 * MappingContext is a frequently used class but MongoMappingContext uses locks frequently in its implementations,
 * so we implement our own context for better performance and fine-grained control.
 *
 * @author James Chen
 * @implNote We extends MongoMappingContext because ReactiveMongoTemplate has some operations only supported for MongoMappingContext
 */
public class TurmsMongoMappingContext extends MongoMappingContext {

    /**
     * Use volatile to implement a copy-on-write reference map
     */
    @SuppressWarnings("java:S3077")
    private volatile Map<Class<?>, BasicMongoPersistentEntity<?>> entities = new IdentityHashMap<>(128);

    @Override
    public Collection<BasicMongoPersistentEntity<?>> getPersistentEntities() {
        return entities.values();
    }

    @Override
    public BasicMongoPersistentEntity<?> getPersistentEntity(Class<?> type) {
        Assert.notNull(type, "Type must not be null!");
        BasicMongoPersistentEntity<?> entity = entities.get(type);
        if (entity != null) {
            return entity;
        }
        BasicMongoPersistentEntity<?> persistentEntity = super.getPersistentEntity(ClassTypeInformation.from(type));
        if (persistentEntity != null) {
            addEntity(type, persistentEntity);
        }
        return persistentEntity;
    }

    @Override
    public boolean hasPersistentEntityFor(Class<?> type) {
        Assert.notNull(type, "Type must not be null!");
        return entities.containsKey(type);
    }

    @Override
    public BasicMongoPersistentEntity<?> getPersistentEntity(TypeInformation<?> type) {
        return getPersistentEntity(type.getType());
    }

    @Override
    public BasicMongoPersistentEntity<?> getPersistentEntity(MongoPersistentProperty persistentProperty) {
        Assert.notNull(persistentProperty, "PersistentProperty must not be null!");
        if (!persistentProperty.isEntity()) {
            return null;
        }
        TypeInformation<?> typeInfo = persistentProperty.getTypeInformation();
        return getPersistentEntity(typeInfo.getRequiredActualType());
    }

    @Override
    public PersistentPropertyPath<MongoPersistentProperty> getPersistentPropertyPath(PropertyPath propertyPath) throws InvalidPersistentPropertyPath {
        return super.getPersistentPropertyPath(propertyPath);
    }

    @Override
    public PersistentPropertyPath<MongoPersistentProperty> getPersistentPropertyPath(String propertyPath, Class<?> type) throws InvalidPersistentPropertyPath {
        return super.getPersistentPropertyPath(propertyPath, type);
    }

    @Override
    public <T> PersistentPropertyPaths<T, MongoPersistentProperty> findPersistentPropertyPaths(Class<T> type, Predicate<? super MongoPersistentProperty> predicate) {
        return super.findPersistentPropertyPaths(type, predicate);
    }

    @Override
    public Collection<TypeInformation<?>> getManagedTypes() {
        Set<Class<?>> keys = entities.keySet();
        List<TypeInformation<?>> informationList = new ArrayList<>(keys.size());
        for (Class<?> clazz : keys) {
            ClassTypeInformation<?> information = ClassTypeInformation.from(clazz);
            informationList.add(information);
        }
        return informationList;
    }

    private synchronized void addEntity(Class<?> type, BasicMongoPersistentEntity<?> persistentEntity) {
        Map<Class<?>, BasicMongoPersistentEntity<?>> map = new IdentityHashMap<>(entities);
        map.put(type, persistentEntity);
        entities = map;
    }

}