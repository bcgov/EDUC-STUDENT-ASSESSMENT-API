package ca.bc.gov.educ.eas.api.constants.v1;

import lombok.Getter;

import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

@Getter
public enum AssessmentTypeCodes {
  LTE10("LTE10"),
  LTE12("LTE12"),
  LTF12("LTF12"),
  LTP10("LTP10"),
  LTP12("LTP12"),
  NME("NME"),
  NME10("NME10"),
  NMF("NMF"),
  NMF10("NMF10");

  private final String code;
  AssessmentTypeCodes(String code) {
    this.code = code;
  }

  public static Optional<AssessmentTypeCodes> findByValue(String value) {
    return Arrays.stream(values()).filter(e -> Objects.equals(e.code, value)).findFirst();
  }
}
