package ca.bc.gov.educ.assessment.api.struct.v1.reports;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class DistrictReportAvailability implements Serializable {
  private static final long serialVersionUID = 1L;

  private boolean resultsAvailable;
  private boolean doarSummaryAvailable;
  private boolean nmeDetailedDoar;
  private boolean nmfDetailedDoar;
  private boolean lte10DetailedDoar;
  private boolean lte12DetailedDoar;
  private boolean ltp10DetailedDoar;
  private boolean ltp12DetailedDoar;
  private boolean ltf12DetailedDoar;
}
