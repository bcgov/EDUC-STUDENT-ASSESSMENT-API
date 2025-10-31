package ca.bc.gov.educ.assessment.api.constants.v1.reports;

import lombok.Getter;

@Getter
public enum FrenchLTF12DoarHeader {

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
    COMPRE2("Compréhension: Partie B Information %"),
    COMPRE3("Compréhension: Partie B Expression %"),
    COM1("Communication écrite: Analyze %"),
    COM2("Communication écrite: Dissertation Fond %"),
    COM3("Communication écrite: Dissertation Forme %"),
    COM4("Communication orale: Partie 1 Prise de postition Fond %"),
    COM5("Communication orale: Partie 1 Prise de position Forme %"),
    COM6("Communication orale: Partie 1 Prise de position Expression orale %"),
    COM7("Communication orale: Partie 2 Prise de postition Fond %"),
    COM8("Communication orale: Partie 2 Prise de position Forme %"),
    COM9("Communication orale: Partie 2 Prise de position Expression orale %"),
    COGNITION1("Rigueur cognitive: Niveau 1 %"),
    COGNITION2("Rigueur cognitive: Niveau 2 %"),
    COGNITION3("Rigueur cognitive: Niveau 3 %"),
    ;

    private final String code;
    FrenchLTF12DoarHeader(String code) { this.code = code; }
}
