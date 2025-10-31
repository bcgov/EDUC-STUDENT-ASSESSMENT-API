package ca.bc.gov.educ.assessment.api.constants.v1.reports;

import lombok.Getter;

@Getter
public enum FrenchNMFDoarHeader {

    SESSION("Session d’évaluation"),
    SCHOOL_CODE("Code de l’école"),
    ASSESSMENT_CODE("Code de l’évaluation"),
    PEN("NSP"),
    LOCAL_ID("Identifiant local de l’élève"),
    SURNAME("Nom de famille de l’élève"),
    GIVEN_NAME("Prénoms de l’élève"),
    PROFICIENCY_SCORE("Palier de compétence"),
    SPECIAL_CASE("Circonstances inhabituelles"),
    TASK1("Tâche 1: Planification et conception (%)"),
    TASK2("Tâche 2: Estimations raisonnées (%)"),
    TASK3("Tâche 3: Partage équitable (%)"),
    TASK4("Tâche 4: Modélisation (%)"),
    CLAIM1("Processus 1 : Interpréter (%)"),
    CLAIM2("Processus 2 : Appliquer (%)"),
    CLAIM3("Processus 3 : Résoudre (%)"),
    CLAIM4("Processus 4 : Analyser (%)"),
    COGNITION1("Rigueur cognitive 1 (%)"),
    COGNITION2("Rigueur cognitive 2 (%)"),
    COGNITION3("Rigueur cognitive 3 (%)"),
    ;

    private final String code;
    FrenchNMFDoarHeader(String code) { this.code = code; }
}
