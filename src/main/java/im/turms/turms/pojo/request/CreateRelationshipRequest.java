// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: request/user/relationship/create_relationship_request.proto

package im.turms.turms.pojo.request;

/**
 * Protobuf type {@code im.turms.proto.CreateRelationshipRequest}
 */
public  final class CreateRelationshipRequest extends
    com.google.protobuf.GeneratedMessageV3 implements
    // @@protoc_insertion_point(message_implements:im.turms.proto.CreateRelationshipRequest)
    CreateRelationshipRequestOrBuilder {
private static final long serialVersionUID = 0L;
  // Use CreateRelationshipRequest.newBuilder() to construct.
  private CreateRelationshipRequest(com.google.protobuf.GeneratedMessageV3.Builder<?> builder) {
    super(builder);
  }
  private CreateRelationshipRequest() {
  }

  @java.lang.Override
  @SuppressWarnings({"unused"})
  protected java.lang.Object newInstance(
      UnusedPrivateParameter unused) {
    return new CreateRelationshipRequest();
  }

  @java.lang.Override
  public final com.google.protobuf.UnknownFieldSet
  getUnknownFields() {
    return this.unknownFields;
  }
  private CreateRelationshipRequest(
      com.google.protobuf.CodedInputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    this();
    if (extensionRegistry == null) {
      throw new java.lang.NullPointerException();
    }
    com.google.protobuf.UnknownFieldSet.Builder unknownFields =
        com.google.protobuf.UnknownFieldSet.newBuilder();
    try {
      boolean done = false;
      while (!done) {
        int tag = input.readTag();
        switch (tag) {
          case 0:
            done = true;
            break;
          case 8: {

            userId_ = input.readInt64();
            break;
          }
          case 16: {

            isBlocked_ = input.readBool();
            break;
          }
          case 26: {
            com.google.protobuf.Int32Value.Builder subBuilder = null;
            if (groupIndex_ != null) {
              subBuilder = groupIndex_.toBuilder();
            }
            groupIndex_ = input.readMessage(com.google.protobuf.Int32Value.parser(), extensionRegistry);
            if (subBuilder != null) {
              subBuilder.mergeFrom(groupIndex_);
              groupIndex_ = subBuilder.buildPartial();
            }

            break;
          }
          default: {
            if (!parseUnknownField(
                input, unknownFields, extensionRegistry, tag)) {
              done = true;
            }
            break;
          }
        }
      }
    } catch (com.google.protobuf.InvalidProtocolBufferException e) {
      throw e.setUnfinishedMessage(this);
    } catch (java.io.IOException e) {
      throw new com.google.protobuf.InvalidProtocolBufferException(
          e).setUnfinishedMessage(this);
    } finally {
      this.unknownFields = unknownFields.build();
      makeExtensionsImmutable();
    }
  }
  public static final com.google.protobuf.Descriptors.Descriptor
      getDescriptor() {
    return im.turms.turms.pojo.request.CreateRelationshipRequestOuterClass.internal_static_im_turms_proto_CreateRelationshipRequest_descriptor;
  }

  @java.lang.Override
  protected com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
      internalGetFieldAccessorTable() {
    return im.turms.turms.pojo.request.CreateRelationshipRequestOuterClass.internal_static_im_turms_proto_CreateRelationshipRequest_fieldAccessorTable
        .ensureFieldAccessorsInitialized(
            im.turms.turms.pojo.request.CreateRelationshipRequest.class, im.turms.turms.pojo.request.CreateRelationshipRequest.Builder.class);
  }

  public static final int USER_ID_FIELD_NUMBER = 1;
  private long userId_;
  /**
   * <code>int64 user_id = 1;</code>
   * @return The userId.
   */
  public long getUserId() {
    return userId_;
  }

  public static final int IS_BLOCKED_FIELD_NUMBER = 2;
  private boolean isBlocked_;
  /**
   * <code>bool is_blocked = 2;</code>
   * @return The isBlocked.
   */
  public boolean getIsBlocked() {
    return isBlocked_;
  }

  public static final int GROUP_INDEX_FIELD_NUMBER = 3;
  private com.google.protobuf.Int32Value groupIndex_;
  /**
   * <code>.google.protobuf.Int32Value group_index = 3;</code>
   * @return Whether the groupIndex field is set.
   */
  public boolean hasGroupIndex() {
    return groupIndex_ != null;
  }
  /**
   * <code>.google.protobuf.Int32Value group_index = 3;</code>
   * @return The groupIndex.
   */
  public com.google.protobuf.Int32Value getGroupIndex() {
    return groupIndex_ == null ? com.google.protobuf.Int32Value.getDefaultInstance() : groupIndex_;
  }
  /**
   * <code>.google.protobuf.Int32Value group_index = 3;</code>
   */
  public com.google.protobuf.Int32ValueOrBuilder getGroupIndexOrBuilder() {
    return getGroupIndex();
  }

  private byte memoizedIsInitialized = -1;
  @java.lang.Override
  public final boolean isInitialized() {
    byte isInitialized = memoizedIsInitialized;
    if (isInitialized == 1) return true;
    if (isInitialized == 0) return false;

    memoizedIsInitialized = 1;
    return true;
  }

  @java.lang.Override
  public void writeTo(com.google.protobuf.CodedOutputStream output)
                      throws java.io.IOException {
    if (userId_ != 0L) {
      output.writeInt64(1, userId_);
    }
    if (isBlocked_ != false) {
      output.writeBool(2, isBlocked_);
    }
    if (groupIndex_ != null) {
      output.writeMessage(3, getGroupIndex());
    }
    unknownFields.writeTo(output);
  }

  @java.lang.Override
  public int getSerializedSize() {
    int size = memoizedSize;
    if (size != -1) return size;

    size = 0;
    if (userId_ != 0L) {
      size += com.google.protobuf.CodedOutputStream
        .computeInt64Size(1, userId_);
    }
    if (isBlocked_ != false) {
      size += com.google.protobuf.CodedOutputStream
        .computeBoolSize(2, isBlocked_);
    }
    if (groupIndex_ != null) {
      size += com.google.protobuf.CodedOutputStream
        .computeMessageSize(3, getGroupIndex());
    }
    size += unknownFields.getSerializedSize();
    memoizedSize = size;
    return size;
  }

  @java.lang.Override
  public boolean equals(final java.lang.Object obj) {
    if (obj == this) {
     return true;
    }
    if (!(obj instanceof im.turms.turms.pojo.request.CreateRelationshipRequest)) {
      return super.equals(obj);
    }
    im.turms.turms.pojo.request.CreateRelationshipRequest other = (im.turms.turms.pojo.request.CreateRelationshipRequest) obj;

    if (getUserId()
        != other.getUserId()) return false;
    if (getIsBlocked()
        != other.getIsBlocked()) return false;
    if (hasGroupIndex() != other.hasGroupIndex()) return false;
    if (hasGroupIndex()) {
      if (!getGroupIndex()
          .equals(other.getGroupIndex())) return false;
    }
    if (!unknownFields.equals(other.unknownFields)) return false;
    return true;
  }

  @java.lang.Override
  public int hashCode() {
    if (memoizedHashCode != 0) {
      return memoizedHashCode;
    }
    int hash = 41;
    hash = (19 * hash) + getDescriptor().hashCode();
    hash = (37 * hash) + USER_ID_FIELD_NUMBER;
    hash = (53 * hash) + com.google.protobuf.Internal.hashLong(
        getUserId());
    hash = (37 * hash) + IS_BLOCKED_FIELD_NUMBER;
    hash = (53 * hash) + com.google.protobuf.Internal.hashBoolean(
        getIsBlocked());
    if (hasGroupIndex()) {
      hash = (37 * hash) + GROUP_INDEX_FIELD_NUMBER;
      hash = (53 * hash) + getGroupIndex().hashCode();
    }
    hash = (29 * hash) + unknownFields.hashCode();
    memoizedHashCode = hash;
    return hash;
  }

  public static im.turms.turms.pojo.request.CreateRelationshipRequest parseFrom(
      java.nio.ByteBuffer data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data);
  }
  public static im.turms.turms.pojo.request.CreateRelationshipRequest parseFrom(
      java.nio.ByteBuffer data,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data, extensionRegistry);
  }
  public static im.turms.turms.pojo.request.CreateRelationshipRequest parseFrom(
      com.google.protobuf.ByteString data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data);
  }
  public static im.turms.turms.pojo.request.CreateRelationshipRequest parseFrom(
      com.google.protobuf.ByteString data,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data, extensionRegistry);
  }
  public static im.turms.turms.pojo.request.CreateRelationshipRequest parseFrom(byte[] data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data);
  }
  public static im.turms.turms.pojo.request.CreateRelationshipRequest parseFrom(
      byte[] data,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data, extensionRegistry);
  }
  public static im.turms.turms.pojo.request.CreateRelationshipRequest parseFrom(java.io.InputStream input)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3
        .parseWithIOException(PARSER, input);
  }
  public static im.turms.turms.pojo.request.CreateRelationshipRequest parseFrom(
      java.io.InputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3
        .parseWithIOException(PARSER, input, extensionRegistry);
  }
  public static im.turms.turms.pojo.request.CreateRelationshipRequest parseDelimitedFrom(java.io.InputStream input)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3
        .parseDelimitedWithIOException(PARSER, input);
  }
  public static im.turms.turms.pojo.request.CreateRelationshipRequest parseDelimitedFrom(
      java.io.InputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3
        .parseDelimitedWithIOException(PARSER, input, extensionRegistry);
  }
  public static im.turms.turms.pojo.request.CreateRelationshipRequest parseFrom(
      com.google.protobuf.CodedInputStream input)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3
        .parseWithIOException(PARSER, input);
  }
  public static im.turms.turms.pojo.request.CreateRelationshipRequest parseFrom(
      com.google.protobuf.CodedInputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3
        .parseWithIOException(PARSER, input, extensionRegistry);
  }

  @java.lang.Override
  public Builder newBuilderForType() { return newBuilder(); }
  public static Builder newBuilder() {
    return DEFAULT_INSTANCE.toBuilder();
  }
  public static Builder newBuilder(im.turms.turms.pojo.request.CreateRelationshipRequest prototype) {
    return DEFAULT_INSTANCE.toBuilder().mergeFrom(prototype);
  }
  @java.lang.Override
  public Builder toBuilder() {
    return this == DEFAULT_INSTANCE
        ? new Builder() : new Builder().mergeFrom(this);
  }

  @java.lang.Override
  protected Builder newBuilderForType(
      com.google.protobuf.GeneratedMessageV3.BuilderParent parent) {
    Builder builder = new Builder(parent);
    return builder;
  }
  /**
   * Protobuf type {@code im.turms.proto.CreateRelationshipRequest}
   */
  public static final class Builder extends
      com.google.protobuf.GeneratedMessageV3.Builder<Builder> implements
      // @@protoc_insertion_point(builder_implements:im.turms.proto.CreateRelationshipRequest)
      im.turms.turms.pojo.request.CreateRelationshipRequestOrBuilder {
    public static final com.google.protobuf.Descriptors.Descriptor
        getDescriptor() {
      return im.turms.turms.pojo.request.CreateRelationshipRequestOuterClass.internal_static_im_turms_proto_CreateRelationshipRequest_descriptor;
    }

    @java.lang.Override
    protected com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
        internalGetFieldAccessorTable() {
      return im.turms.turms.pojo.request.CreateRelationshipRequestOuterClass.internal_static_im_turms_proto_CreateRelationshipRequest_fieldAccessorTable
          .ensureFieldAccessorsInitialized(
              im.turms.turms.pojo.request.CreateRelationshipRequest.class, im.turms.turms.pojo.request.CreateRelationshipRequest.Builder.class);
    }

    // Construct using im.turms.turms.pojo.request.CreateRelationshipRequest.newBuilder()
    private Builder() {
      maybeForceBuilderInitialization();
    }

    private Builder(
        com.google.protobuf.GeneratedMessageV3.BuilderParent parent) {
      super(parent);
      maybeForceBuilderInitialization();
    }
    private void maybeForceBuilderInitialization() {
      if (com.google.protobuf.GeneratedMessageV3
              .alwaysUseFieldBuilders) {
      }
    }
    @java.lang.Override
    public Builder clear() {
      super.clear();
      userId_ = 0L;

      isBlocked_ = false;

      if (groupIndexBuilder_ == null) {
        groupIndex_ = null;
      } else {
        groupIndex_ = null;
        groupIndexBuilder_ = null;
      }
      return this;
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.Descriptor
        getDescriptorForType() {
      return im.turms.turms.pojo.request.CreateRelationshipRequestOuterClass.internal_static_im_turms_proto_CreateRelationshipRequest_descriptor;
    }

    @java.lang.Override
    public im.turms.turms.pojo.request.CreateRelationshipRequest getDefaultInstanceForType() {
      return im.turms.turms.pojo.request.CreateRelationshipRequest.getDefaultInstance();
    }

    @java.lang.Override
    public im.turms.turms.pojo.request.CreateRelationshipRequest build() {
      im.turms.turms.pojo.request.CreateRelationshipRequest result = buildPartial();
      if (!result.isInitialized()) {
        throw newUninitializedMessageException(result);
      }
      return result;
    }

    @java.lang.Override
    public im.turms.turms.pojo.request.CreateRelationshipRequest buildPartial() {
      im.turms.turms.pojo.request.CreateRelationshipRequest result = new im.turms.turms.pojo.request.CreateRelationshipRequest(this);
      result.userId_ = userId_;
      result.isBlocked_ = isBlocked_;
      if (groupIndexBuilder_ == null) {
        result.groupIndex_ = groupIndex_;
      } else {
        result.groupIndex_ = groupIndexBuilder_.build();
      }
      onBuilt();
      return result;
    }

    @java.lang.Override
    public Builder clone() {
      return super.clone();
    }
    @java.lang.Override
    public Builder setField(
        com.google.protobuf.Descriptors.FieldDescriptor field,
        java.lang.Object value) {
      return super.setField(field, value);
    }
    @java.lang.Override
    public Builder clearField(
        com.google.protobuf.Descriptors.FieldDescriptor field) {
      return super.clearField(field);
    }
    @java.lang.Override
    public Builder clearOneof(
        com.google.protobuf.Descriptors.OneofDescriptor oneof) {
      return super.clearOneof(oneof);
    }
    @java.lang.Override
    public Builder setRepeatedField(
        com.google.protobuf.Descriptors.FieldDescriptor field,
        int index, java.lang.Object value) {
      return super.setRepeatedField(field, index, value);
    }
    @java.lang.Override
    public Builder addRepeatedField(
        com.google.protobuf.Descriptors.FieldDescriptor field,
        java.lang.Object value) {
      return super.addRepeatedField(field, value);
    }
    @java.lang.Override
    public Builder mergeFrom(com.google.protobuf.Message other) {
      if (other instanceof im.turms.turms.pojo.request.CreateRelationshipRequest) {
        return mergeFrom((im.turms.turms.pojo.request.CreateRelationshipRequest)other);
      } else {
        super.mergeFrom(other);
        return this;
      }
    }

    public Builder mergeFrom(im.turms.turms.pojo.request.CreateRelationshipRequest other) {
      if (other == im.turms.turms.pojo.request.CreateRelationshipRequest.getDefaultInstance()) return this;
      if (other.getUserId() != 0L) {
        setUserId(other.getUserId());
      }
      if (other.getIsBlocked() != false) {
        setIsBlocked(other.getIsBlocked());
      }
      if (other.hasGroupIndex()) {
        mergeGroupIndex(other.getGroupIndex());
      }
      this.mergeUnknownFields(other.unknownFields);
      onChanged();
      return this;
    }

    @java.lang.Override
    public final boolean isInitialized() {
      return true;
    }

    @java.lang.Override
    public Builder mergeFrom(
        com.google.protobuf.CodedInputStream input,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws java.io.IOException {
      im.turms.turms.pojo.request.CreateRelationshipRequest parsedMessage = null;
      try {
        parsedMessage = PARSER.parsePartialFrom(input, extensionRegistry);
      } catch (com.google.protobuf.InvalidProtocolBufferException e) {
        parsedMessage = (im.turms.turms.pojo.request.CreateRelationshipRequest) e.getUnfinishedMessage();
        throw e.unwrapIOException();
      } finally {
        if (parsedMessage != null) {
          mergeFrom(parsedMessage);
        }
      }
      return this;
    }

    private long userId_ ;
    /**
     * <code>int64 user_id = 1;</code>
     * @return The userId.
     */
    public long getUserId() {
      return userId_;
    }
    /**
     * <code>int64 user_id = 1;</code>
     * @param value The userId to set.
     * @return This builder for chaining.
     */
    public Builder setUserId(long value) {
      
      userId_ = value;
      onChanged();
      return this;
    }
    /**
     * <code>int64 user_id = 1;</code>
     * @return This builder for chaining.
     */
    public Builder clearUserId() {
      
      userId_ = 0L;
      onChanged();
      return this;
    }

    private boolean isBlocked_ ;
    /**
     * <code>bool is_blocked = 2;</code>
     * @return The isBlocked.
     */
    public boolean getIsBlocked() {
      return isBlocked_;
    }
    /**
     * <code>bool is_blocked = 2;</code>
     * @param value The isBlocked to set.
     * @return This builder for chaining.
     */
    public Builder setIsBlocked(boolean value) {
      
      isBlocked_ = value;
      onChanged();
      return this;
    }
    /**
     * <code>bool is_blocked = 2;</code>
     * @return This builder for chaining.
     */
    public Builder clearIsBlocked() {
      
      isBlocked_ = false;
      onChanged();
      return this;
    }

    private com.google.protobuf.Int32Value groupIndex_;
    private com.google.protobuf.SingleFieldBuilderV3<
        com.google.protobuf.Int32Value, com.google.protobuf.Int32Value.Builder, com.google.protobuf.Int32ValueOrBuilder> groupIndexBuilder_;
    /**
     * <code>.google.protobuf.Int32Value group_index = 3;</code>
     * @return Whether the groupIndex field is set.
     */
    public boolean hasGroupIndex() {
      return groupIndexBuilder_ != null || groupIndex_ != null;
    }
    /**
     * <code>.google.protobuf.Int32Value group_index = 3;</code>
     * @return The groupIndex.
     */
    public com.google.protobuf.Int32Value getGroupIndex() {
      if (groupIndexBuilder_ == null) {
        return groupIndex_ == null ? com.google.protobuf.Int32Value.getDefaultInstance() : groupIndex_;
      } else {
        return groupIndexBuilder_.getMessage();
      }
    }
    /**
     * <code>.google.protobuf.Int32Value group_index = 3;</code>
     */
    public Builder setGroupIndex(com.google.protobuf.Int32Value value) {
      if (groupIndexBuilder_ == null) {
        if (value == null) {
          throw new NullPointerException();
        }
        groupIndex_ = value;
        onChanged();
      } else {
        groupIndexBuilder_.setMessage(value);
      }

      return this;
    }
    /**
     * <code>.google.protobuf.Int32Value group_index = 3;</code>
     */
    public Builder setGroupIndex(
        com.google.protobuf.Int32Value.Builder builderForValue) {
      if (groupIndexBuilder_ == null) {
        groupIndex_ = builderForValue.build();
        onChanged();
      } else {
        groupIndexBuilder_.setMessage(builderForValue.build());
      }

      return this;
    }
    /**
     * <code>.google.protobuf.Int32Value group_index = 3;</code>
     */
    public Builder mergeGroupIndex(com.google.protobuf.Int32Value value) {
      if (groupIndexBuilder_ == null) {
        if (groupIndex_ != null) {
          groupIndex_ =
            com.google.protobuf.Int32Value.newBuilder(groupIndex_).mergeFrom(value).buildPartial();
        } else {
          groupIndex_ = value;
        }
        onChanged();
      } else {
        groupIndexBuilder_.mergeFrom(value);
      }

      return this;
    }
    /**
     * <code>.google.protobuf.Int32Value group_index = 3;</code>
     */
    public Builder clearGroupIndex() {
      if (groupIndexBuilder_ == null) {
        groupIndex_ = null;
        onChanged();
      } else {
        groupIndex_ = null;
        groupIndexBuilder_ = null;
      }

      return this;
    }
    /**
     * <code>.google.protobuf.Int32Value group_index = 3;</code>
     */
    public com.google.protobuf.Int32Value.Builder getGroupIndexBuilder() {
      
      onChanged();
      return getGroupIndexFieldBuilder().getBuilder();
    }
    /**
     * <code>.google.protobuf.Int32Value group_index = 3;</code>
     */
    public com.google.protobuf.Int32ValueOrBuilder getGroupIndexOrBuilder() {
      if (groupIndexBuilder_ != null) {
        return groupIndexBuilder_.getMessageOrBuilder();
      } else {
        return groupIndex_ == null ?
            com.google.protobuf.Int32Value.getDefaultInstance() : groupIndex_;
      }
    }
    /**
     * <code>.google.protobuf.Int32Value group_index = 3;</code>
     */
    private com.google.protobuf.SingleFieldBuilderV3<
        com.google.protobuf.Int32Value, com.google.protobuf.Int32Value.Builder, com.google.protobuf.Int32ValueOrBuilder> 
        getGroupIndexFieldBuilder() {
      if (groupIndexBuilder_ == null) {
        groupIndexBuilder_ = new com.google.protobuf.SingleFieldBuilderV3<
            com.google.protobuf.Int32Value, com.google.protobuf.Int32Value.Builder, com.google.protobuf.Int32ValueOrBuilder>(
                getGroupIndex(),
                getParentForChildren(),
                isClean());
        groupIndex_ = null;
      }
      return groupIndexBuilder_;
    }
    @java.lang.Override
    public final Builder setUnknownFields(
        final com.google.protobuf.UnknownFieldSet unknownFields) {
      return super.setUnknownFields(unknownFields);
    }

    @java.lang.Override
    public final Builder mergeUnknownFields(
        final com.google.protobuf.UnknownFieldSet unknownFields) {
      return super.mergeUnknownFields(unknownFields);
    }


    // @@protoc_insertion_point(builder_scope:im.turms.proto.CreateRelationshipRequest)
  }

  // @@protoc_insertion_point(class_scope:im.turms.proto.CreateRelationshipRequest)
  private static final im.turms.turms.pojo.request.CreateRelationshipRequest DEFAULT_INSTANCE;
  static {
    DEFAULT_INSTANCE = new im.turms.turms.pojo.request.CreateRelationshipRequest();
  }

  public static im.turms.turms.pojo.request.CreateRelationshipRequest getDefaultInstance() {
    return DEFAULT_INSTANCE;
  }

  private static final com.google.protobuf.Parser<CreateRelationshipRequest>
      PARSER = new com.google.protobuf.AbstractParser<CreateRelationshipRequest>() {
    @java.lang.Override
    public CreateRelationshipRequest parsePartialFrom(
        com.google.protobuf.CodedInputStream input,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws com.google.protobuf.InvalidProtocolBufferException {
      return new CreateRelationshipRequest(input, extensionRegistry);
    }
  };

  public static com.google.protobuf.Parser<CreateRelationshipRequest> parser() {
    return PARSER;
  }

  @java.lang.Override
  public com.google.protobuf.Parser<CreateRelationshipRequest> getParserForType() {
    return PARSER;
  }

  @java.lang.Override
  public im.turms.turms.pojo.request.CreateRelationshipRequest getDefaultInstanceForType() {
    return DEFAULT_INSTANCE;
  }

}

