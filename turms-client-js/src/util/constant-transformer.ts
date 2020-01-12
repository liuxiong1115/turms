import {im} from "../model/proto-bundle";
import ChatType = im.turms.proto.ChatType;
import UserStatus = im.turms.proto.UserStatus;
import ProfileAccessStrategy = im.turms.proto.ProfileAccessStrategy;
import ResponseAction = im.turms.proto.ResponseAction;
import GroupMemberRole = im.turms.proto.GroupMemberRole;
import MessageDeliveryStatus = im.turms.proto.MessageDeliveryStatus;

export default class ConstantTransformer {
    static string2ChatType(chatType: string): ChatType {
        chatType = chatType.toUpperCase();
        switch (chatType) {
            case 'PRIVATE':
                return ChatType.PRIVATE;
            case 'GROUP':
                return ChatType.GROUP;
            default:
                throw new Error('illegal params');
        }
    }

    static string2GroupMemberRole(role: string): GroupMemberRole {
        role = role.toUpperCase();
        switch (role) {
            case 'OWNER':
                return GroupMemberRole.OWNER;
            case 'MANAGER':
                return GroupMemberRole.MANAGER;
            case 'MEMBER':
                return GroupMemberRole.MEMBER;
            case 'GUEST':
                return GroupMemberRole.GUEST;
            case 'ANONYMOUS_GUEST':
                return GroupMemberRole.ANONYMOUS_GUEST;
            default:
                throw new Error('illegal params');
        }
    }

    static string2UserOnlineStatus(userOnlineStatus: string): UserStatus {
        userOnlineStatus = userOnlineStatus.toUpperCase();
        switch (userOnlineStatus) {
            case 'AVAILABLE':
                return UserStatus.AVAILABLE;
            case 'INVISIBLE':
                return UserStatus.INVISIBLE;
            case 'BUSY':
                return UserStatus.BUSY;
            case 'DO_NOT_DISTURB':
                return UserStatus.DO_NOT_DISTURB;
            case 'AWAY':
                return UserStatus.AWAY;
            case 'BE_RIGHT_BACK':
                return UserStatus.BE_RIGHT_BACK;
            default:
                throw new Error('illegal params');
        }
    }

    static string2ProfileAccessStrategy(strategy: string): ProfileAccessStrategy {
        strategy = strategy.toUpperCase();
        switch (strategy) {
            case 'ALL':
                return ProfileAccessStrategy.ALL;
            case 'ALL_EXCEPT_BLACKLISTED_USERS':
                return ProfileAccessStrategy.ALL_EXCEPT_BLACKLISTED_USERS;
            case 'FRIENDS':
                return ProfileAccessStrategy.FRIENDS;
            default:
                throw new Error('illegal params');
        }
    }

    static string2ResponseAction(responseAction: string): ResponseAction {
        responseAction = responseAction.toUpperCase();
        switch (responseAction) {
            case 'ACCEPT':
                return ResponseAction.ACCEPT;
            case 'DECLINE':
                return ResponseAction.DECLINE;
            case 'IGNORE':
                return ResponseAction.IGNORE;
            default:
                throw new Error('illegal params');
        }
    }

    static string2DeliveryStatus(deliveryStatus: string): MessageDeliveryStatus {
        deliveryStatus = deliveryStatus.toUpperCase();
        switch (deliveryStatus) {
            case 'READY':
                return MessageDeliveryStatus.READY;
            case 'SENDING':
                return MessageDeliveryStatus.SENDING;
            case 'RECEIVED':
                return MessageDeliveryStatus.RECEIVED;
            case 'RECALLING':
                return MessageDeliveryStatus.RECALLING;
            case 'RECALLED':
                return MessageDeliveryStatus.RECALLED;
            default:
                throw new Error('illegal params');
        }
    }
}