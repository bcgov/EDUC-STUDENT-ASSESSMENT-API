package ca.bc.gov.educ.assessment.api.constants.v1;

import lombok.Getter;

import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

@Getter
public enum ComponentSubTypeCodes {
  NONE("NONE"),
  ORAL("ORAL");

  private final String code;
  ComponentSubTypeCodes(String code) {
    this.code = code;
  }

  public static Optional<ComponentSubTypeCodes> findByValue(String value) {
    return Arrays.stream(values()).filter(e -> Objects.equals(e.code, value)).findFirst();
  }
}
