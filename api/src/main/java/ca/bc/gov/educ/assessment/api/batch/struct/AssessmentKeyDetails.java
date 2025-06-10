package ca.bc.gov.educ.assessment.api.batch.struct;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AssessmentKeyDetails {
    private String assessmentSession;
    private String assessmentCode;
    private String formCode;
    private String questionNumber;
    private String itemType;
    private String answer;
    private String mark;
    private String cognLevel;
    private String taskCode;
    private String claimCode;
    private String contextCode;
    private String conceptsCode;
    private String topicType;
    private String scaleFactor;
    private String questionOrigin;
    private String item;
    private String irt;
    private String assessmentSection;
}
