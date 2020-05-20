import {im} from "../model/proto-bundle";
import UserStatus = im.turms.proto.UserStatus;
import ProfileAccessStrategy = im.turms.proto.ProfileAccessStrategy;
import ResponseAction = im.turms.proto.ResponseAction;
import GroupMemberRole = im.turms.proto.GroupMemberRole;
import MessageDeliveryStatus = im.turms.proto.MessageDeliveryStatus;
import DeviceType = im.turms.proto.DeviceType;

export default class ConstantTransformer {

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
                throw 'illegal GroupMemberRole';
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
                throw 'illegal UserStatus';
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
                throw 'illegal ProfileAccessStrategy';
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
                throw 'illegal ResponseAction';
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
                throw 'illegal MessageDeliveryStatus';
        }
    }

    static string2DeviceType(deviceType: string): DeviceType {
        deviceType = deviceType.toUpperCase();
        switch (deviceType) {
            case 'ANDROID':
                return DeviceType.ANDROID;
            case 'IOS':
                return DeviceType.IOS;
            case 'BROWSER':
                return DeviceType.BROWSER;
            case 'DESKTOP':
                return DeviceType.DESKTOP;
            case 'OTHERS':
                return DeviceType.OTHERS;
            case 'UNKNOWN':
                return DeviceType.UNKNOWN;
            default:
                throw 'illegal DeviceType';
        }
    }
}