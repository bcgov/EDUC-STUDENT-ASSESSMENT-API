package ca.bc.gov.educ.assessment.api.struct.v1;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

@EqualsAndHashCode(callSuper = true)
@Data
@SuperBuilder
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class AssessmentQuestion extends BaseRequest implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private String assessmentQuestionID;
    private String assessmentComponentID;
    private Integer questionNumber;
    private String cognitiveLevelCode;
    private String taskCode;
    private String claimCode;
    private String contextCode;
    private String conceptCode;
    private String assessmentSection;
    private Integer itemNumber;
    private BigDecimal questionValue;
    private BigDecimal maxQuestionValue;
    private Integer masterQuestionNumber;
    private BigDecimal irtIncrement;
    private String preloadAnswer;
    private Integer irt;
    private Integer scaleFactor;
}
