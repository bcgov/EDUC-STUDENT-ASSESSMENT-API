package ca.bc.gov.educ.eas.api.rules.assessment;

import lombok.Getter;

import java.util.Arrays;
import java.util.Optional;

public enum AssessmentValidationRulesDependencyMatrix {
    ENTRY1("V304", new String[]{AssessmentStudentValidationIssueTypeCode.DEM_DATA_MISSING.getCode(), AssessmentStudentValidationIssueTypeCode.COURSE_CODE_INVALID.getCode(), AssessmentStudentValidationIssueTypeCode.COURSE_STATUS_INVALID.getCode()}),
    ENTRY2("V318", new String[]{AssessmentStudentValidationIssueTypeCode.DEM_DATA_MISSING.getCode(), AssessmentStudentValidationIssueTypeCode.COURSE_CODE_INVALID.getCode()}),
    ENTRY3("V319", new String[]{AssessmentStudentValidationIssueTypeCode.COURSE_CODE_INVALID.getCode()}),
    ENTRY4("V320", new String[]{AssessmentStudentValidationIssueTypeCode.DEM_DATA_MISSING.getCode()});

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
