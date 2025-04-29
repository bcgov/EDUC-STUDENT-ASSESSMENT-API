package ca.bc.gov.educ.assessment.api.rules.assessment.ruleset;

import ca.bc.gov.educ.assessment.api.rules.assessment.AssessmentStudentValidationFieldCode;
import ca.bc.gov.educ.assessment.api.rules.assessment.AssessmentStudentValidationIssueTypeCode;
import ca.bc.gov.educ.assessment.api.rules.assessment.AssessmentValidationBaseRule;
import ca.bc.gov.educ.assessment.api.struct.v1.AssessmentStudentValidationIssue;
import ca.bc.gov.educ.assessment.api.struct.v1.StudentRuleData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 *  | ID   | Severity | Rule                                                                  | Dependent On |
 *  |------|----------|-----------------------------------------------------------------------|--------------|
 *  | V001 | ERROR    |  PEN is invalid                                                       |--------------|
 *
 *
 */
@Component
@Slf4j
@Order(100)
public class V001StudentPEN implements AssessmentValidationBaseRule {

    @Override
    public boolean shouldExecute(StudentRuleData studentRuleData, List<AssessmentStudentValidationIssue> validationErrorsMap) {
        log.debug("In shouldExecute of V001: for assessment {} and assessment student PEN :: {}", studentRuleData.getAssessmentStudentEntity().getAssessmentEntity().getAssessmentID() ,
                studentRuleData.getAssessmentStudentEntity().getPen());

        var shouldExecute = isValidationDependencyResolved("V001", validationErrorsMap);

        log.debug("In shouldExecute of V001: Condition returned - {} for assessment student PEN :: {}" ,
                shouldExecute,
                studentRuleData.getAssessmentStudentEntity().getPen());

        return  shouldExecute;
    }

    @Override
    public List<AssessmentStudentValidationIssue> executeValidation(StudentRuleData studentRuleData) {
        var student = studentRuleData.getAssessmentStudentEntity();
        log.debug("In executeValidation of V001 for assessment student PEN :: {}", student.getPen());
        final List<AssessmentStudentValidationIssue> errors = new ArrayList<>();

        if(studentRuleData.getStudentApiStudent() == null){
            log.debug("V001: Student PEN is invalid :: {}", student.getPen());
            errors.add(createValidationIssue(AssessmentStudentValidationFieldCode.PEN, AssessmentStudentValidationIssueTypeCode.PEN_INVALID));
        }

        return errors;
    }

}
