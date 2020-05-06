package im.turms.turms.serializer.task;

import com.hazelcast.internal.nio.Bits;
import com.hazelcast.nio.ObjectDataInput;
import com.hazelcast.nio.ObjectDataOutput;
import com.hazelcast.nio.serialization.StreamSerializer;
import im.turms.turms.config.hazelcast.IdentifiedDataFactory;
import im.turms.turms.task.DeliveryTurmsNotificationTask;

import java.io.EOFException;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

public class DeliveryTurmsNotificationTaskSerializer implements StreamSerializer<DeliveryTurmsNotificationTask> {

    private static final int MAX_NOTIFICATION_SIZE = (1 << 14) - 1;

    @Override
    public void write(ObjectDataOutput out, DeliveryTurmsNotificationTask object) throws IOException {
        // Schema (2 bytes)
        // The highest 2 bits for determining the size of the number of recipients.
        // 00 -> if there is only one recipient, use 0 extra byte
        // 01 -> if 1 < recipientNumber < 256, use 1 extra byte
        // 10 -> Otherwise, use 2 extra bytes
        // 11 -> illegal
        // The remaining 14 bits for the size of notification
        short schema = 0;
        short recipientsNumber = (short) object.getRecipientIds().size();
        if (recipientsNumber == 0) {
            throw new IllegalArgumentException("The recipientsIds must be greater than 0");
        } else if (recipientsNumber != 1) {
            schema |= 1 << (recipientsNumber < 256 ? 14 : 15);
        }
        byte[] notificationBytes = object.getNotificationBytes();
        short notificationSize = (short) notificationBytes.length;
        if (notificationSize > MAX_NOTIFICATION_SIZE) {
            String message = String.format("The size of notification must be less than %d", MAX_NOTIFICATION_SIZE);
            throw new IllegalArgumentException(message);
        }
        schema |= notificationSize;
        out.writeShort(schema);

        // Recipients
        if (recipientsNumber != 1) {
            if (recipientsNumber < 256) {
                out.writeByte(recipientsNumber);
            } else {
                out.writeShort(recipientsNumber);
            }
        }
        for (Long id : object.getRecipientIds()) {
            out.writeLong(id);
        }

        // Notifications
        out.write(notificationBytes);

        // RemoteRequesterIdForCaching
        if (object.getRemoteRequesterIdForCaching() != null) {
            out.writeLong(object.getRemoteRequesterIdForCaching());
        }
    }

    @SuppressWarnings("squid:S3012")
    @Override
    public DeliveryTurmsNotificationTask read(ObjectDataInput in) throws IOException {
        // Schema
        short schema = in.readShort();

        // Recipients
        int recipientsNumber;
        if (Bits.isBitSet(schema, 15)) {
            recipientsNumber = in.readByte() & 0xFF;
        } else if (Bits.isBitSet(schema, 16)) {
            recipientsNumber = in.readShort() & 0xFFFF;
        } else {
            recipientsNumber = 1;
        }
        Set<Long> recipientIds = new HashSet<>(recipientsNumber);
        for (int i = 0; i < recipientsNumber; i++) {
            recipientIds.add(in.readLong());
        }

        // Notification
        short notificationBytesLength = (short) (schema & 0b0011_1111_1111_1111);
        byte[] notificationBytes = new byte[notificationBytesLength];
        in.readFully(notificationBytes);

        // remoteRequesterIdForCaching
        Long remoteRequesterIdForCaching;
        try {
            // Use com.hazelcast.internal.serialization.impl.ByteArrayObjectDataInput.checkAvailable
            // to check when it become a public class
            remoteRequesterIdForCaching = in.readLong();
        } catch (EOFException ignored) {
            remoteRequesterIdForCaching = null;
        }
        return new DeliveryTurmsNotificationTask(notificationBytes, recipientIds, remoteRequesterIdForCaching);
    }

    @Override
    public int getTypeId() {
        return IdentifiedDataFactory.Type.TASK_DELIVERY_TURMS_NOTIFICATION.getValue();
    }

}
