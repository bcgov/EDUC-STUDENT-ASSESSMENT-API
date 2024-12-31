package ca.bc.gov.educ.eas.api.batch.exception;

import lombok.Getter;


public enum AssessmentKeyFileError {

    EMPTY_FILE("The uploaded file is empty."),

    INVALID_FILE_EXTENSION("File extension invalid. Files must be of type \".tab\"."),

    NO_FILE_EXTENSION("No file extension provided. Files must be of type \".tab\"."),

    INVALID_FILE_NAME("Invalid file name. File name must be of type \".TRAX_YYYYMM_{ASSESSMENT_TYPE}\"."),

    CONFLICT_FILE_ALREADY_IN_FLIGHT("File is already being processed for this assessment. Assessment is: $?"),

    INVALID_SESSION("Session id is not valid."),

    FILE_NOT_ALLOWED("File type not allowed"),

    GENERIC_ERROR_MESSAGE("Unexpected failure during file processing."),

    INVALID_DATA("Validation failed on $? in line $?");

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
    AssessmentKeyFileError(final String message) {
        this.message = message;
    }
}