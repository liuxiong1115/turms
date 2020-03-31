package im.turms.turms.service.storage;

import im.turms.common.TurmsStatusCode;
import im.turms.common.constant.ContentType;
import im.turms.common.exception.TurmsBusinessException;
import im.turms.turms.manager.TurmsPluginManager;
import im.turms.turms.plugin.StorageServiceProvider;
import im.turms.turms.property.TurmsProperties;
import im.turms.turms.service.group.GroupMemberService;
import im.turms.turms.service.message.MessageService;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;
import reactor.core.publisher.Mono;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;

@Service
@Validated
public class StorageService {
    private final StorageServiceProvider provider;
    private final TurmsProperties turmsProperties;
    private final MessageService messageService;
    private final GroupMemberService groupMemberService;

    public StorageService(TurmsPluginManager turmsPluginManager, TurmsProperties turmsProperties, MessageService messageService, GroupMemberService groupMemberService) {
        this.provider = turmsPluginManager.getStorageServiceProvider();
        this.turmsProperties = turmsProperties;
        this.messageService = messageService;
        this.groupMemberService = groupMemberService;
    }

    public Mono<String> queryPresignedGetUrl(@NotNull Long requesterId, @NotNull ContentType contentType, @Nullable String keyStr, @Nullable Long keyNum) {
        if (provider != null) {
            if (provider.isServing()) {
                return hasPermissionToGet(requesterId, contentType, keyStr, keyNum)
                        .flatMap(hasPermission -> {
                            if (hasPermission) {
                                return provider.queryPresignedGetUrl(requesterId, contentType, keyStr, keyNum);
                            } else {
                                return Mono.error(TurmsBusinessException.get(TurmsStatusCode.UNAUTHORIZED));
                            }
                        });
            } else {
                throw TurmsBusinessException.get(TurmsStatusCode.UNAVAILABLE);
            }
        } else {
            throw TurmsBusinessException.get(TurmsStatusCode.NOT_IMPLEMENTED);
        }
    }

    public Mono<String> queryPresignedPutUrl(@NotNull Long requesterId, @NotNull ContentType contentType, @Nullable String keyStr, @Nullable Long keyNum, long contentLength) {
        if (provider != null) {
            if (provider.isServing()) {
                int sizeLimit;
                switch (contentType) {
                    case PROFILE:
                        sizeLimit = turmsProperties.getStorage().getProfileSizeLimit();
                        break;
                    case GROUP_PROFILE:
                        sizeLimit = turmsProperties.getStorage().getGroupProfileSizeLimit();
                        break;
                    case ATTACHMENT:
                        sizeLimit = turmsProperties.getStorage().getAttachmentSizeLimit();
                        break;
                    default:
                        throw new IllegalStateException("Unexpected value: " + contentType);
                }
                if (sizeLimit == 0 || contentLength <= sizeLimit) {
                    return hasPermissionToPut(requesterId, contentType, keyStr, keyNum)
                            .flatMap(hasPermission -> {
                                if (hasPermission) {
                                    return provider.queryPresignedPutUrl(requesterId, contentType, keyStr, keyNum, contentLength);
                                } else {
                                    return Mono.error(TurmsBusinessException.get(TurmsStatusCode.UNAUTHORIZED));
                                }
                            });
                } else {
                    throw TurmsBusinessException.get(TurmsStatusCode.FILE_TOO_LARGE);
                }
            } else {
                throw TurmsBusinessException.get(TurmsStatusCode.UNAVAILABLE);
            }
        } else {
            throw TurmsBusinessException.get(TurmsStatusCode.NOT_IMPLEMENTED);
        }
    }

    public Mono<Void> deleteResource(@NotNull Long requesterId, @NotNull ContentType contentType, @Nullable String keyStr, @Nullable Long keyNum) {
        if (provider != null) {
            if (provider.isServing()) {
                return hasPermissionToDelete(requesterId, contentType, keyStr, keyNum)
                        .flatMap(hasPermission -> {
                            if (hasPermission) {
                                return provider.deleteResource(requesterId, contentType, keyStr, keyNum);
                            } else {
                                return Mono.error(TurmsBusinessException.get(TurmsStatusCode.UNAUTHORIZED));
                            }
                        });
            } else {
                throw TurmsBusinessException.get(TurmsStatusCode.UNAVAILABLE);
            }
        } else {
            throw TurmsBusinessException.get(TurmsStatusCode.NOT_IMPLEMENTED);
        }
    }

    // Permission

    private Mono<Boolean> hasPermissionToGet(@NotNull Long requesterId, @NotNull ContentType contentType, @Nullable String keyStr, @Nullable Long keyNum) {
        switch (contentType) {
            case PROFILE:
            case GROUP_PROFILE:
                return Mono.just(true);
            case ATTACHMENT:
                if (keyNum != null) {
                    return messageService.isMessageSentToUserOrByUser(keyNum, requesterId);
                } else {
                    throw TurmsBusinessException.get(TurmsStatusCode.ILLEGAL_ARGUMENTS, "The message ID must not be null");
                }
            default:
                throw new IllegalStateException("Unexpected value: " + contentType);
        }
    }

    private Mono<Boolean> hasPermissionToPut(@NotNull Long requesterId, @NotNull ContentType contentType, @Nullable String keyStr, @Nullable Long keyNum) {
        switch (contentType) {
            case PROFILE:
                return Mono.just(true);
            case GROUP_PROFILE:
                if (keyNum != null) {
                    return groupMemberService.isOwnerOrManager(requesterId, keyNum);
                } else {
                    throw TurmsBusinessException.get(TurmsStatusCode.ILLEGAL_ARGUMENTS, "The group ID must not be null");
                }
            case ATTACHMENT:
                if (keyNum != null) {
                    return messageService.isMessageSentToUserOrByUser(keyNum, requesterId);
                } else {
                    throw TurmsBusinessException.get(TurmsStatusCode.ILLEGAL_ARGUMENTS, "The message ID must not be null");
                }
            default:
                throw new IllegalStateException("Unexpected value: " + contentType);
        }
    }

    private Mono<Boolean> hasPermissionToDelete(@NotNull Long requesterId, @NotNull ContentType contentType, @Nullable String keyStr, @Nullable Long keyNum) {
        switch (contentType) {
            case PROFILE:
                return Mono.just(true);
            case GROUP_PROFILE:
                if (keyNum != null) {
                    return groupMemberService.isOwnerOrManager(requesterId, keyNum);
                } else {
                    throw TurmsBusinessException.get(TurmsStatusCode.ILLEGAL_ARGUMENTS, "The group ID must not be null");
                }
            case ATTACHMENT:
                return Mono.just(false);
            default:
                throw new IllegalStateException("Unexpected value: " + contentType);
        }
    }
}
