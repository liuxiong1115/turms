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

package im.turms.turms.access.websocket.controller;

import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.Multimap;
import com.google.protobuf.ByteString;
import com.google.protobuf.Int64Value;
import im.turms.common.TurmsStatusCode;
import im.turms.common.constant.MessageDeliveryStatus;
import im.turms.common.exception.TurmsBusinessException;
import im.turms.common.model.bo.message.MessageStatuses;
import im.turms.common.model.bo.message.Messages;
import im.turms.common.model.bo.message.MessagesWithTotal;
import im.turms.common.model.bo.message.MessagesWithTotalList;
import im.turms.common.model.dto.notification.TurmsNotification;
import im.turms.common.model.dto.request.TurmsRequest;
import im.turms.common.model.dto.request.message.*;
import im.turms.turms.annotation.websocket.TurmsRequestMapping;
import im.turms.turms.manager.TurmsClusterManager;
import im.turms.turms.pojo.bo.DateRange;
import im.turms.turms.pojo.bo.RequestResult;
import im.turms.turms.pojo.bo.TurmsRequestWrapper;
import im.turms.turms.pojo.domain.Message;
import im.turms.turms.service.message.MessageService;
import im.turms.turms.service.message.MessageStatusService;
import im.turms.turms.util.PageUtil;
import im.turms.turms.util.ProtoUtil;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.springframework.stereotype.Controller;
import reactor.core.publisher.Mono;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static im.turms.common.model.dto.request.TurmsRequest.KindCase.*;
import static im.turms.turms.property.business.Message.TimeType;

@Controller
public class WsMessageController {
    private final TurmsClusterManager turmsClusterManager;
    private final MessageService messageService;
    private final MessageStatusService messageStatusService;
    private final PageUtil pageUtil;

    public WsMessageController(MessageService messageService, TurmsClusterManager turmsClusterManager, PageUtil pageUtil, MessageStatusService messageStatusService) {
        this.messageService = messageService;
        this.turmsClusterManager = turmsClusterManager;
        this.pageUtil = pageUtil;
        this.messageStatusService = messageStatusService;
    }

    @TurmsRequestMapping(CREATE_MESSAGE_REQUEST)
    public Function<TurmsRequestWrapper, Mono<RequestResult>> handleCreateMessageRequest() {
        return turmsRequestWrapper -> {
            CreateMessageRequest request = turmsRequestWrapper.getTurmsRequest().getCreateMessageRequest();
            if (request.hasIsSystemMessage() && request.getIsSystemMessage().getValue()) {
                return Mono.error(TurmsBusinessException.get(TurmsStatusCode.ILLEGAL_ARGUMENTS, "Users cannot create system messages"));
            }
            Mono<Pair<Message, Set<Long>>> messageAndRelatedUserIdsMono;
            boolean isGroupMessage = request.hasGroupId();
            if (!isGroupMessage && !request.hasRecipientId()) {
                return Mono.error(TurmsBusinessException.get(TurmsStatusCode.ILLEGAL_ARGUMENTS, "The recipientId must not be null for private messages"));
            }
            long targetId = isGroupMessage ? request.getGroupId().getValue() : request.getRecipientId().getValue();
            if (request.hasMessageId()) {
                messageAndRelatedUserIdsMono = messageService.authAndCloneAndSaveMessage(
                        turmsRequestWrapper.getUserId(),
                        request.getMessageId().getValue(),
                        isGroupMessage,
                        false,
                        targetId);
            } else {
                List<byte[]> records = null;
                if (request.getRecordsCount() != 0) {
                    records = new ArrayList<>(request.getRecordsCount());
                    for (ByteString byteString : request.getRecordsList()) {
                        records.add(byteString.toByteArray());
                    }
                }
                Integer burnAfter = request.hasBurnAfter() ? request.getBurnAfter().getValue() : null;
                Date deliveryDate = new Date(request.getDeliveryDate());
                messageAndRelatedUserIdsMono = messageService.authAndSaveMessage(
                        null,
                        turmsRequestWrapper.getUserId(),
                        targetId,
                        isGroupMessage,
                        false,
                        request.hasText() ? request.getText().getValue() : null,
                        records,
                        burnAfter,
                        deliveryDate,
                        null);
            }
            return messageAndRelatedUserIdsMono.map(pair -> {
                Message message = pair.getLeft();
                Long messageId = message != null ? message.getId() : null;
                Set<Long> recipientsIds = pair.getRight();
                if (messageId != null) {
                    if (recipientsIds != null && !recipientsIds.isEmpty()) {
                        TurmsRequest turmsRequest;
                        if (request.hasMessageId()) {
                            turmsRequest = turmsRequestWrapper.getTurmsRequest()
                                    .toBuilder()
                                    .setCreateMessageRequest(ProtoUtil.fillCloneMessageRequest(request, message))
                                    .build();
                        } else {
                            Int64Value.Builder messageIdBuilder = Int64Value.newBuilder().setValue(messageId);
                            CreateMessageRequest.Builder requestBuilder = request.toBuilder().setMessageId(messageIdBuilder);
                            if (messageService.getTimeType() == TimeType.LOCAL_SERVER_TIME) {
                                requestBuilder.setDeliveryDate(message.getDeliveryDate().getTime());
                            }
                            turmsRequest = turmsRequestWrapper.getTurmsRequest()
                                    .toBuilder()
                                    .setCreateMessageRequest(requestBuilder)
                                    .build();
                        }
                        return RequestResult.create(
                                messageId,
                                recipientsIds,
                                turmsClusterManager.getTurmsProperties().getMessage().isSendMessageToOtherSenderOnlineDevices(),
                                turmsRequest);
                    } else {
                        return RequestResult.create(messageId);
                    }
                } else if (recipientsIds != null && !recipientsIds.isEmpty()) {
                    TurmsRequest turmsRequest = turmsRequestWrapper.getTurmsRequest();
                    if (messageService.getTimeType() == TimeType.LOCAL_SERVER_TIME) {
                        turmsRequest = turmsRequest.toBuilder()
                                .setCreateMessageRequest(request.toBuilder().setDeliveryDate(System.currentTimeMillis()))
                                .build();
                    }
                    return RequestResult.create(
                            recipientsIds,
                            turmsRequest);
                } else {
                    return RequestResult.create(TurmsStatusCode.OK);
                }
            });
        };
    }

