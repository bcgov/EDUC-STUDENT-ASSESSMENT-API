package ca.bc.gov.educ.eas.api.rules.assessment;

import lombok.Getter;

import java.util.Arrays;
import java.util.Optional;

public enum AssessmentValidationRulesDependencyMatrix {
    ENTRY1("V304", new String[]{}),
    ENTRY2("V318", new String[]{}),
    ENTRY3("V319", new String[]{}),
    ENTRY4("V320", new String[]{});

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
