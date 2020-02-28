package im.turms.turms.service.storage;

import im.turms.common.TurmsStatusCode;
import im.turms.common.constant.ContentType;
import im.turms.common.exception.TurmsBusinessException;
import im.turms.turms.plugin.StorageServiceProvider;
import im.turms.turms.plugin.TurmsPluginManager;
import im.turms.turms.property.TurmsProperties;
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

    public StorageService(TurmsPluginManager turmsPluginManager, TurmsProperties turmsProperties, MessageService messageService) {
        this.provider = turmsPluginManager.getStorageServiceProvider();
        this.turmsProperties = turmsProperties;
        this.messageService = messageService;
    }

    public Mono<String> queryPreSignedGetUrl(@NotNull Long requesterId, @NotNull ContentType contentType, @Nullable String keyStr, @Nullable Long keyNum) {
        if (provider != null) {
            switch (contentType) {
                case ATTACHMENT:
                    if (keyNum != null) {
                        return messageService.isMessageSentToUser(keyNum, requesterId)
                                .flatMap(isSentToUser -> {
                                    if (isSentToUser) {
                                        return provider.queryPresignedGetUrl(requesterId, contentType, keyStr, keyNum);
                                    } else {
                                        return messageService.isMessageSentByUser(keyNum, requesterId)
                                                .flatMap(isSentByUser -> {
                                                    if (isSentByUser) {
                                                        return provider.queryPresignedGetUrl(requesterId, contentType, keyStr, keyNum);
                                                    } else {
                                                        return Mono.error(TurmsBusinessException.get(TurmsStatusCode.UNAUTHORIZED));
                                                    }
                                                });
                                    }
                                });
                    } else {
                        throw TurmsBusinessException.get(TurmsStatusCode.ILLEGAL_ARGUMENTS);
                    }
            }
            return provider.queryPresignedGetUrl(requesterId, contentType, keyStr, keyNum);
        } else {
            throw TurmsBusinessException.get(TurmsStatusCode.NOT_IMPLEMENTED);
        }
    }

    public Mono<String> queryPresignedPutUrl(@NotNull Long requesterId, @NotNull ContentType contentType, @Nullable String keyStr, @Nullable Long keyNum, long contentLength) {
        if (provider != null) {
            int sizeLimit;
            switch (contentType) {
                case PROFILE:
                    sizeLimit = turmsProperties.getStorage().getProfileSizeLimit();
                    break;
                case ATTACHMENT:
                    sizeLimit = turmsProperties.getStorage().getAttachmentSizeLimit();
                    break;
                default:
                    throw new IllegalStateException("Unexpected value: " + contentType);
            }
            if (sizeLimit == 0 || contentLength <= sizeLimit) {
                return provider.queryPresignedPutUrl(requesterId, contentType, keyStr, keyNum, contentLength);
            } else {
                throw TurmsBusinessException.get(TurmsStatusCode.FILE_TOO_LARGE);
            }
        } else {
            throw TurmsBusinessException.get(TurmsStatusCode.NOT_IMPLEMENTED);
        }
    }

    public Mono<Boolean> deleteResource(@NotNull Long requesterId, @NotNull ContentType contentType, @Nullable String keyStr, @Nullable Long keyNum) {
        if (provider != null) {
            return provider.deleteResource(requesterId, contentType, keyStr, keyNum);
        } else {
            throw TurmsBusinessException.get(TurmsStatusCode.NOT_IMPLEMENTED);
        }
    }
}
