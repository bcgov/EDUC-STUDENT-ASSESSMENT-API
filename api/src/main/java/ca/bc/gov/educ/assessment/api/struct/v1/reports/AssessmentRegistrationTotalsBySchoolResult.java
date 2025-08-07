package ca.bc.gov.educ.assessment.api.struct.v1.reports;

public interface AssessmentRegistrationTotalsBySchoolResult {
    String getAssessmentID();
    String getSchoolOfRecordSchoolID();
    String getGrade8Count();
    String getGrade9Count();
    String getGrade10Count();
    String getGrade11Count();
    String getGrade12Count();
    String getGradeADCount();
    String getGradeOTCount();
    String getGradeHSCount();
    String getGradeANCount();
    String getBlankGradeCount();
    String getTotal();
}
