package ca.bc.gov.educ.assessment.api.constants.v1.reports;

import lombok.Getter;

import java.util.Arrays;
import java.util.Optional;

@Getter
public enum AssessmentStudentReportTypeCode {
    ISR("ISR");
    private final String code;
    AssessmentStudentReportTypeCode(String code) { this.code = code; }

    public static Optional<AssessmentStudentReportTypeCode> findByValue(String value) {
        return Arrays.stream(values()).filter(e -> Arrays.asList(e.code).contains(value)).findFirst();
    }
}
