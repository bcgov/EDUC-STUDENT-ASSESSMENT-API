package ca.bc.gov.educ.eas.api.rules.assessment.ruleset;

import ca.bc.gov.educ.eas.api.constants.v1.SchoolReportingRequirementCodes;
import ca.bc.gov.educ.eas.api.rules.StudentValidationIssueSeverityCode;
import ca.bc.gov.educ.eas.api.rules.assessment.AssessmentStudentValidationFieldCode;
import ca.bc.gov.educ.eas.api.rules.assessment.AssessmentStudentValidationIssueTypeCode;
import ca.bc.gov.educ.eas.api.rules.assessment.AssessmentValidationBaseRule;
import ca.bc.gov.educ.eas.api.rules.utils.RuleUtil;
import ca.bc.gov.educ.eas.api.service.v1.AssessmentRulesService;
import ca.bc.gov.educ.eas.api.struct.v1.AssessmentStudentValidationIssue;
import ca.bc.gov.educ.eas.api.struct.v1.StudentRuleData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 *  | ID   | Severity | Rule                                                                  | Dependent On |
 *  |------|----------|-----------------------------------------------------------------------|--------------|
 *  | V320 | ERROR    |  Student XAM record will not be processed due to an issue with the    |--------------|
 *                       student's demographics
 *
 */
@Component
@Slf4j
@Order(101)
public class V320ValidStudentInDEM implements AssessmentValidationBaseRule {
    @Override
    public boolean shouldExecute(StudentRuleData studentRuleData, List<AssessmentStudentValidationIssue> validationErrorsMap) {
        log.debug("In shouldExecute of V320: for assessment {} and assessment student PEN :: {}", studentRuleData.getAssessmentStudentEntity().getAssessmentEntity().getAssessmentID() ,
                studentRuleData.getAssessmentStudentEntity().getPen());

        var shouldExecute = isValidationDependencyResolved("V320", validationErrorsMap);

        log.debug("In shouldExecute of V320: Condition returned - {} for assessment student PEN :: {}" ,
                shouldExecute,
                studentRuleData.getAssessmentStudentEntity().getPen());

        return  shouldExecute;
    }

    @Override
    public List<AssessmentStudentValidationIssue> executeValidation(StudentRuleData studentRuleData) {
        var student = studentRuleData.getAssessmentStudentEntity();
        log.debug("In executeValidation of V320 for assessment student PEN :: {}", student.getPen());
        final List<AssessmentStudentValidationIssue> errors = new ArrayList<>();

        if(studentRuleData.getStudentApiStudent() == null){
            log.debug("V320: No matches found for assessment student PEN :: {}", student.getPen());
            errors.add(createValidationIssue(StudentValidationIssueSeverityCode.ERROR, AssessmentStudentValidationFieldCode.PEN, AssessmentStudentValidationIssueTypeCode.DEM_ISSUE));
        }

        if (!RuleUtil.validateStudentSurnameMatches(student, studentRuleData.getStudentApiStudent()) ||
            !RuleUtil.validateStudentGivenNameMatches(student, studentRuleData.getStudentApiStudent())) {
            log.debug("V320: Student name does not match Student API record for PEN :: {}", student.getPen());
            errors.add(createValidationIssue(StudentValidationIssueSeverityCode.ERROR, AssessmentStudentValidationFieldCode.PEN, AssessmentStudentValidationIssueTypeCode.DEM_ISSUE));
        }
        return errors;
    }

}
