package im.turms.client.util;


import com.google.protobuf.*;
import im.turms.client.common.TurmsLogger;
import im.turms.common.model.dto.request.group.enrollment.CheckGroupJoinQuestionsAnswersRequest;

import java.util.*;
import java.util.logging.Level;

public class ProtoUtil {

    private static final HashMap<String, com.google.protobuf.GeneratedMessageV3> RESOLVER_MAP;

    static {
        RESOLVER_MAP = new HashMap<>();
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
                                    && messageTypeFields.get(0).getJsonName().equals("key")
                                    && messageTypeFields.get(1).getJsonName().equals("value");
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
        if (messageType.getFullName().equals("im.turms.proto.CheckGroupJoinQuestionsAnswersRequest.QuestionIdAndAnswerEntry")) {
            CheckGroupJoinQuestionsAnswersRequest.Builder requestBuilder = (CheckGroupJoinQuestionsAnswersRequest.Builder) builder;
            requestBuilder.putAllQuestionIdAndAnswer((Map<Long, String>) value);
            return requestBuilder;
        }
        return builder;
    }
}