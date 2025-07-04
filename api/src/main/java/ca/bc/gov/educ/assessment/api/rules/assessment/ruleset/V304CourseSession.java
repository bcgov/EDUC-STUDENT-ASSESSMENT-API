package ca.bc.gov.educ.assessment.api.rules.assessment.ruleset;

import ca.bc.gov.educ.assessment.api.rules.assessment.AssessmentStudentValidationFieldCode;
import ca.bc.gov.educ.assessment.api.rules.assessment.AssessmentStudentValidationIssueTypeCode;
import ca.bc.gov.educ.assessment.api.rules.assessment.AssessmentValidationBaseRule;
import ca.bc.gov.educ.assessment.api.service.v1.AssessmentRulesService;
import ca.bc.gov.educ.assessment.api.service.v1.AssessmentStudentService;
import ca.bc.gov.educ.assessment.api.struct.v1.AssessmentStudentValidationIssue;
import ca.bc.gov.educ.assessment.api.struct.v1.StudentRuleData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 *  | ID          | Severity | Rule                                                                                                       | Dependent On |
 *  |-------------|----------|------------------------------------------------------------------------------------------------------------|--------------|
 *  | V304        | ERROR    | The assessment session is a duplicate of an existing assessment session for this student/assessment/level  |--------------|
 *  |             | ERROR    | Student has already reached the maximum number of writes for this Assessment specified                     |--------------|
 *  |             | ERROR    | Assessment has been written by the student, withdrawal is not allowed                                      |--------------|
 *
 */
@Component
@Slf4j
@Order(130)
public class V304CourseSession implements AssessmentValidationBaseRule {

    private final AssessmentRulesService assessmentRulesService;

    public V304CourseSession(AssessmentRulesService assessmentRulesService) {
        this.assessmentRulesService = assessmentRulesService;
    }


    @Override
    public boolean shouldExecute(StudentRuleData studentRuleData, List<AssessmentStudentValidationIssue> validationErrorsMap) {
        log.debug("In shouldExecute of V304: for assessment {} and assessment student PEN :: {}", studentRuleData.getAssessmentStudentEntity().getAssessmentEntity().getAssessmentID() ,
                studentRuleData.getAssessmentStudentEntity().getPen());

        var shouldExecute = isValidationDependencyResolved("V304", validationErrorsMap);

        log.debug("In shouldExecute of V304: Condition returned - {} for assessment student PEN :: {}" ,
                shouldExecute,
                studentRuleData.getAssessmentStudentEntity().getPen());

        return  shouldExecute;
    }

    @Override
    public List<AssessmentStudentValidationIssue> executeValidation(StudentRuleData studentRuleData) {
        var student = studentRuleData.getAssessmentStudentEntity();
        log.debug("In executeValidation of V304 for assessment student PEN :: {}", student.getPen());
        final List<AssessmentStudentValidationIssue> errors = new ArrayList<>();
        
        boolean hasStudentAssessmentDuplicate = assessmentRulesService.hasStudentAssessmentDuplicate(student.getStudentID(), student.getAssessmentEntity().getAssessmentID(), student.getAssessmentStudentID());

        boolean studentWritesExceeded = assessmentRulesService.studentAssessmentWritesExceeded(student.getStudentID(), student.getAssessmentEntity().getAssessmentTypeCode());

        if (hasStudentAssessmentDuplicate) {
            log.debug("V304: The student has already been registered for this assessment in this session :: {}", student.getPen());
            errors.add(createValidationIssue(AssessmentStudentValidationFieldCode.COURSE_CODE, AssessmentStudentValidationIssueTypeCode.COURSE_SESSION_DUP));
        }else if (studentWritesExceeded) {
            log.debug("V304: Student has already reached the maximum number of writes for the {} Assessment for student PEN :: {}", student.getAssessmentEntity().getAssessmentTypeCode(), student.getPen());
            errors.add(createValidationIssue(AssessmentStudentValidationFieldCode.COURSE_CODE, AssessmentStudentValidationIssueTypeCode.COURSE_SESSION_EXCEED));
        }

        return errors;
    }

}
