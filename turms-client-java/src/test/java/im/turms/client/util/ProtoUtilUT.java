package im.turms.client.util;

import com.google.protobuf.Descriptors;
import im.turms.common.constant.DeviceType;
import im.turms.common.constant.UserStatus;
import im.turms.common.model.dto.request.group.enrollment.CheckGroupJoinQuestionsAnswersRequest;
import im.turms.common.model.dto.request.message.CreateMessageRequest;
import im.turms.common.model.dto.request.user.UpdateUserOnlineStatusRequest;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ProtoUtilUT {

    @Test
    void fillFields_shouldFillFields_forCheckGroupJoinQuestionsAnswersRequest() {
        CheckGroupJoinQuestionsAnswersRequest.Builder builder = CheckGroupJoinQuestionsAnswersRequest.newBuilder();
        Map<Long, String> expectedMap = new HashMap<>();
        expectedMap.put(123L, "TestAnswerA");
        expectedMap.put(321L, "TestAnswerB");
        builder = (CheckGroupJoinQuestionsAnswersRequest.Builder) ProtoUtil.fillFields(builder, MapUtil.of(
                "question_id_and_answer", expectedMap));

        assertEquals(expectedMap, builder.getQuestionIdAndAnswerMap());
    }

    @Test
    void fillFields_shouldFillFields_forCreateMessageRequest() {
        long targetId = 321L;
        Date deliveryDate = new Date();
        String text = "TestText";
        List<byte[]> records = new ArrayList<>(1);
        records.add(new byte[]{1, 2, 3});
        int burnAfter = 999;
        CreateMessageRequest.Builder builder = CreateMessageRequest.newBuilder();
        builder = (CreateMessageRequest.Builder) ProtoUtil.fillFields(builder, MapUtil.of(
                "group_id", targetId,
                "delivery_date", deliveryDate,
                "text", text,
                "records", records,
                "burn_after", burnAfter));

        assertEquals(targetId, builder.getGroupId().getValue());
        assertEquals(deliveryDate.getTime(), builder.getDeliveryDate());
        assertEquals(text, builder.getText().getValue());
        assertArrayEquals(records.get(0), builder.getRecords(0).toByteArray());
        assertEquals(burnAfter, builder.getBurnAfter().getValue());
    }

    @Test
    void fillFields_shouldFillFields_forUpdateUserOnlineStatusRequest() {
        UserStatus userStatus = UserStatus.OFFLINE;
        Set<DeviceType> deviceTypes = new HashSet<>();
        deviceTypes.add(DeviceType.ANDROID);
        deviceTypes.add(DeviceType.DESKTOP);
        UpdateUserOnlineStatusRequest.Builder builder = UpdateUserOnlineStatusRequest.newBuilder();
        builder = (UpdateUserOnlineStatusRequest.Builder) ProtoUtil.fillFields(builder, MapUtil.of(
                "user_status", userStatus,
                "device_types", deviceTypes));

        assertEquals(userStatus, builder.getUserStatus());
        assertEquals(new ArrayList<>(deviceTypes), builder.getDeviceTypesList());
    }

}
