/*
 * Copyright (C) 2019 The Turms Project
 * https://github.com/turms-im/turms
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: notification/turms_notification.proto

package im.turms.common.model.dto.notification;

public final class TurmsNotificationOuterClass {
  private TurmsNotificationOuterClass() {}
  public static void registerAllExtensions(
      com.google.protobuf.ExtensionRegistryLite registry) {
  }

  public static void registerAllExtensions(
      com.google.protobuf.ExtensionRegistry registry) {
    registerAllExtensions(
        (com.google.protobuf.ExtensionRegistryLite) registry);
  }
  static final com.google.protobuf.Descriptors.Descriptor
    internal_static_im_turms_proto_TurmsNotification_descriptor;
  static final
    com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
      internal_static_im_turms_proto_TurmsNotification_fieldAccessorTable;
  static final com.google.protobuf.Descriptors.Descriptor
    internal_static_im_turms_proto_TurmsNotification_Data_descriptor;
  static final
    com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
      internal_static_im_turms_proto_TurmsNotification_Data_fieldAccessorTable;

  public static com.google.protobuf.Descriptors.FileDescriptor
      getDescriptor() {
    return descriptor;
  }
  private static  com.google.protobuf.Descriptors.FileDescriptor
      descriptor;
  static {
    java.lang.String[] descriptorData = {
      "\n%notification/turms_notification.proto\022" +
      "\016im.turms.proto\032\036google/protobuf/wrapper" +
      "s.proto\032\036model/signal/acknowledge.proto\032" +
      "\032model/signal/session.proto\032\033request/tur" +
      "ms_request.proto\032\037model/common/int64_val" +
      "ues.proto\032,model/common/int64_values_wit" +
      "h_version.proto\0320model/group/group_invit" +
      "ations_with_version.proto\0324model/group/g" +
      "roup_join_questions_answer_result.proto\032" +
      "3model/group/group_join_questions_with_v" +
      "ersion.proto\0322model/group/group_join_req" +
      "uests_with_version.proto\032,model/group/gr" +
      "oup_members_with_version.proto\032%model/gr" +
      "oup/groups_with_version.proto\032$model/mes" +
      "sage/message_statuses.proto\032\034model/messa" +
      "ge/messages.proto\032,model/message/message" +
      "s_with_total_list.proto\0322model/user/user" +
      "_friend_requests_with_version.proto\0326mod" +
      "el/user/user_relationship_groups_with_ve" +
      "rsion.proto\0320model/user/user_relationshi" +
      "ps_with_version.proto\032!model/user/user_s" +
      "ession_ids.proto\032)model/user/users_infos" +
      "_with_version.proto\032&model/user/users_on" +
      "line_statuses.proto\"\225\016\n\021TurmsNotificatio" +
      "n\022/\n\nrequest_id\030\001 \001(\0132\033.google.protobuf." +
      "Int64Value\022)\n\004code\030\002 \001(\0132\033.google.protob" +
      "uf.Int32Value\022,\n\006reason\030\003 \001(\0132\034.google.p" +
      "rotobuf.StringValue\0224\n\004data\030\004 \001(\0132&.im.t" +
      "urms.proto.TurmsNotification.Data\0225\n\017rel" +
      "ayed_request\030\005 \001(\0132\034.im.turms.proto.Turm" +
      "sRequest\0221\n\014requester_id\030\006 \001(\0132\033.google." +
      "protobuf.Int64Value\0221\n\014close_status\030\007 \001(" +
      "\0132\033.google.protobuf.Int32Value\032\242\013\n\004Data\022" +
      "*\n\003ids\030\001 \001(\0132\033.im.turms.proto.Int64Value" +
      "sH\000\022B\n\020ids_with_version\030\002 \001(\0132&.im.turms" +
      ".proto.Int64ValuesWithVersionH\000\022+\n\003url\030\003" +
      " \001(\0132\034.google.protobuf.StringValueH\000\0222\n\013" +
      "acknowledge\030\004 \001(\0132\033.im.turms.proto.Ackno" +
      "wledgeH\000\022*\n\007session\030\005 \001(\0132\027.im.turms.pro" +
      "to.SessionH\000\022,\n\010messages\030\006 \001(\0132\030.im.turm" +
      "s.proto.MessagesH\000\022;\n\020message_statuses\030\007" +
      " \001(\0132\037.im.turms.proto.MessageStatusesH\000\022" +
      "I\n\030messages_with_total_list\030\010 \001(\0132%.im.t" +
      "urms.proto.MessagesWithTotalListH\000\022I\n\030us" +
      "ers_infos_with_version\030\t \001(\0132%.im.turms." +
      "proto.UsersInfosWithVersionH\000\022D\n\025users_o" +
      "nline_statuses\030\n \001(\0132#.im.turms.proto.Us" +
      "ersOnlineStatusesH\000\022Z\n!user_friend_reque" +
      "sts_with_version\030\013 \001(\0132-.im.turms.proto." +
      "UserFriendRequestsWithVersionH\000\022b\n%user_" +
      "relationship_groups_with_version\030\014 \001(\01321" +
      ".im.turms.proto.UserRelationshipGroupsWi" +
      "thVersionH\000\022W\n\037user_relationships_with_v" +
      "ersion\030\r \001(\0132,.im.turms.proto.UserRelati" +
      "onshipsWithVersionH\000\022:\n\020user_session_ids" +
      "\030\016 \001(\0132\036.im.turms.proto.UserSessionIdsH\000" +
      "\022U\n\036group_invitations_with_version\030\017 \001(\013" +
      "2+.im.turms.proto.GroupInvitationsWithVe" +
      "rsionH\000\022[\n!group_join_question_answer_re" +
      "sult\030\020 \001(\0132..im.turms.proto.GroupJoinQue" +
      "stionsAnswerResultH\000\022X\n group_join_reque" +
      "sts_with_version\030\021 \001(\0132,.im.turms.proto." +
      "GroupJoinRequestsWithVersionH\000\022Z\n!group_" +
      "join_questions_with_version\030\022 \001(\0132-.im.t" +
      "urms.proto.GroupJoinQuestionsWithVersion" +
      "H\000\022M\n\032group_members_with_version\030\023 \001(\0132\'" +
      ".im.turms.proto.GroupMembersWithVersionH" +
      "\000\022@\n\023groups_with_version\030\024 \001(\0132!.im.turm" +
      "s.proto.GroupsWithVersionH\000B\006\n\004kindB-\n&i" +
      "m.turms.common.model.dto.notificationP\001\272" +
      "\002\000b\006proto3"
    };
    descriptor = com.google.protobuf.Descriptors.FileDescriptor
      .internalBuildGeneratedFileFrom(descriptorData,
        new com.google.protobuf.Descriptors.FileDescriptor[] {
          com.google.protobuf.WrappersProto.getDescriptor(),
          im.turms.common.model.bo.signal.AcknowledgeOuterClass.getDescriptor(),
          im.turms.common.model.bo.signal.SessionOuterClass.getDescriptor(),
          im.turms.common.model.dto.request.TurmsRequestOuterClass.getDescriptor(),
          im.turms.common.model.bo.common.Int64ValuesOuterClass.getDescriptor(),
          im.turms.common.model.bo.common.Int64ValuesWithVersionOuterClass.getDescriptor(),
          im.turms.common.model.bo.group.GroupInvitationsWithVersionOuterClass.getDescriptor(),
          im.turms.common.model.bo.group.GroupJoinQuestionsAnswerResultOuterClass.getDescriptor(),
          im.turms.common.model.bo.group.GroupJoinQuestionsWithVersionOuterClass.getDescriptor(),
          im.turms.common.model.bo.group.GroupJoinRequestsWithVersionOuterClass.getDescriptor(),
          im.turms.common.model.bo.group.GroupMembersWithVersionOuterClass.getDescriptor(),
          im.turms.common.model.bo.group.GroupsWithVersionOuterClass.getDescriptor(),
          im.turms.common.model.bo.message.MessageStatusesOuterClass.getDescriptor(),
          im.turms.common.model.bo.message.MessagesOuterClass.getDescriptor(),
          im.turms.common.model.bo.message.MessagesWithTotalListOuterClass.getDescriptor(),
          im.turms.common.model.bo.user.UserFriendRequestsWithVersionOuterClass.getDescriptor(),
          im.turms.common.model.bo.user.UserRelationshipGroupsWithVersionOuterClass.getDescriptor(),
          im.turms.common.model.bo.user.UserRelationshipsWithVersionOuterClass.getDescriptor(),
          im.turms.common.model.bo.user.UserSessionIdsOuterClass.getDescriptor(),
          im.turms.common.model.bo.user.UsersInfosWithVersionOuterClass.getDescriptor(),
          im.turms.common.model.bo.user.UsersOnlineStatusesOuterClass.getDescriptor(),
        });
    internal_static_im_turms_proto_TurmsNotification_descriptor =
      getDescriptor().getMessageTypes().get(0);
    internal_static_im_turms_proto_TurmsNotification_fieldAccessorTable = new
      com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
        internal_static_im_turms_proto_TurmsNotification_descriptor,
        new java.lang.String[] { "RequestId", "Code", "Reason", "Data", "RelayedRequest", "RequesterId", "CloseStatus", });
    internal_static_im_turms_proto_TurmsNotification_Data_descriptor =
      internal_static_im_turms_proto_TurmsNotification_descriptor.getNestedTypes().get(0);
    internal_static_im_turms_proto_TurmsNotification_Data_fieldAccessorTable = new
      com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
        internal_static_im_turms_proto_TurmsNotification_Data_descriptor,
        new java.lang.String[] { "Ids", "IdsWithVersion", "Url", "Acknowledge", "Session", "Messages", "MessageStatuses", "MessagesWithTotalList", "UsersInfosWithVersion", "UsersOnlineStatuses", "UserFriendRequestsWithVersion", "UserRelationshipGroupsWithVersion", "UserRelationshipsWithVersion", "UserSessionIds", "GroupInvitationsWithVersion", "GroupJoinQuestionAnswerResult", "GroupJoinRequestsWithVersion", "GroupJoinQuestionsWithVersion", "GroupMembersWithVersion", "GroupsWithVersion", "Kind", });
    com.google.protobuf.WrappersProto.getDescriptor();
    im.turms.common.model.bo.signal.AcknowledgeOuterClass.getDescriptor();
    im.turms.common.model.bo.signal.SessionOuterClass.getDescriptor();
    im.turms.common.model.dto.request.TurmsRequestOuterClass.getDescriptor();
    im.turms.common.model.bo.common.Int64ValuesOuterClass.getDescriptor();
    im.turms.common.model.bo.common.Int64ValuesWithVersionOuterClass.getDescriptor();
    im.turms.common.model.bo.group.GroupInvitationsWithVersionOuterClass.getDescriptor();
    im.turms.common.model.bo.group.GroupJoinQuestionsAnswerResultOuterClass.getDescriptor();
    im.turms.common.model.bo.group.GroupJoinQuestionsWithVersionOuterClass.getDescriptor();
    im.turms.common.model.bo.group.GroupJoinRequestsWithVersionOuterClass.getDescriptor();
    im.turms.common.model.bo.group.GroupMembersWithVersionOuterClass.getDescriptor();
    im.turms.common.model.bo.group.GroupsWithVersionOuterClass.getDescriptor();
    im.turms.common.model.bo.message.MessageStatusesOuterClass.getDescriptor();
    im.turms.common.model.bo.message.MessagesOuterClass.getDescriptor();
    im.turms.common.model.bo.message.MessagesWithTotalListOuterClass.getDescriptor();
    im.turms.common.model.bo.user.UserFriendRequestsWithVersionOuterClass.getDescriptor();
    im.turms.common.model.bo.user.UserRelationshipGroupsWithVersionOuterClass.getDescriptor();
    im.turms.common.model.bo.user.UserRelationshipsWithVersionOuterClass.getDescriptor();
    im.turms.common.model.bo.user.UserSessionIdsOuterClass.getDescriptor();
    im.turms.common.model.bo.user.UsersInfosWithVersionOuterClass.getDescriptor();
    im.turms.common.model.bo.user.UsersOnlineStatusesOuterClass.getDescriptor();
  }

  // @@protoc_insertion_point(outer_class_scope)
}
