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

package im.turms.gateway.redis.serializer;

import org.springframework.data.redis.serializer.RedisElementReader;
import org.springframework.data.redis.serializer.RedisElementWriter;

import java.nio.ByteBuffer;

/**
 * @author James Chen
 */
public class SessionDisconnectionReasonSerializer implements RedisElementWriter<Integer>, RedisElementReader<Integer> {

    @Override
    public ByteBuffer write(Integer element) {
        return ByteBuffer.allocateDirect(Short.BYTES)
                .putShort(element.shortValue())
                .flip();
    }

    @Override
    public Integer read(ByteBuffer buffer) {
        return (int) buffer.getShort();
    }

}
