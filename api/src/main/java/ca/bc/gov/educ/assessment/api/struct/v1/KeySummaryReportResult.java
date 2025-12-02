package ca.bc.gov.educ.assessment.api.struct.v1;

import ca.bc.gov.educ.assessment.api.model.v1.AssessmentChoiceEntity;
import ca.bc.gov.educ.assessment.api.model.v1.AssessmentComponentEntity;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.Column;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.validation.constraints.PastOrPresent;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.UuidGenerator;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@SuperBuilder
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class KeySummaryReportResult implements Serializable {

    private String assessmentQuestionID;
    private String assessmentChoiceID;
    private String formCode;
    private String componentType;
    private String questionNumber;
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
