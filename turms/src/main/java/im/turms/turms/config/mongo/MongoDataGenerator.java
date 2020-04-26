package im.turms.turms.config.mongo;

import com.google.common.net.InetAddresses;
import im.turms.common.constant.*;
import im.turms.turms.compiler.CompilerOptions;
import im.turms.turms.constant.AdminPermission;
import im.turms.turms.manager.TurmsClusterManager;
import im.turms.turms.pojo.domain.*;
import im.turms.turms.util.TurmsPasswordUtil;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.SetUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.mongodb.core.CollectionOptions;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import javax.annotation.Nullable;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import static im.turms.turms.constant.Common.*;

@Log4j2
@Component
public class MongoDataGenerator {

    private final TurmsClusterManager turmsClusterManager;
    private final ReactiveMongoTemplate mongoTemplate;
    private final TurmsPasswordUtil passwordUtil;

    public MongoDataGenerator(TurmsClusterManager turmsClusterManager, ReactiveMongoTemplate mongoTemplate, TurmsPasswordUtil passwordUtil) {
        this.turmsClusterManager = turmsClusterManager;
        this.mongoTemplate = mongoTemplate;
        this.passwordUtil = passwordUtil;
    }

    @EventListener(classes = ContextRefreshedEvent.class)
    public void createCollectionsIfNotExist() {
        if (isDevEnv()) {
            log.info("Start clearing collections...");
            Query queryAll = new Query();
            mongoTemplate.getCollectionNames()
                    .flatMap(name -> {
                        if (name.equals("admin")) {
                            Query query = new Query();
                            query.addCriteria(Criteria.where(Admin.Fields.roleId).ne(ADMIN_ROLE_ROOT_ID));
                            return mongoTemplate.remove(query, name);
                        } else {
                            return mongoTemplate.remove(queryAll, name);
                        }
                    })
                    .blockLast();
            log.info("All collections are cleared");
        }
        log.info("Start creating collections...");
        Mono.when(
                createCollectionIfNotExist(Admin.class, null),
                createCollectionIfNotExist(AdminActionLog.class, null),
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
                createCollectionIfNotExist(UserActionLog.class, null),
                createCollectionIfNotExist(UserFriendRequest.class, null),
                createCollectionIfNotExist(UserLocation.class, null),
                createCollectionIfNotExist(UserLoginLog.class, null),
                createCollectionIfNotExist(UserOnlineUserNumber.class, null),
                createCollectionIfNotExist(UserPermissionGroup.class, null),
                createCollectionIfNotExist(UserRelationship.class, null),
                createCollectionIfNotExist(UserRelationshipGroup.class, null),
                createCollectionIfNotExist(UserRelationshipGroupMember.class, null),
                createCollectionIfNotExist(UserVersion.class, null))
                .doOnTerminate(() -> {
                    log.info("All collections are created");
                    mockIfDev();
                })
                .subscribe();
    }

    private boolean isDevEnv() {
        return CompilerOptions.ENV == CompilerOptions.Env.DEV && mongoTemplate.getMongoDatabase().getName().contains("-dev");
    }

