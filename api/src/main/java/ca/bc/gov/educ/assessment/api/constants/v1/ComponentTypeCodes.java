package ca.bc.gov.educ.assessment.api.constants.v1;

import lombok.Getter;

import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

@Getter
public enum ComponentTypeCodes {
  MUL_CHOICE("MUL_CHOICE"),
  OPEN_ENDED("OPEN_ENDED");

  private final String code;
  ComponentTypeCodes(String code) {
    this.code = code;
  }

  public static Optional<ComponentTypeCodes> findByValue(String value) {
    return Arrays.stream(values()).filter(e -> Objects.equals(e.code, value)).findFirst();
  }
}
