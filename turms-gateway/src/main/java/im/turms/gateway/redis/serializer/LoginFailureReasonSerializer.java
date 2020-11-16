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

import im.turms.common.constant.statuscode.TurmsStatusCode;
import org.springframework.data.redis.serializer.RedisElementReader;
import org.springframework.data.redis.serializer.RedisElementWriter;

import java.nio.ByteBuffer;

/**
 * @author James Chen
 */
public class LoginFailureReasonSerializer implements RedisElementWriter<TurmsStatusCode>, RedisElementReader<TurmsStatusCode> {

    @Override
    public ByteBuffer write(TurmsStatusCode element) {
        return ByteBuffer.allocateDirect(Short.BYTES)
                .putShort((short) element.getBusinessCode())
                .flip();
    }

    @Override
    public TurmsStatusCode read(ByteBuffer buffer) {
        return TurmsStatusCode.from(buffer.getShort());
    }

}
