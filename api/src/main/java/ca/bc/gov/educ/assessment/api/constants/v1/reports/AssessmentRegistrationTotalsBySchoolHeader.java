package ca.bc.gov.educ.assessment.api.constants.v1.reports;

import lombok.Getter;

@Getter
public enum AssessmentRegistrationTotalsBySchoolHeader {

    ASSESSMENT_TYPE("Assessment", 0),
    SCHOOL("School", 1),
    GRADE_01_COUNT("Grade 1", 2),
    GRADE_02_COUNT("Grade 2", 3),
    GRADE_03_COUNT("Grade 3", 4),
    GRADE_04_COUNT("Grade 4", 5),
    GRADE_05_COUNT("Grade 5", 6),
    GRADE_06_COUNT("Grade 6", 7),
    GRADE_07_COUNT("Grade 7", 8),
    GRADE_08_COUNT("Grade 8", 9),
    GRADE_09_COUNT("Grade 9", 10),
    GRADE_10_COUNT("Grade 10", 11),
    GRADE_11_COUNT("Grade 11", 12),
    GRADE_12_COUNT("Grade 12", 13),
    GRADE_AD_COUNT("AD", 14),
    GRADE_OT_COUNT("OT", 15),
    GRADE_HS_COUNT("HS", 16),
    GRADE_AN_COUNT("AN", 17),
    GRADE_GA_COUNT("GA", 18),
    GRADE_KF_COUNT("KF", 19),
    GRADE_KH_COUNT("KH", 20),
    GRADE_SU_COUNT("SU", 21),
    GRADE_EU_COUNT("EU", 22),
    BLANK_GRADE_COUNT("Blank", 23),
    TOTAL("TOTAL", 24);

    private final String code;
    private final int order;

    AssessmentRegistrationTotalsBySchoolHeader(String code, int order) {
        this.code = code;
        this.order = order;
    }
}