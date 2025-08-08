package ca.bc.gov.educ.assessment.api.constants.v1.reports;

import lombok.Getter;

@Getter
public enum AssessmentRegistrationTotalsBySchoolHeader {

    ASSESSMENT_TYPE("Assessment", 0),
    SCHOOL("School", 1),
    GRADE_08_COUNT("Grade 8", 2),
    GRADE_09_COUNT("Grade 9", 3),
    GRADE_10_COUNT("Grade 10", 4),
    GRADE_11_COUNT("Grade 11", 5),
    GRADE_12_COUNT("Grade 12", 6),
    GRADE_AD_COUNT("AD", 7),
    GRADE_OT_COUNT("OT", 8),
    GRADE_HS_COUNT("HS", 9),
    GRADE_AN_COUNT("AN", 10),
    BLANK_GRADE_COUNT("Blank", 11),
    TOTAL("TOTAL", 12);

    private final String code;
    private final int order;

    AssessmentRegistrationTotalsBySchoolHeader(String code, int order) {
        this.code = code;
        this.order = order;
    }
}
