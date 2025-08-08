package ca.bc.gov.educ.assessment.api.struct.v1;

public interface SummaryByGradeQueryResponse {

  String getAssessmentTypeCode();
  String getGrade();
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
