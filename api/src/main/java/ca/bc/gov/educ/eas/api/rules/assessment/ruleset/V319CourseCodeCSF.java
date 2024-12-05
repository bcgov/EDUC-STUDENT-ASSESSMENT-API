package ca.bc.gov.educ.eas.api.rules.assessment.ruleset;

import ca.bc.gov.educ.eas.api.constants.v1.AssessmentTypeCodes;
import ca.bc.gov.educ.eas.api.constants.v1.SchoolReportingRequirementCodes;
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
 *  | V319 | ERROR    |  Student is in a Francophone school and cannot register for           |--------------|
 *                       this assessment session for this student
 *
 */
@Component
@Slf4j
@Order(260)
public class V319CourseCodeCSF implements AssessmentValidationBaseRule {

    @Override
    public boolean shouldExecute(StudentRuleData studentRuleData, List<AssessmentStudentValidationIssue> validationErrorsMap) {
        log.debug("In shouldExecute of V319: for assessment {} and assessment student PEN :: {}", studentRuleData.getAssessmentStudentEntity().getAssessmentEntity().getAssessmentID() ,
                studentRuleData.getAssessmentStudentEntity().getPen());

        var shouldExecute = isValidationDependencyResolved("V319", validationErrorsMap);

        log.debug("In shouldExecute of V319: Condition returned - {} for assessment student PEN :: {}" ,
                shouldExecute,
                studentRuleData.getAssessmentStudentEntity().getPen());

        return  shouldExecute;
    }

    @Override
    public List<AssessmentStudentValidationIssue> executeValidation(StudentRuleData studentRuleData) {
        var student = studentRuleData.getAssessmentStudentEntity();
        log.debug("In executeValidation of V319 for assessment student PEN :: {}", student.getPen());
        final List<AssessmentStudentValidationIssue> errors = new ArrayList<>();

        if(studentRuleData.getSchool() == null){
            log.debug("V319: School is not valid for student with PEN :: {}", student.getPen());
            errors.add(createValidationIssue(StudentValidationIssueSeverityCode.ERROR, AssessmentStudentValidationFieldCode.SCHOOL, AssessmentStudentValidationIssueTypeCode.SCHOOL_INVALID));
        } else {
            if (studentRuleData.getSchool().getSchoolReportingRequirementCode().equalsIgnoreCase(SchoolReportingRequirementCodes.CSF.getCode()) && student.getAssessmentEntity().getAssessmentTypeCode().equalsIgnoreCase(AssessmentTypeCodes.LTF12.getCode())){
                log.debug("V319: Student is in a Francophone school and cannot register for this assessment session for student with PEN :: {}", student.getPen());
                errors.add(createValidationIssue(StudentValidationIssueSeverityCode.ERROR, AssessmentStudentValidationFieldCode.COURSE_CODE, AssessmentStudentValidationIssueTypeCode.COURSE_CODE_CSF));
            }
        }

        return errors;
    }

}
