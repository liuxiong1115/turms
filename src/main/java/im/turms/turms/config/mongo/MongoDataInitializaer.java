package im.turms.turms.config.mongo;

import com.google.common.net.InetAddresses;
import im.turms.turms.cluster.TurmsClusterManager;
import im.turms.turms.common.TurmsLogger;
import im.turms.turms.constant.*;
import im.turms.turms.pojo.domain.AdminRole;
import im.turms.turms.pojo.domain.*;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.data.mongodb.core.CollectionOptions;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import javax.annotation.Nullable;
import javax.annotation.PostConstruct;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import static im.turms.turms.common.Constants.*;

@Component
public class MongoDataInitializaer {

    private final TurmsClusterManager turmsClusterManager;
    private final ReactiveMongoTemplate mongoTemplate;

    public MongoDataInitializaer(TurmsClusterManager turmsClusterManager, ReactiveMongoTemplate mongoTemplate) {
        this.turmsClusterManager = turmsClusterManager;
        this.mongoTemplate = mongoTemplate;
    }

    @PostConstruct
    public void createCollectionsIfNotExist() {
        TurmsLogger.log("Start creating collections...");
        Mono.zip(
                objects -> objects,
                createCollectionIfNotExist(Admin.class, null),
                createCollectionIfNotExist(AdminActionLog.class,
                        CollectionOptions.empty().size(1024L * 1024 * 128).capped()),
                createCollectionIfNotExist(AdminRole.class, null),
                createCollectionIfNotExist(Group.class, null),
                createCollectionIfNotExist(GroupBlacklistedUser.class, null),
                createCollectionIfNotExist(GroupInvitation.class, null),
                createCollectionIfNotExist(GroupJoinQuestion.class, null),
                createCollectionIfNotExist(GroupMember.class, null),
                createCollectionIfNotExist(GroupType.class, null),
                createCollectionIfNotExist(GroupVersion.class, null),
                createCollectionIfNotExist(Message.class, null),
                createCollectionIfNotExist(MessageStatus.class, null),
                createCollectionIfNotExist(User.class, null),
                createCollectionIfNotExist(UserFriendRequest.class, null),
                createCollectionIfNotExist(UserLocation.class, null),
                createCollectionIfNotExist(UserLoginLog.class,
                        CollectionOptions.empty().size(1024L * 1024 * 1024).capped()),
                createCollectionIfNotExist(UserMaxDailyOnlineUserNumber.class, null),
                createCollectionIfNotExist(UserOnlineUserNumber.class, null),
                createCollectionIfNotExist(UserPermissionGroup.class, null),
                createCollectionIfNotExist(UserPermissionGroupMember.class, null),
                createCollectionIfNotExist(UserRelationship.class, null),
                createCollectionIfNotExist(UserRelationshipGroup.class, null),
                createCollectionIfNotExist(UserRelationshipGroupMember.class, null),
                createCollectionIfNotExist(UserVersion.class, null))
                .doOnTerminate(() -> {
                    TurmsLogger.log("All collections are created");
                    try {
                        mockIfDev();
                    } catch (UnknownHostException e) {
                        TurmsLogger.logThrowable(e);
                    }
                })
                .subscribe();
    }

