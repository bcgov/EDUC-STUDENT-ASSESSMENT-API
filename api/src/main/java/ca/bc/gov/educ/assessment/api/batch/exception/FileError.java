package ca.bc.gov.educ.assessment.api.batch.exception;

import lombok.Getter;

/**
 * The enum File error.
 *
 * @author OM
 */
public enum FileError {
  /**
   * Upload file did not contain any content.
   */
  EMPTY_FILE("The DEM data file contains no records."),
  NO_HEADING("Heading row is missing."),
  BLANK_CELL_IN_HEADING_ROW("Heading row has a blank cell at column $?."),
  MISSING_MANDATORY_HEADER("Missing required header $?."),

  /**
   * The Invalid transaction code student details.
   */
  INVALID_TRANSACTION_CODE_STUDENT_DETAILS_DEM("Invalid transaction code on Detail record $? for student with Local ID $?. Must be one of \"D02\" or \"E02\"."),

  INVALID_TRANSACTION_CODE_STUDENT_DETAILS_CRS("Invalid transaction code on Detail record $? for student with Local ID $?. Must be one of \"D08\" or \"E08\"."),

  INVALID_TRANSACTION_CODE_STUDENT_DETAILS_XAM("Invalid transaction code on Detail record $? for student with Local ID $?. Must be one of \"E06\" or \"D06\"."),

  /**
   * The filetype ended in the wrong extension and may be the wrong filetype.
   */
  INVALID_FILE_EXTENSION("File extension invalid. Files must be of type \".dem\" or \".crs\" or \".xam\"."),

  NO_FILE_EXTENSION("No file extension provided. Files must be of type \".dem\" or \".crs\" or \".xam\"."),

  CONFLICT_FILE_ALREADY_IN_FLIGHT("File is already being processed for this school. School ministry code is: $?."),

  /**
   * No record for the provided school ID was found.
   */
  INVALID_SCHOOL("Unable to find a school record for school ministry code $?."),

  INVALID_FILENAME("File not processed due to invalid filename. Must be the school ministry code."),

  /**
   * The mincode on the uploaded document does not match the collection record.
   */
  MINCODE_MISMATCH("The school codes in your file do not match your school's code. Please ensure that all school codes in the file correspond to your school code."),

  DISTRICT_MINCODE_MISMATCH("The school codes in the file must match. Please verify the school codes supplied."),
  /**
   * Invalid row length file error.
   * This will be thrown when any row in the given file is longer or shorter than expected.
   */
  INVALID_ROW_LENGTH("$?"),

  INVALID_INCOMING_REQUEST_SESSION("Invalid assessment session."),
  INVALID_ASSESSMENT_KEY_SESSION("Invalid assessment session on line $?."),
  INVALID_ASSESSMENT_TYPE("Invalid assessment type on line $?."),
  INVALID_ASSESSMENT_CODE("Invalid assessment."),
  BLANK_ASSESSMENT_CODE("Assessment code cannot be blank."),
  BLANK_FORM_CODE("Form code cannot be blank."),
  GENERIC_ERROR_MESSAGE("Unexpected failure during file processing."),
  INVALID_ITEM_TYPE("Invalid item type on line $?."),
  INVALID_TASK_CODE("Invalid task code on line $?."),
  INVALID_CLAIM_CODE("Invalid claim code on line $?."),
  INVALID_COGNITIVE_LEVEL_CODE("Invalid cognitive level code on line $?."),
  INVALID_CONCEPT_CODE("Invalid concept code on line $?."),
  INVALID_CONTEXT_CODE("Invalid context code on line $?."),
  SESSION_LENGTH_ERROR("Assessment session must not be longer than 6 on line $?."),
  ASSMT_CODE_LENGTH_ERROR("Assessment code must not be longer than 5 on line $?."),
  FORM_CODE_LENGTH_ERROR("Form code must not be longer than 1 on line $?."),
  QUES_NUM_LENGTH_ERROR("Question number must not be longer than 2 on line $?."),
  ITEM_TYPE_LENGTH_ERROR("Item type must not be longer than 12 on line $?."),
  ANSWER_LENGTH_ERROR("Answer must not be longer than 150 on line $?."),
  MARK_LENGTH_ERROR("Mark value must not be longer than 2 on line $?."),
  COGN_LEVEL_LENGTH_ERROR("Cognitive level code must not be longer than 4 on line $?."),
  TASK_CODE_LENGTH_ERROR("Task Code must not be longer than 2 on line $?."),
  CLAIM_CODE_LENGTH_ERROR("Claim code must not be longer than 3 on line $?."),
  CONTEXT_CODE_LENGTH_ERROR("Context code must not be longer than 1 on line $?."),
  CONCEPTS_CODE_LENGTH_ERROR("Concept code must not be longer than 3 on line $?."),
  SCALE_FACTOR_LENGTH_ERROR("Scale factor must not be longer than 8 on line $?."),
  ASSMT_SECTION_LENGTH_ERROR("Assessment section must not be longer than 8 on line $?."),
  ;

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
  FileError(final String message) {
    this.message = message;
  }
}
