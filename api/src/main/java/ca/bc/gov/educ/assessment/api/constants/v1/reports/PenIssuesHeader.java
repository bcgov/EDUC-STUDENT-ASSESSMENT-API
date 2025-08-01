package ca.bc.gov.educ.assessment.api.constants.v1.reports;

import lombok.Getter;

@Getter
public enum PenIssuesHeader {

    PEN("PEN"),
    MERGED_PEN("Merged Pen"),
    ISSUE("Issue"),
    ;

    private final String code;
    PenIssuesHeader(String code) { this.code = code; }
}
