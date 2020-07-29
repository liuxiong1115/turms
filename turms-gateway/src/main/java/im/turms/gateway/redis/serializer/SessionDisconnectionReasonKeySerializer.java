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

import im.turms.common.constant.DeviceType;
import im.turms.gateway.pojo.bo.session.SessionDisconnectionReasonKey;
import im.turms.server.common.redis.RedisEntryId;
import io.netty.buffer.PooledByteBufAllocator;
import org.springframework.data.redis.serializer.RedisElementReader;
import org.springframework.data.redis.serializer.RedisElementWriter;

import java.nio.ByteBuffer;

/**
 * @author James Chen
 */
public class SessionDisconnectionReasonKeySerializer implements RedisElementWriter<SessionDisconnectionReasonKey>, RedisElementReader<SessionDisconnectionReasonKey> {

    @Override
    public ByteBuffer write(SessionDisconnectionReasonKey element) {
        return PooledByteBufAllocator.DEFAULT.directBuffer(Long.BYTES * 2 + Byte.BYTES * 2)
                .writeByte(RedisEntryId.SESSION_DISCONNECTION_REASON)
                .writeLong(element.getUserId())
                .writeByte(element.getDeviceType().getNumber())
                .writeInt(element.getSessionId())
                .nioBuffer();
    }

    @Override
    public SessionDisconnectionReasonKey read(ByteBuffer buffer) {
        buffer.get();
        return new SessionDisconnectionReasonKey(buffer.getLong(), DeviceType.forNumber(buffer.get()), buffer.getInt());
    }

}