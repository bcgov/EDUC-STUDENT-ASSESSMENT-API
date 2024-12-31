package ca.bc.gov.educ.eas.api.batch.struct;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AssessmentKeyDetail {

    private String assessmentSession;
    private String assessmentTypeCode;
    private String formCode;
    private Integer questionNumber;
    private String itemType;
    private String multipleChoiceAnswer;
    private Integer markValue;
    private String cognitiveLevel;
    private String taskCode;
    private String claimCode;
    private String contextCode;
    private String conceptsCode;
    private String topicType;
    private String scaleFactor;
    private String questionOrigin;
    private String item;
    private Integer irtColumn;
    private String assessmentSection;

}
