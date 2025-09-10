package ca.bc.gov.educ.assessment.api.constants.v1.reports;

import lombok.Getter;

@Getter
public enum RegistrationSummaryHeader {

    ASSESSMENT_TYPE("Assessment"),
    GRADE_04_COUNT("Grade 4"),
    GRADE_05_COUNT("Grade 5"),
    GRADE_06_COUNT("Grade 6"),
    GRADE_07_COUNT("Grade 7"),
    GRADE_08_COUNT("Grade 8"),
    GRADE_09_COUNT("Grade 9"),
    GRADE_10_COUNT("Grade 10"),
    GRADE_11_COUNT("Grade 11"),
    GRADE_12_COUNT("Grade 12"),
    GRADE_AD_COUNT("AD"),
    GRADE_OT_COUNT("OT"),
    GRADE_HS_COUNT("HS"),
    GRADE_AN_COUNT("AN"),
    TOTAL("TOTAL")
    ;

    private final String code;
    RegistrationSummaryHeader(String code) { this.code = code; }
}
