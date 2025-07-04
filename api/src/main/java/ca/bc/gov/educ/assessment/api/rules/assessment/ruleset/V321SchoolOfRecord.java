package ca.bc.gov.educ.assessment.api.rules.assessment.ruleset;

import ca.bc.gov.educ.assessment.api.model.v1.AssessmentStudentEntity;
import ca.bc.gov.educ.assessment.api.rest.RestUtils;
import ca.bc.gov.educ.assessment.api.rules.assessment.AssessmentStudentValidationFieldCode;
import ca.bc.gov.educ.assessment.api.rules.assessment.AssessmentStudentValidationIssueTypeCode;
import ca.bc.gov.educ.assessment.api.rules.assessment.AssessmentValidationBaseRule;
import ca.bc.gov.educ.assessment.api.rules.utils.RuleUtil;
import ca.bc.gov.educ.assessment.api.struct.v1.AssessmentStudentValidationIssue;
import ca.bc.gov.educ.assessment.api.struct.v1.StudentRuleData;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 *  | ID   | Severity | Rule                                                                  | Dependent On |
 *  |------|----------|-----------------------------------------------------------------------|--------------|
 *  | V321 | ERROR    |  The student must have a school of record in GRAD that matches the    | V001         |
 *                       school of the logged in school user or is the school selected by a 
 *                       logged in district user.
 *
 */
@Component
@Slf4j
@Order(101)
public class V321SchoolOfRecord implements AssessmentValidationBaseRule {
    private final RestUtils restUtils;

    public V321SchoolOfRecord(RestUtils restUtils) {
        this.restUtils = restUtils;
    }

    @Override
    public boolean shouldExecute(StudentRuleData studentRuleData, List<AssessmentStudentValidationIssue> validationErrorsMap) {
        log.debug("In shouldExecute of V321: for assessment {} and assessment student PEN :: {}", studentRuleData.getAssessmentStudentEntity().getAssessmentEntity().getAssessmentID() ,
                studentRuleData.getAssessmentStudentEntity().getPen());

        var shouldExecute = isValidationDependencyResolved("V321", validationErrorsMap);

        log.debug("In shouldExecute of V321: Condition returned - {} for assessment student PEN :: {}" ,
                shouldExecute,
                studentRuleData.getAssessmentStudentEntity().getPen());

        return  shouldExecute;
    }

    @Override
    public List<AssessmentStudentValidationIssue> executeValidation(StudentRuleData studentRuleData) {
        AssessmentStudentEntity student = studentRuleData.getAssessmentStudentEntity();
        log.debug("In executeValidation of V321 for assessment student PEN :: {}", student.getPen());
        final List<AssessmentStudentValidationIssue> errors = new ArrayList<>();

        var gradStudentRecord = restUtils.getGradStudentRecordByStudentID(UUID.randomUUID(), student.getStudentID());
        if (gradStudentRecord == null || !gradStudentRecord.getSchoolOfRecordId().equalsIgnoreCase(student.getSchoolOfRecordSchoolID().toString())) {
            var school = restUtils.getSchoolBySchoolID(student.getSchoolOfRecordSchoolID().toString());
            log.debug("V321:" + school.get().getDisplayName() + " is not currently the student's School of Record in GRAD so the registration cannot be added.");
            String message = school.get().getDisplayName() + " is not currently the student's School of Record in GRAD so the registration cannot be added.";
            errors.add(createValidationIssue(AssessmentStudentValidationFieldCode.SCHOOL, AssessmentStudentValidationIssueTypeCode.SCHOOL_OF_RECORD_INVALID, message));
        }

        return errors;
    }

}
