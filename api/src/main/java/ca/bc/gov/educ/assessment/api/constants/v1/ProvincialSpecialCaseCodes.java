package ca.bc.gov.educ.assessment.api.constants.v1;

import lombok.Getter;

import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

@Getter
public enum ProvincialSpecialCaseCodes {
  AEGROTAT("A", "AEG"),
  EXEMPT("E", "XMT"),
  DISQUALIFIED("Q", "DSQ"),
  NOTCOMPLETED("X", "NC");

  private final String code;
  private final String description;
  ProvincialSpecialCaseCodes(String code, String description) {
    this.code = code;
    this.description = description;
  }

  public static Optional<ProvincialSpecialCaseCodes> findByValue(String value) {
    return Arrays.stream(values()).filter(e -> Objects.equals(e.code, value)).findFirst();
  }
}
