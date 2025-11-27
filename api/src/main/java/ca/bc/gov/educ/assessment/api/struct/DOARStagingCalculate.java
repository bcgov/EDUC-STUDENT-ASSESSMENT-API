package ca.bc.gov.educ.assessment.api.struct;

import ca.bc.gov.educ.assessment.api.model.v1.AssessmentFormEntity;
import ca.bc.gov.educ.assessment.api.model.v1.StagedAssessmentStudentEntity;

public interface DOARStagingCalculate {
   String  calculateStagingTotal(AssessmentFormEntity selectedAssessmentForm, StagedAssessmentStudentEntity student, String code, boolean includeChoiceCalc);

}
