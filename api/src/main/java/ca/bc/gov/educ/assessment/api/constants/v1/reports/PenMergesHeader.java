package ca.bc.gov.educ.assessment.api.constants.v1.reports;

import lombok.Getter;

@Getter
public enum PenMergesHeader {

    CURRENT_PEN("Current PEN"),
    MERGED_PEN("Merged PEN");

    private final String code;
    PenMergesHeader(String code) { this.code = code; }
}