    // Note: Better not to remove all mock data after turms closed
    private void mockIfDev() throws UnknownHostException {
        if (mongoTemplate.getMongoDatabase().getName().contains("-dev")
                || mongoTemplate.getMongoDatabase().getName().contains("-test")) {
            TurmsLogger.log("Start mocking...");
            // Admin
            final int ADMIN_COUNT = 5;
            final int USER_COUNT = 100;
            final Date now = new Date();
            List<Object> objects = new LinkedList<>();
            for (int i = 1; i <= ADMIN_COUNT; i++) {
                Admin admin = new Admin(
                        "account" + i,
                        "123",
                        "myname",
                        ADMIN_ROLE_ROOT_ID,
                        now);
                objects.add(admin);
            }
            for (int i = 1; i <= 100; i++) {
                AdminActionLog adminActionLog = new AdminActionLog(
                        turmsClusterManager.generateRandomId(),
                        "account" + (1 + i % ADMIN_COUNT),
                        new Date(),
                        InetAddresses.coerceToInteger(InetAddress.getLocalHost()),
                        "testaction");
                objects.add(adminActionLog);
            }
            // Group
            Group group = new Group(
                    1L,
                    DEFAULT_GROUP_TYPE_ID,
                    1L,
                    1L,
                    "Turms Developers Group",
                    "This is a group for the developers who are interested in Turms",
                    "nope",
                    "https://avatars2.githubusercontent.com/u/50931793?s=200&v=4",
                    0,
                    now,
                    null,
                    null,
                    true);
            objects.add(group);
            GroupVersion groupVersion = new GroupVersion(1L, now, now, now, now, now, now);
            objects.add(groupVersion);
            for (int i = 1 + USER_COUNT / 10 * 9; i <= USER_COUNT; i++) {
                GroupBlacklistedUser groupBlacklistedUser = new GroupBlacklistedUser(
                        1L,
                        (long) i,
                        now,
                        1L);
                objects.add(groupBlacklistedUser);
            }
            for (int i = 1 + USER_COUNT / 10 * 8; i <= USER_COUNT / 10 * 9; i++) {
                GroupInvitation groupInvitation = new GroupInvitation(
                        turmsClusterManager.generateRandomId(),
                        now,
                        "test-content",
                        RequestStatus.PENDING,
                        null,
                        1L,
                        1L,
                        (long) i);
                objects.add(groupInvitation);
            }
            GroupJoinQuestion groupJoinQuestion = new GroupJoinQuestion(
                    turmsClusterManager.generateRandomId(),
                    1L,
                    "test-question",
                    List.of("a", "b", "c"),
                    20);
            objects.add(groupJoinQuestion);
            for (int i = 1 + USER_COUNT / 10 * 7; i <= USER_COUNT / 10 * 8; i++) {
                GroupJoinRequest groupJoinRequest = new GroupJoinRequest(
                        turmsClusterManager.generateRandomId(),
                        now,
                        "test-content",
                        RequestStatus.PENDING,
                        null,
                        1L,
                        (long) i,
                        null);
                objects.add(groupJoinRequest);
            }
            for (int i = 1; i <= USER_COUNT / 10; i++) {
                GroupMember groupMember = new GroupMember(
                        1L,
                        (long) i,
                        "test-name",
                        i == 1 ? GroupMemberRole.OWNER : GroupMemberRole.MEMBER,
                        new Date(),
                        i > USER_COUNT / 10 / 2 ? new Date(9999999999999L) : null);
                objects.add(groupMember);
            }

            // Message
            for (int i = 1; i <= 100; i++) {
                long id = turmsClusterManager.generateRandomId();
                Message privateMessage = new Message(
                        id,
                        ChatType.PRIVATE,
                        now,
                        "private-message-text" + RandomStringUtils.randomAlphanumeric(16),
                        1L,
                        (long) 2 + (i % 9),
                        null,
                        30,
                        null);
                MessageStatus privateMessageStatus = new MessageStatus(
                        id,
                        null,
                        1L,
                        (long) 2 + (i % 9),
                        MessageDeliveryStatus.READY);
                objects.add(privateMessageStatus);
                id = turmsClusterManager.generateRandomId();
                Message groupMessage = new Message(
                        id,
                        ChatType.GROUP,
                        now,
                        "group-message-text" + RandomStringUtils.randomAlphanumeric(16),
                        1L,
                        1L,
                        null,
                        30,
                        null);
                for (int j = 2; j <= USER_COUNT / 10; j++) {
                    MessageStatus groupMessageStatus = new MessageStatus(
                            id,
                            1L,
                            1L,
                            (long) j,
                            MessageDeliveryStatus.READY);
                    objects.add(groupMessageStatus);
                }
                objects.add(privateMessage);
                objects.add(groupMessage);
            }

            // User
            for (int i = 1; i <= USER_COUNT; i++) {
                User user = new User(
                        (long) i,
                        "123",
                        "user-name",
                        "user-intro",
                        null,
                        ProfileAccessStrategy.ALL,
                        now,
                        null,
                        true);
                UserVersion userVersion = new UserVersion(
                        (long) i, now, now, now, now, now, now);
                objects.add(user);
                objects.add(userVersion);
            }
            for (int i = 1 + USER_COUNT / 10; i <= USER_COUNT / 10 * 2; i++) {
                UserFriendRequest userFriendRequest = new UserFriendRequest(
                        turmsClusterManager.generateRandomId(),
                        now,
                        "test-request",
                        RequestStatus.PENDING,
                        null,
                        null,
                        1L,
                        (long) i);
                objects.add(userFriendRequest);
            }
            // P.S. Do not need to put them into the default relationship group
            for (int i = 2; i <= USER_COUNT / 10; i++) {
                UserRelationship userRelationship = new UserRelationship(
                        new UserRelationship.Key(1L, (long) i),
                        false,
                        now);
                UserRelationship userRelationship1 = new UserRelationship(
                        new UserRelationship.Key((long) i, 1L),
                        false,
                        now);
                objects.add(userRelationship);
                objects.add(userRelationship1);
            }
            // Execute
            mongoTemplate.insertAll(objects)
                    .doOnTerminate(() -> TurmsLogger.log("Mock has been finished"))
                    .subscribe();
        }
    }

    private <T> Mono<Boolean> createCollectionIfNotExist(
            Class<T> clazz,
            @Nullable CollectionOptions options) {
        return mongoTemplate.collectionExists(clazz)
                .flatMap(exists -> {
                    if (exists != null && !exists) {
                        return mongoTemplate.createCollection(clazz, options)
                                .thenReturn(true);
                    } else {
                        return Mono.just(false);
                    }
                });
    }
}
