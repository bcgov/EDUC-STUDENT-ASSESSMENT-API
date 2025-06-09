package ca.bc.gov.educ.assessment.api.model.v1;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.PastOrPresent;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@DynamicUpdate
@Entity
@Builder
@Table(name = "ASSESSMENT_ANSWER")
public class AssessmentAnswerEntity {

    @Id
    @UuidGenerator
    @Column(name = "ASSESSMENT_ANSWER_ID", unique = true, updatable = false, columnDefinition = "BINARY(16)")
    private UUID assessmentAnswerID;

    @Column(name = "ASSESSMENT_QUESTION_ID", updatable = false)
    UUID assessmentQuestionID;

    @Column(name = "MC_OE_FLAG")
    private String mcOeFlag;

    @Column(name = "OE_ITEM_TYPE")
    private String oeItemType;

    @Column(name = "ANSWER_NUMBER")
    private String answerNumber;

    @Column(name = "MC_QUES_TYPE")
    private String mcQuesType;

    @Column(name = "MC_ANSWER")
    private String mcAnswer;

    @Column(name = "MC_ANSWER_LOWER")
    private String mcAnswerLower;

    @Column(name = "MC_ANSWER_UPPER")
    private String mcAnswerUpper;

    @Column(name = "QUES_VALUE")
    private String questionValue;

    @Column(name = "IRT")
    private String irt;

    @Column(name = "ITEM_NUMBER")
    private String itemNumber;

    @Column(name = "LINKED_ITEM_NUMBER")
    private String linkedItemNumber;

    @Column(name = "SCALE_FACTOR")
    private String scaleFactor;

    @Column(name = "COGN_LEVEL_CODE")
    private String cognitiveLevelCode;

    @Column(name = "TASK_CODE")
    private String taskCode;

    @Column(name = "CLAIM_CODE")
    private String claimCode;

    @Column(name = "CONTEXT_CODE")
    private String contextCode;

    @Column(name = "CONCEPT_CODE")
    private String conceptCode;

    @Column(name = "CREATE_USER", updatable = false, length = 100)
    private String createUser;

    @PastOrPresent
    @Column(name = "CREATE_DATE", updatable = false)
    private LocalDateTime createDate;

    @Column(name = "UPDATE_USER", nullable = false, length = 100)
    private String updateUser;

    @PastOrPresent
    @Column(name = "UPDATE_DATE", nullable = false)
    private LocalDateTime updateDate;

}
