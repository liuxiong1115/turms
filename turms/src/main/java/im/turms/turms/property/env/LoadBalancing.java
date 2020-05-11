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
public class LoadBalancing implements IdentifiedDataSerializable {

    @Description("Whether to enable the load balancing mechanism of this node")
    private boolean enabled = true;

    @Description("The identify or address type specified by the advertise strategy is " +
            "used to help clients or load balancing servers to identify the nodes in cluster")
    private AdvertiseStrategy advertiseStrategy = AdvertiseStrategy.BIND_ADDRESS;

    @Description("Whether to add the port of this node to the IP.\n" +
            "e.g. The IP is 100.131.251.96 and the port of this node is 9510" +
            "so that the address exposed will be 100.131.251.96:9510")
    private boolean attachPortToIp = true;

    @JsonView(MutablePropertiesView.class)
    @Description("The identity of this node exposed to the public. " +
            "The property is usually used to help load balancing servers (like Nginx) proxy\n" +
            "(e.g. \"turms-east-0001\")")
    private String identity = "";

    @JsonView(MutablePropertiesView.class)
    @Description("The advertise address of this node exposed to the public. " +
            "The property is usually used to advertise the DDoS Protected IP address " +
            "to hide the origin IP address)\n" +
            "(e.g. 100.131.251.96)")
    private String advertiseAddress = "";

    @JsonView(MutablePropertiesView.class)
    @Description("The IP detectors will be used to query the public IP if \"identity\" property isn't valid and useLocalIp is false")
    private List<String> ipDetectorAddresses = List.of("https://checkip.amazonaws.com", "https://bot.whatismyipaddress.com", "https://myip.dnsomatic.com");

    @JsonIgnore
    @Override
    public int getFactoryId() {
        return IdentifiedDataFactory.FACTORY_ID;
    }

    @JsonIgnore
    @Override
    public int getClassId() {
        return IdentifiedDataFactory.Type.PROPERTY_LOAD_BALANCING.getValue();
    }

    @Override
    public void writeData(ObjectDataOutput out) throws IOException {
        out.writeUTF(identity);
        out.writeUTF(advertiseAddress);
        out.writeUTFArray(ipDetectorAddresses.toArray(new String[0]));
    }

    @Override
    public void readData(ObjectDataInput in) throws IOException {
        identity = in.readUTF();
        advertiseAddress = in.readUTF();
        ipDetectorAddresses = Arrays.asList(in.readUTFArray());
    }

    public enum AdvertiseStrategy {
        IDENTIFY,
        ADVERTISE_ADDRESS,
        BIND_ADDRESS,
        LOCAL_ADDRESS,
        /**
         * WARNING: For security, do NOT use REAL_PUBLIC_ADDRESS in the production environment
         * to prevent from exposing the origin IP address for DDoS attack.
         */
        PUBLIC_ADDRESS,
    }
}