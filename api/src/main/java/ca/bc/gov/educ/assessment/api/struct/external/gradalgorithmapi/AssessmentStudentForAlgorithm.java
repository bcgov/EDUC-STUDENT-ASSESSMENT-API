package ca.bc.gov.educ.assessment.api.struct.external.gradalgorithmapi;

import ca.bc.gov.educ.assessment.api.struct.v1.BaseRequest;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@JsonIgnoreProperties(ignoreUnknown = true)
@ToString(callSuper = true)
public class AssessmentStudentForAlgorithm extends BaseRequest {

  private String pen;
  private String assessmentCode;
  private String sessionDate;
  private String specialCase;
  private Boolean exceededWriteFlag;
  private Double proficiencyScore;
  private Boolean wroteFlag;
  private Double rawScore;
  private Double irtScore;
}
