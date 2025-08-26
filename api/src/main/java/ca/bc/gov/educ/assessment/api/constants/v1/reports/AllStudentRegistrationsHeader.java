package ca.bc.gov.educ.assessment.api.constants.v1.reports;

import lombok.Getter;

@Getter
public enum AllStudentRegistrationsHeader {

    SCHOOL_MINCODE("School"),
    PEN("PEN"),
    GIVEN_NAME("Given Name"),
    SURNAME("Surname"),
    ASSESSMENT_CENTRE("Assmt Centre"),
    ASSESSMENT_CODE("Assmt Code");

    private final String code;
    AllStudentRegistrationsHeader(String code) { this.code = code; }
}
