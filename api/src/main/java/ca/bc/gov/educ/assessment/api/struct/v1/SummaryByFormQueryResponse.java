package ca.bc.gov.educ.assessment.api.struct.v1;

import java.util.UUID;

public interface SummaryByFormQueryResponse {

  String getAssessmentTypeCode();
  UUID getFormID();
  long getProfScore1();
  long getProfScore2();
  long getProfScore3();
  long getProfScore4();
  long getAegCount();
  long getNcCount();
  long getDsqCount();
  long getXmtCount();
  long getTotal();
}
