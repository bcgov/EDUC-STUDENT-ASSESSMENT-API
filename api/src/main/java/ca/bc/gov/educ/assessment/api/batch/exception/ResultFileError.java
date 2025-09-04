package ca.bc.gov.educ.assessment.api.batch.exception;

import lombok.Getter;

/**
 * The enum File error.
 *
 * @author OM
 */
public enum ResultFileError {

  INVALID_ROW_LENGTH("$?"),
  EMPTY_FILE("The results file contains no records."),

  INVALID_INCOMING_REQUEST_SESSION("Invalid assessment session."),

  INVALID_TXID("Invalid transaction ID (TX_ID), value must be A01."),
  INVALID_COMPONENT_TYPE_CODE("Invalid component type code on line $?."),
  INVALID_SPECIAL_CASE_CODE("Invalid special case code on line $?."),
  INVALID_ADAPTED_ASSESSMENT_CODE("Invalid adapted assessment indicator code on line $?."),
  BLANK_ADAPTED_ASSESSMENT_CODE("Adapted assessment indicator code cannot be blank on line $?."),
  INVALID_COMPONENT_SUB_TYPE_CODE("Invalid component sub type code on line $?."),
  INVALID_ASSESSMENT_TYPE("Invalid assessment type on line $?."),
  INVALID_ASSESSMENT_SESSION("Invalid assessment session on line $?."),
  INVALID_MARKING_SESSION("Invalid marking session on line $?."),
  BLANK_MARKING_SESSION("Marking session cannot be blank on line $?."),
  INVALID_OPEN_ENDED_MARKS("Invalid open ended marks string on line $?."),
  INVALID_SELECTED_CHOICE_MARKS("Invalid selected choice marks string on line $?."),
  INVALID_MINCODE("Invalid school ministry code on line $?."),
  INVALID_PROFICIENCY_SCORE("Invalid proficiency score on line $?."),
  INVALID_IRT_SCORE("Invalid IRT score on line $?."),
  INVALID_CHOICE_PATH("Invalid choice path on line $?."),
  INVALID_PEN("Invalid personal education number (PEN) on line $?."),
  INVALID_FORM_CODE("Invalid form code provided on line $?."),
  INVALID_KEY("Key does not exist for this assessment."),
  INVALID_COMPONENT("Could not find valid component key on line $?."),
  INVALID_QUESTION("Could not find valid question key on line $?.");

  /**
   * The Message.
   */
  @Getter
  private final String message;

  /**
   * Instantiates a new File error.
   *
   * @param message the message
   */
  ResultFileError(final String message) {
    this.message = message;
  }
}
