package ca.bc.gov.educ.eas.api.rules.assessment.ruleset;

import ca.bc.gov.educ.eas.api.rules.StudentValidationIssueSeverityCode;
import ca.bc.gov.educ.eas.api.rules.assessment.AssessmentStudentValidationFieldCode;
import ca.bc.gov.educ.eas.api.rules.assessment.AssessmentStudentValidationIssueTypeCode;
import ca.bc.gov.educ.eas.api.rules.assessment.AssessmentValidationBaseRule;
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
 *  | V002 | ERROR    |  School ID is invalid                                                 |--------------|
 *
 *
 */
@Component
@Slf4j
@Order(120)
public class V002StudentSchool implements AssessmentValidationBaseRule {

    @Override
    public boolean shouldExecute(StudentRuleData studentRuleData, List<AssessmentStudentValidationIssue> validationErrorsMap) {
        log.debug("In shouldExecute of V002: for assessment {} and assessment student PEN :: {}", studentRuleData.getAssessmentStudentEntity().getAssessmentEntity().getAssessmentID() ,
                studentRuleData.getAssessmentStudentEntity().getPen());

        var shouldExecute = isValidationDependencyResolved("V001", validationErrorsMap);

        log.debug("In shouldExecute of V002: Condition returned - {} for assessment student PEN :: {}" ,
                shouldExecute,
                studentRuleData.getAssessmentStudentEntity().getPen());

        return  shouldExecute;
    }

    @Override
    public List<AssessmentStudentValidationIssue> executeValidation(StudentRuleData studentRuleData) {
        var student = studentRuleData.getAssessmentStudentEntity();
        log.debug("In executeValidation of V001 for assessment student PEN :: {}", student.getPen());
        final List<AssessmentStudentValidationIssue> errors = new ArrayList<>();

        if(studentRuleData.getSchool() == null){
            log.debug("V002: School is not valid for student with PEN :: {}", student.getPen());
            errors.add(createValidationIssue(StudentValidationIssueSeverityCode.ERROR, AssessmentStudentValidationFieldCode.SCHOOL, AssessmentStudentValidationIssueTypeCode.SCHOOL_INVALID));
        }

        return errors;
    }

}