    @TurmsRequestMapping(QUERY_MESSAGE_STATUSES_REQUEST)
    public Function<TurmsRequestWrapper, Mono<RequestResult>> handleQueryMessageStatusRequest() {
        return turmsRequestWrapper -> {
            QueryMessageStatusesRequest request = turmsRequestWrapper.getTurmsRequest().getQueryMessageStatusesRequest();
            return messageStatusService.queryMessageStatus(request.getMessageId())
                    .map(messageStatus -> {
                        MessageStatuses.Builder builder = MessageStatuses.newBuilder();
                        builder.addMessageStatuses(ProtoUtil.messageStatus2proto(messageStatus));
                        TurmsNotification.Data data = TurmsNotification.Data
                                .newBuilder()
                                .setMessageStatuses(builder)
                                .build();
                        return RequestResult.create(data);
                    });
        };
    }

    @TurmsRequestMapping(QUERY_PENDING_MESSAGES_WITH_TOTAL_REQUEST)
    public Function<TurmsRequestWrapper, Mono<RequestResult>> handleQueryPendingMessagesWithTotalRequest() {
        return turmsRequestWrapper -> {
            QueryPendingMessagesWithTotalRequest request = turmsRequestWrapper.getTurmsRequest().getQueryPendingMessagesWithTotalRequest();
            // chat type, is system message, group id or sender id -> message
            Multimap<Triple<Boolean, Boolean, Long>, Message> multimap = LinkedListMultimap.create();
            Integer size = request.hasSize() ? request.getSize().getValue() : null;
            if (size == null) {
                size = turmsClusterManager.getTurmsProperties().getMessage().getDefaultAvailableMessagesNumberWithTotal();
            }
            size = pageUtil.getSize(size);
            return messageService.queryMessages(
                    false, null, null, null, null,
                    Set.of(turmsRequestWrapper.getUserId()), null, null,
                    Set.of(MessageDeliveryStatus.READY), 0, size)
                    .doOnNext(message -> multimap.put(Triple.of(message.getIsGroupMessage(),
                            message.getIsSystemMessage(),
                            message.getIsGroupMessage() ? message.getTargetId() : message.getSenderId()), message))
                    .collectList()
                    .flatMap(messages -> {
                        if (messages.isEmpty()) {
                            return Mono.error(TurmsBusinessException.get(TurmsStatusCode.NO_CONTENT));
                        }
                        MessagesWithTotalList.Builder listBuilder = MessagesWithTotalList.newBuilder();
                        List<Mono<Long>> countMonos = new ArrayList<>(multimap.size());
                        for (Triple<Boolean, Boolean, Long> key : multimap.keys()) {
                            countMonos.add(messageStatusService.countPendingMessages(key.getLeft(),
                                    key.getMiddle(),
                                    key.getRight(),
                                    turmsRequestWrapper.getUserId()));
                        }
                        return Mono.zip(countMonos, objects -> objects)
                                .map(numberObjects -> {
                                    Iterator<Triple<Boolean, Boolean, Long>> keyIterator = multimap.keys().iterator();
                                    for (int i = 0; i < multimap.keys().size(); i++) {
                                        Triple<Boolean, Boolean, Long> key = keyIterator.next();
                                        int number = ((Long) numberObjects[i]).intValue();
                                        MessagesWithTotal.Builder messagesWithTotalBuilder = MessagesWithTotal.newBuilder()
                                                .setTotal(number)
                                                .setIsGroupMessage(key.getLeft())
                                                .setFromId(key.getRight());
                                        for (Message message : multimap.get(key)) {
                                            messagesWithTotalBuilder.addMessages(ProtoUtil.message2proto(message));
                                        }
                                        listBuilder.addMessagesWithTotalList(messagesWithTotalBuilder);
                                    }
                                    return RequestResult.create(TurmsNotification.Data.newBuilder()
                                            .setMessagesWithTotalList(listBuilder).build());
                                });
                    });
        };
    }

