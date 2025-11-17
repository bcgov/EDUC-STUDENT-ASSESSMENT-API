package ca.bc.gov.educ.assessment.api.struct.v1;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.List;
import java.util.UUID;

@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssessmentStudentTransfer extends BaseRequest{
  @NotNull
  private UUID sourceStudentID;

  @NotNull
  private UUID targetStudentID;

  @NotEmpty
  private List<UUID> studentAssessmentIDsToMove;

}
