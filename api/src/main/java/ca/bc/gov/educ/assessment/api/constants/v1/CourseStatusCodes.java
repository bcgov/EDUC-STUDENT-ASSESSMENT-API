package ca.bc.gov.educ.assessment.api.constants.v1;

import lombok.Getter;

import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

@Getter
public enum CourseStatusCodes {
  ACTIVE("A"),
  WITHDRAWN("W");

  private final String code;
  CourseStatusCodes(String code) {
    this.code = code;
  }

  public static Optional<CourseStatusCodes> findByValue(String value) {
    return Arrays.stream(values()).filter(e -> Objects.equals(e.code, value)).findFirst();
  }
}
