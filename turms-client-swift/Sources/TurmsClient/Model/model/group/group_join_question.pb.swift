// DO NOT EDIT.
//
// Generated by the Swift generator plugin for the protocol buffer compiler.
// Source: model/group/group_join_question.proto
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

public struct GroupJoinQuestion {
  // SwiftProtobuf.Message conformance is added in an extension below. See the
  // `Message` and `Message+*Additions` files in the SwiftProtobuf library for
  // methods supported on all messages.

  public var id: SwiftProtobuf.Google_Protobuf_Int64Value {
    get {return _storage._id ?? SwiftProtobuf.Google_Protobuf_Int64Value()}
    set {_uniqueStorage()._id = newValue}
  }
  /// Returns true if `id` has been explicitly set.
  public var hasID: Bool {return _storage._id != nil}
  /// Clears the value of `id`. Subsequent reads from it will return its default value.
  public mutating func clearID() {_uniqueStorage()._id = nil}

  public var groupID: SwiftProtobuf.Google_Protobuf_Int64Value {
    get {return _storage._groupID ?? SwiftProtobuf.Google_Protobuf_Int64Value()}
    set {_uniqueStorage()._groupID = newValue}
  }
  /// Returns true if `groupID` has been explicitly set.
  public var hasGroupID: Bool {return _storage._groupID != nil}
  /// Clears the value of `groupID`. Subsequent reads from it will return its default value.
  public mutating func clearGroupID() {_uniqueStorage()._groupID = nil}

  public var question: SwiftProtobuf.Google_Protobuf_StringValue {
    get {return _storage._question ?? SwiftProtobuf.Google_Protobuf_StringValue()}
    set {_uniqueStorage()._question = newValue}
  }
  /// Returns true if `question` has been explicitly set.
  public var hasQuestion: Bool {return _storage._question != nil}
  /// Clears the value of `question`. Subsequent reads from it will return its default value.
  public mutating func clearQuestion() {_uniqueStorage()._question = nil}

  public var answers: [SwiftProtobuf.Google_Protobuf_StringValue] {
    get {return _storage._answers}
    set {_uniqueStorage()._answers = newValue}
  }

  public var score: SwiftProtobuf.Google_Protobuf_Int32Value {
    get {return _storage._score ?? SwiftProtobuf.Google_Protobuf_Int32Value()}
    set {_uniqueStorage()._score = newValue}
  }
  /// Returns true if `score` has been explicitly set.
  public var hasScore: Bool {return _storage._score != nil}
  /// Clears the value of `score`. Subsequent reads from it will return its default value.
  public mutating func clearScore() {_uniqueStorage()._score = nil}

  public var unknownFields = SwiftProtobuf.UnknownStorage()

  public init() {}

  fileprivate var _storage = _StorageClass.defaultInstance
}

// MARK: - Code below here is support for the SwiftProtobuf runtime.

fileprivate let _protobuf_package = "im.turms.proto"

extension GroupJoinQuestion: SwiftProtobuf.Message, SwiftProtobuf._MessageImplementationBase, SwiftProtobuf._ProtoNameProviding {
  public static let protoMessageName: String = _protobuf_package + ".GroupJoinQuestion"
  public static let _protobuf_nameMap: SwiftProtobuf._NameMap = [
    1: .same(proto: "id"),
    2: .standard(proto: "group_id"),
    3: .same(proto: "question"),
    4: .same(proto: "answers"),
    5: .same(proto: "score"),
  ]

  fileprivate class _StorageClass {
    var _id: SwiftProtobuf.Google_Protobuf_Int64Value? = nil
    var _groupID: SwiftProtobuf.Google_Protobuf_Int64Value? = nil
    var _question: SwiftProtobuf.Google_Protobuf_StringValue? = nil
    var _answers: [SwiftProtobuf.Google_Protobuf_StringValue] = []
    var _score: SwiftProtobuf.Google_Protobuf_Int32Value? = nil

    static let defaultInstance = _StorageClass()

    private init() {}

    init(copying source: _StorageClass) {
      _id = source._id
      _groupID = source._groupID
      _question = source._question
      _answers = source._answers
      _score = source._score
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
        case 1: try decoder.decodeSingularMessageField(value: &_storage._id)
        case 2: try decoder.decodeSingularMessageField(value: &_storage._groupID)
        case 3: try decoder.decodeSingularMessageField(value: &_storage._question)
        case 4: try decoder.decodeRepeatedMessageField(value: &_storage._answers)
        case 5: try decoder.decodeSingularMessageField(value: &_storage._score)
        default: break
        }
      }
    }
  }

  public func traverse<V: SwiftProtobuf.Visitor>(visitor: inout V) throws {
    try withExtendedLifetime(_storage) { (_storage: _StorageClass) in
      if let v = _storage._id {
        try visitor.visitSingularMessageField(value: v, fieldNumber: 1)
      }
      if let v = _storage._groupID {
        try visitor.visitSingularMessageField(value: v, fieldNumber: 2)
      }
      if let v = _storage._question {
        try visitor.visitSingularMessageField(value: v, fieldNumber: 3)
      }
      if !_storage._answers.isEmpty {
        try visitor.visitRepeatedMessageField(value: _storage._answers, fieldNumber: 4)
      }
      if let v = _storage._score {
        try visitor.visitSingularMessageField(value: v, fieldNumber: 5)
      }
    }
    try unknownFields.traverse(visitor: &visitor)
  }

  public static func ==(lhs: GroupJoinQuestion, rhs: GroupJoinQuestion) -> Bool {
    if lhs._storage !== rhs._storage {
      let storagesAreEqual: Bool = withExtendedLifetime((lhs._storage, rhs._storage)) { (_args: (_StorageClass, _StorageClass)) in
        let _storage = _args.0
        let rhs_storage = _args.1
        if _storage._id != rhs_storage._id {return false}
        if _storage._groupID != rhs_storage._groupID {return false}
        if _storage._question != rhs_storage._question {return false}
        if _storage._answers != rhs_storage._answers {return false}
        if _storage._score != rhs_storage._score {return false}
        return true
      }
      if !storagesAreEqual {return false}
    }
    if lhs.unknownFields != rhs.unknownFields {return false}
    return true
  }
}
