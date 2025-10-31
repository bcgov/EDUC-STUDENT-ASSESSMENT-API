package ca.bc.gov.educ.assessment.api.constants.v1.reports;

import lombok.Getter;

@Getter
public enum FrenchNMEDoarHeader {

    SESSION("Session d’évaluation"),
    SCHOOL_CODE("Code de l’école"),
    ASSESSMENT_CODE("Code de l’évaluation"),
    PEN("NSP"),
    LOCAL_ID("Identifiant local de l’élève"),
    SURNAME("Nom de famille de l’élève"),
    GIVEN_NAME("Prénoms de l’élève"),
    PROFICIENCY_SCORE("Palier de compétence"),
    SPECIAL_CASE("Circonstances inhabituelles"),
    TASK1("Task 1: Plan and Design %"),
    TASK2("Task 2: Reasoned Estimates %"),
    TASK3("Task 3: Fair Share %"),
    TASK4("Task 4: Model %"),
    CLAIM1("Claim 1: Interpret %"),
    CLAIM2("Claim 2: Apply %"),
    CLAIM3("Claim 3: Solve %"),
    CLAIM4("Claim 4: Analyze %"),
    COGNITION1("Cognition 1: L1%"),
    COGNITION2("Cognition 2: L2%"),
    COGNITION3("Cognition 3: L3%"),
    ;

    private final String code;
    FrenchNMEDoarHeader(String code) { this.code = code; }
}
