package ca.bc.gov.educ.assessment.api.constants.v1.reports;

import lombok.Getter;

@Getter
public enum DataItemAnalysisHeader {
    PEN("PEN"),
    SESSION("Session"),
    GRADE("Grade"),
    MINCODE("Mincode"),
    ASSESSMENT_CODE("Assessment Code"),
    FORM("Form"),
    GENDER("Gender"),
    FRANCOPHONE("Francophone"),
    EARLY_FRENCH_IMMERSION("Early Fr Immersion"),
    LATE_FRENCH_IMMERSION("Late Fr Immersion"),
    ELL("ELL"),
    INDIGENOUS_ANCESTRY("Indigenous Ancestry"),
    SLD_COLLECTION("SLD Collection"),
    PROFICIENCY_SCORE("Proficiency Score"),
    MC_SCORE("MC Score"),
    OE_SCORE("OE Score"),
    TOTAL_SCORE("Total Score"),
    IRT("IRT");

    private final String code;
    DataItemAnalysisHeader(String code) { this.code = code; }
}
