package ca.bc.gov.educ.assessment.api.struct.v1;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.springframework.data.annotation.ReadOnlyProperty;

import java.io.Serializable;
import java.util.List;

/**
 * DTO for Session entity.
 */
@EqualsAndHashCode(callSuper = true)
@Data
@SuperBuilder
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class AssessmentSession extends BaseRequest implements Serializable {

  private static final long serialVersionUID = 1L;

  @ReadOnlyProperty
  private String sessionID;

  @ReadOnlyProperty
  private String schoolYear;

  @ReadOnlyProperty
  private String courseMonth;

  @ReadOnlyProperty
  private String courseYear;

  private String approvalStudentCertUserID;

  private String approvalStudentCertSignDate;

  private String approvalAssessmentDesignUserID;

  private String approvalAssessmentDesignSignDate;

  private String approvalAssessmentAnalysisUserID;

  private String approvalAssessmentAnalysisSignDate;

  @NotNull(message = "activeFromDate cannot be null")
  private String activeFromDate;

  @NotNull(message = "activeUntilDate cannot be null")
  private String activeUntilDate;

  private List<Assessment> assessments;
}
