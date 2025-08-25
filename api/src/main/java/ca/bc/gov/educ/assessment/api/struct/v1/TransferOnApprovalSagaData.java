package ca.bc.gov.educ.assessment.api.struct.v1;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.io.Serializable;

@SuperBuilder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TransferOnApprovalSagaData implements Serializable {
  private static final long serialVersionUID = -2329245910142215178L;
  private String stagedStudentAssessmentID;
  private String studentID;
  private String assessmentID;
}
