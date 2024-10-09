package ca.bc.gov.educ.eas.api.struct.v1;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
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
public class AssessmentStudent extends BaseRequest implements Serializable {
  private static final long serialVersionUID = 1L;

  private String assessmentStudentID;

  @NotNull(message = "sessionID cannot be null")
  private String sessionID;

  @NotNull(message = "assessmentTypeCode cannot be null")
  @Size(max = 10)
  private String assessmentTypeCode;

  @NotNull(message = "schoolID cannot be null")
  private String schoolID;

  @NotNull(message = "studentID cannot be null")
  private String studentID;

  @NotNull(message = "pen cannot be null")
  @Size(max = 9)
  private String pen;

  @Size(max = 12)
  private String localID;

  private Boolean isElectronicExam;

  @Size(max = 3)
  private String finalPercentage;

  @Size(max = 1)
  private String provincialSpecialCaseCode;

  @Size(max = 1)
  private String courseStatusCode;
}
