// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: request/group/enrollment/create_group_join_question_request.proto

package im.turms.turms.pojo.request;

public interface CreateGroupJoinQuestionRequestOrBuilder extends
    // @@protoc_insertion_point(interface_extends:im.turms.proto.CreateGroupJoinQuestionRequest)
    com.google.protobuf.MessageOrBuilder {

  /**
   * <code>int64 group_id = 1;</code>
   * @return The groupId.
   */
  long getGroupId();

  /**
   * <code>string question = 2;</code>
   * @return The question.
   */
  java.lang.String getQuestion();
  /**
   * <code>string question = 2;</code>
   * @return The bytes for question.
   */
  com.google.protobuf.ByteString
      getQuestionBytes();

  /**
   * <code>repeated string answers = 3;</code>
   * @return A list containing the answers.
   */
  java.util.List<java.lang.String>
      getAnswersList();
  /**
   * <code>repeated string answers = 3;</code>
   * @return The count of answers.
   */
  int getAnswersCount();
  /**
   * <code>repeated string answers = 3;</code>
   * @param index The index of the element to return.
   * @return The answers at the given index.
   */
  java.lang.String getAnswers(int index);
  /**
   * <code>repeated string answers = 3;</code>
   * @param index The index of the value to return.
   * @return The bytes of the answers at the given index.
   */
  com.google.protobuf.ByteString
      getAnswersBytes(int index);

  /**
   * <code>int32 score = 4;</code>
   * @return The score.
   */
  int getScore();
}
