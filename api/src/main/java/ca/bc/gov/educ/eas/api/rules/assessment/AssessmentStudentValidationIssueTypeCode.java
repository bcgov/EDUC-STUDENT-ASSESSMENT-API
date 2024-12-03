package ca.bc.gov.educ.eas.api.rules.assessment;

import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

public enum AssessmentStudentValidationIssueTypeCode {

    DEM_DATA_MISSING("DEM_DATA_MISSING", "This student is missing demographic data based on Student PEN, Surname, Mincode and Local ID."),
    COURSE_LEVEL_NOT_BLANK("COURSE_LEVEL_NOT_BLANK", "Course level value is ignored and must be blank."),
    COURSE_CODE_INVALID("COURSE_CODE_INVALID", "The Assessment Code provided is not valid for the Assessment Session specified."),
    COURSE_SESSION_DUP("COURSE_SESSION_DUP", "The assessment session is a duplicate of an existing assessment session for this student/assessment/level."),
    COURSE_SESSION_EXCEED("COURSE_SESSION_EXCEED", "Student has already reached the maximum number of writes for this Assessment."),
    INTERIM_SCHOOL_PERCENTAGE_NOT_BLANK("INTERIM_SCHOOL_PERCENTAGE_NOT_BLANK", "Interim school percentage value is ignored and must be blank."),
    INTERIM_LETTER_GRADE_NOT_BLANK("INTERIM_LETTER_GRADE_NOT_BLANK", "Interim letter grade value is ignored and must be blank."),
    FINAL_SCHOOL_PERCENTAGE_NOT_BLANK("FINAL_SCHOOL_PERCENTAGE_NOT_BLANK", "Final school percentage value is ignored and must be blank."),
    FINAL_PERCENTAGE_NOT_BLANK("FINAL_PERCENTAGE_NOT_BLANK", "Final percentage result cannot be submitted by the school."),
    FINAL_LETTER_GRADE_NOT_BLANK("FINAL_LETTER_GRADE_NOT_BLANK", "Final letter grade value is ignored and must be blank."),
    PROVINCIAL_SPECIAL_CASE_NOT_BLANK("PROVINCIAL_SPECIAL_CASE_NOT_BLANK", "Provincial special case cannot be submitted by the school."),
    COURSE_STATUS_INVALID("COURSE_STATUS_INVALID", "Assessment registration must be A=active or W=withdraw."),
    NUMBER_OF_CREDITS_NOT_BLANK("NUMBER_OF_CREDITS_NOT_BLANK", "Number of credits value is ignored and must be blank."),
    COURSE_TYPE_NOT_BLANK("COURSE_TYPE_NOT_BLANK", "Course type value is ignored and must be blank."),
    TO_WRITE_FLAG_NOT_BLANK("TO_WRITE_FLAG_NOT_BLANK", "To write flag value is ignored and must be blank."),
    EXAM_SCHOOL_INVALID("EXAM_SCHOOL_INVALID", "Invalid assessment center provided."),
    DUPLICATE_XAM_RECORD("DUPLICATE_XAM_RECORD", "The assessment session is a duplicate of an existing assessment session for this student."),
    COURSE_CODE_CSF("COURSE_CODE_CSF", "Student is in a Francophone school and cannot register for this assessment session for this student."),
    COURSE_ALREADY_WRITTEN("COURSE_ALREADY_WRITTEN", "Assessment has been written by the student, withdrawal is not allowed."),
    DEM_ISSUE("DEM_ISSUE", "Student XAM record will not be processed due to an issue with the student's demographics.");

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

