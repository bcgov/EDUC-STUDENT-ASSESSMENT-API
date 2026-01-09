package ca.bc.gov.educ.assessment.api.constants.v1.reports;

import lombok.Getter;

@Getter
public enum LTP10DoarHeader {

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
    TASK2("Task: Written %"),
    TASK3("Task: Oral %"),
    COMPRE1("Comprehension: Part A %"),
    COMPRE2("Comprehension: Part B %"),
    COM1("Communication: Part A Short %"),
    COM2("Communication: Part A Graphic Organizer %"),
    COM3("Communication: Part A Long %"),
    COM4("Communication: Part B Short %"),
    COM5("Oral Communication: Part 1 %"),
    COM6("Oral Communication: Part 2 %"),
    COM7("Oral Communication: Part 3 %"),
    COGNITION1("Cognitive Level: DOK1 %"),
    COGNITION2("Cognitive Level: DOK2 %"),
    COGNITION3("Cognitive Level: DOK3 %"),
    ;

    private final String code;
    LTP10DoarHeader(String code) { this.code = code; }
}
