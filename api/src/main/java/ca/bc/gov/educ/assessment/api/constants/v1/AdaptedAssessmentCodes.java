package ca.bc.gov.educ.assessment.api.constants.v1;

import lombok.Getter;

import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

@Getter
public enum AdaptedAssessmentCodes {
  BRAILLE("BRAILLE", "B"),
  LARGEPRINT("LARGEPRINT", "L");

  private final String code;
  private final String legacyCode;
  AdaptedAssessmentCodes(String code, String legacyCode) {
    this.code = code;
    this.legacyCode = legacyCode;
  }

  public static Optional<AdaptedAssessmentCodes> findByValue(String value) {
    return Arrays.stream(values()).filter(e -> Objects.equals(e.code, value)).findFirst();
  }

  public static Optional<AdaptedAssessmentCodes> findByLegacyValue(String value) {
    return Arrays.stream(values()).filter(e -> Objects.equals(e.legacyCode, value)).findFirst();
  }
}
