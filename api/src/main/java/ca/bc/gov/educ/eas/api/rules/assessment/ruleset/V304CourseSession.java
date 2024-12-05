package ca.bc.gov.educ.eas.api.rules.assessment.ruleset;

import ca.bc.gov.educ.eas.api.constants.v1.AssessmentTypeCodes;
import ca.bc.gov.educ.eas.api.constants.v1.CourseStatusCodes;
import ca.bc.gov.educ.eas.api.rules.StudentValidationIssueSeverityCode;
import ca.bc.gov.educ.eas.api.rules.assessment.AssessmentStudentValidationFieldCode;
import ca.bc.gov.educ.eas.api.rules.assessment.AssessmentStudentValidationIssueTypeCode;
import ca.bc.gov.educ.eas.api.rules.assessment.AssessmentValidationBaseRule;
import ca.bc.gov.educ.eas.api.service.v1.AssessmentRulesService;
import ca.bc.gov.educ.eas.api.struct.v1.AssessmentStudentValidationIssue;
import ca.bc.gov.educ.eas.api.struct.v1.StudentRuleData;
import ca.bc.gov.educ.eas.api.model.v1.AssessmentStudentEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.*;

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
    private static final List<String> NUMERACY_ASSESSMENT_CODES = Arrays.asList(AssessmentTypeCodes.NMF.getCode(), AssessmentTypeCodes.NMF10.getCode(), AssessmentTypeCodes.NME10.getCode(), AssessmentTypeCodes.NME.getCode());

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

        if(student.getCourseStatusCode() != null && student.getCourseStatusCode().equals(CourseStatusCodes.WITHDRAWN.getCode())){
            if(assessmentRulesService.studentHasWrittenAssessment(student.getAssessmentStudentID())){
                errors.add(createValidationIssue(StudentValidationIssueSeverityCode.ERROR, AssessmentStudentValidationFieldCode.COURSE_STATUS, AssessmentStudentValidationIssueTypeCode.COURSE_ALREADY_WRITTEN));
            }
        } else {
            List<String> assessmentCodes = NUMERACY_ASSESSMENT_CODES.contains(student.getAssessmentEntity().getAssessmentTypeCode()) ? NUMERACY_ASSESSMENT_CODES : Collections.singletonList(student.getAssessmentEntity().getAssessmentTypeCode());

            AssessmentStudentEntity studentAssessmentDuplicate = assessmentRulesService.studentAssessmentDuplicate(student.getPen(), student.getAssessmentEntity().getAssessmentID(), student.getAssessmentStudentID());

            boolean studentWritesExceeded = assessmentRulesService.studentAssessmentWritesExceeded(student.getPen(), assessmentCodes);

            if (studentAssessmentDuplicate != null) {
                log.debug("V304: The assessment session is a duplicate of an existing {} assessment session for student PEN :: {}", student.getAssessmentEntity().getAssessmentTypeCode(), student.getPen());
                errors.add(createValidationIssue(StudentValidationIssueSeverityCode.ERROR, AssessmentStudentValidationFieldCode.COURSE_CODE, AssessmentStudentValidationIssueTypeCode.COURSE_SESSION_DUP));
            }else if (studentWritesExceeded) {
                log.debug("V304: Student has already reached the maximum number of writes for the {} Assessment for student PEN :: {}", student.getAssessmentEntity().getAssessmentTypeCode(), student.getPen());
                errors.add(createValidationIssue(StudentValidationIssueSeverityCode.ERROR, AssessmentStudentValidationFieldCode.COURSE_CODE, AssessmentStudentValidationIssueTypeCode.COURSE_SESSION_EXCEED));
            }
        }

        return errors;
    }

}