    @TurmsRequestMapping(QUERY_MESSAGES_REQUEST)
    public Function<TurmsRequestWrapper, Mono<RequestResult>> handleQueryMessagesRequest() {
        return turmsRequestWrapper -> {
            QueryMessagesRequest request = turmsRequestWrapper.getTurmsRequest().getQueryMessagesRequest();
            List<Long> idList = request.getIdsCount() != 0 ? request.getIdsList() : null;
            Boolean areGroupMessages = request.hasAreGroupMessages() ? request.getAreGroupMessages().getValue() : null;
            Boolean areSystemMessages = request.hasAreSystemMessages() ? request.getAreSystemMessages().getValue() : null;
            Long fromId = request.hasFromId() ? request.getFromId().getValue() : null;
            Date deliveryDateAfter = request.hasDeliveryDateAfter() ? new Date(request.getDeliveryDateAfter().getValue()) : null;
            Date deliveryDateBefore = request.hasDeliveryDateBefore() && deliveryDateAfter == null ?
                    new Date(request.getDeliveryDateBefore().getValue()) : null;
            MessageDeliveryStatus deliveryStatus = null;
            if (request.getDeliveryStatus() == MessageDeliveryStatus.READY
                    || request.getDeliveryStatus() == MessageDeliveryStatus.RECEIVED
                    || request.getDeliveryStatus() == MessageDeliveryStatus.RECALLING) {
                deliveryStatus = request.getDeliveryStatus();
            }
            Integer size = request.hasSize() ? pageUtil.getSize(request.getSize().getValue()) : null;
            return messageService.authAndQueryCompleteMessages(
                    true,
                    idList,
                    areGroupMessages,
                    areSystemMessages,
                    fromId,
                    turmsRequestWrapper.getUserId(),
                    DateRange.of(deliveryDateAfter, deliveryDateBefore),
                    null,
                    deliveryStatus,
                    0,
                    size)
                    .collectList()
                    .flatMap(messages -> {
                        if (messages.isEmpty()) {
                            return Mono.empty();
                        }
                        Messages.Builder messagesListBuilder = Messages.newBuilder();
                        for (Message message : messages) {
                            im.turms.common.model.bo.message.Message.Builder builder = ProtoUtil.message2proto(message);
                            messagesListBuilder.addMessages(builder);
                        }
                        Messages messagesList = messagesListBuilder.build();
                        TurmsNotification.Data data = TurmsNotification.Data.newBuilder()
                                .setMessages(messagesList)
                                .build();
                        Set<Long> messagesIds = new HashSet<>(messages.size());
                        for (Message message : messages) {
                            messagesIds.add(message.getId());
                        }
                        return Mono.just(RequestResult.create(data))
                                .flatMap(response -> messageStatusService.acknowledge(messagesIds)
                                        .thenReturn(response));
                    });
        };
    }

