package im.turms.turms.service.storage;

import im.turms.common.TurmsStatusCode;
import im.turms.common.constant.ContentType;
import im.turms.common.exception.TurmsBusinessException;
import im.turms.turms.manager.TurmsPluginManager;
import im.turms.turms.plugin.StorageServiceProvider;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;
import reactor.core.publisher.Mono;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;

@Service
@Validated
public class StorageService {
    private final StorageServiceProvider provider;

    public StorageService(TurmsPluginManager turmsPluginManager) {
        this.provider = turmsPluginManager.getStorageServiceProvider();
    }

    public Mono<String> queryPresignedGetUrl(@NotNull Long requesterId, @NotNull ContentType contentType, @Nullable String keyStr, @Nullable Long keyNum) {
        if (provider != null) {
            if (provider.isServing()) {
                return provider.queryPresignedGetUrl(requesterId, contentType, keyStr, keyNum);
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
                return provider.queryPresignedPutUrl(requesterId, contentType, keyStr, keyNum, contentLength);
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
                return provider.deleteResource(requesterId, contentType, keyStr, keyNum);
            } else {
                throw TurmsBusinessException.get(TurmsStatusCode.UNAVAILABLE);
            }
        } else {
            throw TurmsBusinessException.get(TurmsStatusCode.NOT_IMPLEMENTED);
        }
    }
}
