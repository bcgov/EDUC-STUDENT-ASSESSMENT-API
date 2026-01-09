package ca.bc.gov.educ.assessment.api.constants.v1.reports;

import lombok.Getter;

@Getter
public enum YukonStudentResultsHeader {
    STUDENT_PEN("PEN"),
    SCHOOL_CODE("School Code"),
    ASSESSMENT_CODE("Assessment"),
    ASSESSMENT_SESSION("Session"),
    ASSESSMENT_PROFICIENCY_SCORE("Proficiency Score"),
    SPECIAL_CASE("Special Case");
    
    private final String code;
    YukonStudentResultsHeader(String code) { this.code = code; }
}
