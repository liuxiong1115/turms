// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: request/user/update_user_online_status_request.proto

package im.turms.turms.pojo.request;

public final class UpdateUserOnlineStatusRequestOuterClass {
  private UpdateUserOnlineStatusRequestOuterClass() {}
  public static void registerAllExtensions(
      com.google.protobuf.ExtensionRegistryLite registry) {
  }

  public static void registerAllExtensions(
      com.google.protobuf.ExtensionRegistry registry) {
    registerAllExtensions(
        (com.google.protobuf.ExtensionRegistryLite) registry);
  }
  static final com.google.protobuf.Descriptors.Descriptor
    internal_static_im_turms_proto_UpdateUserOnlineStatusRequest_descriptor;
  static final 
    com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
      internal_static_im_turms_proto_UpdateUserOnlineStatusRequest_fieldAccessorTable;

  public static com.google.protobuf.Descriptors.FileDescriptor
      getDescriptor() {
    return descriptor;
  }
  private static  com.google.protobuf.Descriptors.FileDescriptor
      descriptor;
  static {
    java.lang.String[] descriptorData = {
      "\n4request/user/update_user_online_status" +
      "_request.proto\022\016im.turms.proto\032\032constant" +
      "/user_status.proto\"P\n\035UpdateUserOnlineSt" +
      "atusRequest\022/\n\013user_status\030\001 \001(\0162\032.im.tu" +
      "rms.proto.UserStatusB\037\n\033im.turms.turms.p" +
      "ojo.requestP\001b\006proto3"
    };
    descriptor = com.google.protobuf.Descriptors.FileDescriptor
      .internalBuildGeneratedFileFrom(descriptorData,
        new com.google.protobuf.Descriptors.FileDescriptor[] {
          im.turms.turms.constant.UserStatusOuterClass.getDescriptor(),
        });
    internal_static_im_turms_proto_UpdateUserOnlineStatusRequest_descriptor =
      getDescriptor().getMessageTypes().get(0);
    internal_static_im_turms_proto_UpdateUserOnlineStatusRequest_fieldAccessorTable = new
      com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
        internal_static_im_turms_proto_UpdateUserOnlineStatusRequest_descriptor,
        new java.lang.String[] { "UserStatus", });
    im.turms.turms.constant.UserStatusOuterClass.getDescriptor();
  }

  // @@protoc_insertion_point(outer_class_scope)
}