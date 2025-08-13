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
    NMFDoarHeader(String code) { this.code = code; }
}
