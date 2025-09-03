package ca.bc.gov.educ.assessment.api.constants.v1.reports;

import lombok.Getter;

@Getter
public enum RegistrationDetailsHeader {

    PEN("PEN"),
    GRADE("Grade"),
    SURNAME("Surname"),
    ASSESSMENT("Assessment"),
    SESSION("Session"),
    MINCODE("School"),
    DISTRICT_NUMBER("District"),
    SCHOOL_CATEGORY("School Category"),
    ;

    private final String code;
    RegistrationDetailsHeader(String code) { this.code = code; }
}
