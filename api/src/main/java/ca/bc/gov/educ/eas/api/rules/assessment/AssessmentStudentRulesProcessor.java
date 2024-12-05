package ca.bc.gov.educ.eas.api.rules.assessment;

import ca.bc.gov.educ.eas.api.struct.v1.AssessmentStudentValidationIssue;
import ca.bc.gov.educ.eas.api.struct.v1.StudentRuleData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class AssessmentStudentRulesProcessor {
    private final List<AssessmentValidationBaseRule> rules;

    @Autowired
    public AssessmentStudentRulesProcessor(final List<AssessmentValidationBaseRule> rules) {
        this.rules = rules;
    }

    public List<AssessmentStudentValidationIssue> processRules(StudentRuleData ruleStudent) {
        final List<AssessmentStudentValidationIssue> validationErrorsMap = new ArrayList<>();
        log.debug("Starting validations check for student :: {} with data :: {}", ruleStudent.getAssessmentStudentEntity().getAssessmentStudentID(), ruleStudent);
        rules.forEach(rule -> {
            if(rule.shouldExecute(ruleStudent, validationErrorsMap)) {
                validationErrorsMap.addAll(rule.executeValidation(ruleStudent));
            }
        });
        return validationErrorsMap;
    }
}
