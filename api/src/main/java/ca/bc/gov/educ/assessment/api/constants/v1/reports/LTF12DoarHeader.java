package ca.bc.gov.educ.assessment.api.constants.v1.reports;

import lombok.Getter;

@Getter
public enum LTF12DoarHeader {

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
    TASK3("Task: Oral %"),
    COMPRE1("Comprehension: Part A %"),
    COMPRE2("Comprehension: Part B Info %"),
    COMPRE3("Comprehension: Part B Expression %"),
    COM1("Communication: Critical Analysis %"),
    COM2("Communication: Written Content %"),
    COM3("Communication: Written Form %"),
    COM4("Oral Communication: Part 1 Content %"),
    COM5("Oral Communication: Part 1 Form %"),
    COM6("Oral Communication: Part 1 Expression %"),
    COM7("Oral Communication: Part 2 Content %"),
    COM8("Oral Communication: Part 2 Form %"),
    COM9("Oral Communication: Part 2 Expression %"),
    COGNITION1("Cognitive Level: DOK1 %"),
    COGNITION2("Cognitive Level: DOK2 %"),
    COGNITION3("Cognitive Level: DOK3 %"),
    ;

    private final String code;
    LTF12DoarHeader(String code) { this.code = code; }
}
