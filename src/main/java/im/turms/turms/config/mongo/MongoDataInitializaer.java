package im.turms.turms.config.mongo;

import im.turms.turms.cluster.TurmsClusterManager;
import im.turms.turms.common.TurmsLogger;
import im.turms.turms.constant.*;
import im.turms.turms.pojo.domain.AdminRole;
import im.turms.turms.pojo.domain.*;
import org.springframework.data.mongodb.core.CollectionOptions;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import javax.annotation.Nullable;
import javax.annotation.PostConstruct;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import static im.turms.turms.common.Constants.DEFAULT_GROUP_TYPE_ID;
import static im.turms.turms.common.Constants.DEV_MODE;

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
                    mockIfDev();
                })
                .subscribe();
    }

    // Note: Better not to remove all mock data after turms closed
    private void mockIfDev() {
        if (DEV_MODE && mongoTemplate.getMongoDatabase().getName().contains("-dev")) {
            TurmsLogger.log("Start mocking...");
            // Admin
            final int ADMIN_COUNT = 50;
            final int USER_COUNT = 1000;
            final Date now = new Date();
            List<Object> objects = new LinkedList<>();
            for (int i = 0; i < ADMIN_COUNT; i++) {
                Admin admin = new Admin(
                        "account" + i,
                        "123",
                        "myname",
                        0L,
                        now);
                objects.add(admin);
            }
            for (int i = 0; i < 1000; i++) {
                AdminActionLog adminActionLog = new AdminActionLog(
                        turmsClusterManager.generateRandomId(),
                        "account" + (i % ADMIN_COUNT),
                        new Date(),
                        turmsClusterManager.generateRandomId().intValue(),
                        "testaction");
                objects.add(adminActionLog);
            }
            // Group
            Group group = new Group(
                    turmsClusterManager.generateRandomId(),
                    DEFAULT_GROUP_TYPE_ID,
                    0L,
                    0L,
                    "Turms Developers Group",
                    "This is a group for the developers who are interested in Turms",
                    "nope",
                    null,
                    now,
                    null,
                    null,
                    true);
            objects.add(group);
            GroupVersion groupVersion = new GroupVersion(0L, now, now, now, now, now, now);
            objects.add(groupVersion);
            for (int i = USER_COUNT / 10 * 9; i < USER_COUNT; i++) {
                GroupBlacklistedUser groupBlacklistedUser = new GroupBlacklistedUser(
                        0L,
                        (long) i,
                        new Date(),
                        0L);
                objects.add(groupBlacklistedUser);
            }
            for (int i = USER_COUNT / 10 * 8; i < USER_COUNT / 10 * 9; i++) {
                GroupInvitation groupInvitation = new GroupInvitation(
                        turmsClusterManager.generateRandomId(),
                        new Date(),
                        "test-content",
                        RequestStatus.PENDING,
                        null,
                        0L,
                        0L,
                        (long) i);
                objects.add(groupInvitation);
            }
            GroupJoinQuestion groupJoinQuestion = new GroupJoinQuestion(
                    turmsClusterManager.generateRandomId(),
                    0L,
                    "test-question",
                    List.of("a", "b", "c"));
            objects.add(groupJoinQuestion);
            for (int i = USER_COUNT / 10 * 7; i < USER_COUNT / 10 * 8; i++) {
                GroupJoinRequest groupJoinRequest = new GroupJoinRequest(
                        turmsClusterManager.generateRandomId(),
                        new Date(),
                        "test-content",
                        RequestStatus.PENDING,
                        null,
                        0L,
                        (long) i,
                        null);
                objects.add(groupJoinRequest);
            }
            for (int i = 0; i < USER_COUNT / 10; i++) {
                GroupMember groupMember = new GroupMember(
                        0L,
                        (long) i,
                        "test-name",
                        i == 0 ? GroupMemberRole.OWNER : GroupMemberRole.MEMBER,
                        new Date(),
                        null);
                objects.add(groupMember);
            }

            // Message
            for (int i = 0; i < 100; i++) {
                long id = turmsClusterManager.generateRandomId();
                Message privateMessage = new Message(
                        id,
                        ChatType.PRIVATE,
                        now,
                        "message-text",
                        0L,
                        (long) (i + 50),
                        null,
                        30);
                MessageStatus privateMessageStatus = new MessageStatus(
                        id,
                        ChatType.PRIVATE,
                        (long) (i % 10),
                        0L,
                        MessageDeliveryStatus.READY);
                objects.add(privateMessageStatus);
                id = turmsClusterManager.generateRandomId();
                Message groupMessage = new Message(
                        id,
                        ChatType.GROUP,
                        now,
                        "message-text",
                        0L,
                        0L,
                        null,
                        30);
                for (int j = 1; j < USER_COUNT / 10; j++) {
                    MessageStatus groupMessageStatus = new MessageStatus(
                            id,
                            ChatType.GROUP,
                            0L,
                            (long) j,
                            MessageDeliveryStatus.READY);
                    objects.add(groupMessageStatus);
                }
                objects.add(privateMessage);
                objects.add(groupMessage);
            }

            // User
            for (int i = 0; i < USER_COUNT; i++) {
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
            for (int i = 1; i < USER_COUNT / 10 * 2; i++) {
                UserFriendRequest userFriendRequest = new UserFriendRequest(
                        turmsClusterManager.generateRandomId(),
                        now,
                        "test-request",
                        RequestStatus.PENDING,
                        null,
                        null,
                        0L,
                        (long) i);
                objects.add(userFriendRequest);
            }
            for (int i = 1; i < USER_COUNT / 10; i++) {
                UserRelationship userRelationship = new UserRelationship(
                        new UserRelationship.Key(0L, (long) i),
                        false,
                        now);
                UserRelationship userRelationship1 = new UserRelationship(
                        new UserRelationship.Key((long) i, 0L),
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
