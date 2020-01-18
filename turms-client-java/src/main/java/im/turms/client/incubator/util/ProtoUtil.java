package im.turms.client.incubator.util;


import com.google.protobuf.*;
import im.turms.client.incubator.common.TurmsLogger;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

public class ProtoUtil {

    private static final HashMap<String, com.google.protobuf.GeneratedMessageV3> RESOLVER_MAP;

    static {
        RESOLVER_MAP = new HashMap<>();
        RESOLVER_MAP.put("google.protobuf.StringValue.value", StringValue.getDefaultInstance());
        RESOLVER_MAP.put("google.protobuf.Int32Value.value", Int32Value.getDefaultInstance());
        RESOLVER_MAP.put("google.protobuf.Int64Value.value", Int64Value.getDefaultInstance());
        RESOLVER_MAP.put("google.protobuf.BoolValue.value", BoolValue.getDefaultInstance());
//        RESOLVER_MAP.put("google.protobuf.FloatValue.value", FloatValue.getDefaultInstance());
    }

    private ProtoUtil() {
    }

    public static com.google.protobuf.Message.Builder fillFields(com.google.protobuf.Message.Builder builder, Map<String, ?> fields) {
        if (fields != null && !fields.isEmpty()) {
            Descriptors.Descriptor descriptor = builder.getDescriptorForType();
            for (Map.Entry<String, ?> entry : fields.entrySet()) {
                Object value = entry.getValue();
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
                            if (messageTypeFields.size() == 1) {
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
                            builder.setField(fieldDescriptor, value);
                        }
                    }
                }
            }
        }
        return builder;
    }
}