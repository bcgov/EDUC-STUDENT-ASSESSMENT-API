package ca.bc.gov.educ.assessment.api.constants.v1.reports;

import lombok.Getter;

@Getter
public enum AssessmentStudentSearchReportHeader {

    PEN("PEN"),
    SURNAME("Surname"),
    GIVEN_NAME("Given Name"),
    GRADE("Grade"),
    SCHOOL_OF_RECORD("School of Record"),
    SCHOOL_AT_WRITE("School at Write"),
    ASSESSMENT_CODE("Assessment Code"),
    ASSESSMENT_SESSION("Assessment Session"),
    PROFICIENCY_SCORE("Proficiency Score"),
    SPECIAL_CASE("Special Case");

    private final String code;
    AssessmentStudentSearchReportHeader(String code) { this.code = code; }
}

