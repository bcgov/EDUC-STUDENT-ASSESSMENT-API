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
      var trimmed = StringUtils.trimToNull(value);
      return StringUtils.isNotBlank(trimmed) ? trimmed.toUpperCase() : null;
    }
    return value;
  }

}
