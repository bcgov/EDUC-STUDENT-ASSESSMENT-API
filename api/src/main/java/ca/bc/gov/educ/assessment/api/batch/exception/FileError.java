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

  DUPLICATE_PEN_IN_DEM_FILE("The same PEN $? is appearing more than once in the DEM file on $? $?."),

  BLANK_PEN_IN_DEM_FILE("The PEN field is blank for one or more records in the DEM file on $? $?."),

  BLANK_PEN_IN_XAM_FILE("The PEN field is blank for one or more records in the XAM file on $? $?."),

  BLANK_PEN_IN_CRS_FILE("The PEN field is blank for one or more records in the CRS file on $? $?."),

  INCORRECT_COURSE_DATE_IN_CRS_FILE("The course session year/month is invalid in the CRS file on line $?."),
  FILE_NOT_ALLOWED("File type not allowed."),
  EMPTY_EXCEL_NOT_ALLOWED("The file does not contain any records."),
  COURSE_FILE_SESSION_ERROR(".CRS file must have at least 1 record with a current or future course session."),
  INVALID_SCHOOL_FOR_UPLOAD("This school is not eligible for graduation records and achievement data collection."),
  /**
   * School is opening.
   */
  SCHOOL_IS_OPENING("Invalid school provided - school is not yet open."),
  INVALID_SCHOOL_DATES("Invalid school dates - this was not expected."),
  SCHOOL_OUTSIDE_OF_DISTRICT("The school is not in your district and cannot be uploaded."),
  MISSING_MINCODE("The school has been closed for more than 3 months or is not Transcript Eligible. The data cannot be uploaded."),
  FILE_ENCRYPTED("File is password protected."),
  BLANK_PEN_IN_EXCEL("The PEN field cannot be blank. Review the data on line $?."),
  PEN_LENGTH_IN_EXCEL("Submitted PENs cannot be more than 10 digits. Review the data on line $?."),
  LEGAL_SURNAME_IN_EXCEL("Legal Surnames cannot be longer than 25 characters. Review the data on line $?."),
  LEGAL_FIRST_NAME_IN_EXCEL("Legal Given Names cannot be longer than 25 characters. Review the data on line $?."),
  LEGAL_MIDDLE_NAME_IN_EXCEL("Legal Middle Names cannot be longer than 25 characters. Review the data on line $?."),
  COURSE_IN_EXCEL("Course code and level cannot be longer than 8 characters. Review the data on line $?."),
  FINAL_SCH_PERCENT_EXCEL("Final School Percent cannot be more than 3 digits. Review the data on line $?."),
  FINAL_LETTER_GRADE_EXCEL("Final Letter Grade cannot be more than 2 characters. Review the data on line $?."),
  NO_OF_CREDITS_EXCEL("Number of Credits cannot be more than 1 digit. Review the data on line $?."),
  BIRTHDATE_FORMAT_EXCEL("Birthdate must be in the format YYYYMMDD. Review the data on line $?."),
  SESSION_DATE_FORMAT_EXCEL("Can only report courses in the $? or $? sessions. Review the data on line $?."),
  GENERIC_ERROR_MESSAGE("Unexpected failure during file processing.");

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
