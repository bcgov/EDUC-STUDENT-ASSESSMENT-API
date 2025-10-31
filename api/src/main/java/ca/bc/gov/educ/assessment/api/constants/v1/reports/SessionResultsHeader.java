package ca.bc.gov.educ.assessment.api.constants.v1.reports;

import lombok.Getter;

@Getter
public enum SessionResultsHeader {
    ASSESSMENT_SESSION("Assessment Session"),
    SCHOOL_CODE("School Code"),
    ASSESSMENT_CODE("Assessment Code"),
    STUDENT_PEN("PEN"),
    STUDENT_LOCAL_ID("Student Local ID"),
    STUDENT_SURNAME("Student Surname"),
    STUDENT_GIVEN("Student Given"),
    ASSESSMENT_PROFICIENCY_SCORE("Proficiency Score"),
    SPECIAL_CASE("Special Case"),
    ASSESSMENT_CENTRE_SCHOOL_CODE("Assessment Centre School Code");

    private final String code;
    SessionResultsHeader(String code) { this.code = code; }
}
