package ca.bc.gov.educ.assessment.api.constants.v1.reports;

import lombok.Getter;

@Getter
public enum AssessmentStudentSearchReportHeader {

    PEN("PEN"),
    SURNAME("Surname"),
    GIVEN_NAME("Given Name"),
    GRADE("Grade"),
    SCHOOL_OF_RECORD_CODE("School of Record Code"),
    SCHOOL_OF_RECORD_NAME("School of Record Name"),
    SCHOOL_AT_WRITE_CODE("School at Write Code"),
    SCHOOL_AT_WRITE_NAME("School at Write Name"),
    ASSESSMENT_CODE("Assessment Code"),
    ASSESSMENT_SESSION("Assessment Session"),
    PROFICIENCY_SCORE("Proficiency Score"),
    SPECIAL_CASE("Special Case");

    private final String code;
    AssessmentStudentSearchReportHeader(String code) { this.code = code; }
}

