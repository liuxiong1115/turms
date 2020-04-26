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

package im.turms.turms.property.env;


import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonView;
import com.hazelcast.nio.ObjectDataInput;
import com.hazelcast.nio.ObjectDataOutput;
import com.hazelcast.nio.serialization.IdentifiedDataSerializable;
import im.turms.turms.annotation.property.ImmutableOnceApplied;
import im.turms.turms.config.hazelcast.IdentifiedDataFactory;
import im.turms.turms.property.MutablePropertiesView;
import jdk.jfr.Description;
import lombok.Data;

import javax.validation.constraints.Min;
import java.io.IOException;

@Data
public class Security implements IdentifiedDataSerializable {

    @ImmutableOnceApplied
    @Description("The password encoding algorithm for users")
    private PasswordEncodingAlgorithm userPasswordEncodingAlgorithm = PasswordEncodingAlgorithm.SALTED_SHA256;

    @ImmutableOnceApplied
    @Description("The password encoding algorithm for admins")
    private PasswordEncodingAlgorithm adminPasswordEncodingAlgorithm = PasswordEncodingAlgorithm.BCRYPT;

    @JsonView(MutablePropertiesView.class)
    @Description("The maximum day difference per query request")
    @Min(0)
    private int maxDayDifferencePerRequest = 3 * 30;

    @JsonView(MutablePropertiesView.class)
    @Description("The maximum hour difference per count request")
    @Min(0)
    private int maxHourDifferencePerCountRequest = 24;

    @JsonView(MutablePropertiesView.class)
    @Description("The maximum day difference per count request")
    @Min(0)
    private int maxDayDifferencePerCountRequest = 31;

    @JsonView(MutablePropertiesView.class)
    @Description("The maximum month difference per count request")
    @Min(0)
    private int maxMonthDifferencePerCountRequest = 12;

    @JsonView(MutablePropertiesView.class)
    @Description("The maximum available records per query request")
    @Min(0)
    private int maxAvailableRecordsPerRequest = 1000;

    @JsonView(MutablePropertiesView.class)
    @Description("The maximum available online users' status per query request")
    @Min(0)
    private int maxAvailableOnlineUsersStatusPerRequest = 20;

    @JsonView(MutablePropertiesView.class)
    @Description("The default available records per query request")
    @Min(0)
    private int defaultAvailableRecordsPerRequest = 10;

    /**
     * If 0, there is no debounce.
     * Better set the same value as client's for a better UX.
     */
    @JsonView(MutablePropertiesView.class)
    @Description("The minimum allowed interval between client requests")
    @Min(0)
    private int minClientRequestsIntervalMillis = 0;

    @Description("Whether to respond the stack trace information to client when an exception is thrown")
    private boolean respondStackTraceWhenExceptionThrown = true;

    @JsonIgnore
    @Override
    public int getFactoryId() {
        return IdentifiedDataFactory.FACTORY_ID;
    }

    @JsonIgnore
    @Override
    public int getClassId() {
        return IdentifiedDataFactory.Type.PROPERTY_SECURITY.getValue();
    }

    @Override
    public void writeData(ObjectDataOutput out) throws IOException {
        out.writeInt(userPasswordEncodingAlgorithm.ordinal());
        out.writeInt(adminPasswordEncodingAlgorithm.ordinal());
        out.writeInt(maxDayDifferencePerRequest);
        out.writeInt(maxHourDifferencePerCountRequest);
        out.writeInt(maxDayDifferencePerCountRequest);
        out.writeInt(maxMonthDifferencePerCountRequest);
        out.writeInt(maxAvailableRecordsPerRequest);
        out.writeInt(maxAvailableOnlineUsersStatusPerRequest);
        out.writeInt(defaultAvailableRecordsPerRequest);
        out.writeInt(minClientRequestsIntervalMillis);
    }

    @Override
    public void readData(ObjectDataInput in) throws IOException {
        userPasswordEncodingAlgorithm = PasswordEncodingAlgorithm.values()[in.readInt()];
        adminPasswordEncodingAlgorithm = PasswordEncodingAlgorithm.values()[in.readInt()];
        maxDayDifferencePerRequest = in.readInt();
        maxHourDifferencePerCountRequest = in.readInt();
        maxDayDifferencePerCountRequest = in.readInt();
        maxMonthDifferencePerCountRequest = in.readInt();
        maxAvailableRecordsPerRequest = in.readInt();
        maxAvailableOnlineUsersStatusPerRequest = in.readInt();
        defaultAvailableRecordsPerRequest = in.readInt();
        minClientRequestsIntervalMillis = in.readInt();
    }

    public enum PasswordEncodingAlgorithm {
        BCRYPT,
        SALTED_SHA256,
        RAW //NO-OP
    }
}