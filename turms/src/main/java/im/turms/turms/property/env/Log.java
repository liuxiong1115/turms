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
import im.turms.turms.config.hazelcast.IdentifiedDataFactory;
import im.turms.turms.property.MutablePropertiesView;
import jdk.jfr.Description;
import lombok.Data;

import java.io.IOException;

@Data
public class Log implements IdentifiedDataSerializable {

    @JsonView(MutablePropertiesView.class)
    @Description("Whether to log admins' actions")
    private boolean logAdminAction = true;

    @JsonView(MutablePropertiesView.class)
    @Description("Whether to log users' login actions")
    private boolean logUserLogin = true;

    @JsonView(MutablePropertiesView.class)
    @Description("Whether to log the parameters of requests")
    private boolean logRequestParams = true;

    @JsonView(MutablePropertiesView.class)
    @Description("Whether to log the body of requests. Better log the body of requests by monitor systems (e.g. Nginx, AWS)")
    private boolean logRequestBody = true;

    @JsonIgnore
    @Override
    public int getFactoryId() {
        return IdentifiedDataFactory.FACTORY_ID;
    }

    @JsonIgnore
    @Override
    public int getClassId() {
        return IdentifiedDataFactory.Type.PROPERTY_LOG.getValue();
    }

    @Override
    public void writeData(ObjectDataOutput out) throws IOException {
        out.writeBoolean(logAdminAction);
        out.writeBoolean(logUserLogin);
        out.writeBoolean(logRequestParams);
        out.writeBoolean(logRequestBody);
    }

    @Override
    public void readData(ObjectDataInput in) throws IOException {
        logAdminAction = in.readBoolean();
        logUserLogin = in.readBoolean();
        logRequestParams = in.readBoolean();
        logRequestBody = in.readBoolean();
    }
}