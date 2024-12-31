package ca.bc.gov.educ.eas.api.batch.constants;


import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

public enum AssessmentFileStatus {
    LOAD_FAIL("LOAD_FAIL");

    private final String code;

    AssessmentFileStatus(String code) {
        this.code = code;
    }

    public static Optional<AssessmentFileStatus> findByValue(String value) {
        return Arrays.stream(values()).filter(e -> Objects.equals(e.code, value)).findFirst();
    }
}
