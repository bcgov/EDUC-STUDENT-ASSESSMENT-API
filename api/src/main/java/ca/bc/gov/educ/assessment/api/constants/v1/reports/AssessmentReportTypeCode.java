package ca.bc.gov.educ.assessment.api.constants.v1.reports;

import lombok.Getter;

import java.util.Arrays;
import java.util.Optional;

@Getter
public enum AssessmentReportTypeCode {
    ALL_SESSION_REGISTRATIONS("ALL_SESSION_REGISTRATIONS"),
    ATTEMPTS("ATTEMPTS"),
    PEN_MERGES("PEN_MERGES");
    private final String code;
    AssessmentReportTypeCode(String code) { this.code = code; }

    public static Optional<AssessmentReportTypeCode> findByValue(String value) {
        return Arrays.stream(values()).filter(e -> Arrays.asList(e.code).contains(value)).findFirst();
    }
}
