// DO NOT EDIT.
//
// Generated by the Swift generator plugin for the protocol buffer compiler.
// Source: model/message/message_status.proto
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

public struct MessageStatus {
  // SwiftProtobuf.Message conformance is added in an extension below. See the
  // `Message` and `Message+*Additions` files in the SwiftProtobuf library for
  // methods supported on all messages.

  public var messageID: SwiftProtobuf.Google_Protobuf_Int64Value {
    get {return _storage._messageID ?? SwiftProtobuf.Google_Protobuf_Int64Value()}
    set {_uniqueStorage()._messageID = newValue}
  }
  /// Returns true if `messageID` has been explicitly set.
  public var hasMessageID: Bool {return _storage._messageID != nil}
  /// Clears the value of `messageID`. Subsequent reads from it will return its default value.
  public mutating func clearMessageID() {_uniqueStorage()._messageID = nil}

  public var toUserID: SwiftProtobuf.Google_Protobuf_Int64Value {
    get {return _storage._toUserID ?? SwiftProtobuf.Google_Protobuf_Int64Value()}
    set {_uniqueStorage()._toUserID = newValue}
  }
  /// Returns true if `toUserID` has been explicitly set.
  public var hasToUserID: Bool {return _storage._toUserID != nil}
  /// Clears the value of `toUserID`. Subsequent reads from it will return its default value.
  public mutating func clearToUserID() {_uniqueStorage()._toUserID = nil}

  public var deliveryStatus: MessageDeliveryStatus {
    get {return _storage._deliveryStatus}
    set {_uniqueStorage()._deliveryStatus = newValue}
  }

  public var receptionDate: SwiftProtobuf.Google_Protobuf_Int64Value {
    get {return _storage._receptionDate ?? SwiftProtobuf.Google_Protobuf_Int64Value()}
    set {_uniqueStorage()._receptionDate = newValue}
  }
  /// Returns true if `receptionDate` has been explicitly set.
  public var hasReceptionDate: Bool {return _storage._receptionDate != nil}
  /// Clears the value of `receptionDate`. Subsequent reads from it will return its default value.
  public mutating func clearReceptionDate() {_uniqueStorage()._receptionDate = nil}

  public var readDate: SwiftProtobuf.Google_Protobuf_Int64Value {
    get {return _storage._readDate ?? SwiftProtobuf.Google_Protobuf_Int64Value()}
    set {_uniqueStorage()._readDate = newValue}
  }
  /// Returns true if `readDate` has been explicitly set.
  public var hasReadDate: Bool {return _storage._readDate != nil}
  /// Clears the value of `readDate`. Subsequent reads from it will return its default value.
  public mutating func clearReadDate() {_uniqueStorage()._readDate = nil}

  public var recallDate: SwiftProtobuf.Google_Protobuf_Int64Value {
    get {return _storage._recallDate ?? SwiftProtobuf.Google_Protobuf_Int64Value()}
    set {_uniqueStorage()._recallDate = newValue}
  }
  /// Returns true if `recallDate` has been explicitly set.
  public var hasRecallDate: Bool {return _storage._recallDate != nil}
  /// Clears the value of `recallDate`. Subsequent reads from it will return its default value.
  public mutating func clearRecallDate() {_uniqueStorage()._recallDate = nil}

  public var unknownFields = SwiftProtobuf.UnknownStorage()

  public init() {}

  fileprivate var _storage = _StorageClass.defaultInstance
}

// MARK: - Code below here is support for the SwiftProtobuf runtime.

fileprivate let _protobuf_package = "im.turms.proto"

extension MessageStatus: SwiftProtobuf.Message, SwiftProtobuf._MessageImplementationBase, SwiftProtobuf._ProtoNameProviding {
  public static let protoMessageName: String = _protobuf_package + ".MessageStatus"
  public static let _protobuf_nameMap: SwiftProtobuf._NameMap = [
    1: .standard(proto: "message_id"),
    2: .standard(proto: "to_user_id"),
    3: .standard(proto: "delivery_status"),
    4: .standard(proto: "reception_date"),
    5: .standard(proto: "read_date"),
    6: .standard(proto: "recall_date"),
  ]

  fileprivate class _StorageClass {
    var _messageID: SwiftProtobuf.Google_Protobuf_Int64Value? = nil
    var _toUserID: SwiftProtobuf.Google_Protobuf_Int64Value? = nil
    var _deliveryStatus: MessageDeliveryStatus = .ready
    var _receptionDate: SwiftProtobuf.Google_Protobuf_Int64Value? = nil
    var _readDate: SwiftProtobuf.Google_Protobuf_Int64Value? = nil
    var _recallDate: SwiftProtobuf.Google_Protobuf_Int64Value? = nil

    static let defaultInstance = _StorageClass()

    private init() {}

    init(copying source: _StorageClass) {
      _messageID = source._messageID
      _toUserID = source._toUserID
      _deliveryStatus = source._deliveryStatus
      _receptionDate = source._receptionDate
      _readDate = source._readDate
      _recallDate = source._recallDate
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
        case 1: try decoder.decodeSingularMessageField(value: &_storage._messageID)
        case 2: try decoder.decodeSingularMessageField(value: &_storage._toUserID)
        case 3: try decoder.decodeSingularEnumField(value: &_storage._deliveryStatus)
        case 4: try decoder.decodeSingularMessageField(value: &_storage._receptionDate)
        case 5: try decoder.decodeSingularMessageField(value: &_storage._readDate)
        case 6: try decoder.decodeSingularMessageField(value: &_storage._recallDate)
        default: break
        }
      }
    }
  }

  public func traverse<V: SwiftProtobuf.Visitor>(visitor: inout V) throws {
    try withExtendedLifetime(_storage) { (_storage: _StorageClass) in
      if let v = _storage._messageID {
        try visitor.visitSingularMessageField(value: v, fieldNumber: 1)
      }
      if let v = _storage._toUserID {
        try visitor.visitSingularMessageField(value: v, fieldNumber: 2)
      }
      if _storage._deliveryStatus != .ready {
        try visitor.visitSingularEnumField(value: _storage._deliveryStatus, fieldNumber: 3)
      }
      if let v = _storage._receptionDate {
        try visitor.visitSingularMessageField(value: v, fieldNumber: 4)
      }
      if let v = _storage._readDate {
        try visitor.visitSingularMessageField(value: v, fieldNumber: 5)
      }
      if let v = _storage._recallDate {
        try visitor.visitSingularMessageField(value: v, fieldNumber: 6)
      }
    }
    try unknownFields.traverse(visitor: &visitor)
  }

  public static func ==(lhs: MessageStatus, rhs: MessageStatus) -> Bool {
    if lhs._storage !== rhs._storage {
      let storagesAreEqual: Bool = withExtendedLifetime((lhs._storage, rhs._storage)) { (_args: (_StorageClass, _StorageClass)) in
        let _storage = _args.0
        let rhs_storage = _args.1
        if _storage._messageID != rhs_storage._messageID {return false}
        if _storage._toUserID != rhs_storage._toUserID {return false}
        if _storage._deliveryStatus != rhs_storage._deliveryStatus {return false}
        if _storage._receptionDate != rhs_storage._receptionDate {return false}
        if _storage._readDate != rhs_storage._readDate {return false}
        if _storage._recallDate != rhs_storage._recallDate {return false}
        return true
      }
      if !storagesAreEqual {return false}
    }
    if lhs.unknownFields != rhs.unknownFields {return false}
    return true
  }
}
