package ca.bc.gov.educ.assessment.api.constants.v1.reports;

import lombok.Getter;

@Getter
public enum KeySummaryHeader {

    ASSESSMENT("Assessment"),
    SESSION("Session"),
    FORM("Form"),
    COMPONENT("Component"),
    ITEM_NUMBER("Item Number"),
    ITEM_TYPE("Item Type"),
    QUESTION_NUMBER("Question Number"),
    QUESTION_VALUE("Value"),
    SCALING_FACTOR("Scaling Factor"),
    SCALED_VALUE("Scaled Value"),
    MAX_VALUE("Max Value"),
    COGN_LEVEL("Cognitive Level"),
    TASK_CODE("Task Code"),
    CLAIM_CODE("Claim Code"),
    CONTEXT_CODE("Context Code"),
    CONCEPTS_CODE("Concepts Code"),
    SECTION("Section"),

    ;

    private final String code;
    KeySummaryHeader(String code) { this.code = code; }
}
