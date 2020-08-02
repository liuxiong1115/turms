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
import im.turms.client.common.TurmsLogger;
import im.turms.common.model.dto.request.group.enrollment.CheckGroupJoinQuestionsAnswersRequest;

import java.util.*;
import java.util.logging.Level;

/**
 * @author James Chen
 */
public class ProtoUtil {

    private static final String QUESTION_ID_AND_ANSWER_ENTRY_PROTO_PACKAGE_NAME = "im.turms.proto.CheckGroupJoinQuestionsAnswersRequest.QuestionIdAndAnswerEntry";
    private static final HashMap<String, com.google.protobuf.GeneratedMessageV3> RESOLVER_MAP;

    static {
        RESOLVER_MAP = new HashMap<>(MapUtil.getCapability(4));
        RESOLVER_MAP.put("google.protobuf.StringValue.value", StringValue.getDefaultInstance());
        RESOLVER_MAP.put("google.protobuf.Int32Value.value", Int32Value.getDefaultInstance());
        RESOLVER_MAP.put("google.protobuf.Int64Value.value", Int64Value.getDefaultInstance());
        RESOLVER_MAP.put("google.protobuf.BoolValue.value", BoolValue.getDefaultInstance());
    }

    private ProtoUtil() {
    }

    public static com.google.protobuf.Message.Builder fillFields(com.google.protobuf.Message.Builder builder, Map<String, ?> fields) {
        if (fields != null && !fields.isEmpty()) {
            Descriptors.Descriptor descriptor = builder.getDescriptorForType();
            for (Map.Entry<String, ?> entry : fields.entrySet()) {
                Object value = entry.getValue();
                if (value instanceof Date) {
                    value = ((Date) value).getTime();
                } else if (value instanceof ProtocolMessageEnum) {
                    value = ((ProtocolMessageEnum) value).getValueDescriptor();
                }
                if (value != null) {
                    String key = entry.getKey();
                    Descriptors.FieldDescriptor fieldDescriptor = descriptor.findFieldByName(key);
                    if (fieldDescriptor == null) {
                        TurmsLogger.logger.log(Level.WARNING, "", new NoSuchFieldException(key));
                    } else {
                        Descriptors.FieldDescriptor.JavaType javaType = fieldDescriptor.getJavaType();
                        if (javaType == Descriptors.FieldDescriptor.JavaType.MESSAGE) {
                            Descriptors.Descriptor messageType = fieldDescriptor.getMessageType();
                            List<Descriptors.FieldDescriptor> messageTypeFields = messageType.getFields();
                            boolean isMap = messageTypeFields.size() == 2
                                    && "key".equals(messageTypeFields.get(0).getJsonName())
                                    && "value".equals(messageTypeFields.get(1).getJsonName());
                            if (isMap) {
                                builder = getCustomMapBuilder(builder, messageType, value);
                            } else if (messageTypeFields.size() == 1) {
                                Descriptors.FieldDescriptor subfieldDescriptor = messageTypeFields.get(0);
                                String fullName = subfieldDescriptor.getFullName();
                                GeneratedMessageV3 defaultMessage = RESOLVER_MAP.get(fullName);
                                if (defaultMessage != null) {
                                    Message subMessage = defaultMessage.toBuilder().setField(subfieldDescriptor, value).build();
                                    builder.setField(fieldDescriptor, subMessage);
                                } else {
                                    TurmsLogger.logger.log(Level.WARNING, "", new ClassNotFoundException(fullName));
                                }
                            } else {
                                String reason = String.format("The message %s can only have one field at most", key);
                                TurmsLogger.logger.log(Level.WARNING, "", new IllegalArgumentException(reason));
                            }
                        } else {
                            if (value instanceof Set) {
                                value = new ArrayList<>(((Set<?>) value));
                            }
                            builder.setField(fieldDescriptor, value);
                        }
                    }
                }
            }
        }
        return builder;
    }

    private static Message.Builder getCustomMapBuilder(Message.Builder builder, Descriptors.Descriptor messageType, Object value) {
        if (QUESTION_ID_AND_ANSWER_ENTRY_PROTO_PACKAGE_NAME.equals(messageType.getFullName())) {
            CheckGroupJoinQuestionsAnswersRequest.Builder requestBuilder = (CheckGroupJoinQuestionsAnswersRequest.Builder) builder;
            requestBuilder.putAllQuestionIdAndAnswer((Map<Long, String>) value);
            return requestBuilder;
        }
        return builder;
    }

}