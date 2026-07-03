package ca.bc.gov.educ.assessment.api.struct.v1.reports;

public interface AssessmentCompletionSummaryResult {
  String getPen();
  Integer getLte10Completed();
  Integer getNme10Completed();
  Integer getNmf10Completed();
  Integer getLte12Completed();
  Integer getLtf12Completed();
  Integer getLtp10Completed();
  Integer getLtp12Completed();
}
