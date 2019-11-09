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

package im.turms.turms.property.business;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonView;
import com.hazelcast.nio.ObjectDataInput;
import com.hazelcast.nio.ObjectDataOutput;
import com.hazelcast.nio.serialization.IdentifiedDataSerializable;
import im.turms.turms.config.hazelcast.IdentifiedDataFactory;
import im.turms.turms.property.MutablePropertiesView;
import lombok.Data;

import java.io.IOException;

@Data
public class User implements IdentifiedDataSerializable {
    /**
     * If 0, these requests will not to be deleted
     */
    @JsonView(MutablePropertiesView.class)
    private int friendRequestExpiryHours = 30 * 24;
    private Location location = new Location();
    private SimultaneousLogin simultaneousLogin = new SimultaneousLogin();
    private FriendRequest friendRequest = new FriendRequest();
    //TODO
    @JsonView(MutablePropertiesView.class)
    private int logOnlineUserNumberIntervalSeconds = 60 * 5;
    /**
     * If true, use the operating system class as the device type rather than the agent class.
     * Note: Because Turms only provide turms-client-js for now,
     * all turms clients except the ones running on Node.js will recognized as browser environment
     */
    @JsonView(MutablePropertiesView.class)
    private boolean useOsAsDefaultDeviceType = true;
//    @JsonView(MutablePropertiesView.class)
//    private Set<DeviceType> mainDeviceTypes = ALL_DEVICE_TYPES;
    @JsonView(MutablePropertiesView.class)
    private boolean deleteTwoSidedRelationships = false;
    @JsonView(MutablePropertiesView.class)
    private boolean logicallyDeleteUser = true;

    @JsonIgnore
    @Override
    public int getFactoryId() {
        return IdentifiedDataFactory.FACTORY_ID;
    }

    @JsonIgnore
    @Override
    public int getClassId() {
        return IdentifiedDataFactory.Type.PROPERTY_USER.getValue();
    }

    @Override
    public void writeData(ObjectDataOutput out) throws IOException {
        out.writeInt(friendRequestExpiryHours);
        location.writeData(out);
        simultaneousLogin.writeData(out);
        friendRequest.writeData(out);
        out.writeInt(logOnlineUserNumberIntervalSeconds);
        out.writeBoolean(useOsAsDefaultDeviceType);
//        int[] types = mainDeviceTypes
//                .stream()
//                .mapToInt(DeviceType::getNumber).toArray();
//        out.writeIntArray(types);
        out.writeBoolean(deleteTwoSidedRelationships);
        out.writeBoolean(logicallyDeleteUser);
    }

    @Override
    public void readData(ObjectDataInput in) throws IOException {
        friendRequestExpiryHours = in.readInt();
        location.readData(in);
        simultaneousLogin.readData(in);
        friendRequest.readData(in);
        logOnlineUserNumberIntervalSeconds = in.readInt();
        useOsAsDefaultDeviceType = in.readBoolean();
        int[] types = in.readIntArray();
//        mainDeviceTypes = Arrays.stream(types)
//                .mapToObj(index -> DeviceType.values()[index])
//                .collect(Collectors.toSet());
        deleteTwoSidedRelationships = in.readBoolean();
        logicallyDeleteUser = in.readBoolean();
    }

    @Data
    public static class Location implements IdentifiedDataSerializable {
        @JsonView(MutablePropertiesView.class)
        private boolean enabled = true;
        @JsonView(MutablePropertiesView.class)
        private boolean persistent = true;
        @JsonView(MutablePropertiesView.class)
        private int maxQueryUsersNearbyNumber = 20;
        @JsonView(MutablePropertiesView.class)
        private double maxDistance = 0.1;
        @JsonView(MutablePropertiesView.class)
        private String removeLocationsCron = ""; //TODO

        @JsonIgnore
        @Override
        public int getFactoryId() {
            return IdentifiedDataFactory.FACTORY_ID;
        }

        @JsonIgnore
        @Override
        public int getClassId() {
            return IdentifiedDataFactory.Type.PROPERTY_USER_LOCATION.getValue();
        }

        @Override
        public void writeData(ObjectDataOutput out) throws IOException {
            out.writeBoolean(enabled);
            out.writeBoolean(persistent);
            out.writeInt(maxQueryUsersNearbyNumber);
            out.writeDouble(maxDistance);
            out.writeUTF(removeLocationsCron);
        }

