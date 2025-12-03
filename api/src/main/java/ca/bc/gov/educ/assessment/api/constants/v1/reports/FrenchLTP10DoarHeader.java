package ca.bc.gov.educ.assessment.api.constants.v1.reports;

import lombok.Getter;

@Getter
public enum FrenchLTP10DoarHeader {

    SESSION("Session d’évaluation"),
    SCHOOL_CODE("Code de l’école"),
    ASSESSMENT_CODE("Code de l’évaluation"),
    PEN("NSP"),
    LOCAL_ID("Identifiant local de l’élève"),
    SURNAME("Nom de famille de l’élève"),
    GIVEN_NAME("Prénoms de l’élève"),
    PROFICIENCY_SCORE("Palier de compétence"),
    SPECIAL_CASE("Circonstances inhabituelles"),
    TASK1("Task: Compréhension %"),
    TASK2("Task: Communication écrite % "),
    TASK3("Task: Communication orale %"),
    COMPRE1("Compréhension: Partie A %"),
    COMPRE2("Compréhension: Partie B %"),
    COM1("Communication écrite: Partie A Courte %"),
    COM2("Communication écrite: Partie A  Organigramme %"),
    COM3("Communication écrite: Partie A Longue %"),
    COM4("Communication écrite: Partie B Courte %"),
    COM5("Communication orale: Partie 1 %"),
    COM6("Communication orale: Partie 2 %"),
    COM7("Communication orale: Partie 3 %"),
    COGNITION1("Rigueur cognitive: Niveau 1 %"),
    COGNITION2("Rigueur cognitive: Niveau 2 %"),
    COGNITION3("Rigueur cognitive: Niveau 3 %"),
    ;

    private final String code;
    FrenchLTP10DoarHeader(String code) { this.code = code; }
}
