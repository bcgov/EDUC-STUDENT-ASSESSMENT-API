package ca.bc.gov.educ.assessment.api.constants.v1.reports;

import lombok.Getter;

@Getter
public enum LTP12DoarHeader {

    SESSION("Assessment Session "),
    SCHOOL_CODE("School Code"),
    ASSESSMENT_CODE("Assessment Code"),
    PEN("PEN"),
    LOCAL_ID("Student Local ID"),
    SURNAME("Student Surname"),
    GIVEN_NAME("Student Given"),
    PROFICIENCY_SCORE("Proficiency Score"),
    SPECIAL_CASE("Special Case"),
    TASK1("Task: Compréhension %"),
    TASK2("Task: Communication écrite % "),
    TASK3("Task: Communication orale %"),
    COMPRE1("Compréhension: Partie A %"),
    COMPRE2("Compréhension: Partie B %"),
    COM1("Communication écrite: Partie A Organigramme %"),
    COM2("Communication écrite: Partie A Texte argumentatif %"),
    COM3("Communication écrite: Partie B Communication écrite créative %"),
    COM4("Communication orale: Partie 1 %"),
    COM5("Communication orale: Partie 2 %"),
    COM6("Communication orale: Partie 3 %"),
    COGNITION1("Rigueur cognitive: Niveau 1 %"),
    COGNITION2("Rigueur cognitive: Niveau 2 %"),
    COGNITION3("Rigueur cognitive: Niveau 3 %"),
    ;

    private final String code;
    LTP12DoarHeader(String code) { this.code = code; }
}
