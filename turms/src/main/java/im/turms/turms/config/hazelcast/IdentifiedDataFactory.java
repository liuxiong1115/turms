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

import com.hazelcast.nio.ObjectDataInput;
import com.hazelcast.nio.ObjectDataOutput;
import com.hazelcast.nio.serialization.DataSerializableFactory;
import com.hazelcast.nio.serialization.IdentifiedDataSerializable;
import im.turms.turms.pojo.bo.AdminInfo;
import im.turms.turms.pojo.domain.Admin;
import im.turms.turms.pojo.domain.AdminRole;
import im.turms.turms.pojo.domain.GroupType;
import im.turms.turms.pojo.domain.UserPermissionGroup;
import im.turms.turms.property.TurmsProperties;
import im.turms.turms.property.business.Group;
import im.turms.turms.property.business.Message;
import im.turms.turms.property.business.Notification;
import im.turms.turms.property.business.User;
import im.turms.turms.property.env.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class IdentifiedDataFactory implements DataSerializableFactory {
    public static final int FACTORY_ID = 1;

    @Override
    public IdentifiedDataSerializable create(int typeId) {
        Type type = Type.values()[typeId - 1];
        switch (type) {
            case BO_ADMIN_INFO:
                return new AdminInfo();
            case DOMAIN_ADMIN:
                return new Admin();
            case DOMAIN_ADMIN_ROLE:
                return new AdminRole();
            case DOMAIN_GROUP_TYPE:
                return new GroupType();
            case DOMAIN_USER_PERMISSION_GROUP:
                return new UserPermissionGroup();
            case PROPERTIES:
                return new TurmsProperties();
            case PROPERTY_ADMIN:
                return new im.turms.turms.property.env.Admin();
            case PROPERTY_CACHE:
                return new Cache();
            case PROPERTY_CLUSTER:
                return new Cluster();
            case PROPERTY_DATABASE:
                return new Database();
            case PROPERTY_ADDRESS:
                return new Address();
            case PROPERTY_LOG:
                return new Log();
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
            case PROPERTY_MOCK:
                return new Mock();
            case PROPERTY_PLUGIN:
                return new Plugin();
            case PROPERTY_RPC:
                return new Rpc();
            case PROPERTY_SECURITY:
                return new Security();
            case PROPERTY_SESSION:
                return new Session();
            case PROPERTY_STORAGE:
                return new Storage();
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
        BO_ADMIN_INFO,
        DOMAIN_ADMIN,
        DOMAIN_ADMIN_ROLE,
        DOMAIN_GROUP_TYPE,
        DOMAIN_USER_PERMISSION_GROUP,
        PROPERTIES,
        PROPERTY_ADMIN,
        PROPERTY_CACHE,
        PROPERTY_CLUSTER,
        PROPERTY_DATABASE,
        PROPERTY_GROUP,
        PROPERTY_ADDRESS,
        PROPERTY_LOG,
        PROPERTY_MESSAGE,
        PROPERTY_MESSAGE_READ_RECEIPT,
        PROPERTY_MESSAGE_TYPING_STATUS,
        PROPERTY_NOTIFICATION,
        PROPERTY_MOCK,
        PROPERTY_PLUGIN,
        PROPERTY_RPC,
        PROPERTY_SECURITY,
        PROPERTY_SESSION,
        PROPERTY_STORAGE,
        PROPERTY_USER,
        PROPERTY_USER_FRIEND_REQUEST,
        PROPERTY_USER_LOCATION,
        PROPERTY_USER_SIMULTANEOUS_LOGIN;

        public Integer getValue() {
            return this.ordinal() + 1;
        }
    }

    public static void writeMap(Map map, ObjectDataOutput rawDataOutput) throws IOException {
        int size = map.size();
        rawDataOutput.writeInt(size);
        final Set<Map.Entry> set = map.entrySet();
        for (Map.Entry entry : set) {
            final Object key = entry.getKey();
            final Object value = entry.getValue();
            rawDataOutput.writeObject(key);
            rawDataOutput.writeObject(value);
        }
    }

    public static Map readMaps(ObjectDataInput rawData) throws IOException {
        final int size = rawData.readInt();
        Map map = new HashMap<>(size);
        for (int i = 0; i < size; i++) {
            map.put(rawData.readObject(), rawData.readObject());
        }
        return map;
    }
}
