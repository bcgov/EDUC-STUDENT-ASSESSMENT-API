package ca.bc.gov.educ.assessment.api.constants.v1;

import lombok.Getter;

import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

@Getter
public enum LegacyComponentTypeCodes {
  MUL_CHOICE("1"),
  OPEN_ENDED("2"),
  BOTH("3"),
  ORAL("7");

  private final String code;
  LegacyComponentTypeCodes(String code) {
    this.code = code;
  }

  public static Optional<LegacyComponentTypeCodes> findByValue(String value) {
    return Arrays.stream(values()).filter(e -> Objects.equals(e.code, value)).findFirst();
  }
}
