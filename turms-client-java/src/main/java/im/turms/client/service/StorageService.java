package im.turms.client.service;

import com.google.protobuf.Int64Value;
import com.google.protobuf.StringValue;
import im.turms.client.TurmsClient;
import im.turms.common.TurmsStatusCode;
import im.turms.common.constant.ContentType;
import im.turms.common.exception.TurmsBusinessException;
import im.turms.common.model.dto.request.TurmsRequest;
import im.turms.common.model.dto.request.storage.DeleteResourceRequest;
import im.turms.common.model.dto.request.storage.QuerySignedGetUrlRequest;
import im.turms.common.model.dto.request.storage.QuerySignedPutUrlRequest;
import okhttp3.*;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;

public class StorageService {

    private final TurmsClient turmsClient;
    private final OkHttpClient httpClient;
    private String serverUrl = "http://localhost:9000";

    public StorageService(TurmsClient turmsClient, String storageServerUrl) {
        this.turmsClient = turmsClient;
        if (storageServerUrl != null) {
            serverUrl = storageServerUrl;
        }
        httpClient = new OkHttpClient.Builder().build();
    }

    public void setServerUrl(String serverUrl) {
        this.serverUrl = serverUrl;
    }

    // Profile picture

    public CompletableFuture<String> queryProfilePictureUrlForAccess(long userId) {
        String url = String.format("%s/%s/%d", serverUrl, getBucketName(ContentType.PROFILE), userId);
        return CompletableFuture.completedFuture(url);
    }

    public CompletableFuture<byte[]> queryProfilePicture(long userId) {
        return queryProfilePictureUrlForAccess(userId).thenCompose(this::getBytesFromGetUrl);
    }

    public CompletableFuture<String> queryProfilePictureUrlForUpload(long pictureSize) {
        Long userId = turmsClient.getUserService().getUserId();
        if (userId != null) {
            return getSignedPutUrl(ContentType.PROFILE, pictureSize, null, userId);
        } else {
            CompletableFuture<String> future = new CompletableFuture<>();
            future.completeExceptionally(TurmsBusinessException.get(TurmsStatusCode.UNAUTHORIZED));
            return future;
        }
    }

    public CompletableFuture<String> uploadProfilePicture(byte[] bytes) {
        return queryProfilePictureUrlForUpload(bytes.length)
                .thenCompose(url -> upload(url, bytes));
    }

    public CompletableFuture<Void> deleteProfile() {
        return deleteResource(ContentType.PROFILE, null, null);
    }

    // Group profile picture

    public CompletableFuture<String> queryGroupProfilePictureUrlForAccess(long groupId) {
        String url = String.format("%s/%s/%d", serverUrl, getBucketName(ContentType.GROUP_PROFILE), groupId);
        return CompletableFuture.completedFuture(url);
    }

    public CompletableFuture<byte[]> queryGroupProfilePicture(long groupId) {
        return queryGroupProfilePictureUrlForAccess(groupId).thenCompose(this::getBytesFromGetUrl);
    }

    public CompletableFuture<String> queryGroupProfilePictureUrlForUpload(long pictureSize, long groupId) {
        return getSignedPutUrl(ContentType.GROUP_PROFILE, pictureSize, null, groupId);
    }

    public CompletableFuture<String> uploadGroupProfilePicture(byte[] bytes, long groupId) {
        return queryGroupProfilePictureUrlForUpload(bytes.length, groupId)
                .thenCompose(url -> upload(url, bytes));
    }

    public CompletableFuture<Void> deleteGroupProfile(long groupId) {
        return deleteResource(ContentType.GROUP_PROFILE, null, groupId);
    }

    // Message attachment

    public CompletableFuture<String> queryAttachmentUrlForAccess(long messageId, @Nullable String name) {
        return getSignedGetUrl(ContentType.ATTACHMENT, name, messageId);
    }

    public CompletableFuture<byte[]> queryAttachment(long messageId, @Nullable String name) {
        return queryAttachmentUrlForAccess(messageId, name).thenCompose(this::getBytesFromGetUrl);
    }

    public CompletableFuture<String> queryAttachmentUrlForUpload(long messageId, long attachmentSize) {
        return getSignedPutUrl(ContentType.ATTACHMENT, attachmentSize, null, messageId);
    }

    public CompletableFuture<String> uploadAttachment(long messageId, byte[] bytes) {
        return queryAttachmentUrlForUpload(messageId, bytes.length)
                .thenCompose(url -> upload(url, bytes));
    }

