package ca.bc.gov.educ.eas.api.rules.assessment;

import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

public enum AssessmentStudentValidationIssueTypeCode {

    DEM_DATA_MISSING("DEM_DATA_MISSING", "This student is missing demographic data based on Student PEN, Surname, and Given Name."),
    COURSE_CODE_INVALID("COURSE_CODE_INVALID", "The Assessment Code provided is not valid for the Assessment Session specified."),
    COURSE_SESSION_DUP("COURSE_SESSION_DUP", "The assessment session is a duplicate of an existing assessment session for this student/assessment/level."),
    COURSE_SESSION_EXCEED("COURSE_SESSION_EXCEED", "Student has already reached the maximum number of writes for this Assessment."),
    COURSE_STATUS_INVALID("COURSE_STATUS_INVALID", "Assessment registration must be A=active or W=withdraw."),
    NUMBER_OF_CREDITS_NOT_BLANK("NUMBER_OF_CREDITS_NOT_BLANK", "Number of credits value is ignored and must be blank."),
    EXAM_SCHOOL_INVALID("EXAM_SCHOOL_INVALID", "Invalid assessment center provided."),
    SCHOOL_INVALID("SCHOOL_INVALID", "Invalid school provided."),
    COURSE_CODE_CSF("COURSE_CODE_CSF", "Student is in a Francophone school and cannot register for this assessment session for this student."),
    COURSE_ALREADY_WRITTEN("COURSE_ALREADY_WRITTEN", "Assessment has been written by the student, withdrawal is not allowed."),
    PEN_INVALID("PEN_NOT_FOUND", "Invalid PEN provided."),
    SURNAME_MISMATCH("SURNAME_MISMATCH", "Student's surname does not match the surname for this PEN."),
    GIVEN_NAME_MISMATCH("GIVEN_NAME_MISMATCH", "Student's given name does not match the given name for this PEN.");

    private static final Map<String, AssessmentStudentValidationIssueTypeCode> CODE_MAP = new HashMap<>();

    static {
        for (AssessmentStudentValidationIssueTypeCode type : values()) {
            CODE_MAP.put(type.getCode(), type);
        }
    }

    @Getter
    private final String code;

    @Getter
    private final String message;

    AssessmentStudentValidationIssueTypeCode(String code, String message) {
        this.code = code;
        this.message = message;
    }
    public static AssessmentStudentValidationIssueTypeCode findByValue(String value) {
        return CODE_MAP.get(value);
    }
}

