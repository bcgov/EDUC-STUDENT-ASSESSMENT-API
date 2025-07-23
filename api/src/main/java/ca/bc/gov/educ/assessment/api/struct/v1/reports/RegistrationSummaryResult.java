package ca.bc.gov.educ.assessment.api.struct.v1.reports;

public interface RegistrationSummaryResult {
    String getAssessmentID();
    String getGrade8Count();
    String getGrade9Count();
    String getGrade10Count();
    String getGrade11Count();
    String getGrade12Count();
    String getGradeADCount();
    String getGradeOTCount();
    String getGradeHSCount();
    String getGradeANCount();
    String getTotal();
}
