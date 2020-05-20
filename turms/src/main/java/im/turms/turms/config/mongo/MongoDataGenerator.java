package im.turms.turms.config.mongo;

import com.google.common.net.InetAddresses;
import im.turms.common.constant.GroupMemberRole;
import im.turms.common.constant.MessageDeliveryStatus;
import im.turms.common.constant.ProfileAccessStrategy;
import im.turms.common.constant.RequestStatus;
import im.turms.turms.compiler.CompilerOptions;
import im.turms.turms.constant.AdminPermission;
import im.turms.turms.manager.TurmsClusterManager;
import im.turms.turms.pojo.domain.*;
import im.turms.turms.property.TurmsProperties;
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
import reactor.core.publisher.Flux;
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

    public final boolean isMockEnabled;
    public final int userNumber;
    public final int step;

    public final int targetUserToBeGroupMemberStart;
    public final int targetUserToBeGroupMemberEnd;
    public final int targetUserForGroupJoinRequestStart;
    public final int targetUserForGroupJoinRequestEnd;
    public final int targetUserForGroupInvitationStart;
    public final int targetUserForGroupInvitationEnd;
    public final int targetUserToBlacklistInGroupStart;
    public final int targetUserToBlacklistInGroupEnd;

    public final int targetUserToRequestFriendRequestStart;
    public final int targetUserToRequestFriendRequestEnd;
    public final int targetUserToBeFriendRelationshipStart;
    public final int targetUserToBeFriendRelationshipEnd;

    private final TurmsClusterManager turmsClusterManager;
    private final ReactiveMongoTemplate logMongoTemplate;
    private final ReactiveMongoTemplate adminMongoTemplate;
    private final ReactiveMongoTemplate userMongoTemplate;
    private final ReactiveMongoTemplate groupMongoTemplate;
    private final ReactiveMongoTemplate messageMongoTemplate;
    private final TurmsPasswordUtil passwordUtil;

    public MongoDataGenerator(
            TurmsClusterManager turmsClusterManager,
            ReactiveMongoTemplate logMongoTemplate,
            ReactiveMongoTemplate adminMongoTemplate,
            ReactiveMongoTemplate userMongoTemplate,
            ReactiveMongoTemplate groupMongoTemplate,
            ReactiveMongoTemplate messageMongoTemplate,
            TurmsPasswordUtil passwordUtil,
            TurmsProperties turmsProperties) {
        this.turmsClusterManager = turmsClusterManager;
        this.logMongoTemplate = logMongoTemplate;
        this.adminMongoTemplate = adminMongoTemplate;
        this.userMongoTemplate = userMongoTemplate;
        this.groupMongoTemplate = groupMongoTemplate;
        this.messageMongoTemplate = messageMongoTemplate;
        this.passwordUtil = passwordUtil;

        isMockEnabled = turmsProperties.getMock().isEnabled();
        userNumber = turmsProperties.getMock().getUserNumber();
        step = userNumber / 10;

        targetUserToBeGroupMemberStart = 1;
        targetUserToBeGroupMemberEnd = step;
        targetUserForGroupJoinRequestStart = 1 + step * 7;
        targetUserForGroupJoinRequestEnd = step * 8;
        targetUserForGroupInvitationStart = 1 + step * 8;
        targetUserForGroupInvitationEnd = step * 9;
        targetUserToBlacklistInGroupStart = 1 + step * 9;
        targetUserToBlacklistInGroupEnd = step * 10;

        targetUserToBeFriendRelationshipStart = 2;
        targetUserToBeFriendRelationshipEnd = step;
        targetUserToRequestFriendRequestStart = 1 + step;
        targetUserToRequestFriendRequestEnd = 1 + step * 2;
    }

    @EventListener(classes = ContextRefreshedEvent.class)
    public void createCollectionsIfNotExist() {
        if (isDevAndMockEnabled()) {
            log.info("Start clearing collections...");
            clearAllCollections();
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
                    mockIfDevOrTest();
                })
                .subscribe();
    }

    private void clearAllCollections() {
        Query queryAll = new Query();
        logMongoTemplate.getCollectionNames()
                .flatMap(name -> logMongoTemplate.remove(queryAll, name))
                .blockLast();
        adminMongoTemplate.getCollectionNames()
                .flatMap(name -> {
                    if (name.equals("admin")) {
                        Query query = new Query();
                        query.addCriteria(Criteria.where(Admin.Fields.ROLE_ID).ne(ADMIN_ROLE_ROOT_ID));
                        return adminMongoTemplate.remove(query, name);
                    } else {
                        return adminMongoTemplate.remove(queryAll, name);
                    }
                })
                .blockLast();
        userMongoTemplate.getCollectionNames()
                .flatMap(name -> userMongoTemplate.remove(queryAll, name))
                .blockLast();
        groupMongoTemplate.getCollectionNames()
                .flatMap(name -> groupMongoTemplate.remove(queryAll, name))
                .blockLast();
        messageMongoTemplate.getCollectionNames()
                .flatMap(name -> messageMongoTemplate.remove(queryAll, name))
                .blockLast();
    }

    private boolean isDevAndMockEnabled() {
        return CompilerOptions.ENV == CompilerOptions.Env.DEV && isMockEnabled;
    }

    // Note: Better not to remove all mock data after turms closed
    private void mockIfDevOrTest() {
        if (isDevAndMockEnabled()) {
            log.info("Start mocking...");

            final int ADMIN_COUNT = 10;

            List<Object> logRelatedObjs = new LinkedList<>();
            List<Object> adminRelatedObjs = new LinkedList<>();
            List<Object> userRelatedObjs = new LinkedList<>();
            List<Object> groupRelatedObjs = new LinkedList<>();
            List<Object> messageRelatedObjs = new LinkedList<>();

            // Admin
            final Date now = new Date();
            final long GUEST_ROLE_ID = 2L;
            Admin guest = new Admin(
                    "guest",
                    passwordUtil.encodeAdminPassword("guest"),
                    "guest",
                    GUEST_ROLE_ID,
                    now);
            adminRelatedObjs.add(guest);
            for (int i = 1; i <= ADMIN_COUNT; i++) {
                Admin admin = new Admin(
                        "account" + i,
                        passwordUtil.encodeAdminPassword("123"),
                        "myname",
                        1L,
                        DateUtils.addDays(now, -i));
                adminRelatedObjs.add(admin);
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
                    logRelatedObjs.add(adminActionLog);
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
            adminRelatedObjs.add(adminRole);
            adminRelatedObjs.add(guestRole);
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
            groupRelatedObjs.add(group);
            GroupVersion groupVersion = new GroupVersion(1L, now, now, now, now, now, now);
            groupRelatedObjs.add(groupVersion);
            for (int i = targetUserToBlacklistInGroupStart; i <= targetUserToBlacklistInGroupEnd; i++) {
                GroupBlacklistedUser groupBlacklistedUser = new GroupBlacklistedUser(
                        1L,
                        (long) i,
                        now,
                        1L);
                groupRelatedObjs.add(groupBlacklistedUser);
            }
            for (int i = targetUserForGroupInvitationStart; i <= targetUserForGroupInvitationEnd; i++) {
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
                groupRelatedObjs.add(groupInvitation);
            }
            GroupJoinQuestion groupJoinQuestion = new GroupJoinQuestion(
                    turmsClusterManager.generateRandomId(),
                    1L,
                    "test-question",
                    Set.of("a", "b", "c"),
                    20);
            groupRelatedObjs.add(groupJoinQuestion);
            for (int i = targetUserForGroupJoinRequestStart; i <= targetUserForGroupJoinRequestEnd; i++) {
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
                groupRelatedObjs.add(groupJoinRequest);
            }
            for (int i = targetUserToBeGroupMemberStart; i <= targetUserToBeGroupMemberEnd; i++) {
                GroupMember groupMember = new GroupMember(
                        1L,
                        (long) i,
                        "test-name",
                        i == 1 ? GroupMemberRole.OWNER : GroupMemberRole.MEMBER,
                        now,
                        i > userNumber / 10 / 2 ? new Date(9999999999999L) : null);
                groupRelatedObjs.add(groupMember);
            }

            // Message
            for (int i = 1; i <= 100; i++) {
                long id = turmsClusterManager.generateRandomId();
                Message privateMessage = new Message(
                        id,
                        false,
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
                messageRelatedObjs.add(privateMessageStatus);
                id = turmsClusterManager.generateRandomId();
                Message groupMessage = new Message(
                        id,
                        true,
                        false,
                        now,
                        null,
                        "group-message-text" + RandomStringUtils.randomAlphanumeric(16),
                        1L,
                        1L,
                        null,
                        30,
                        null);
                for (long j = 2; j <= step; j++) {
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
                    messageRelatedObjs.add(groupMessageStatus);
                }
                messageRelatedObjs.add(privateMessage);
                messageRelatedObjs.add(groupMessage);
            }

            // User
            for (int i = 1; i <= userNumber; i++) {
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
                userRelatedObjs.add(user);
                userRelatedObjs.add(userVersion);
                userRelatedObjs.add(relationshipGroup);
            }
            for (int i = targetUserToRequestFriendRequestStart; i <= targetUserToRequestFriendRequestEnd; i++) {
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
                userRelatedObjs.add(userFriendRequest);
            }
            for (int i = targetUserToBeFriendRelationshipStart; i <= targetUserToBeFriendRelationshipEnd; i++) {
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
                userRelatedObjs.add(userRelationship1);
                userRelatedObjs.add(userRelationship2);
                userRelatedObjs.add(relationshipGroupMember1);
                userRelatedObjs.add(relationshipGroupMember2);
            }

            // Execute
            Flux.merge(
                    logMongoTemplate.insertAll(logRelatedObjs)
                            .doOnError(error -> log.error("Mocking log-related data failed", error))
                            .doOnComplete(() -> log.info("Mocking log-related data succeeded")),
                    adminMongoTemplate.insertAll(adminRelatedObjs)
                            .doOnError(error -> log.error("Mocking admin-related data failed", error))
                            .doOnComplete(() -> log.info("Mocking admin-related data succeeded")),
                    userMongoTemplate.insertAll(userRelatedObjs)
                            .doOnError(error -> log.error("Mocking user-related data failed", error))
                            .doOnComplete(() -> log.info("Mocking user-related data succeeded")),
                    groupMongoTemplate.insertAll(groupRelatedObjs)
                            .doOnError(error -> log.error("Mocking group-related data failed", error))
                            .doOnComplete(() -> log.info("Mocking group-related data succeeded")),
                    messageMongoTemplate.insertAll(messageRelatedObjs)
                            .doOnError(error -> log.error("Mocking message-related data failed", error))
                            .doOnComplete(() -> log.info("Mocking message-related data succeeded")))
                    .subscribe();
        }
    }

    private <T> Mono<Boolean> createCollectionIfNotExist(
            Class<T> clazz,
            @Nullable CollectionOptions options) {
        ReactiveMongoTemplate mongoTemplate;
        if (clazz == AdminActionLog.class) mongoTemplate = logMongoTemplate;
        else if (clazz == UserActionLog.class) mongoTemplate = logMongoTemplate;
        else if (clazz == UserLoginLog.class) mongoTemplate = logMongoTemplate;

        else if (clazz == Admin.class) mongoTemplate = adminMongoTemplate;
        else if (clazz == AdminRole.class) mongoTemplate = adminMongoTemplate;

        else if (clazz == User.class) mongoTemplate = userMongoTemplate;
        else if (clazz == UserFriendRequest.class) mongoTemplate = userMongoTemplate;
        else if (clazz == UserLocation.class) mongoTemplate = userMongoTemplate;
        else if (clazz == UserOnlineUserNumber.class) mongoTemplate = userMongoTemplate;
        else if (clazz == UserPermissionGroup.class) mongoTemplate = userMongoTemplate;
        else if (clazz == UserRelationship.class) mongoTemplate = userMongoTemplate;
        else if (clazz == UserRelationshipGroup.class) mongoTemplate = userMongoTemplate;
        else if (clazz == UserRelationshipGroupMember.class) mongoTemplate = userMongoTemplate;
        else if (clazz == UserVersion.class) mongoTemplate = userMongoTemplate;

        else if (clazz == Group.class) mongoTemplate = groupMongoTemplate;
        else if (clazz == GroupBlacklistedUser.class) mongoTemplate = groupMongoTemplate;
        else if (clazz == GroupInvitation.class) mongoTemplate = groupMongoTemplate;
        else if (clazz == GroupJoinQuestion.class) mongoTemplate = groupMongoTemplate;
        else if (clazz == GroupJoinRequest.class) mongoTemplate = groupMongoTemplate;
        else if (clazz == GroupMember.class) mongoTemplate = groupMongoTemplate;
        else if (clazz == GroupType.class) mongoTemplate = groupMongoTemplate;
        else if (clazz == GroupVersion.class) mongoTemplate = groupMongoTemplate;

        else if (clazz == Message.class) mongoTemplate = messageMongoTemplate;
        else if (clazz == MessageStatus.class) mongoTemplate = messageMongoTemplate;
        else {
            return Mono.error(new IllegalArgumentException());
        }

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
