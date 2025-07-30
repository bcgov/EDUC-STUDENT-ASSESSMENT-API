package ca.bc.gov.educ.assessment.api.constants.v1;

import lombok.Getter;

@Getter
public enum PenStatusCodeDesc {
  MERGED("PEN MERGED"),
  NOPENFOUND("PEN NOT FOUND");

  private final String code;
  PenStatusCodeDesc(String code) {
    this.code = code;
  }
}
