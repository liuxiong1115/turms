// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: response/message/messages_with_total_list.proto

package im.turms.turms.pojo.response;

/**
 * Protobuf type {@code im.turms.proto.MessagesWithTotalList}
 */
public  final class MessagesWithTotalList extends
    com.google.protobuf.GeneratedMessageV3 implements
    // @@protoc_insertion_point(message_implements:im.turms.proto.MessagesWithTotalList)
    MessagesWithTotalListOrBuilder {
private static final long serialVersionUID = 0L;
  // Use MessagesWithTotalList.newBuilder() to construct.
  private MessagesWithTotalList(com.google.protobuf.GeneratedMessageV3.Builder<?> builder) {
    super(builder);
  }
  private MessagesWithTotalList() {
    messagesWithTotalList_ = java.util.Collections.emptyList();
  }

  @java.lang.Override
  @SuppressWarnings({"unused"})
  protected java.lang.Object newInstance(
      UnusedPrivateParameter unused) {
    return new MessagesWithTotalList();
  }

  @java.lang.Override
  public final com.google.protobuf.UnknownFieldSet
  getUnknownFields() {
    return this.unknownFields;
  }
  private MessagesWithTotalList(
      com.google.protobuf.CodedInputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    this();
    if (extensionRegistry == null) {
      throw new java.lang.NullPointerException();
    }
    int mutable_bitField0_ = 0;
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
          case 10: {
            if (!((mutable_bitField0_ & 0x00000001) != 0)) {
              messagesWithTotalList_ = new java.util.ArrayList<im.turms.turms.pojo.dto.MessagesWithTotal>();
              mutable_bitField0_ |= 0x00000001;
            }
            messagesWithTotalList_.add(
                input.readMessage(im.turms.turms.pojo.dto.MessagesWithTotal.parser(), extensionRegistry));
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
      if (((mutable_bitField0_ & 0x00000001) != 0)) {
        messagesWithTotalList_ = java.util.Collections.unmodifiableList(messagesWithTotalList_);
      }
      this.unknownFields = unknownFields.build();
      makeExtensionsImmutable();
    }
  }
  public static final com.google.protobuf.Descriptors.Descriptor
      getDescriptor() {
    return im.turms.turms.pojo.response.MessagesWithTotalListOuterClass.internal_static_im_turms_proto_MessagesWithTotalList_descriptor;
  }

  @java.lang.Override
  protected com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
      internalGetFieldAccessorTable() {
    return im.turms.turms.pojo.response.MessagesWithTotalListOuterClass.internal_static_im_turms_proto_MessagesWithTotalList_fieldAccessorTable
        .ensureFieldAccessorsInitialized(
            im.turms.turms.pojo.response.MessagesWithTotalList.class, im.turms.turms.pojo.response.MessagesWithTotalList.Builder.class);
  }

  public static final int MESSAGES_WITH_TOTAL_LIST_FIELD_NUMBER = 1;
  private java.util.List<im.turms.turms.pojo.dto.MessagesWithTotal> messagesWithTotalList_;
  /**
   * <code>repeated .im.turms.proto.MessagesWithTotal messages_with_total_list = 1;</code>
   */
  public java.util.List<im.turms.turms.pojo.dto.MessagesWithTotal> getMessagesWithTotalListList() {
    return messagesWithTotalList_;
  }
  /**
   * <code>repeated .im.turms.proto.MessagesWithTotal messages_with_total_list = 1;</code>
   */
  public java.util.List<? extends im.turms.turms.pojo.dto.MessagesWithTotalOrBuilder>
      getMessagesWithTotalListOrBuilderList() {
    return messagesWithTotalList_;
  }
  /**
   * <code>repeated .im.turms.proto.MessagesWithTotal messages_with_total_list = 1;</code>
   */
  public int getMessagesWithTotalListCount() {
    return messagesWithTotalList_.size();
  }
  /**
   * <code>repeated .im.turms.proto.MessagesWithTotal messages_with_total_list = 1;</code>
   */
  public im.turms.turms.pojo.dto.MessagesWithTotal getMessagesWithTotalList(int index) {
    return messagesWithTotalList_.get(index);
  }
  /**
   * <code>repeated .im.turms.proto.MessagesWithTotal messages_with_total_list = 1;</code>
   */
  public im.turms.turms.pojo.dto.MessagesWithTotalOrBuilder getMessagesWithTotalListOrBuilder(
      int index) {
    return messagesWithTotalList_.get(index);
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
    for (int i = 0; i < messagesWithTotalList_.size(); i++) {
      output.writeMessage(1, messagesWithTotalList_.get(i));
    }
    unknownFields.writeTo(output);
  }

  @java.lang.Override
  public int getSerializedSize() {
    int size = memoizedSize;
    if (size != -1) return size;

    size = 0;
    for (int i = 0; i < messagesWithTotalList_.size(); i++) {
      size += com.google.protobuf.CodedOutputStream
        .computeMessageSize(1, messagesWithTotalList_.get(i));
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
    if (!(obj instanceof im.turms.turms.pojo.response.MessagesWithTotalList)) {
      return super.equals(obj);
    }
    im.turms.turms.pojo.response.MessagesWithTotalList other = (im.turms.turms.pojo.response.MessagesWithTotalList) obj;

    if (!getMessagesWithTotalListList()
        .equals(other.getMessagesWithTotalListList())) return false;
    return unknownFields.equals(other.unknownFields);
  }

  @java.lang.Override
  public int hashCode() {
    if (memoizedHashCode != 0) {
      return memoizedHashCode;
    }
    int hash = 41;
    hash = (19 * hash) + getDescriptor().hashCode();
    if (getMessagesWithTotalListCount() > 0) {
      hash = (37 * hash) + MESSAGES_WITH_TOTAL_LIST_FIELD_NUMBER;
      hash = (53 * hash) + getMessagesWithTotalListList().hashCode();
    }
    hash = (29 * hash) + unknownFields.hashCode();
    memoizedHashCode = hash;
    return hash;
  }

  public static im.turms.turms.pojo.response.MessagesWithTotalList parseFrom(
      java.nio.ByteBuffer data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data);
  }
  public static im.turms.turms.pojo.response.MessagesWithTotalList parseFrom(
      java.nio.ByteBuffer data,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data, extensionRegistry);
  }
  public static im.turms.turms.pojo.response.MessagesWithTotalList parseFrom(
      com.google.protobuf.ByteString data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data);
  }
  public static im.turms.turms.pojo.response.MessagesWithTotalList parseFrom(
      com.google.protobuf.ByteString data,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data, extensionRegistry);
  }
  public static im.turms.turms.pojo.response.MessagesWithTotalList parseFrom(byte[] data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data);
  }
  public static im.turms.turms.pojo.response.MessagesWithTotalList parseFrom(
      byte[] data,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data, extensionRegistry);
  }
  public static im.turms.turms.pojo.response.MessagesWithTotalList parseFrom(java.io.InputStream input)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3
        .parseWithIOException(PARSER, input);
  }
  public static im.turms.turms.pojo.response.MessagesWithTotalList parseFrom(
      java.io.InputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3
        .parseWithIOException(PARSER, input, extensionRegistry);
  }
  public static im.turms.turms.pojo.response.MessagesWithTotalList parseDelimitedFrom(java.io.InputStream input)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3
        .parseDelimitedWithIOException(PARSER, input);
  }
  public static im.turms.turms.pojo.response.MessagesWithTotalList parseDelimitedFrom(
      java.io.InputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3
        .parseDelimitedWithIOException(PARSER, input, extensionRegistry);
  }
  public static im.turms.turms.pojo.response.MessagesWithTotalList parseFrom(
      com.google.protobuf.CodedInputStream input)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3
        .parseWithIOException(PARSER, input);
  }
  public static im.turms.turms.pojo.response.MessagesWithTotalList parseFrom(
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
  public static Builder newBuilder(im.turms.turms.pojo.response.MessagesWithTotalList prototype) {
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
   * Protobuf type {@code im.turms.proto.MessagesWithTotalList}
   */
  public static final class Builder extends
      com.google.protobuf.GeneratedMessageV3.Builder<Builder> implements
      // @@protoc_insertion_point(builder_implements:im.turms.proto.MessagesWithTotalList)
      im.turms.turms.pojo.response.MessagesWithTotalListOrBuilder {
    public static final com.google.protobuf.Descriptors.Descriptor
        getDescriptor() {
      return im.turms.turms.pojo.response.MessagesWithTotalListOuterClass.internal_static_im_turms_proto_MessagesWithTotalList_descriptor;
    }

    @java.lang.Override
    protected com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
        internalGetFieldAccessorTable() {
      return im.turms.turms.pojo.response.MessagesWithTotalListOuterClass.internal_static_im_turms_proto_MessagesWithTotalList_fieldAccessorTable
          .ensureFieldAccessorsInitialized(
              im.turms.turms.pojo.response.MessagesWithTotalList.class, im.turms.turms.pojo.response.MessagesWithTotalList.Builder.class);
    }

    // Construct using im.turms.turms.pojo.response.MessagesWithTotalList.newBuilder()
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
        getMessagesWithTotalListFieldBuilder();
      }
    }
    @java.lang.Override
    public Builder clear() {
      super.clear();
      if (messagesWithTotalListBuilder_ == null) {
        messagesWithTotalList_ = java.util.Collections.emptyList();
        bitField0_ = (bitField0_ & ~0x00000001);
      } else {
        messagesWithTotalListBuilder_.clear();
      }
      return this;
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.Descriptor
        getDescriptorForType() {
      return im.turms.turms.pojo.response.MessagesWithTotalListOuterClass.internal_static_im_turms_proto_MessagesWithTotalList_descriptor;
    }

    @java.lang.Override
    public im.turms.turms.pojo.response.MessagesWithTotalList getDefaultInstanceForType() {
      return im.turms.turms.pojo.response.MessagesWithTotalList.getDefaultInstance();
    }

    @java.lang.Override
    public im.turms.turms.pojo.response.MessagesWithTotalList build() {
      im.turms.turms.pojo.response.MessagesWithTotalList result = buildPartial();
      if (!result.isInitialized()) {
        throw newUninitializedMessageException(result);
      }
      return result;
    }

    @java.lang.Override
    public im.turms.turms.pojo.response.MessagesWithTotalList buildPartial() {
      im.turms.turms.pojo.response.MessagesWithTotalList result = new im.turms.turms.pojo.response.MessagesWithTotalList(this);
      int from_bitField0_ = bitField0_;
      if (messagesWithTotalListBuilder_ == null) {
        if (((bitField0_ & 0x00000001) != 0)) {
          messagesWithTotalList_ = java.util.Collections.unmodifiableList(messagesWithTotalList_);
          bitField0_ = (bitField0_ & ~0x00000001);
        }
        result.messagesWithTotalList_ = messagesWithTotalList_;
      } else {
        result.messagesWithTotalList_ = messagesWithTotalListBuilder_.build();
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
      if (other instanceof im.turms.turms.pojo.response.MessagesWithTotalList) {
        return mergeFrom((im.turms.turms.pojo.response.MessagesWithTotalList)other);
      } else {
        super.mergeFrom(other);
        return this;
      }
    }

    public Builder mergeFrom(im.turms.turms.pojo.response.MessagesWithTotalList other) {
      if (other == im.turms.turms.pojo.response.MessagesWithTotalList.getDefaultInstance()) return this;
      if (messagesWithTotalListBuilder_ == null) {
        if (!other.messagesWithTotalList_.isEmpty()) {
          if (messagesWithTotalList_.isEmpty()) {
            messagesWithTotalList_ = other.messagesWithTotalList_;
            bitField0_ = (bitField0_ & ~0x00000001);
          } else {
            ensureMessagesWithTotalListIsMutable();
            messagesWithTotalList_.addAll(other.messagesWithTotalList_);
          }
          onChanged();
        }
      } else {
        if (!other.messagesWithTotalList_.isEmpty()) {
          if (messagesWithTotalListBuilder_.isEmpty()) {
            messagesWithTotalListBuilder_.dispose();
            messagesWithTotalListBuilder_ = null;
            messagesWithTotalList_ = other.messagesWithTotalList_;
            bitField0_ = (bitField0_ & ~0x00000001);
            messagesWithTotalListBuilder_ =
              com.google.protobuf.GeneratedMessageV3.alwaysUseFieldBuilders ?
                 getMessagesWithTotalListFieldBuilder() : null;
          } else {
            messagesWithTotalListBuilder_.addAllMessages(other.messagesWithTotalList_);
          }
        }
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
      im.turms.turms.pojo.response.MessagesWithTotalList parsedMessage = null;
      try {
        parsedMessage = PARSER.parsePartialFrom(input, extensionRegistry);
      } catch (com.google.protobuf.InvalidProtocolBufferException e) {
        parsedMessage = (im.turms.turms.pojo.response.MessagesWithTotalList) e.getUnfinishedMessage();
        throw e.unwrapIOException();
      } finally {
        if (parsedMessage != null) {
          mergeFrom(parsedMessage);
        }
      }
      return this;
    }
    private int bitField0_;

    private java.util.List<im.turms.turms.pojo.dto.MessagesWithTotal> messagesWithTotalList_ =
      java.util.Collections.emptyList();
    private void ensureMessagesWithTotalListIsMutable() {
      if (!((bitField0_ & 0x00000001) != 0)) {
        messagesWithTotalList_ = new java.util.ArrayList<im.turms.turms.pojo.dto.MessagesWithTotal>(messagesWithTotalList_);
        bitField0_ |= 0x00000001;
       }
    }

    private com.google.protobuf.RepeatedFieldBuilderV3<
        im.turms.turms.pojo.dto.MessagesWithTotal, im.turms.turms.pojo.dto.MessagesWithTotal.Builder, im.turms.turms.pojo.dto.MessagesWithTotalOrBuilder> messagesWithTotalListBuilder_;

    /**
     * <code>repeated .im.turms.proto.MessagesWithTotal messages_with_total_list = 1;</code>
     */
    public java.util.List<im.turms.turms.pojo.dto.MessagesWithTotal> getMessagesWithTotalListList() {
      if (messagesWithTotalListBuilder_ == null) {
        return java.util.Collections.unmodifiableList(messagesWithTotalList_);
      } else {
        return messagesWithTotalListBuilder_.getMessageList();
      }
    }
    /**
     * <code>repeated .im.turms.proto.MessagesWithTotal messages_with_total_list = 1;</code>
     */
    public int getMessagesWithTotalListCount() {
      if (messagesWithTotalListBuilder_ == null) {
        return messagesWithTotalList_.size();
      } else {
        return messagesWithTotalListBuilder_.getCount();
      }
    }
    /**
     * <code>repeated .im.turms.proto.MessagesWithTotal messages_with_total_list = 1;</code>
     */
    public im.turms.turms.pojo.dto.MessagesWithTotal getMessagesWithTotalList(int index) {
      if (messagesWithTotalListBuilder_ == null) {
        return messagesWithTotalList_.get(index);
      } else {
        return messagesWithTotalListBuilder_.getMessage(index);
      }
    }
    /**
     * <code>repeated .im.turms.proto.MessagesWithTotal messages_with_total_list = 1;</code>
     */
    public Builder setMessagesWithTotalList(
        int index, im.turms.turms.pojo.dto.MessagesWithTotal value) {
      if (messagesWithTotalListBuilder_ == null) {
        if (value == null) {
          throw new NullPointerException();
        }
        ensureMessagesWithTotalListIsMutable();
        messagesWithTotalList_.set(index, value);
        onChanged();
      } else {
        messagesWithTotalListBuilder_.setMessage(index, value);
      }
      return this;
    }
    /**
     * <code>repeated .im.turms.proto.MessagesWithTotal messages_with_total_list = 1;</code>
     */
    public Builder setMessagesWithTotalList(
        int index, im.turms.turms.pojo.dto.MessagesWithTotal.Builder builderForValue) {
      if (messagesWithTotalListBuilder_ == null) {
        ensureMessagesWithTotalListIsMutable();
        messagesWithTotalList_.set(index, builderForValue.build());
        onChanged();
      } else {
        messagesWithTotalListBuilder_.setMessage(index, builderForValue.build());
      }
      return this;
    }
    /**
     * <code>repeated .im.turms.proto.MessagesWithTotal messages_with_total_list = 1;</code>
     */
    public Builder addMessagesWithTotalList(im.turms.turms.pojo.dto.MessagesWithTotal value) {
      if (messagesWithTotalListBuilder_ == null) {
        if (value == null) {
          throw new NullPointerException();
        }
        ensureMessagesWithTotalListIsMutable();
        messagesWithTotalList_.add(value);
        onChanged();
      } else {
        messagesWithTotalListBuilder_.addMessage(value);
      }
      return this;
    }
    /**
     * <code>repeated .im.turms.proto.MessagesWithTotal messages_with_total_list = 1;</code>
     */
    public Builder addMessagesWithTotalList(
        int index, im.turms.turms.pojo.dto.MessagesWithTotal value) {
      if (messagesWithTotalListBuilder_ == null) {
        if (value == null) {
          throw new NullPointerException();
        }
        ensureMessagesWithTotalListIsMutable();
        messagesWithTotalList_.add(index, value);
        onChanged();
      } else {
        messagesWithTotalListBuilder_.addMessage(index, value);
      }
      return this;
    }
    /**
     * <code>repeated .im.turms.proto.MessagesWithTotal messages_with_total_list = 1;</code>
     */
    public Builder addMessagesWithTotalList(
        im.turms.turms.pojo.dto.MessagesWithTotal.Builder builderForValue) {
      if (messagesWithTotalListBuilder_ == null) {
        ensureMessagesWithTotalListIsMutable();
        messagesWithTotalList_.add(builderForValue.build());
        onChanged();
      } else {
        messagesWithTotalListBuilder_.addMessage(builderForValue.build());
      }
      return this;
    }
    /**
     * <code>repeated .im.turms.proto.MessagesWithTotal messages_with_total_list = 1;</code>
     */
    public Builder addMessagesWithTotalList(
        int index, im.turms.turms.pojo.dto.MessagesWithTotal.Builder builderForValue) {
      if (messagesWithTotalListBuilder_ == null) {
        ensureMessagesWithTotalListIsMutable();
        messagesWithTotalList_.add(index, builderForValue.build());
        onChanged();
      } else {
        messagesWithTotalListBuilder_.addMessage(index, builderForValue.build());
      }
      return this;
    }
    /**
     * <code>repeated .im.turms.proto.MessagesWithTotal messages_with_total_list = 1;</code>
     */
    public Builder addAllMessagesWithTotalList(
        java.lang.Iterable<? extends im.turms.turms.pojo.dto.MessagesWithTotal> values) {
      if (messagesWithTotalListBuilder_ == null) {
        ensureMessagesWithTotalListIsMutable();
        com.google.protobuf.AbstractMessageLite.Builder.addAll(
            values, messagesWithTotalList_);
        onChanged();
      } else {
        messagesWithTotalListBuilder_.addAllMessages(values);
      }
      return this;
    }
    /**
     * <code>repeated .im.turms.proto.MessagesWithTotal messages_with_total_list = 1;</code>
     */
    public Builder clearMessagesWithTotalList() {
      if (messagesWithTotalListBuilder_ == null) {
        messagesWithTotalList_ = java.util.Collections.emptyList();
        bitField0_ = (bitField0_ & ~0x00000001);
        onChanged();
      } else {
        messagesWithTotalListBuilder_.clear();
      }
      return this;
    }
    /**
     * <code>repeated .im.turms.proto.MessagesWithTotal messages_with_total_list = 1;</code>
     */
    public Builder removeMessagesWithTotalList(int index) {
      if (messagesWithTotalListBuilder_ == null) {
        ensureMessagesWithTotalListIsMutable();
        messagesWithTotalList_.remove(index);
        onChanged();
      } else {
        messagesWithTotalListBuilder_.remove(index);
      }
      return this;
    }
    /**
     * <code>repeated .im.turms.proto.MessagesWithTotal messages_with_total_list = 1;</code>
     */
    public im.turms.turms.pojo.dto.MessagesWithTotal.Builder getMessagesWithTotalListBuilder(
        int index) {
      return getMessagesWithTotalListFieldBuilder().getBuilder(index);
    }
    /**
     * <code>repeated .im.turms.proto.MessagesWithTotal messages_with_total_list = 1;</code>
     */
    public im.turms.turms.pojo.dto.MessagesWithTotalOrBuilder getMessagesWithTotalListOrBuilder(
        int index) {
      if (messagesWithTotalListBuilder_ == null) {
        return messagesWithTotalList_.get(index);  } else {
        return messagesWithTotalListBuilder_.getMessageOrBuilder(index);
      }
    }
    /**
     * <code>repeated .im.turms.proto.MessagesWithTotal messages_with_total_list = 1;</code>
     */
    public java.util.List<? extends im.turms.turms.pojo.dto.MessagesWithTotalOrBuilder>
         getMessagesWithTotalListOrBuilderList() {
      if (messagesWithTotalListBuilder_ != null) {
        return messagesWithTotalListBuilder_.getMessageOrBuilderList();
      } else {
        return java.util.Collections.unmodifiableList(messagesWithTotalList_);
      }
    }
    /**
     * <code>repeated .im.turms.proto.MessagesWithTotal messages_with_total_list = 1;</code>
     */
    public im.turms.turms.pojo.dto.MessagesWithTotal.Builder addMessagesWithTotalListBuilder() {
      return getMessagesWithTotalListFieldBuilder().addBuilder(
          im.turms.turms.pojo.dto.MessagesWithTotal.getDefaultInstance());
    }
    /**
     * <code>repeated .im.turms.proto.MessagesWithTotal messages_with_total_list = 1;</code>
     */
    public im.turms.turms.pojo.dto.MessagesWithTotal.Builder addMessagesWithTotalListBuilder(
        int index) {
      return getMessagesWithTotalListFieldBuilder().addBuilder(
          index, im.turms.turms.pojo.dto.MessagesWithTotal.getDefaultInstance());
    }
    /**
     * <code>repeated .im.turms.proto.MessagesWithTotal messages_with_total_list = 1;</code>
     */
    public java.util.List<im.turms.turms.pojo.dto.MessagesWithTotal.Builder>
         getMessagesWithTotalListBuilderList() {
      return getMessagesWithTotalListFieldBuilder().getBuilderList();
    }
    private com.google.protobuf.RepeatedFieldBuilderV3<
        im.turms.turms.pojo.dto.MessagesWithTotal, im.turms.turms.pojo.dto.MessagesWithTotal.Builder, im.turms.turms.pojo.dto.MessagesWithTotalOrBuilder>
        getMessagesWithTotalListFieldBuilder() {
      if (messagesWithTotalListBuilder_ == null) {
        messagesWithTotalListBuilder_ = new com.google.protobuf.RepeatedFieldBuilderV3<
            im.turms.turms.pojo.dto.MessagesWithTotal, im.turms.turms.pojo.dto.MessagesWithTotal.Builder, im.turms.turms.pojo.dto.MessagesWithTotalOrBuilder>(
                messagesWithTotalList_,
                ((bitField0_ & 0x00000001) != 0),
                getParentForChildren(),
                isClean());
        messagesWithTotalList_ = null;
      }
      return messagesWithTotalListBuilder_;
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


    // @@protoc_insertion_point(builder_scope:im.turms.proto.MessagesWithTotalList)
  }

  // @@protoc_insertion_point(class_scope:im.turms.proto.MessagesWithTotalList)
  private static final im.turms.turms.pojo.response.MessagesWithTotalList DEFAULT_INSTANCE;
  static {
    DEFAULT_INSTANCE = new im.turms.turms.pojo.response.MessagesWithTotalList();
  }

  public static im.turms.turms.pojo.response.MessagesWithTotalList getDefaultInstance() {
    return DEFAULT_INSTANCE;
  }

  private static final com.google.protobuf.Parser<MessagesWithTotalList>
      PARSER = new com.google.protobuf.AbstractParser<MessagesWithTotalList>() {
    @java.lang.Override
    public MessagesWithTotalList parsePartialFrom(
        com.google.protobuf.CodedInputStream input,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws com.google.protobuf.InvalidProtocolBufferException {
      return new MessagesWithTotalList(input, extensionRegistry);
    }
  };

  public static com.google.protobuf.Parser<MessagesWithTotalList> parser() {
    return PARSER;
  }

  @java.lang.Override
  public com.google.protobuf.Parser<MessagesWithTotalList> getParserForType() {
    return PARSER;
  }

  @java.lang.Override
  public im.turms.turms.pojo.response.MessagesWithTotalList getDefaultInstanceForType() {
    return DEFAULT_INSTANCE;
  }

}