    // Note: Better not to remove all mock data after turms closed
    private void mockIfDev() {
        if (isDevEnv()) {
            log.info("Start mocking...");
            // Admin
            final int ADMIN_COUNT = 10;
            final int USER_COUNT = 1000;
            final Date now = new Date();
            List<Object> objects = new LinkedList<>();
            final long GUEST_ROLE_ID = 2L;
            Admin guest = new Admin(
                    "guest",
                    passwordUtil.encodeAdminPassword("guest"),
                    "guest",
                    GUEST_ROLE_ID,
                    now);
            objects.add(guest);
            for (int i = 1; i <= ADMIN_COUNT; i++) {
                Admin admin = new Admin(
                        "account" + i,
                        passwordUtil.encodeAdminPassword("123"),
                        "myname",
                        1L,
                        DateUtils.addDays(now, -i));
                objects.add(admin);
            }
            for (int i = 1; i <= 100; i++) {
                AdminActionLog adminActionLog;
                try {
                    adminActionLog = new AdminActionLog(
                            turmsClusterManager.generateRandomId(),
                            "account" + (1 + i % ADMIN_COUNT),
                            DateUtils.addDays(now, -i),
                            InetAddresses.coerceToInteger(InetAddress.getByName("127.0.0.1")),
                            "testaction",
                            null,
                            null);
                    objects.add(adminActionLog);
                } catch (UnknownHostException e) {
                    // This should never happen
                    e.printStackTrace();
                }
            }
            AdminRole adminRole = new AdminRole(
                    1L,
                    "ADMIN",
                    AdminPermission.ALL,
                    0);
            AdminRole guestRole = new AdminRole(
                    GUEST_ROLE_ID,
                    "GUEST",
                    SetUtils.union(AdminPermission.ALL_QUERY, AdminPermission.ALL_CREATE),
                    0);
            objects.add(adminRole);
            objects.add(guestRole);
            // Group
            Group group = new Group(
                    1L,
                    DEFAULT_GROUP_TYPE_ID,
                    1L,
                    1L,
                    "Turms Developers Group",
                    "This is a group for the developers who are interested in Turms",
                    "nope",
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
                        1L,
                        1L,
                        (long) i,
                        "test-content",
                        RequestStatus.PENDING,
                        DateUtils.addDays(now, -i),
                        null,
                        null);
                objects.add(groupInvitation);
            }
            GroupJoinQuestion groupJoinQuestion = new GroupJoinQuestion(
                    turmsClusterManager.generateRandomId(),
                    1L,
                    "test-question",
                    Set.of("a", "b", "c"),
                    20);
            objects.add(groupJoinQuestion);
            for (int i = 1 + USER_COUNT / 10 * 7; i <= USER_COUNT / 10 * 8; i++) {
                GroupJoinRequest groupJoinRequest = new GroupJoinRequest(
                        turmsClusterManager.generateRandomId(),
                        "test-content",
                        RequestStatus.PENDING,
                        now,
                        null,
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
                        now,
                        i > USER_COUNT / 10 / 2 ? new Date(9999999999999L) : null);
                objects.add(groupMember);
            }

            // Message
            for (int i = 1; i <= 100; i++) {
                long id = turmsClusterManager.generateRandomId();
                Message privateMessage = new Message(
                        id,
                        ChatType.PRIVATE,
                        false,
                        DateUtils.addHours(now, -i),
                        null,
                        "private-message-text" + RandomStringUtils.randomAlphanumeric(16),
                        1L,
                        (long) 2 + (i % 9),
                        null,
                        30,
                        null);
                MessageStatus privateMessageStatus = new MessageStatus(
                        id,
                        null,
                        false,
                        1L,
                        (long) 2 + (i % 9),
                        MessageDeliveryStatus.READY,
                        null,
                        null,
                        null);
                objects.add(privateMessageStatus);
                id = turmsClusterManager.generateRandomId();
                Message groupMessage = new Message(
                        id,
                        ChatType.GROUP,
                        false,
                        now,
                        null,
                        "group-message-text" + RandomStringUtils.randomAlphanumeric(16),
                        1L,
                        1L,
                        null,
                        30,
                        null);
                for (long j = 2; j <= USER_COUNT / 10; j++) {
                    MessageStatus groupMessageStatus = new MessageStatus(
                            id,
                            1L,
                            false,
                            1L,
                            j,
                            MessageDeliveryStatus.READY,
                            null,
                            null,
                            null);
                    objects.add(groupMessageStatus);
                }
                objects.add(privateMessage);
                objects.add(groupMessage);
            }

            // User
            for (int i = 1; i <= USER_COUNT; i++) {
                Date userDate = DateUtils.addDays(now, -i);
                User user = new User(
                        (long) i,
                        passwordUtil.encodeUserPassword("123"),
                        "user-name",
                        "user-intro",
                        ProfileAccessStrategy.ALL,
                        DEFAULT_USER_PERMISSION_GROUP_ID,
                        userDate,
                        null,
                        true,
                        userDate);
                UserVersion userVersion = new UserVersion(
                        (long) i, userDate, userDate, userDate, userDate,
                        userDate, userDate, userDate, userDate, userDate);
                UserRelationshipGroup relationshipGroup = new UserRelationshipGroup((long) i, 0, "", userDate);
                objects.add(user);
                objects.add(userVersion);
                objects.add(relationshipGroup);
            }
            for (int i = 1 + USER_COUNT / 10; i <= USER_COUNT / 10 * 2; i++) {
                UserFriendRequest userFriendRequest = new UserFriendRequest(
                        turmsClusterManager.generateRandomId(),
                        "test-request",
                        RequestStatus.PENDING,
                        null,
                        now,
                        null,
                        null,
                        1L,
                        (long) i);
                objects.add(userFriendRequest);
            }
            for (int i = 2; i <= USER_COUNT / 10; i++) {
                UserRelationship userRelationship1 = new UserRelationship(
                        new UserRelationship.Key(1L, (long) i),
                        false,
                        now);
                UserRelationship userRelationship2 = new UserRelationship(
                        new UserRelationship.Key((long) i, 1L),
                        false,
                        now);
                UserRelationshipGroupMember relationshipGroupMember1 = new UserRelationshipGroupMember(
                        1L, 0, (long) i, now);
                UserRelationshipGroupMember relationshipGroupMember2 = new UserRelationshipGroupMember(
                        (long) i, 0, 1L, now);
                objects.add(userRelationship1);
                objects.add(userRelationship2);
                objects.add(relationshipGroupMember1);
                objects.add(relationshipGroupMember2);
            }
            // Execute
            mongoTemplate.insertAll(objects)
                    .doOnError(error -> log.error("Mocking failed", error))
                    .doOnComplete(() -> log.info("Mocking succeeded"))
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
