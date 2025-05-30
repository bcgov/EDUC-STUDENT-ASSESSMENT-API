package ca.bc.gov.educ.assessment.api.mappers;

import org.apache.commons.lang3.StringUtils;

public class StringMapper {

  private StringMapper() {
  }

  public static String map(final String value) {
    if (StringUtils.isNotBlank(value)) {
      return value.trim();
    }
    return value;
  }

  public static String trimAndUppercase(String value){
    if (StringUtils.isNotBlank(value)) {
      return StringUtils.trim(value).toUpperCase();
    }
    return value;
  }

  public static String removeLeadingApostrophes(String value) {
    if (StringUtils.isNotBlank(value)) {
      value = value.trim();
      if (value.startsWith("'") && value.length() == 1) {
        return null;
      }
    }
    return value;
  }

  public static String processGivenName(String value) {
    String legalGivenName = removeLeadingApostrophes(value);
    return trimAndUppercase(legalGivenName);
  }
}
