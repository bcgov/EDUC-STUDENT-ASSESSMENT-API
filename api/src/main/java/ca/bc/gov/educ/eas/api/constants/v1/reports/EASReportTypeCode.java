package ca.bc.gov.educ.eas.api.constants.v1.reports;

import lombok.Getter;

import java.util.Arrays;
import java.util.Optional;

@Getter
public enum EASReportTypeCode {
    ALL_SESSION_REGISTRATIONS("ALL_SESSION_REGISTRATIONS");

    private final String code;
    EASReportTypeCode(String code) { this.code = code; }

    public static Optional<EASReportTypeCode> findByValue(String value) {
        return Arrays.stream(values()).filter(e -> Arrays.asList(e.code).contains(value)).findFirst();
    }
}
