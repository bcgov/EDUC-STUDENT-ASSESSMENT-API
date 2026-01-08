package ca.bc.gov.educ.assessment.api.constants.v1.reports;

import lombok.Getter;

@Getter
public enum YukonSummaryReportHeader {

    SESSION("Session"),
    MINCODE("Mincode"),
    LTE10("LTE10"),
    LTE12("LTE12"),
    LTP10("LTP10"),
    NME10("NME10"),
    NMF10("NMF10"),
    LTP12("LTP12"),
    LTF12("LTF12"),
    TOTAL("TOTAL")
    ;

    private final String code;
    YukonSummaryReportHeader(String code) { this.code = code; }
}
