package ca.bc.gov.educ.assessment.api.batch.constants;

import lombok.Getter;

@Getter
public enum AssessmentKeysBatchFile {
    ASSMT_SESSION("assmtSession"),
    ASSMT_CODE("assmtCode"),
    FORM_CODE("formCode"),
    QUES_NUMBER("quesNumber"),
    ITEM_TYPE("itemType"),
    MC_ANSWER150("mcAnswer"),
    MARK_VALUE("mark"),
    COGN_LEVEL("cognLevel"),
    TASK_CODE("taskCode"),
    CLAIM_CODE("claimCode"),
    CONTEXT_CODE("contextCode"),
    CONCEPTS_CODE("conceptsCode"),
    TOPIC_TYPE("topicType"),
    SCALE_FACTOR("scaleFactor"),
    QUES_ORIGIN("quesOrigin"),
    ITEM("item"),
    IRT_COLUMN("irt"),
    ASSMT_SECTION("assmtSection"),
    ;
    private final String name;

    AssessmentKeysBatchFile(String name) {
        this.name = name;
    }
}
