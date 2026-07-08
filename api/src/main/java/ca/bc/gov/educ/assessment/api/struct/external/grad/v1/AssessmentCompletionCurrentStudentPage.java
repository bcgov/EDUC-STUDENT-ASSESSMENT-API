package ca.bc.gov.educ.assessment.api.struct.external.grad.v1;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class AssessmentCompletionCurrentStudentPage {
  private List<ReportGradStudentData> content;
  private Integer pageNumber;
  private Integer pageSize;
  private Integer numberOfElements;
  private Boolean hasNext;
}
