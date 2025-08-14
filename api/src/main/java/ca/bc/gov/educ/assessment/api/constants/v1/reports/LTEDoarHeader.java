package ca.bc.gov.educ.assessment.api.constants.v1.reports;

import lombok.Getter;

@Getter
public enum LTEDoarHeader {

    SESSION("Assessment Session "),
    SCHOOL_CODE("School Code"),
    ASSESSMENT_CODE("Assessment Code"),
    PEN("PEN"),
    LOCAL_ID("Student Local ID"),
    SURNAME("Student Surname"),
    GIVEN_NAME("Student Given"),
    PROFICIENCY_SCORE("Proficiency Score"),
    SPECIAL_CASE("Special Case"),
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
    LTEDoarHeader(String code) { this.code = code; }
}
