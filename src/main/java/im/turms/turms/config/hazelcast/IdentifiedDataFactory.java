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

package im.turms.turms.config.hazelcast;

import com.hazelcast.nio.serialization.DataSerializableFactory;
import com.hazelcast.nio.serialization.IdentifiedDataSerializable;
import im.turms.turms.pojo.domain.Admin;
import im.turms.turms.pojo.domain.AdminRole;
import im.turms.turms.pojo.domain.GroupType;
import im.turms.turms.property.TurmsProperties;
import im.turms.turms.property.business.Group;
import im.turms.turms.property.business.Message;
import im.turms.turms.property.business.Notification;
import im.turms.turms.property.business.User;
import im.turms.turms.property.env.*;

public class IdentifiedDataFactory implements DataSerializableFactory {
    public static final int FACTORY_ID = 1;

    @Override
    public IdentifiedDataSerializable create(int typeId) {
        Type type = Type.values()[typeId - 1];
        switch (type) {
            case DOMAIN_ADMIN:
                return new Admin();
            case DOMAIN_ADMIN_ROLE:
                return new AdminRole();
            case DOMAIN_GROUP_TYPE:
                return new GroupType();
            case PROPERTIES:
                return new TurmsProperties();
            case PROPERTY_CACHE:
                return new Cache();
            case PROPERTY_CLUSTER:
                return new Cluster();
            case PROPERTY_DATABASE:
                return new Database();
            case PROPERTY_GROUP:
                return new Group();
            case PROPERTY_MESSAGE:
                return new Message();
            case PROPERTY_MESSAGE_READ_RECEIPT:
                return new Message.ReadReceipt();
            case PROPERTY_MESSAGE_TYPING_STATUS:
                return new Message.TypingStatus();
            case PROPERTY_NOTIFICATION:
                return new Notification();
            case PROPERTY_PLUGIN:
                return new Plugin();
            case PROPERTY_SECURITY:
                return new Security();
            case PROPERTY_SESSION:
                return new Session();
            case PROPERTY_USER:
                return new User();
            case PROPERTY_USER_LOCATION:
                return new User.Location();
            case PROPERTY_USER_FRIEND_REQUEST:
                return new User.FriendRequest();
            case PROPERTY_USER_SIMULTANEOUS_LOGIN:
                return new User.SimultaneousLogin();
        }
        return null;
    }

    private IdentifiedDataFactory() {
    }

    public enum Type {
        DOMAIN_ADMIN,
        DOMAIN_ADMIN_ROLE,
        DOMAIN_GROUP_TYPE,
        PROPERTIES,
        PROPERTY_CACHE,
        PROPERTY_CLUSTER,
        PROPERTY_DATABASE,
        PROPERTY_GROUP,
        PROPERTY_MESSAGE,
        PROPERTY_MESSAGE_TYPING_STATUS,
        PROPERTY_MESSAGE_READ_RECEIPT,
        PROPERTY_NOTIFICATION,
        PROPERTY_PLUGIN,
        PROPERTY_SECURITY,
        PROPERTY_SESSION,
        PROPERTY_USER,
        PROPERTY_USER_LOCATION,
        PROPERTY_USER_FRIEND_REQUEST,
        PROPERTY_USER_SIMULTANEOUS_LOGIN;

        public Integer getValue() {
            return this.ordinal() + 1;
        }
    }
}
