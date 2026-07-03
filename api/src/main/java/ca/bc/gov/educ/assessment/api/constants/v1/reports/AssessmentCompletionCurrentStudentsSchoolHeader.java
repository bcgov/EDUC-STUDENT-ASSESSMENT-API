package ca.bc.gov.educ.assessment.api.constants.v1.reports;

import lombok.Getter;

@Getter
public enum AssessmentCompletionCurrentStudentsSchoolHeader {
    PEN("PEN"),
    LOCAL_ID("Local ID"),
    LAST_NAME("Last Name"),
    FIRST_NAME("First Name"),
    MIDDLE_NAME("Middle Name"),
    BIRTHDATE("Birthdate"),
    GRADE("Grade"),
    PROGRAM("Program"),
    LTE10("LTE10"),
    NME10("NME10"),
    NMF10("NMF10"),
    LTE12("LTE12"),
    LTF12("LTF12"),
    LTP10("LTP10"),
    LTP12("LTP12");

    private final String code;

    AssessmentCompletionCurrentStudentsSchoolHeader(String code) {
        this.code = code;
    }
}