    @TurmsRequestMapping(UPDATE_MESSAGE_REQUEST)
    public Function<TurmsRequestWrapper, Mono<RequestResult>> handleUpdateMessageRequest() {
        return turmsRequestWrapper -> {
            UpdateMessageRequest request = turmsRequestWrapper.getTurmsRequest().getUpdateMessageRequest();
            if (request.hasIsSystemMessage() && request.getIsSystemMessage().getValue()) {
                return Mono.error(TurmsBusinessException.get(TurmsStatusCode.ILLEGAL_ARGUMENTS, "Users cannot create system messages"));
            }
            long messageId = request.getMessageId();
            if (request.hasReadDate()) {
                long readDateValue = request.getReadDate().getValue();
                Date readDate = readDateValue > 0 ? new Date(readDateValue) : null;
                return authAndUpdateMessageReadDate(
                        turmsRequestWrapper.getUserId(),
                        messageId,
                        readDate)
                        .flatMap(updatedOrDeleted -> {
                            if (updatedOrDeleted != null && updatedOrDeleted) {
                                if (turmsClusterManager.getTurmsProperties().getMessage().getReadReceipt().isEnabled()) {
                                    return messageService.queryMessageSenderId(messageId)
                                            .flatMap(senderId -> {
                                                RequestResult result = RequestResult.create(
                                                        senderId,
                                                        turmsRequestWrapper.getTurmsRequest(),
                                                        TurmsStatusCode.OK);
                                                return Mono.just(result);
                                            });
                                } else {
                                    // return OK because the update operation was successful
                                    return Mono.just(RequestResult.ok());
                                }
                            } else {
                                return Mono.error(TurmsBusinessException.get(TurmsStatusCode.RESOURCES_HAVE_CHANGED));
                            }
                        });
            }
            String text = request.hasText() ? request.getText().getValue() : null;
            List<byte[]> records = null;
            if (request.getRecordsCount() != 0) {
                records = new ArrayList<>(request.getRecordsCount());
                for (ByteString byteString : request.getRecordsList()) {
                    records.add(byteString.toByteArray());
                }
            }
            Date recallDate = request.hasRecallDate() ? new Date(request.getRecallDate().getValue()) : null;
            return messageService.authAndUpdateMessageAndMessageStatus(
                    turmsRequestWrapper.getUserId(),
                    messageId,
                    turmsRequestWrapper.getUserId(),
                    text,
                    records,
                    recallDate,
                    null)
                    .flatMap(success -> messageService.queryMessageRecipients(messageId) //TODO: optimize the notification logic
                            .collect(Collectors.toSet())
                            .map(recipientsIds -> recipientsIds.isEmpty() ? RequestResult.ok()
                                    : RequestResult.create(
                                    recipientsIds,
                                    turmsRequestWrapper.getTurmsRequest())));
        };
    }

    /**
     * To save a lot of resources, allow sending typing status to recipients without checking their relationships.
     */
    @TurmsRequestMapping(UPDATE_TYPING_STATUS_REQUEST)
    public Function<TurmsRequestWrapper, Mono<RequestResult>> handleUpdateTypingStatusRequest() {
        return turmsRequestWrapper -> {
            if (turmsClusterManager.getTurmsProperties().getMessage().getTypingStatus().isEnabled()) {
                UpdateTypingStatusRequest request = turmsRequestWrapper.getTurmsRequest()
                        .getUpdateTypingStatusRequest();
                return Mono.just(RequestResult.create(
                        request.getToId(),
                        turmsRequestWrapper.getTurmsRequest(),
                        TurmsStatusCode.OK));
            } else {
                return Mono.just(RequestResult.create(TurmsStatusCode.DISABLED_FUNCTION));
            }
        };
    }

    private Mono<Boolean> authAndUpdateMessageReadDate(
            @NotNull Long userId,
            @NotNull Long messageId,
            @Nullable Date readDate) {
        return messageService.isMessageSentToUser(messageId, userId)
                .flatMap(authenticated -> {
                    if (authenticated != null && authenticated) {
                        Date date = null;
                        if (readDate != null) {
                            if (turmsClusterManager.getTurmsProperties().getMessage()
                                    .getReadReceipt().isUseServerTime()) {
                                date = new Date();
                            } else {
                                date = readDate;
                            }
                        } // else unset the read date
                        return messageStatusService.updateMessagesReadDate(
                                messageId,
                                date);
                    } else {
                        return Mono.error(TurmsBusinessException.get(TurmsStatusCode.UNAUTHORIZED));
                    }
                });
    }
}
