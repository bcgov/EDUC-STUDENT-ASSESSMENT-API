package ca.bc.gov.educ.assessment.api.util;

import ca.bc.gov.educ.assessment.api.constants.v1.AdaptedAssessmentCodes;
import ca.bc.gov.educ.assessment.api.constants.v1.AssessmentTypeCodes;
import ca.bc.gov.educ.assessment.api.exception.StudentAssessmentAPIRuntimeException;
import io.micrometer.common.util.StringUtils;
import lombok.extern.slf4j.Slf4j;

import java.beans.Expression;
import java.beans.Statement;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.springframework.util.StringUtils.capitalize;

/**
 * The type Transform util.
 */

@Slf4j
public class AssessmentUtil {
  private AssessmentUtil() {
  }

  public static List<String> getAssessmentTypeCodeList(String assessmentTypeCode){
    if(assessmentTypeCode.startsWith("NM")){
      return Arrays.asList(AssessmentTypeCodes.NME.getCode(), AssessmentTypeCodes.NME10.getCode(), AssessmentTypeCodes.NMF.getCode(), AssessmentTypeCodes.NMF10.getCode());
    }
    return Arrays.asList(assessmentTypeCode);
  }
  
  public static String getAdaptedAssessmentCode(String legacyValue){
    if(StringUtils.isNotBlank(legacyValue)){
      var code = AdaptedAssessmentCodes.findByLegacyValue(legacyValue);
      if(code.isPresent()){
        return code.get().getCode();
      }
    }
    return null;
  }
}
