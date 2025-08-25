package ca.bc.gov.educ.assessment.api.struct.v1.reports.isr;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@SuppressWarnings("squid:S1700")
public class ISRAssessmentSummary implements Serializable {
  private static final long serialVersionUID = 6118916290604876032L;
  private String session;
  private String assessment;
  private String score;
}
