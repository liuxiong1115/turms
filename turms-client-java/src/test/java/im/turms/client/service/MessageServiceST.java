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

package im.turms.client.service;

import im.turms.client.TurmsClient;
import im.turms.common.model.bo.message.Message;
import im.turms.common.model.bo.message.MessageStatus;
import im.turms.common.model.bo.message.MessagesWithTotal;
import org.junit.jupiter.api.*;

import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static helper.Constants.*;
import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class MessageServiceST {
    private static final long SENDER_ID = 1L;
    private static final long RECIPIENT_ID = 2L;
    private static final long GROUP_MEMBER_ID = 3L;
    private static final long TARGET_GROUP_ID = 1L;
    private static TurmsClient senderClient;
    private static TurmsClient recipientClient;
    private static TurmsClient groupMemberClient;
    private static Long privateMessageId;
    private static Long groupMessageId;

    @BeforeAll
    static void setup() throws ExecutionException, InterruptedException, TimeoutException {
        senderClient = new TurmsClient(WS_URL);
        recipientClient = new TurmsClient(WS_URL);
        groupMemberClient = new TurmsClient(WS_URL);
        senderClient.getDriver().connect(SENDER_ID, "123").get(5, TimeUnit.SECONDS);
        recipientClient.getDriver().connect(RECIPIENT_ID, "123").get(5, TimeUnit.SECONDS);
        groupMemberClient.getDriver().connect(GROUP_MEMBER_ID, "123").get(5, TimeUnit.SECONDS);
    }

    @AfterAll
    static void tearDown() {
        if (senderClient.getDriver().connected()) {
            senderClient.getDriver().disconnect();
        }
        if (recipientClient.getDriver().connected()) {
            recipientClient.getDriver().disconnect();
        }
        if (groupMemberClient.getDriver().connected()) {
            groupMemberClient.getDriver().disconnect();
        }
    }

    // Constructor

    @Test
    @Order(ORDER_FIRST)
    void constructor_shouldReturnNotNullMessageServiceInstance() {
        assertNotNull(senderClient.getMessageService());
        assertNotNull(recipientClient.getMessageService());
    }

    // Create

    @Test
    @Order(ORDER_HIGHEST_PRIORITY)
    void sendPrivateMessage_shouldReturnMessageId() throws ExecutionException, InterruptedException, TimeoutException {
        privateMessageId = senderClient.getMessageService().sendMessage(false, RECIPIENT_ID, new Date(), "hello", null, null)
                .get(5, TimeUnit.SECONDS);
        assertNotNull(privateMessageId);
    }

    @Test
    @Order(ORDER_HIGHEST_PRIORITY)
    void sendGroupMessage_shouldReturnMessageId() throws ExecutionException, InterruptedException, TimeoutException {
        groupMessageId = senderClient.getMessageService().sendMessage(true, TARGET_GROUP_ID, new Date(), "hello", null, null)
                .get(5, TimeUnit.SECONDS);
        assertNotNull(groupMessageId);
    }

    @Test
    @Order(ORDER_HIGH_PRIORITY)
    void forwardPrivateMessage_shouldReturnForwardedMessageId() throws ExecutionException, InterruptedException, TimeoutException {
        Long messageId = senderClient.getMessageService().forwardMessage(privateMessageId, false, RECIPIENT_ID)
                .get(5, TimeUnit.SECONDS);
        assertNotNull(messageId);
    }

    @Test
    @Order(ORDER_HIGH_PRIORITY)
    void forwardGroupMessage_shouldReturnForwardedMessageId() throws ExecutionException, InterruptedException, TimeoutException {
        Long messageId = senderClient.getMessageService().forwardMessage(groupMessageId, true, TARGET_GROUP_ID)
                .get(5, TimeUnit.SECONDS);
        assertNotNull(messageId);
    }

    // Update

    @Test
    @Order(ORDER_MIDDLE_PRIORITY)
    void recallMessage_shouldSucceed() throws ExecutionException, InterruptedException, TimeoutException {
        Void result = senderClient.getMessageService().recallMessage(groupMessageId, null)
                .get(5, TimeUnit.SECONDS);
        assertNull(result);
    }

    @Test
    @Order(ORDER_MIDDLE_PRIORITY)
    void updateSentMessage_shouldSucceed() throws ExecutionException, InterruptedException, TimeoutException {
        Void result = senderClient.getMessageService().updateSentMessage(privateMessageId, "I have modified the message", null)
                .get(5, TimeUnit.SECONDS);
        assertNull(result);
    }

    @Test
    @Order(ORDER_MIDDLE_PRIORITY)
    void readMessage_shouldSucceed() throws ExecutionException, InterruptedException, TimeoutException {
        Void result = recipientClient.getMessageService().readMessage(privateMessageId, new Date())
                .get(5, TimeUnit.SECONDS);
        assertNull(result);
    }

    @Test
    @Order(ORDER_MIDDLE_PRIORITY - 1)
    void markMessageUnread_shouldSucceed() throws ExecutionException, InterruptedException, TimeoutException {
        Void result = recipientClient.getMessageService().markMessageUnread(privateMessageId)
                .get(5, TimeUnit.SECONDS);
        assertNull(result);
    }

    @Test
    @Order(ORDER_MIDDLE_PRIORITY)
    void updateTypingStatus_shouldSucceed() throws ExecutionException, InterruptedException, TimeoutException {
        Void result = senderClient.getMessageService().updateTypingStatusRequest(false, privateMessageId)
                .get(5, TimeUnit.SECONDS);
        assertNull(result);
    }

    // Query

    @Test
    @Order(ORDER_LOW_PRIORITY)
    void queryMessages_shouldReturnNotEmptyMessages() throws ExecutionException, InterruptedException, TimeoutException {
        List<Message> messages = recipientClient.getMessageService().queryMessages(null, false, null, SENDER_ID, null, null, null, 10)
                .get(5, TimeUnit.SECONDS);
        assertFalse(messages.isEmpty());
    }

    @Test
    @Order(ORDER_LOW_PRIORITY)
    void queryPendingMessagesWithTotal_shouldReturnNotEmptyPendingMessagesWithTotal() throws ExecutionException, InterruptedException, TimeoutException {
        List<MessagesWithTotal> messagesWithTotals = senderClient.getMessageService().queryPendingMessagesWithTotal(10)
                .get(5, TimeUnit.SECONDS);
        assertFalse(messagesWithTotals.isEmpty());
    }

    @Test
    @Order(ORDER_LOW_PRIORITY)
    void queryMessageStatus_shouldReturnNotEmptyMessageStatus() throws ExecutionException, InterruptedException, TimeoutException {
        List<MessageStatus> messageStatusesOfMember1 = senderClient.getMessageService().queryMessageStatus(groupMessageId)
                .get(5, TimeUnit.SECONDS);
        List<MessageStatus> messageStatusesOfMember2 = groupMemberClient.getMessageService().queryMessageStatus(groupMessageId)
                .get(5, TimeUnit.SECONDS);
        assertEquals(messageStatusesOfMember1.get(0).getMessageId(), messageStatusesOfMember2.get(0).getMessageId());
    }

    // Util

    @Test
    void generateLocationRecord() {
        byte[] data = MessageService.generateLocationRecord(1.0f, 1.0f, "name", "address");
        assertNotNull(data);
    }

    @Test
    void generateAudioRecordByDescription() {
        byte[] data = MessageService.generateAudioRecordByDescription("https://abc.com", null, null, null);
        assertNotNull(data);
    }

    @Test
    void generateAudioRecordByData() {
        byte[] source = new byte[]{0, 1, 2};
        byte[] data = MessageService.generateAudioRecordByData(source);
        assertNotNull(data);
    }

    @Test
    void generateVideoRecordByDescription() {
        byte[] data = MessageService.generateVideoRecordByDescription("https://abc.com", null, null, null);
        assertNotNull(data);
    }

    @Test
    void generateVideoRecordByData() {
        byte[] source = new byte[]{0, 1, 2};
        byte[] data = MessageService.generateVideoRecordByData(source);
        assertNotNull(data);
    }

    @Test
    void generateImageRecordByData() {
        byte[] source = new byte[]{0, 1, 2};
        byte[] data = MessageService.generateImageRecordByData(source);
        assertNotNull(data);
    }

    @Test
    void generateFileRecordByDate() {
        byte[] source = new byte[]{0, 1, 2};
        byte[] data = MessageService.generateFileRecordByDate(source);
        assertNotNull(data);
    }

    @Test
    void generateImageRecordByDescription() {
        byte[] data = MessageService.generateImageRecordByDescription("https://abc.com", null, null, null);
        assertNotNull(data);
    }

    @Test
    void generateFileRecordByDescription() {
        byte[] data = MessageService.generateFileRecordByDescription("https://abc.com", null, null);
        assertNotNull(data);
    }
}
