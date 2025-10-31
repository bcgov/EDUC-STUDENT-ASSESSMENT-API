package ca.bc.gov.educ.assessment.api.constants.v1.reports;

import lombok.Getter;

@Getter
public enum FrenchLTEDoarHeader {

    SESSION("Session d’évaluation"),
    SCHOOL_CODE("Code de l’école"),
    ASSESSMENT_CODE("Code de l’évaluation"),
    PEN("NSP"),
    LOCAL_ID("Identifiant local de l’élève"),
    SURNAME("Nom de famille de l’élève"),
    GIVEN_NAME("Prénoms de l’élève"),
    PROFICIENCY_SCORE("Palier de compétence"),
    SPECIAL_CASE("Circonstances inhabituelles"),
    TASK1("Task: Comprehend %"),
    TASK2("Task: Communicate %"),
    COMPRE1("Comprehension: Part A %"),
    COMPRE2("Comprehension: Com Und"),
    COM1("Communication: Part A Graphic Organizer %"),
    COM2("Communication: Part A Written %"),
    COM3("Communication: Part B Com PCon"),
    COGNITION1("Cognitive Level: DOK1 %"),
    COGNITION2("Cognitive Level: DOK2 %"),
    COGNITION3("Cognitive Level: DOK3 %"),
    ;

    private final String code;
    FrenchLTEDoarHeader(String code) { this.code = code; }
}
