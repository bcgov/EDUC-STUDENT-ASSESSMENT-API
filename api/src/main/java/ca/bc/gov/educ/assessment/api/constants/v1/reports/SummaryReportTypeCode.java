package ca.bc.gov.educ.assessment.api.constants.v1.reports;

import lombok.Getter;

import java.util.Arrays;
import java.util.Optional;

@Getter
public enum SummaryReportTypeCode {
    REGISTRATION_SUMMARY("registration-summary"),
    ;

    private final String code;
    SummaryReportTypeCode(String code) { this.code = code; }

    public static Optional<SummaryReportTypeCode> findByValue(String value) {
        return Arrays.stream(values()).filter(e -> Arrays.asList(e.code).contains(value)).findFirst();
    }
}
