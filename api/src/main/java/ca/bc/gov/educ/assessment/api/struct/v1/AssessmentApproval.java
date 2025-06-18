package ca.bc.gov.educ.assessment.api.struct.v1;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.io.Serializable;

@EqualsAndHashCode(callSuper = true)
@Data
@SuperBuilder
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class AssessmentApproval extends BaseRequest implements Serializable {

  private static final long serialVersionUID = 1L;

  @NotNull
  private String sessionID;

  private String approvalStudentCertUserID;

  private String approvalAssessmentDesignUserID;

  private String approvalAssessmentAnalysisUserID;

}
