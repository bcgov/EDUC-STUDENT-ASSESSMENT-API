package ca.bc.gov.educ.assessment.api.constants.v1.reports;

import lombok.Getter;

@Getter
public enum FrenchSessionResultsHeader {
    ASSESSMENT_SESSION("Session d’évaluation"),
    SCHOOL_CODE("Code de l’école"),
    ASSESSMENT_CODE("Code de l’évaluation"),
    STUDENT_PEN("NSP"),
    STUDENT_LOCAL_ID("Identifiant local de l’élève"),
    STUDENT_SURNAME("Nom de famille de l’élève"),
    STUDENT_GIVEN("Prénoms de l’élève"),
    ASSESSMENT_PROFICIENCY_SCORE("Palier de compétence"),
    SPECIAL_CASE("Circonstances inhabituelles"),
    ASSESSMENT_CENTRE_SCHOOL_CODE("Code de l’école servant de centre d’évaluation");

    private final String code;
    FrenchSessionResultsHeader(String code) { this.code = code; }
}
