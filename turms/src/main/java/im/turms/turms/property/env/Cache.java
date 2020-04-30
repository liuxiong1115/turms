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
import com.hazelcast.nio.ObjectDataInput;
import com.hazelcast.nio.ObjectDataOutput;
import com.hazelcast.nio.serialization.IdentifiedDataSerializable;
import im.turms.turms.config.hazelcast.IdentifiedDataFactory;
import jdk.jfr.Description;
import lombok.Data;

import javax.validation.constraints.Min;
import java.io.IOException;

@Data
public class Cache implements IdentifiedDataSerializable {

    @Description("The maximum size of the cache of disconnection reasons")
    @Min(1024)
    private int disconnectionReasonCacheMaxSize = 1024;

    @Description("The maximum size of the cache of login-failed reasons")
    @Min(1024)
    private int loginFailedReasonCacheMaxSize = 1024;

    @Description("The life duration of each disconnection reason")
    @Min(1)
    private int disconnectionReasonExpireAfter = 60;

    @Description("The life duration of each login-failed reason")
    @Min(1)
    private int loginFailedReasonExpireAfter = 60;

    @Description("The maximum size of the cache of sent messages.")
    @Min(0)
    private int sentMessageCacheMaxSize = 10240;

    @Description("The life duration of each sent message in cache." +
            "For a better performance, it's a good practice to keep the value greater than the allowed recall duration")
    @Min(1)
    private int sentMessageExpireAfter = 30;

    @Description("The maximum size of the cache of remote users' online status.")
    @Min(0)
    private int remoteUserOnlineStatusCacheMaxSize = 10240;

    @Description("The life duration of each remote user's online status in cache." +
            "Note that the cache will make the presentation of users' online status inconsistent" +
            "and only online status will be cached.")
    @Min(1)
    private int remoteUserOnlineStatusExpireAfter = 30;

    @Description("The maximum size of the cache of remote users' online information.")
    @Min(0)
    private int remoteUserOnlineInfoCacheMaxSize = 10240;

    @Description("The life duration of each remote user's online information in cache." +
            "Note that the cache will make the presentation of users' online information inconsistent" +
            "and only online inforamtion will be cached.")
    @Min(1)
    private int remoteUserOnlineInfoExpireAfter = 30;

    @JsonIgnore
    @Override
    public int getFactoryId() {
        return IdentifiedDataFactory.FACTORY_ID;
    }

    @JsonIgnore
    @Override
    public int getClassId() {
        return IdentifiedDataFactory.Type.PROPERTY_CACHE.getValue();
    }

    @Override
    public void writeData(ObjectDataOutput out) throws IOException {
    }

    @Override
    public void readData(ObjectDataInput in) throws IOException {
    }
}