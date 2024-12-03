package ca.bc.gov.educ.eas.api.rules;

import lombok.Getter;

import java.util.Arrays;
import java.util.Optional;

public enum StudentValidationIssueSeverityCode {

  ERROR("ERROR","Error"),
  WARNING("WARNING","Warning"),
  ;

  @Getter
  private final String label;
  @Getter
  private final String code;
  StudentValidationIssueSeverityCode(String code, String label) {
    this.code = code;
    this.label = label;
  }

  public static Optional<StudentValidationIssueSeverityCode> findByValue(String value) {
    return Arrays.stream(values()).filter(e -> Arrays.asList(e.code).contains(value)).findFirst();
  }
}
