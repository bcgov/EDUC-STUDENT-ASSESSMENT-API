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
    SCORE("Score"),
    SPECIAL_CASE("Special Case"),
    ;

    private final String code;
    RegistrationDetailsHeader(String code) { this.code = code; }
}
