// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: model/user/user_relationship.proto

package im.turms.common.model.bo.user;

public final class UserRelationshipOuterClass {
  private UserRelationshipOuterClass() {}
  public static void registerAllExtensions(
      com.google.protobuf.ExtensionRegistryLite registry) {
  }

  public static void registerAllExtensions(
      com.google.protobuf.ExtensionRegistry registry) {
    registerAllExtensions(
        (com.google.protobuf.ExtensionRegistryLite) registry);
  }
  static final com.google.protobuf.Descriptors.Descriptor
    internal_static_im_turms_proto_UserRelationship_descriptor;
  static final 
    com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
      internal_static_im_turms_proto_UserRelationship_fieldAccessorTable;

  public static com.google.protobuf.Descriptors.FileDescriptor
      getDescriptor() {
    return descriptor;
  }
  private static  com.google.protobuf.Descriptors.FileDescriptor
      descriptor;
  static {
    java.lang.String[] descriptorData = {
      "\n\"model/user/user_relationship.proto\022\016im" +
      ".turms.proto\032\036google/protobuf/wrappers.p" +
      "roto\"\222\002\n\020UserRelationship\022-\n\010owner_id\030\001 " +
      "\001(\0132\033.google.protobuf.Int64Value\0224\n\017rela" +
      "ted_user_id\030\002 \001(\0132\033.google.protobuf.Int6" +
      "4Value\022.\n\nis_blocked\030\003 \001(\0132\032.google.prot" +
      "obuf.BoolValue\0220\n\013group_index\030\004 \001(\0132\033.go" +
      "ogle.protobuf.Int64Value\0227\n\022establishmen" +
      "t_date\030\005 \001(\0132\033.google.protobuf.Int64Valu" +
      "eB$\n\035im.turms.common.model.bo.userP\001\272\002\000b" +
      "\006proto3"
    };
    descriptor = com.google.protobuf.Descriptors.FileDescriptor
      .internalBuildGeneratedFileFrom(descriptorData,
        new com.google.protobuf.Descriptors.FileDescriptor[] {
          com.google.protobuf.WrappersProto.getDescriptor(),
        });
    internal_static_im_turms_proto_UserRelationship_descriptor =
      getDescriptor().getMessageTypes().get(0);
    internal_static_im_turms_proto_UserRelationship_fieldAccessorTable = new
      com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
        internal_static_im_turms_proto_UserRelationship_descriptor,
        new java.lang.String[] { "OwnerId", "RelatedUserId", "IsBlocked", "GroupIndex", "EstablishmentDate", });
    com.google.protobuf.WrappersProto.getDescriptor();
  }

  // @@protoc_insertion_point(outer_class_scope)
}
