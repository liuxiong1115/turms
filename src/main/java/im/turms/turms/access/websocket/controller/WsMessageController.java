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
import im.turms.turms.annotation.websocket.TurmsRequestMapping;
import im.turms.turms.cluster.TurmsClusterManager;
import im.turms.turms.common.PageUtil;
import im.turms.turms.common.ProtoUtil;
import im.turms.turms.common.TurmsStatusCode;
import im.turms.turms.constant.ChatType;
import im.turms.turms.constant.MessageDeliveryStatus;
import im.turms.turms.exception.TurmsBusinessException;
import im.turms.turms.pojo.bo.RequestResult;
import im.turms.turms.pojo.domain.Message;
import im.turms.turms.pojo.domain.MessageStatus;
import im.turms.turms.pojo.dto.MessagesWithTotal;
import im.turms.turms.pojo.dto.TurmsRequestWrapper;
import im.turms.turms.pojo.request.*;
import im.turms.turms.pojo.response.MessageStatuses;
import im.turms.turms.pojo.response.Messages;
import im.turms.turms.pojo.response.MessagesWithTotalList;
import im.turms.turms.pojo.response.TurmsResponse;
import im.turms.turms.service.message.MessageService;
import im.turms.turms.service.message.MessageStatusService;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Controller;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

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

    @TurmsRequestMapping(TurmsRequest.KindCase.CREATE_MESSAGE_REQUEST)
    public Function<TurmsRequestWrapper, Mono<RequestResult>> handleCreateMessageRequest() {
        return turmsRequestWrapper -> {
            CreateMessageRequest request = turmsRequestWrapper.getTurmsRequest().getCreateMessageRequest();
            List<byte[]> records = request.getRecordsCount() != 0 ? request.getRecordsList()
                    .stream()
                    .map(ByteString::toByteArray)
                    .collect(Collectors.toList())
                    : null;
            Integer burnAfter = request.hasBurnAfter() ? request.getBurnAfter().getValue() : null;
            Date deliveryDate = new Date(request.getDeliveryDate());
            return messageService.authAndSendMessage(
                    turmsRequestWrapper.getUserId(),
                    request.getToId(),
                    request.getChatType(),
                    request.getText(),
                    records,
                    burnAfter,
                    deliveryDate)
                    .map(pair -> {
                        Long messageId = pair.getLeft();
                        Set<Long> recipientsIds = pair.getRight();
                        if (messageId != null && recipientsIds != null && !recipientsIds.isEmpty()) {
                            return RequestResult.responseIdAndRecipientData(
                                    messageId,
                                    recipientsIds,
                                    turmsRequestWrapper.getTurmsRequest());
                        } else if (messageId != null) {
                            return RequestResult.responseId(messageId);
                        } else if (recipientsIds != null && !recipientsIds.isEmpty()) {
                            return RequestResult.recipientData(
                                    recipientsIds,
                                    turmsRequestWrapper.getTurmsRequest());
                        } else {
                            return RequestResult.status(TurmsStatusCode.OK);
                        }
                    });
        };
    }

    @TurmsRequestMapping(TurmsRequest.KindCase.QUERY_MESSAGE_STATUSES_REQUEST)
    public Function<TurmsRequestWrapper, Mono<RequestResult>> handleQueryMessageStatusRequest() {
        return turmsRequestWrapper -> {
            QueryMessageStatusesRequest request = turmsRequestWrapper.getTurmsRequest().getQueryMessageStatusesRequest();
            return messageStatusService.queryMessageStatuses(request.getMessageId())
                    .collectList()
                    .map(messageStatuses -> {
                        MessageStatuses.Builder builder = MessageStatuses.newBuilder();
                        for (MessageStatus messageStatus : messageStatuses) {
                            builder.addMessageStatuses(ProtoUtil.messageStatus2proto(messageStatus));
                        }
                        TurmsResponse.Data data = TurmsResponse.Data
                                .newBuilder()
                                .setMessageStatuses(builder)
                                .build();
                        return RequestResult.responseData(data);
                    });
        };
    }

    @TurmsRequestMapping(TurmsRequest.KindCase.QUERY_PENDING_MESSAGES_WITH_TOTAL_REQUEST)
    public Function<TurmsRequestWrapper, Mono<RequestResult>> handleQueryPendingMessagesWithTotalRequest() {
        return turmsRequestWrapper -> {
            QueryPendingMessagesWithTotalRequest request = turmsRequestWrapper.getTurmsRequest().getQueryPendingMessagesWithTotalRequest();
            // chat type, group id or sender id -> message
            Multimap<Pair<ChatType, Long>, Message> multimap = LinkedListMultimap.create();
            Integer size = request.hasSize() ? request.getSize().getValue() : null;
            if (size == null) {
                size = turmsClusterManager.getTurmsProperties().getMessage().getDefaultMessagesNumberWithTotal();
            }
            size = pageUtil.getSize(size);
            return messageService.queryCompleteMessages(
                    false, null, null,
                    turmsRequestWrapper.getUserId(), null, null,
                    MessageDeliveryStatus.READY, size)
                    .doOnNext(message -> multimap.put(Pair.of(message.getChatType(),
                            message.getChatType() == ChatType.GROUP ? message.getTargetId() : message.getSenderId()), message))
                    .collectList()
                    .flatMap(messages -> {
                        if (messages.isEmpty()) {
                            return Mono.error(TurmsBusinessException.get(TurmsStatusCode.NOT_FOUND));
                        }
                        MessagesWithTotalList.Builder listBuilder = MessagesWithTotalList.newBuilder();
                        List<Mono<Long>> countMonos = new ArrayList<>(multimap.size());
                        for (Pair<ChatType, Long> key : multimap.keys()) {
                            countMonos.add(messageStatusService.countPendingMessages(key.getLeft(),
                                    key.getRight(),
                                    turmsRequestWrapper.getUserId()));
                        }
                        return Mono.zip(countMonos, objects -> objects)
                                .map(numberObjects -> {
                                    Iterator<Pair<ChatType, Long>> keyIterator = multimap.keys().iterator();
                                    for (int i = 0; i < multimap.keys().size(); i++) {
                                        Pair<ChatType, Long> key = keyIterator.next();
                                        int number = ((Long) numberObjects[i]).intValue();
                                        MessagesWithTotal.Builder messagesWithTotalBuilder = MessagesWithTotal.newBuilder()
                                                .setTotal(number)
                                                .setChatType(key.getLeft())
                                                .setFromId(key.getRight());
                                        for (Message message : multimap.get(key)) {
                                            messagesWithTotalBuilder.addMessages(ProtoUtil.message2proto(message));
                                        }
                                        listBuilder.addMessagesWithTotalList(messagesWithTotalBuilder);
                                    }
                                    return RequestResult.responseData(TurmsResponse.Data.newBuilder()
                                            .setMessagesWithTotalList(listBuilder).build());
                                });
                    });
        };
    }

    @TurmsRequestMapping(TurmsRequest.KindCase.QUERY_MESSAGES_REQUEST)
    public Function<TurmsRequestWrapper, Mono<RequestResult>> handleQueryMessagesRequest() {
        return turmsRequestWrapper -> {
            QueryMessagesRequest request = turmsRequestWrapper.getTurmsRequest().getQueryMessagesRequest();
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
                    request.getChatType(),
                    fromId,
                    turmsRequestWrapper.getUserId(),
                    deliveryDateAfter,
                    deliveryDateBefore,
                    deliveryStatus,
                    size)
                    .collectList()
                    .flatMap(messages -> {
                        if (messages.isEmpty()) {
                            return Mono.empty();
                        }
                        Messages.Builder messagesListBuilder = Messages.newBuilder();
                        for (Message message : messages) {
                            im.turms.turms.pojo.dto.Message.Builder builder = ProtoUtil.message2proto(message);
                            messagesListBuilder.addMessages(builder);
                        }
                        Messages messagesList = messagesListBuilder.build();
                        TurmsResponse.Data data = TurmsResponse.Data.newBuilder()
                                .setMessages(messagesList)
                                .build();
                        Set<Long> messagesIds = messages.stream()
                                .mapToLong(Message::getId).boxed()
                                .collect(Collectors.toSet());
                        return Mono.just(RequestResult.responseData(data))
                                .flatMap(response -> messageStatusService.acknowledge(messagesIds)
                                        .thenReturn(response));
                    });
        };
    }

    @TurmsRequestMapping(TurmsRequest.KindCase.UPDATE_MESSAGE_REQUEST)
    public Function<TurmsRequestWrapper, Mono<RequestResult>> handleUpdateMessageRequest() {
        return turmsRequestWrapper -> {
            UpdateMessageRequest request = turmsRequestWrapper.getTurmsRequest().getUpdateMessageRequest();
            Date readDate = request.hasReadDate() ? new Date(request.getReadDate().getValue()) : null;
            if (readDate != null) {
                return updateMessageReadDate(turmsRequestWrapper);
            }
            String text = request.hasText() ? request.getText().getValue() : null;
            List<byte[]> records = request.getRecordsCount() != 0 ?
                    request.getRecordsList()
                    .stream()
                    .map(ByteString::toByteArray)
                    .collect(Collectors.toList())
                    : null;
            Date recallDate = request.hasRecallDate() ? new Date(request.getRecallDate().getValue()) : null;
            boolean updateMessageContent = text != null || (records != null && !records.isEmpty());
            if (updateMessageContent || recallDate != null) {
                if (recallDate != null && !turmsClusterManager.getTurmsProperties()
                        .getMessage().isAllowRecallingMessage()) {
                    return Mono.just(RequestResult.status(TurmsStatusCode.DISABLE_FUNCTION));
                }
                if (updateMessageContent && !turmsClusterManager.getTurmsProperties()
                        .getMessage().isAllowEditingMessageBySender()) {
                    return Mono.just(RequestResult.status(TurmsStatusCode.DISABLE_FUNCTION));
                }
                return messageService.isMessageSentByUser(request.getMessageId(), turmsRequestWrapper.getUserId())
                        .flatMap(authenticated -> {
                            if (authenticated == null || !authenticated) {
                                return Mono.just(RequestResult.status(TurmsStatusCode.UNAUTHORIZED));
                            }
                            return messageService.isMessageRecallable(request.getMessageId())
                                    .flatMap(recallable -> {
                                        if (recallable == null || !recallable) {
                                            return Mono.just(RequestResult.status(TurmsStatusCode.EXPIRY_RESOURCE));
                                        }
                                        //TODO: Enable/Disable the same authentication logic with message creating in 0.9.0
                                        return messageService.updateMessageAndMessageStatus(
                                                request.getMessageId(),
                                                turmsRequestWrapper.getUserId(),
                                                text,
                                                records,
                                                recallDate,
                                                null);
                                    })
                                    .flatMap(success -> messageService.queryMessageRecipients(request.getMessageId())
                                            .collect(Collectors.toSet())
                                            .map(recipientsIds -> RequestResult.recipientData(
                                                    recipientsIds,
                                                    turmsRequestWrapper.getTurmsRequest())));
                        });
            } else {
                return Mono.just(RequestResult.status(TurmsStatusCode.ILLEGAL_ARGUMENTS));
            }
        };
    }

    /**
     * To save a lot of resources, allow sending typing status to recipients without checking their relationships.
     */
    @TurmsRequestMapping(TurmsRequest.KindCase.UPDATE_TYPING_STATUS_REQUEST)
    public Function<TurmsRequestWrapper, Mono<RequestResult>> handleUpdateTypingStatusRequest() {
        return turmsRequestWrapper -> {
            if (turmsClusterManager.getTurmsProperties().getMessage().getTypingStatus().isEnabled()) {
                UpdateTypingStatusRequest request = turmsRequestWrapper.getTurmsRequest()
                        .getUpdateTypingStatusRequest();
                return Mono.just(RequestResult.recipientData(
                        request.getToId(),
                        turmsRequestWrapper.getTurmsRequest(),
                        TurmsStatusCode.OK));
            } else {
                return Mono.just(RequestResult.status(TurmsStatusCode.DISABLE_FUNCTION));
            }
        };
    }

    private Mono<RequestResult> updateMessageReadDate(TurmsRequestWrapper turmsRequestWrapper) {
        UpdateMessageRequest request = turmsRequestWrapper.getTurmsRequest().getUpdateMessageRequest();
        return messageService.isMessageSentToUser(request.getMessageId(), turmsRequestWrapper.getUserId())
                .flatMap(authenticated -> {
                    if (authenticated != null && authenticated) {
                        Date date;
                        if (turmsClusterManager.getTurmsProperties().getMessage()
                                .getReadReceipt().isUseServerTime()) {
                            date = new Date();
                        } else {
                            date = new Date(request.getReadDate().getValue());
                        }
                        if (turmsClusterManager.getTurmsProperties().getMessage()
                                .isDeletePrivateMessageAfterAcknowledged()) {
                            return messageService.deleteMessage(request.getMessageId(), true, false);
                        } else {
                            return messageStatusService.updateMessagesReadDate(
                                    request.getMessageId(),
                                    date);
                        }
                    } else {
                        return Mono.error(TurmsBusinessException.get(TurmsStatusCode.UNAUTHORIZED));
                    }
                })
                .flatMap(updatedOrDeleted -> {
                    if (updatedOrDeleted != null && updatedOrDeleted) {
                        if (turmsClusterManager.getTurmsProperties().getMessage().getReadReceipt().isEnabled()) {
                            return messageService.queryMessageSenderId(request.getMessageId())
                                    .flatMap(senderId -> {
                                        RequestResult result = RequestResult.recipientData(
                                                senderId,
                                                turmsRequestWrapper.getTurmsRequest(),
                                                TurmsStatusCode.OK);
                                        return Mono.just(result);
                                    });
                        } else {
                            return Mono.just(RequestResult.ok());
                        }
                    } else {
                        return Mono.error(TurmsBusinessException.get(TurmsStatusCode.RESOURCES_HAVE_CHANGED));
                    }
                });
    }
}
