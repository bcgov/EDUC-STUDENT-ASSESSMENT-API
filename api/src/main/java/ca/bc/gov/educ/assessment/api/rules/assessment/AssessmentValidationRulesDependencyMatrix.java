package ca.bc.gov.educ.assessment.api.rules.assessment;

import lombok.Getter;

import java.util.Arrays;
import java.util.Optional;

public enum AssessmentValidationRulesDependencyMatrix {
    ENTRY1("V001", new String[]{}),
    ENTRY2("V002", new String[]{}),
    ENTRY3("V304", new String[]{}),
    ENTRY4("V318", new String[]{}),
    ENTRY5("V319", new String[]{AssessmentStudentValidationIssueTypeCode.SCHOOL_INVALID.getCode()}),
    ENTRY6("V320", new String[]{AssessmentStudentValidationIssueTypeCode.PEN_INVALID.getCode()}),
    ENTRY7("V321", new String[]{AssessmentStudentValidationIssueTypeCode.PEN_INVALID.getCode()});

    @Getter
    private final String ruleID;
    @Getter
    private final String[] baseRuleErrorCode;
    AssessmentValidationRulesDependencyMatrix(String ruleID, String[] baseRuleErrorCode) {
        this.ruleID = ruleID;
        this.baseRuleErrorCode = baseRuleErrorCode;
    }

    public static Optional<AssessmentValidationRulesDependencyMatrix> findByValue(String ruleID) {
        return Arrays.stream(values()).filter(code -> code.ruleID.equalsIgnoreCase(ruleID)).findFirst();
    }
}
