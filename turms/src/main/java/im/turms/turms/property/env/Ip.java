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
import java.util.Arrays;
import java.util.List;

@Data
public class Ip implements IdentifiedDataSerializable {

    @JsonView(MutablePropertiesView.class)
    @Description("The ip will be used if it's a valid IP")
    private String ip;

    @JsonView(MutablePropertiesView.class)
    @Description("Whether to use the local IP to serve if \"ip\" property isn't valid")
    private boolean useLocalIp = false;

    @JsonView(MutablePropertiesView.class)
    @Description("The IP detectors will be used to query the public IP if \"ip\" property isn't valid and useLocalIp is false")
    private List<String> ipDetectorAddresses = List.of("http://checkip.amazonaws.com", "http://bot.whatismyipaddress.com", "http://myip.dnsomatic.com");

    @JsonIgnore
    @Override
    public int getFactoryId() {
        return IdentifiedDataFactory.FACTORY_ID;
    }

    @JsonIgnore
    @Override
    public int getClassId() {
        return IdentifiedDataFactory.Type.PROPERTY_IP.getValue();
    }

    @Override
    public void writeData(ObjectDataOutput out) throws IOException {
        out.writeUTF(ip);
        out.writeBoolean(useLocalIp);
        out.writeUTFArray(ipDetectorAddresses.toArray(new String[0]));
    }

    @Override
    public void readData(ObjectDataInput in) throws IOException {
        ip = in.readUTF();
        useLocalIp = in.readBoolean();
        ipDetectorAddresses = Arrays.asList(in.readUTFArray());
    }
}