package ca.bc.gov.educ.assessment.api.struct.v1;

import ca.bc.gov.educ.assessment.api.model.v1.AssessmentFormEntity;
import ca.bc.gov.educ.assessment.api.model.v1.AssessmentStudentEntity;

public interface DOARCalculate {
    String  calculateTotal(AssessmentFormEntity selectedAssessmentForm, AssessmentStudentEntity student, String code);

}
