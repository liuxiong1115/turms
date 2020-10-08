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

package im.turms.client.util;


import com.google.protobuf.*;
import org.jetbrains.annotations.Nullable;

import java.nio.ByteBuffer;
import java.util.*;

/**
 * @author James Chen
 */
public class ProtoUtil {

    private static final HashMap<String, GeneratedMessageV3> VALUE_WRAPPER_MESSAGE_MAP;

    static {
        VALUE_WRAPPER_MESSAGE_MAP = new HashMap<>(MapUtil.getCapability(4));
        VALUE_WRAPPER_MESSAGE_MAP.put("google.protobuf.StringValue.value", StringValue.getDefaultInstance());
        VALUE_WRAPPER_MESSAGE_MAP.put("google.protobuf.Int32Value.value", Int32Value.getDefaultInstance());
        VALUE_WRAPPER_MESSAGE_MAP.put("google.protobuf.Int64Value.value", Int64Value.getDefaultInstance());
        VALUE_WRAPPER_MESSAGE_MAP.put("google.protobuf.BoolValue.value", BoolValue.getDefaultInstance());
    }

    private ProtoUtil() {
    }

    /**
     * Note that the current implementation doesn't support repeated messages
     * because turms doesn't have this kind of requests
     */
    public static Message.Builder fillFields(Message.Builder builder, @Nullable Map<String, ?> fields) {
        if (fields == null || fields.isEmpty()) {
            return builder;
        }
        Descriptors.Descriptor descriptor = builder.getDescriptorForType();
        for (Map.Entry<String, ?> fieldEntry : fields.entrySet()) {
            Object fieldValue = translateFieldValue(fieldEntry.getValue());
            if (fieldValue == null) {
                continue;
            }
            String fieldName = fieldEntry.getKey();
            Descriptors.FieldDescriptor fieldDescriptor = descriptor.findFieldByName(fieldName);
            if (fieldDescriptor == null) {
                throw new NoSuchElementException(fieldName);
            }
            if (fieldDescriptor.isRepeated()) {
                if (fieldDescriptor.isMapField()) {
                    updateBuilderMapField(builder, fieldDescriptor, fieldValue);
                } else if (fieldValue instanceof Collection) {
                    updateBuilderCollectionField(builder, fieldDescriptor, fieldValue);
                } else {
                    throw new IllegalArgumentException("Expected a repeated field but a non-repeated field found: " + fieldDescriptor.getFullName());
                }
            } else {
                if (fieldValue instanceof Collection) {
                    throw new IllegalArgumentException("Expected a non-collection value but a collection value found: " + fieldValue + ", for the field: " + fieldDescriptor.getFullName());
                }
                Descriptors.FieldDescriptor.JavaType type = fieldDescriptor.getJavaType();
                if (type == Descriptors.FieldDescriptor.JavaType.MESSAGE) {
                    updateMessageFieldValue(builder, fieldDescriptor, fieldValue);
                } else {
                    if (type == Descriptors.FieldDescriptor.JavaType.BYTE_STRING) {
                        if (fieldValue instanceof byte[]) {
                            fieldValue = ByteString.copyFrom((byte[]) fieldValue);
                        } else {
                            throw new IllegalArgumentException("Expected a byte[] value but a value of other type found: " + fieldValue + ", for the field: " + fieldDescriptor.getFullName());
                        }
                    }
                    builder.setField(fieldDescriptor, fieldValue);
                }
            }
        }
        return builder;
    }

    private static Object translateFieldValue(Object value) {
        if (value instanceof Date) {
            return ((Date) value).getTime();
        } else if (value instanceof ProtocolMessageEnum) {
            return ((ProtocolMessageEnum) value).getValueDescriptor();
        }
        return value;
    }

    private static void updateBuilderMapField(Message.Builder builder, Descriptors.FieldDescriptor fieldDescriptor, Object value) {
        if (value instanceof Map) {
            MapEntry.Builder entryBuilder = (MapEntry.Builder) builder.newBuilderForField(fieldDescriptor);
            for (Map.Entry<?, ?> entry : ((Map<?, ?>) value).entrySet()) {
                entryBuilder.setKey(entry.getKey());
                entryBuilder.setValue(entry.getValue());
                builder.addRepeatedField(fieldDescriptor, entryBuilder.build());
            }
        } else {
            throw new IllegalArgumentException("The non-map value cannot be the value of the map field: " + fieldDescriptor.getFullName());
        }
    }

    private static void updateBuilderCollectionField(Message.Builder builder, Descriptors.FieldDescriptor fieldDescriptor, Object value) {
        if (value instanceof Collection) {
            Collection<?> values = (Collection<?>) value;
            Descriptors.FieldDescriptor.JavaType type = fieldDescriptor.getJavaType();
            switch (type) {
                case MESSAGE:
                    throw new UnsupportedOperationException("The field of message cannot be repeated");
                case BYTE_STRING:
                    for (Object item : values) {
                        ByteString byteString;
                        if (item instanceof ByteString) {
                            byteString = (ByteString) item;
                        } else if (item instanceof ByteBuffer) {
                            byteString = ByteString.copyFrom((ByteBuffer) item);
                        } else if (item instanceof byte[]) {
                            byteString = ByteString.copyFrom((byte[]) item);
                        } else {
                            throw new IllegalArgumentException("The item type of the field " + fieldDescriptor + " must be ByteString, ByteBuffer, or byte[]");
                        }
                        builder.addRepeatedField(fieldDescriptor, byteString);
                    }
                    break;
                case ENUM:
                    List<Descriptors.EnumValueDescriptor> descriptors = new ArrayList<>(values.size());
                    for (Object item : values) {
                        descriptors.add(((ProtocolMessageEnum) item).getValueDescriptor());
                    }
                    builder.setField(fieldDescriptor, descriptors);
                    break;
                default:
                    if (!(value instanceof List)) {
                        // Must convert to list because of 
                        // com.google.protobuf.GeneratedMessageV3.FieldAccessorTable.RepeatedFieldAccessor.set
                        value = new ArrayList<>(values);
                    }
                    builder.setField(fieldDescriptor, value);
                    break;
            }
        } else {
            throw new IllegalArgumentException("The non-collection value cannot be the value of the repeated field: " + fieldDescriptor.getFullName());
        }
    }

    private static void updateMessageFieldValue(Message.Builder builder, Descriptors.FieldDescriptor messageFieldDescriptor, Object value) {
        Descriptors.Descriptor messageType = messageFieldDescriptor.getMessageType();
        List<Descriptors.FieldDescriptor> messageTypeFields = messageType.getFields();
        // Check if it's a value wrapper message
        if (messageTypeFields.size() == 1) {
            Descriptors.FieldDescriptor subfieldDescriptor = messageTypeFields.get(0);
            String fullName = subfieldDescriptor.getFullName();
            GeneratedMessageV3 wrapperMessage = VALUE_WRAPPER_MESSAGE_MAP.get(fullName);
            if (wrapperMessage != null) {
                Message subMessage = wrapperMessage.toBuilder().setField(subfieldDescriptor, value).build();
                builder.setField(messageFieldDescriptor, subMessage);
            } else {
                String reason = "Unknown message type: " + fullName;
                throw new UnsupportedOperationException(reason);
            }
        } else {
            String reason = String.format("The message %s can only have one field at most", messageFieldDescriptor.getFullName());
            throw new IllegalArgumentException(reason);
        }
    }

}