package ca.bc.gov.educ.eas.api.batch.constants;

import lombok.Getter;

@Getter
public enum AssessmentKey {

    ASSMT_SESSION("assessmentSession"),
    ASSMT_CODE("assessmentTypeCode"),
    FORM_CODE("formCode"),
    QUES_NUMBER("questionNumber"),
    ITEM_TYPE("itemType"),
    MC_ANSWER150("multipleChoiceAnswer"),
    MARK_VALUE("markValue"),
    COGN_LEVEL("cognitiveLevel"),
    TASK_CODE("taskCode"),
    CLAIM_CODE("claimCode"),
    CONTEXT_CODE("contextCode"),
    CONCEPTS_CODE("conceptsCode"),
    TOPIC_TYPE("topicType"),
    SCALE_FACTOR("scaleFactor"),
    QUES_ORIGIN("questionOrigin"),
    ITEM("item"),
    IRT_COLUMN("irtColumn"),
    ASSMT_SECTION("assessmentSection");

    private final String name;

    AssessmentKey(String name) {
        this.name = name;
    }
}