        @Override
        public void readData(ObjectDataInput in) throws IOException {
            enabled = in.readBoolean();
            persistent = in.readBoolean();
            maxQueryUsersNearbyNumber = in.readInt();
            maxDistance = in.readDouble();
            removeLocationsCron = in.readUTF();
        }
    }

    @Data
    public static class FriendRequest implements IdentifiedDataSerializable {
        /**
         * if 0, there is no length limit.
         */
        @JsonView(MutablePropertiesView.class)
        private int contentLimit = 200;
        @JsonView(MutablePropertiesView.class)
        private boolean deleteExpiryRequests = false;
        @JsonView(MutablePropertiesView.class)
        private boolean allowResendRequestAfterDeclinedOrIgnoredOrExpired = false;

        @JsonIgnore
        @Override
        public int getFactoryId() {
            return IdentifiedDataFactory.FACTORY_ID;
        }

        @JsonIgnore
        @Override
        public int getClassId() {
            return IdentifiedDataFactory.Type.PROPERTY_USER_FRIEND_REQUEST.getValue();
        }

        @Override
        public void writeData(ObjectDataOutput out) throws IOException {
            out.writeInt(contentLimit);
            out.writeBoolean(deleteExpiryRequests);
            out.writeBoolean(allowResendRequestAfterDeclinedOrIgnoredOrExpired);
        }

        @Override
        public void readData(ObjectDataInput in) throws IOException {
            contentLimit = in.readInt();
            deleteExpiryRequests = in.readBoolean();
            allowResendRequestAfterDeclinedOrIgnoredOrExpired = in.readBoolean();
        }
    }

    @Data
    public static class SimultaneousLogin implements IdentifiedDataSerializable {
        @JsonView(MutablePropertiesView.class)
        private SimultaneousLoginStrategy strategy = SimultaneousLoginStrategy.ALLOW_ONE_DEVICE_OF_DESKTOP_AND_ONE_DEVICE_OF_MOBILE_ONLINE;
        @JsonView(MutablePropertiesView.class)
        private ConflictStrategy conflictStrategy = ConflictStrategy.FORCE_LOGGED_DEVICES_OFFLINE;
        @JsonView(MutablePropertiesView.class)
        private boolean allowUnknownDeviceCoexistsWithKnownDevice = false;

        @JsonIgnore
        @Override
        public int getFactoryId() {
            return IdentifiedDataFactory.FACTORY_ID;
        }

        @JsonIgnore
        @Override
        public int getClassId() {
            return IdentifiedDataFactory.Type.PROPERTY_USER_SIMULTANEOUS_LOGIN.getValue();
        }

        @Override
        public void writeData(ObjectDataOutput out) throws IOException {
            out.writeInt(strategy.ordinal());
            out.writeInt(conflictStrategy.ordinal());
            out.writeBoolean(allowUnknownDeviceCoexistsWithKnownDevice);
        }

        @Override
        public void readData(ObjectDataInput in) throws IOException {
            strategy = SimultaneousLoginStrategy.values()[in.readInt()];
            conflictStrategy = ConflictStrategy.values()[in.readInt()];
            allowUnknownDeviceCoexistsWithKnownDevice = in.readBoolean();
        }

        public enum SimultaneousLoginStrategy {
            ALLOW_ONE_DEVICE_OF_ONE_DEVICE_TYPE_ONLINE,
            ALLOW_ONE_DEVICE_OF_EVERY_DEVICE_TYPE_ONLINE,
            ALLOW_ONE_DEVICE_OF_DESKTOP_AND_ONE_DEVICE_OF_MOBILE_ONLINE,
            ALLOW_ONE_DEVICE_OF_DESKTOP_OR_WEB_AND_ONE_DEVICE_OF_MOBILE_ONLINE,
            ALLOW_ONE_DEVICE_OF_DESKTOP_AND_ONE_DEVICE_OF_WEB_AND_ONE_DEVICE_OF_MOBILE_ONLINE,
            ALLOW_ONE_DEVICE_OF_DESKTOP_OR_MOBILE_ONLINE,
            ALLOW_ONE_DEVICE_OF_DESKTOP_OR_WEB_OR_MOBILE_ONLINE
        }

        public enum ConflictStrategy {
            FORCE_LOGGED_DEVICES_OFFLINE,
            //            ACQUIRE_LOGGED_DEVICES_PERMISSION,
            LOGGING_DEVICE_OFFLINE
        }
    }
}