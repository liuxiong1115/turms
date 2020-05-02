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
public class Address implements IdentifiedDataSerializable {

    @Description("Whether to expose any kind of address to the public. " +
            "WARNING: If false, the load balancing balancing mechanism of this node will also be disabled.")
    private boolean enabled = true;

    @JsonView(MutablePropertiesView.class)
    @Description("The identity of this node exposed to the public. " +
            "The identity property is used if not null and not empty for your own custom deployment design " +
            "(e.g. use a custom identity to help Nginx proxy " +
            "or use the DDoS Protected IP address to hide the origin IP address)")
    private String identity = "";

    @JsonView(MutablePropertiesView.class)
    @Description("Whether to use the local IP to serve if \"identity\" property isn't valid." +
            "WARNING: For security, do NOT set this property to true in production to prevent from exposing the public IP address for DDoS attack.")
    private boolean useLocalIp = false;

    @JsonView(MutablePropertiesView.class)
    @Description("The IP detectors will be used to query the public IP if \"identity\" property isn't valid and useLocalIp is false")
    private List<String> ipDetectorAddresses = List.of("http://checkip.amazonaws.com", "http://bot.whatismyipaddress.com", "http://myip.dnsomatic.com");

    @JsonIgnore
    @Override
    public int getFactoryId() {
        return IdentifiedDataFactory.FACTORY_ID;
    }

    @JsonIgnore
    @Override
    public int getClassId() {
        return IdentifiedDataFactory.Type.PROPERTY_ADDRESS.getValue();
    }

    @Override
    public void writeData(ObjectDataOutput out) throws IOException {
        out.writeUTF(identity);
        out.writeBoolean(useLocalIp);
        out.writeUTFArray(ipDetectorAddresses.toArray(new String[0]));
    }

    @Override
    public void readData(ObjectDataInput in) throws IOException {
        identity = in.readUTF();
        useLocalIp = in.readBoolean();
        ipDetectorAddresses = Arrays.asList(in.readUTFArray());
    }
}