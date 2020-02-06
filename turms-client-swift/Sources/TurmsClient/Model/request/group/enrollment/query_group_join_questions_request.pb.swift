// DO NOT EDIT.
//
// Generated by the Swift generator plugin for the protocol buffer compiler.
// Source: request/group/enrollment/query_group_join_questions_request.proto
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

public struct QueryGroupJoinQuestionsRequest {
  // SwiftProtobuf.Message conformance is added in an extension below. See the
  // `Message` and `Message+*Additions` files in the SwiftProtobuf library for
  // methods supported on all messages.

  public var groupID: Int64 {
    get {return _storage._groupID}
    set {_uniqueStorage()._groupID = newValue}
  }

  public var withAnswers: Bool {
    get {return _storage._withAnswers}
    set {_uniqueStorage()._withAnswers = newValue}
  }

  public var lastUpdatedDate: SwiftProtobuf.Google_Protobuf_Int64Value {
    get {return _storage._lastUpdatedDate ?? SwiftProtobuf.Google_Protobuf_Int64Value()}
    set {_uniqueStorage()._lastUpdatedDate = newValue}
  }
  /// Returns true if `lastUpdatedDate` has been explicitly set.
  public var hasLastUpdatedDate: Bool {return _storage._lastUpdatedDate != nil}
  /// Clears the value of `lastUpdatedDate`. Subsequent reads from it will return its default value.
  public mutating func clearLastUpdatedDate() {_uniqueStorage()._lastUpdatedDate = nil}

  public var unknownFields = SwiftProtobuf.UnknownStorage()

  public init() {}

  fileprivate var _storage = _StorageClass.defaultInstance
}

// MARK: - Code below here is support for the SwiftProtobuf runtime.

fileprivate let _protobuf_package = "im.turms.proto"

extension QueryGroupJoinQuestionsRequest: SwiftProtobuf.Message, SwiftProtobuf._MessageImplementationBase, SwiftProtobuf._ProtoNameProviding {
  public static let protoMessageName: String = _protobuf_package + ".QueryGroupJoinQuestionsRequest"
  public static let _protobuf_nameMap: SwiftProtobuf._NameMap = [
    1: .standard(proto: "group_id"),
    2: .standard(proto: "with_answers"),
    3: .standard(proto: "last_updated_date"),
  ]

  fileprivate class _StorageClass {
    var _groupID: Int64 = 0
    var _withAnswers: Bool = false
    var _lastUpdatedDate: SwiftProtobuf.Google_Protobuf_Int64Value? = nil

    static let defaultInstance = _StorageClass()

    private init() {}

    init(copying source: _StorageClass) {
      _groupID = source._groupID
      _withAnswers = source._withAnswers
      _lastUpdatedDate = source._lastUpdatedDate
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
        case 2: try decoder.decodeSingularBoolField(value: &_storage._withAnswers)
        case 3: try decoder.decodeSingularMessageField(value: &_storage._lastUpdatedDate)
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
      if _storage._withAnswers != false {
        try visitor.visitSingularBoolField(value: _storage._withAnswers, fieldNumber: 2)
      }
      if let v = _storage._lastUpdatedDate {
        try visitor.visitSingularMessageField(value: v, fieldNumber: 3)
      }
    }
    try unknownFields.traverse(visitor: &visitor)
  }

  public static func ==(lhs: QueryGroupJoinQuestionsRequest, rhs: QueryGroupJoinQuestionsRequest) -> Bool {
    if lhs._storage !== rhs._storage {
      let storagesAreEqual: Bool = withExtendedLifetime((lhs._storage, rhs._storage)) { (_args: (_StorageClass, _StorageClass)) in
        let _storage = _args.0
        let rhs_storage = _args.1
        if _storage._groupID != rhs_storage._groupID {return false}
        if _storage._withAnswers != rhs_storage._withAnswers {return false}
        if _storage._lastUpdatedDate != rhs_storage._lastUpdatedDate {return false}
        return true
      }
      if !storagesAreEqual {return false}
    }
    if lhs.unknownFields != rhs.unknownFields {return false}
    return true
  }
}
