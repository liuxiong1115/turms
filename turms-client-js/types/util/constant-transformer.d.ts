import { im } from "../model/proto-bundle";
import ChatType = im.turms.proto.ChatType;
import UserStatus = im.turms.proto.UserStatus;
import ProfileAccessStrategy = im.turms.proto.ProfileAccessStrategy;
import ResponseAction = im.turms.proto.ResponseAction;
import GroupMemberRole = im.turms.proto.GroupMemberRole;
import MessageDeliveryStatus = im.turms.proto.MessageDeliveryStatus;
export default class ConstantTransformer {
    static string2ChatType(chatType: string): ChatType;
    static string2GroupMemberRole(role: string): GroupMemberRole;
    static string2UserOnlineStatus(userOnlineStatus: string): UserStatus;
    static string2ProfileAccessStrategy(strategy: string): ProfileAccessStrategy;
    static string2ResponseAction(responseAction: string): ResponseAction;
    static string2DeliveryStatus(deliveryStatus: string): MessageDeliveryStatus;
}
