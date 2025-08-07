package ca.bc.gov.educ.assessment.api.constants.v1.reports;

import lombok.Getter;

@Getter
public enum AllStudentDetailedRegistrationDetailsHeader {

    PEN("PEN"),
    ASSESSMENT_CODE("Assessment"),
    SESSION("Session"),
    FORM("Form"),
    SCHOOL_MINCODE("School"),
    SCHOOL_CATEGORY("School Category"),
    GRADE("Grade"),
    SELECTED_RESPONSE_SCORE("Selected Response Score"),
    CONSTRUCTED_RESPONSE_SCORE("Constructed Response Score"),
    TOTAL_SCORE("Total Score"),
    IRT("IRT"),
    SCORE("Proficiency Score"),
    SPECIAL_CASE("Special Case"),
    ASSESSMENT_CENTER("Assessment Center");

    private final String code;
    AllStudentDetailedRegistrationDetailsHeader(String code) { this.code = code; }
}
