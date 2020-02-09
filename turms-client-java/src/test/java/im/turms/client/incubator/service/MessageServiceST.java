package im.turms.client.incubator.service;

import im.turms.client.incubator.TurmsClient;
import im.turms.turms.constant.ChatType;
import im.turms.turms.constant.DeviceType;
import im.turms.turms.constant.UserStatus;
import im.turms.turms.pojo.bo.message.Message;
import im.turms.turms.pojo.bo.message.MessageStatus;
import im.turms.turms.pojo.bo.message.MessagesWithTotal;
import org.junit.jupiter.api.*;

import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static helper.Constants.*;
import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class MessageServiceST {
    private static TurmsClient senderClient;
    private static TurmsClient recipientClient;
    private static final long SENDER_ID = 1L;
    private static final long RECIPIENT_ID = 2L;
    private static final long TARGET_GROUP_ID = 1L;
    private static Long privateMessageId;
    private static Long groupMessageId;

    @BeforeAll
    static void setup() throws ExecutionException, InterruptedException, TimeoutException {
        senderClient = new TurmsClient(WS_URL, null, null);
        recipientClient = new TurmsClient(WS_URL, null, null);
        senderClient.getDriver()
                .connect(SENDER_ID, "123", 10, null, UserStatus.BUSY, DeviceType.ANDROID)
                .get(5, TimeUnit.SECONDS);
        recipientClient.getDriver()
                .connect(RECIPIENT_ID, "123", 10, null, UserStatus.BUSY, DeviceType.ANDROID)
                .get(5, TimeUnit.SECONDS);
    }

    @AfterAll
    static void tearDown() {
        if (senderClient.getDriver().connected()) {
            senderClient.getDriver().disconnect();
        }
        if (recipientClient.getDriver().connected()) {
            recipientClient.getDriver().disconnect();
        }
    }

    // Constructor

    @Test
    @Order(ORDER_FIRST)
    public void constructor_shouldReturnNotNullMessageServiceInstance() {
        assertNotNull(senderClient.getMessageService());
        assertNotNull(recipientClient.getMessageService());
    }

    // Create

    @Test
    @Order(ORDER_HIGHEST_PRIORITY)
    public void sendPrivateMessage_shouldReturnMessageId() throws ExecutionException, InterruptedException, TimeoutException {
        privateMessageId = senderClient.getMessageService().sendMessage(ChatType.PRIVATE, RECIPIENT_ID, new Date(), "hello", null, null)
                .get(5, TimeUnit.SECONDS);
        assertNotNull(privateMessageId);
    }

    @Test
    @Order(ORDER_HIGHEST_PRIORITY)
    public void sendGroupMessage_shouldReturnMessageId() throws ExecutionException, InterruptedException, TimeoutException {
        groupMessageId = senderClient.getMessageService().sendMessage(ChatType.GROUP, TARGET_GROUP_ID, new Date(), "hello", null, null)
                .get(5, TimeUnit.SECONDS);
        assertNotNull(groupMessageId);
    }

    @Test
    @Order(ORDER_HIGH_PRIORITY)
    public void forwardPrivateMessage_shouldReturnForwardedMessageId() throws ExecutionException, InterruptedException, TimeoutException {
        Long messageId = senderClient.getMessageService().forwardMessage(privateMessageId, ChatType.PRIVATE, RECIPIENT_ID)
                .get(5, TimeUnit.SECONDS);
        assertNotNull(messageId);
    }

    @Test
    @Order(ORDER_HIGH_PRIORITY)
    public void forwardGroupMessage_shouldReturnForwardedMessageId() throws ExecutionException, InterruptedException, TimeoutException {
        Long messageId = senderClient.getMessageService().forwardMessage(groupMessageId, ChatType.GROUP, TARGET_GROUP_ID)
                .get(5, TimeUnit.SECONDS);
        assertNotNull(messageId);
    }

    // Update

    @Test
    @Order(ORDER_MIDDLE_PRIORITY)
    public void recallMessage_shouldSucceed() throws ExecutionException, InterruptedException, TimeoutException {
        Void result = senderClient.getMessageService().recallMessage(groupMessageId, null)
                .get(5, TimeUnit.SECONDS);
        assertNull(result);
    }

    @Test
    @Order(ORDER_MIDDLE_PRIORITY)
    public void updateSentMessage_shouldSucceed() throws ExecutionException, InterruptedException, TimeoutException {
        Void result = senderClient.getMessageService().updateSentMessage(privateMessageId, "I have modified the message", null)
                .get(5, TimeUnit.SECONDS);
        assertNull(result);
    }

    @Test
    @Order(ORDER_MIDDLE_PRIORITY)
    public void readMessage_shouldSucceed() throws ExecutionException, InterruptedException, TimeoutException {
        Void result = recipientClient.getMessageService().readMessage(privateMessageId, new Date())
                .get(5, TimeUnit.SECONDS);
        assertNull(result);
    }

    @Test
    @Order(ORDER_MIDDLE_PRIORITY)
    public void markMessageUnread_shouldSucceed() throws ExecutionException, InterruptedException, TimeoutException {
        Void result = recipientClient.getMessageService().markMessageUnread(privateMessageId)
                .get(5, TimeUnit.SECONDS);
        assertNull(result);
    }

    @Test
    @Order(ORDER_MIDDLE_PRIORITY)
    public void updateTypingStatus_shouldSucceed() throws ExecutionException, InterruptedException, TimeoutException {
        Void result = senderClient.getMessageService().updateTypingStatusRequest(ChatType.PRIVATE, privateMessageId)
                .get(5, TimeUnit.SECONDS);
        assertNull(result);
    }

    // Query

    @Test
    @Order(ORDER_LOW_PRIORITY)
    public void queryMessages_shouldGroupWithVersion() throws ExecutionException, InterruptedException, TimeoutException {
        List<Message> messages = recipientClient.getMessageService().queryMessages(null, ChatType.PRIVATE, null, SENDER_ID, null, null, null, 10)
                .get(5, TimeUnit.SECONDS);
        assertFalse(messages.isEmpty());
    }

    @Test
    @Order(ORDER_LOW_PRIORITY)
    public void queryPendingMessagesWithTotal_shouldGroupWithVersion() throws ExecutionException, InterruptedException, TimeoutException {
        List<MessagesWithTotal> messagesWithTotals = senderClient.getMessageService().queryPendingMessagesWithTotal(10)
                .get(5, TimeUnit.SECONDS);
        assertFalse(messagesWithTotals.isEmpty());
    }

    @Test
    @Order(ORDER_LOW_PRIORITY)
    public void queryMessageStatus_shouldGroupWithVersion() throws ExecutionException, InterruptedException, TimeoutException {
        List<MessageStatus> messageStatuses = senderClient.getMessageService().queryMessageStatus(groupMessageId)
                .get(5, TimeUnit.SECONDS);
        assertFalse(messageStatuses.isEmpty());
    }

    // Util

    @Test
    public void generateLocationRecord() {
        byte[] data = MessageService.generateLocationRecord(1.0f, 1.0f, "name", "address");
        assertNotNull(data);
    }

    @Test
    public void generateAudioRecordByDescription() {
        byte[] data = MessageService.generateAudioRecordByDescription("https://abc.com", null, null, null);
        assertNotNull(data);
    }

    @Test
    public void generateAudioRecordByData() {
        byte[] source = new byte[]{0, 1, 2};
        byte[] data = MessageService.generateAudioRecordByData(source);
        assertNotNull(data);
    }

    @Test
    public void generateVideoRecordByDescription() {
        byte[] data = MessageService.generateVideoRecordByDescription("https://abc.com", null, null, null);
        assertNotNull(data);
    }

    @Test
    public void generateVideoRecordByData() {
        byte[] source = new byte[]{0, 1, 2};
        byte[] data = MessageService.generateVideoRecordByData(source);
        assertNotNull(data);
    }

    @Test
    public void generateImageRecordByData() {
        byte[] source = new byte[]{0, 1, 2};
        byte[] data = MessageService.generateImageRecordByData(source);
        assertNotNull(data);
    }

    @Test
    public void generateFileRecordByDate() {
        byte[] source = new byte[]{0, 1, 2};
        byte[] data = MessageService.generateFileRecordByDate(source);
        assertNotNull(data);
    }

    @Test
    public void generateImageRecordByDescription() {
        byte[] data = MessageService.generateImageRecordByDescription("https://abc.com", null, null, null);
        assertNotNull(data);
    }

    @Test
    public void generateFileRecordByDescription() {
        byte[] data = MessageService.generateFileRecordByDescription("https://abc.com", null, null);
        assertNotNull(data);
    }
}
