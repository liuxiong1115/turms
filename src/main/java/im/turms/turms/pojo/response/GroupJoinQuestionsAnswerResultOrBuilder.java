// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: response/group/group_join_questions_answer_result.proto

package im.turms.turms.pojo.response;

public interface GroupJoinQuestionsAnswerResultOrBuilder extends
    // @@protoc_insertion_point(interface_extends:im.turms.proto.GroupJoinQuestionsAnswerResult)
    com.google.protobuf.MessageOrBuilder {

  /**
   * <code>int32 score = 1;</code>
   * @return The score.
   */
  int getScore();

  /**
   * <code>repeated int64 questions_ids = 2;</code>
   * @return A list containing the questionsIds.
   */
  java.util.List<java.lang.Long> getQuestionsIdsList();
  /**
   * <code>repeated int64 questions_ids = 2;</code>
   * @return The count of questionsIds.
   */
  int getQuestionsIdsCount();
  /**
   * <code>repeated int64 questions_ids = 2;</code>
   * @param index The index of the element to return.
   * @return The questionsIds at the given index.
   */
  long getQuestionsIds(int index);

  /**
   * <code>bool joined = 3;</code>
   * @return The joined.
   */
  boolean getJoined();
}
