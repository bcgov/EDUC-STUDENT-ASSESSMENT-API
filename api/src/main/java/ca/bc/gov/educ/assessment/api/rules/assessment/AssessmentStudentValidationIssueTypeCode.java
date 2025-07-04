package ca.bc.gov.educ.assessment.api.rules.assessment;

import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

public enum AssessmentStudentValidationIssueTypeCode {

    COURSE_SESSION_DUP("COURSE_SESSION_DUP", "Course Session Duplicate", "The student has already been registered for this assessment in this session."),
    COURSE_SESSION_EXCEED("COURSE_SESSION_EXCEED", "Course Writes Exceeded", "The student has reached the maximum number of attempts for this assessment and cannot be registered."),
    COURSE_STATUS_INVALID("COURSE_STATUS_INVALID", "Course Status Invalid", "Assessment registration must be A=active or W=withdraw."),
    EXAM_SCHOOL_INVALID("EXAM_SCHOOL_INVALID", "Invalid Assessment Center", "Invalid assessment center provided."),
    SCHOOL_INVALID("SCHOOL_INVALID", "Invalid School", "Invalid school provided."),
    COURSE_CODE_CSF("COURSE_CODE_CSF", "Francophone School", "Student is enrolled in a Programme Francophone school and cannot be registered for a French Immersion assessment."),
    COURSE_ALREADY_WRITTEN("COURSE_ALREADY_WRITTEN", "Withdrawal Not Allowed", "Assessment has been written by the student, withdrawal is not allowed."),
    PEN_INVALID("PEN_NOT_FOUND", "Invalid PEN", "Invalid PEN provided."),
    SURNAME_MISMATCH("SURNAME_MISMATCH", "Surname Mismatch", "Student's surname does not match the surname for this PEN."),
    SCHOOL_OF_RECORD_INVALID("SCHOOL_OF_RECORD_INVALID", "Invalid School", "Invalid school of recordprovided."),
    GIVEN_NAME_MISMATCH("GIVEN_NAME_MISMATCH", "Given Name Mismatch", "Student's given name does not match the given name for this PEN.");

    private static final Map<String, AssessmentStudentValidationIssueTypeCode> CODE_MAP = new HashMap<>();

    static {
        for (AssessmentStudentValidationIssueTypeCode type : values()) {
            CODE_MAP.put(type.getCode(), type);
        }
    }

    @Getter
    private final String code;

    @Getter
    private final String label;

    @Getter
    private final String message;

    AssessmentStudentValidationIssueTypeCode(String code, String label, String message) {
        this.code = code;
        this.label = label;
        this.message = message;
    }
    public static AssessmentStudentValidationIssueTypeCode findByValue(String value) {
        return CODE_MAP.get(value);
    }
}

