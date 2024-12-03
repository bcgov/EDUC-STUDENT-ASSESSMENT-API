package ca.bc.gov.educ.eas.api.rules.assessment;

import lombok.Getter;

public enum AssessmentStudentValidationFieldCode {
    PEN("PEN"),
    COURSE_LEVEL("COURSE_LEVEL"),
    COURSE_CODE("COURSE_CODE"),
    INTERIM_SCHOOL_PERCENTAGE("INTERIM_SCHOOL_PERCENTAGE"),
    INTERIM_LETTER_GRADE("INTERIM_LETTER_GRADE"),
    FINAL_SCHOOL_PERCENTAGE("FINAL_SCHOOL_PERCENTAGE"),
    FINAL_PERCENTAGE("FINAL_PERCENTAGE"),
    FINAL_LETTER_GRADE("FINAL_LETTER_GRADE"),
    PROVINCIAL_SPECIAL_CASE("PROVINCIAL_SPECIAL_CASE"),
    COURSE_STATUS("COURSE_STATUS"),
    NUMBER_OF_CREDITS("NUMBER_OF_CREDITS"),
    COURSE_TYPE("COURSE_TYPE"),
    TO_WRITE_FLAG("TO_WRITE_FLAG"),
    EXAM_SCHOOL("EXAM_SCHOOL"),
    COURSE_SESSION("COURSE_SESSION");

    @Getter
    private final String code;

    AssessmentStudentValidationFieldCode(String code) {
        this.code = code;
    }
}

