package ca.bc.gov.educ.eas.api.rules.assessment;

import lombok.Getter;

public enum AssessmentStudentValidationFieldCode {
    PEN("PEN"),
    COURSE_CODE("COURSE_CODE"),
    COURSE_STATUS("COURSE_STATUS"),
    EXAM_SCHOOL("EXAM_SCHOOL"),
    SCHOOL("SCHOOL"),
    SURNAME("SURNAME"),
    GIVEN_NAME("GIVEN_NAME");

    @Getter
    private final String code;

    AssessmentStudentValidationFieldCode(String code) {
        this.code = code;
    }
}

