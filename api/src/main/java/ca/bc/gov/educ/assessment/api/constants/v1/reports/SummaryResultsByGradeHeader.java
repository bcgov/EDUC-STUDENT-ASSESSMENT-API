package ca.bc.gov.educ.assessment.api.constants.v1.reports;

import lombok.Getter;

@Getter
public enum SummaryResultsByGradeHeader {

    ASSESSMENT_CODE("Assessment"),
    SESSION("Session"),
    GRADE("Grade"),
    PROF_SCORE_1("Proficiency Score 1"),
    PROF_SCORE_2("Proficiency Score 2"),
    PROF_SCORE_3("Proficiency Score 3"),
    PROF_SCORE_4("Proficiency Score 4"),
    AEG("AEG"),
    NC("NC"),
    DSQ("DSQ"),
    XMT("XMT"),
    TOTAL("Total");

    private final String code;
    SummaryResultsByGradeHeader(String code) { this.code = code; }
}
