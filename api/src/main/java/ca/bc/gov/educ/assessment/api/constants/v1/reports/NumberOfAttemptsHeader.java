package ca.bc.gov.educ.assessment.api.constants.v1.reports;

import lombok.Getter;

@Getter
public enum NumberOfAttemptsHeader {

    ASSESSMENT_GROUP("Assmt Group"),
    PEN("Current PEN"),
    ATTEMPTS("Attempts");

    private final String code;
    NumberOfAttemptsHeader(String code) { this.code = code; }
}
