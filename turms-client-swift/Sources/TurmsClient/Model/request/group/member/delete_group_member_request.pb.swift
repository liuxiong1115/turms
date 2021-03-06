// DO NOT EDIT.
//
// Generated by the Swift generator plugin for the protocol buffer compiler.
// Source: request/group/member/delete_group_member_request.proto
//
// For information on using the generated types, please see the documentation:
//   https://github.com/apple/swift-protobuf/

import Foundation
import SwiftProtobuf

// If the compiler emits an error on this type, it is because this file
// was generated by a version of the `protoc` Swift plug-in that is
// incompatible with the version of SwiftProtobuf to which you are linking.
// Please ensure that your are building against the same version of the API
// that was used to generate this file.
fileprivate struct _GeneratedWithProtocGenSwiftVersion: SwiftProtobuf.ProtobufAPIVersionCheck {
  struct _2: SwiftProtobuf.ProtobufAPIVersion_2 {}
  typealias Version = _2
}

public struct DeleteGroupMemberRequest {
  // SwiftProtobuf.Message conformance is added in an extension below. See the
  // `Message` and `Message+*Additions` files in the SwiftProtobuf library for
  // methods supported on all messages.

  public var groupID: Int64 {
    get {return _storage._groupID}
    set {_uniqueStorage()._groupID = newValue}
  }

  public var memberID: Int64 {
    get {return _storage._memberID}
    set {_uniqueStorage()._memberID = newValue}
  }

  public var successorID: SwiftProtobuf.Google_Protobuf_Int64Value {
    get {return _storage._successorID ?? SwiftProtobuf.Google_Protobuf_Int64Value()}
    set {_uniqueStorage()._successorID = newValue}
  }
  /// Returns true if `successorID` has been explicitly set.
  public var hasSuccessorID: Bool {return _storage._successorID != nil}
  /// Clears the value of `successorID`. Subsequent reads from it will return its default value.
  public mutating func clearSuccessorID() {_uniqueStorage()._successorID = nil}

  public var quitAfterTransfer: SwiftProtobuf.Google_Protobuf_BoolValue {
    get {return _storage._quitAfterTransfer ?? SwiftProtobuf.Google_Protobuf_BoolValue()}
    set {_uniqueStorage()._quitAfterTransfer = newValue}
  }
  /// Returns true if `quitAfterTransfer` has been explicitly set.
  public var hasQuitAfterTransfer: Bool {return _storage._quitAfterTransfer != nil}
  /// Clears the value of `quitAfterTransfer`. Subsequent reads from it will return its default value.
  public mutating func clearQuitAfterTransfer() {_uniqueStorage()._quitAfterTransfer = nil}

  public var unknownFields = SwiftProtobuf.UnknownStorage()

  public init() {}

  fileprivate var _storage = _StorageClass.defaultInstance
}

// MARK: - Code below here is support for the SwiftProtobuf runtime.

fileprivate let _protobuf_package = "im.turms.proto"

extension DeleteGroupMemberRequest: SwiftProtobuf.Message, SwiftProtobuf._MessageImplementationBase, SwiftProtobuf._ProtoNameProviding {
  public static let protoMessageName: String = _protobuf_package + ".DeleteGroupMemberRequest"
  public static let _protobuf_nameMap: SwiftProtobuf._NameMap = [
    1: .standard(proto: "group_id"),
    2: .standard(proto: "member_id"),
    3: .standard(proto: "successor_id"),
    4: .standard(proto: "quit_after_transfer"),
  ]

  fileprivate class _StorageClass {
    var _groupID: Int64 = 0
    var _memberID: Int64 = 0
    var _successorID: SwiftProtobuf.Google_Protobuf_Int64Value? = nil
    var _quitAfterTransfer: SwiftProtobuf.Google_Protobuf_BoolValue? = nil

    static let defaultInstance = _StorageClass()

    private init() {}

    init(copying source: _StorageClass) {
      _groupID = source._groupID
      _memberID = source._memberID
      _successorID = source._successorID
      _quitAfterTransfer = source._quitAfterTransfer
    }
  }

  fileprivate mutating func _uniqueStorage() -> _StorageClass {
    if !isKnownUniquelyReferenced(&_storage) {
      _storage = _StorageClass(copying: _storage)
    }
    return _storage
  }

  public mutating func decodeMessage<D: SwiftProtobuf.Decoder>(decoder: inout D) throws {
    _ = _uniqueStorage()
    try withExtendedLifetime(_storage) { (_storage: _StorageClass) in
      while let fieldNumber = try decoder.nextFieldNumber() {
        switch fieldNumber {
        case 1: try decoder.decodeSingularInt64Field(value: &_storage._groupID)
        case 2: try decoder.decodeSingularInt64Field(value: &_storage._memberID)
        case 3: try decoder.decodeSingularMessageField(value: &_storage._successorID)
        case 4: try decoder.decodeSingularMessageField(value: &_storage._quitAfterTransfer)
        default: break
        }
      }
    }
  }

  public func traverse<V: SwiftProtobuf.Visitor>(visitor: inout V) throws {
    try withExtendedLifetime(_storage) { (_storage: _StorageClass) in
      if _storage._groupID != 0 {
        try visitor.visitSingularInt64Field(value: _storage._groupID, fieldNumber: 1)
      }
      if _storage._memberID != 0 {
        try visitor.visitSingularInt64Field(value: _storage._memberID, fieldNumber: 2)
      }
      if let v = _storage._successorID {
        try visitor.visitSingularMessageField(value: v, fieldNumber: 3)
      }
      if let v = _storage._quitAfterTransfer {
        try visitor.visitSingularMessageField(value: v, fieldNumber: 4)
      }
    }
    try unknownFields.traverse(visitor: &visitor)
  }

  public static func ==(lhs: DeleteGroupMemberRequest, rhs: DeleteGroupMemberRequest) -> Bool {
    if lhs._storage !== rhs._storage {
      let storagesAreEqual: Bool = withExtendedLifetime((lhs._storage, rhs._storage)) { (_args: (_StorageClass, _StorageClass)) in
        let _storage = _args.0
        let rhs_storage = _args.1
        if _storage._groupID != rhs_storage._groupID {return false}
        if _storage._memberID != rhs_storage._memberID {return false}
        if _storage._successorID != rhs_storage._successorID {return false}
        if _storage._quitAfterTransfer != rhs_storage._quitAfterTransfer {return false}
        return true
      }
      if !storagesAreEqual {return false}
    }
    if lhs.unknownFields != rhs.unknownFields {return false}
    return true
  }
}
