// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: request/user/query_users_infos_nearby_request.proto

package im.turms.turms.pojo.request.user;

public final class QueryUsersInfosNearbyRequestOuterClass {
  private QueryUsersInfosNearbyRequestOuterClass() {}
  public static void registerAllExtensions(
      com.google.protobuf.ExtensionRegistryLite registry) {
  }

  public static void registerAllExtensions(
      com.google.protobuf.ExtensionRegistry registry) {
    registerAllExtensions(
        (com.google.protobuf.ExtensionRegistryLite) registry);
  }
  static final com.google.protobuf.Descriptors.Descriptor
    internal_static_im_turms_proto_QueryUsersInfosNearbyRequest_descriptor;
  static final
    com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
      internal_static_im_turms_proto_QueryUsersInfosNearbyRequest_fieldAccessorTable;

  public static com.google.protobuf.Descriptors.FileDescriptor
      getDescriptor() {
    return descriptor;
  }
  private static  com.google.protobuf.Descriptors.FileDescriptor
      descriptor;
  static {
    java.lang.String[] descriptorData = {
            "\n3request/user/query_users_infos_nearby_" +
                    "request.proto\022\016im.turms.proto\032\036google/pr" +
                    "otobuf/wrappers.proto\"\243\001\n\034QueryUsersInfo" +
                    "sNearbyRequest\022\020\n\010latitude\030\001 \001(\002\022\021\n\tlong" +
                    "itude\030\002 \001(\002\022-\n\010distance\030\003 \001(\0132\033.google.p" +
                    "rotobuf.FloatValue\022/\n\tmaxNumber\030\004 \001(\0132\034." +
                    "google.protobuf.UInt32ValueB$\n im.turms." +
                    "turms.pojo.request.userP\001b\006proto3"
    };
    descriptor = com.google.protobuf.Descriptors.FileDescriptor
      .internalBuildGeneratedFileFrom(descriptorData,
        new com.google.protobuf.Descriptors.FileDescriptor[] {
          com.google.protobuf.WrappersProto.getDescriptor(),
        });
    internal_static_im_turms_proto_QueryUsersInfosNearbyRequest_descriptor =
      getDescriptor().getMessageTypes().get(0);
    internal_static_im_turms_proto_QueryUsersInfosNearbyRequest_fieldAccessorTable = new
      com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
        internal_static_im_turms_proto_QueryUsersInfosNearbyRequest_descriptor,
        new java.lang.String[] { "Latitude", "Longitude", "Distance", "MaxNumber", });
    com.google.protobuf.WrappersProto.getDescriptor();
  }

  // @@protoc_insertion_point(outer_class_scope)
}