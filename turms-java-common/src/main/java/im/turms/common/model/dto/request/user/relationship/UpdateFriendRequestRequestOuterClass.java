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
// source: request/user/relationship/update_friend_request_request.proto

package im.turms.common.model.dto.request.user.relationship;

public final class UpdateFriendRequestRequestOuterClass {
  private UpdateFriendRequestRequestOuterClass() {}
  public static void registerAllExtensions(
      com.google.protobuf.ExtensionRegistryLite registry) {
  }

  public static void registerAllExtensions(
      com.google.protobuf.ExtensionRegistry registry) {
    registerAllExtensions(
        (com.google.protobuf.ExtensionRegistryLite) registry);
  }
  static final com.google.protobuf.Descriptors.Descriptor
    internal_static_im_turms_proto_UpdateFriendRequestRequest_descriptor;
  static final
    com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
      internal_static_im_turms_proto_UpdateFriendRequestRequest_fieldAccessorTable;

  public static com.google.protobuf.Descriptors.FileDescriptor
      getDescriptor() {
    return descriptor;
  }
  private static  com.google.protobuf.Descriptors.FileDescriptor
      descriptor;
  static {
    java.lang.String[] descriptorData = {
      "\n=request/user/relationship/update_frien" +
      "d_request_request.proto\022\016im.turms.proto\032" +
      "\036google/protobuf/wrappers.proto\032\036constan" +
      "t/response_action.proto\"\227\001\n\032UpdateFriend" +
      "RequestRequest\022\022\n\nrequest_id\030\001 \001(\003\0227\n\017re" +
      "sponse_action\030\002 \001(\0162\036.im.turms.proto.Res" +
      "ponseAction\022,\n\006reason\030\003 \001(\0132\034.google.pro" +
      "tobuf.StringValueB:\n3im.turms.common.mod" +
      "el.dto.request.user.relationshipP\001\272\002\000b\006p" +
      "roto3"
    };
    descriptor = com.google.protobuf.Descriptors.FileDescriptor
      .internalBuildGeneratedFileFrom(descriptorData,
        new com.google.protobuf.Descriptors.FileDescriptor[] {
          com.google.protobuf.WrappersProto.getDescriptor(),
          im.turms.common.constant.ResponseActionOuterClass.getDescriptor(),
        });
    internal_static_im_turms_proto_UpdateFriendRequestRequest_descriptor =
      getDescriptor().getMessageTypes().get(0);
    internal_static_im_turms_proto_UpdateFriendRequestRequest_fieldAccessorTable = new
      com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
        internal_static_im_turms_proto_UpdateFriendRequestRequest_descriptor,
        new java.lang.String[] { "RequestId", "ResponseAction", "Reason", });
    com.google.protobuf.WrappersProto.getDescriptor();
    im.turms.common.constant.ResponseActionOuterClass.getDescriptor();
  }

  // @@protoc_insertion_point(outer_class_scope)
}
