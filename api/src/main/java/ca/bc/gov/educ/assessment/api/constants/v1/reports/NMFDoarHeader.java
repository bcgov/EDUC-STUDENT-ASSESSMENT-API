package ca.bc.gov.educ.assessment.api.constants.v1.reports;

import lombok.Getter;

@Getter
public enum NMFDoarHeader {

    SESSION("Assessment Session "),
    SCHOOL_CODE("School Code"),
    ASSESSMENT_CODE("Assessment Code"),
    PEN("PEN"),
    LOCAL_ID("Student Local ID"),
    SURNAME("Student Surname"),
    GIVEN_NAME("Student Given"),
    PROFICIENCY_SCORE("Proficiency Score"),
    SPECIAL_CASE("Special Case"),
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
    NMFDoarHeader(String code) { this.code = code; }
}
