package ca.bc.gov.educ.assessment.api.constants.v1.reports;

import lombok.Getter;

import java.util.Arrays;
import java.util.Optional;

@Getter
public enum AssessmentReportTypeCode {
    ALL_SESSION_REGISTRATIONS("ALL_SESSION_REGISTRATIONS"),
    SCHOOL_STUDENTS_IN_SESSION("SCHOOL_STUDENTS_IN_SESSION"),
    ATTEMPTS("ATTEMPTS"),
    PEN_MERGES("PEN_MERGES"),
    XAM_FILE("XAM_FILE"),
    SESSION_RESULTS("SESSION_RESULTS");
    private final String code;
    AssessmentReportTypeCode(String code) { this.code = code; }

    public static Optional<AssessmentReportTypeCode> findByValue(String value) {
        return Arrays.stream(values()).filter(e -> Arrays.asList(e.code).contains(value)).findFirst();
    }
}