    // Base

    private CompletableFuture<String> getSignedGetUrl(@NotNull ContentType contentType, @Nullable String keyStr, @Nullable Long keyNum) {
        QuerySignedGetUrlRequest.Builder urlBuilder = QuerySignedGetUrlRequest.newBuilder()
                .setContentType(contentType);
        if (keyStr != null) {
            urlBuilder.setKeyStr(StringValue.newBuilder().setValue(keyStr).build());
        }
        if (keyNum != null) {
            urlBuilder.setKeyNum(Int64Value.newBuilder().setValue(keyNum).build());
        }
        QuerySignedGetUrlRequest urlRequest = urlBuilder.build();
        TurmsRequest.Builder builder = TurmsRequest.newBuilder()
                .setQuerySignedGetUrlRequest(urlRequest);
        return turmsClient.getDriver()
                .send(builder)
                .thenApply(notification -> notification.getData().getUrl().getValue());
    }

    private CompletableFuture<String> getSignedPutUrl(@NotNull ContentType contentType, long size, @Nullable String keyStr, @Nullable Long keyNum) {
        QuerySignedPutUrlRequest.Builder urlBuilder = QuerySignedPutUrlRequest.newBuilder()
                .setContentLength(size)
                .setContentType(contentType);
        if (keyStr != null) {
            urlBuilder.setKeyStr(StringValue.newBuilder().setValue(keyStr).build());
        }
        if (keyNum != null) {
            urlBuilder.setKeyNum(Int64Value.newBuilder().setValue(keyNum).build());
        }
        QuerySignedPutUrlRequest urlRequest = urlBuilder.build();
        TurmsRequest.Builder builder = TurmsRequest.newBuilder()
                .setQuerySignedPutUrlRequest(urlRequest);
        return turmsClient.getDriver()
                .send(builder)
                .thenApply(notification -> notification.getData().getUrl().getValue());
    }

    private CompletableFuture<Void> deleteResource(@NotNull ContentType contentType, @Nullable String keyStr, @Nullable Long keyNum) {
        DeleteResourceRequest.Builder requestBuilder = DeleteResourceRequest.newBuilder()
                .setContentType(contentType);
        if (keyStr != null) {
            requestBuilder.setKeyStr(StringValue.newBuilder().setValue(keyStr).build());
        }
        if (keyNum != null) {
            requestBuilder.setKeyNum(Int64Value.newBuilder().setValue(keyNum).build());
        }
        DeleteResourceRequest request = requestBuilder.build();
        TurmsRequest.Builder builder = TurmsRequest.newBuilder()
                .setDeleteResourceRequest(request);
        return turmsClient.getDriver()
                .send(builder)
                .thenApply(notification -> null);
    }

    private CompletableFuture<byte[]> getBytesFromGetUrl(@NotNull String url) {
        CompletableFuture<byte[]> future = new CompletableFuture<>();
        Request request = new Request.Builder()
                .url(url)
                .build();
        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@org.jetbrains.annotations.NotNull Call call, @org.jetbrains.annotations.NotNull IOException e) {
                future.completeExceptionally(e);
            }

            @Override
            public void onResponse(@org.jetbrains.annotations.NotNull Call call, @org.jetbrains.annotations.NotNull Response response) throws IOException {
                ResponseBody body = response.body();
                if (body != null) {
                    future.complete(body.bytes());
                } else {
                    future.completeExceptionally(TurmsBusinessException.get(TurmsStatusCode.MISSING_DATA));
                }
            }
        });
        return future;
    }

    private CompletableFuture<String> upload(String url, byte[] bytes) {
        CompletableFuture<String> future = new CompletableFuture<>();
        Request request = new Request.Builder()
                .url(url)
                .put(RequestBody.create(bytes))
                .build();
        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@org.jetbrains.annotations.NotNull Call call, @org.jetbrains.annotations.NotNull IOException e) {
                future.completeExceptionally(e);
            }

            @Override
            public void onResponse(@org.jetbrains.annotations.NotNull Call call, @org.jetbrains.annotations.NotNull Response response) throws IOException {
                ResponseBody body = response.body();
                if (body != null) {
                    future.complete(body.string());
                } else {
                    future.completeExceptionally(TurmsBusinessException.get(TurmsStatusCode.MISSING_DATA));
                }
            }
        });
        return future;
    }

    private String getBucketName(ContentType contentType) {
        return contentType.name().toLowerCase().replace("_", "-");
    }
}
