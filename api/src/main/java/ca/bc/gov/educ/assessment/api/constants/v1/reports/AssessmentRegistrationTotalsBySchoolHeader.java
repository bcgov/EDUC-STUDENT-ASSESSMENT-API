package ca.bc.gov.educ.assessment.api.constants.v1.reports;

import lombok.Getter;

@Getter
public enum AssessmentRegistrationTotalsBySchoolHeader {

    ASSESSMENT_TYPE("Assessment", 0),
    SCHOOL("School", 1),
    GRADE_04_COUNT("Grade 4", 2),
    GRADE_05_COUNT("Grade 5", 3),
    GRADE_06_COUNT("Grade 6", 4),
    GRADE_07_COUNT("Grade 7", 5),
    GRADE_08_COUNT("Grade 8", 6),
    GRADE_09_COUNT("Grade 9", 7),
    GRADE_10_COUNT("Grade 10", 8),
    GRADE_11_COUNT("Grade 11", 9),
    GRADE_12_COUNT("Grade 12", 10),
    GRADE_AD_COUNT("AD", 11),
    GRADE_OT_COUNT("OT", 12),
    GRADE_HS_COUNT("HS", 13),
    GRADE_AN_COUNT("AN", 14),
    BLANK_GRADE_COUNT("Blank", 15),
    TOTAL("TOTAL", 16);

    private final String code;
    private final int order;

    AssessmentRegistrationTotalsBySchoolHeader(String code, int order) {
        this.code = code;
        this.order = order;
    }
}
