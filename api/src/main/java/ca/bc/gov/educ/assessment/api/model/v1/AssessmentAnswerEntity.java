package ca.bc.gov.educ.assessment.api.model.v1;

import jakarta.persistence.*;
import jakarta.validation.constraints.PastOrPresent;
import lombok.*;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.UuidGenerator;

import java.math.BigDecimal;
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

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @ManyToOne(optional = false, targetEntity = AssessmentComponentEntity.class)
    @JoinColumn(name = "ASSESSMENT_COMPONENT_ID", referencedColumnName = "ASSESSMENT_COMPONENT_ID", updatable = false)
    AssessmentComponentEntity assessmentComponentEntity;

    @Column(name = "ASSESSMENT_QUESTION_ID")
    UUID assessmentQuestionID;

    @Column(name = "OE_ITEM_TYPE")
    private String oeItemType;

    @Column(name = "ANSWER_NUMBER")
    private Integer answerNumber;

    @Column(name = "MC_QUES_TYPE")
    private String mcQuesType;

    @Column(name = "MC_ANSWER")
    private String mcAnswer;

    @Column(name = "MC_ANSWER_LOWER")
    private BigDecimal mcAnswerLower;

    @Column(name = "MC_ANSWER_UPPER")
    private BigDecimal mcAnswerUpper;

    @Column(name = "QUESTION_VALUE")
    private BigDecimal questionValue;

    @Column(name = "IRT")
    private Integer irt;

    @Column(name = "ITEM_NUMBER")
    private Integer itemNumber;

    @Column(name = "LINKED_ITEM_NUMBER")
    private Integer linkedItemNumber;

    @Column(name = "SCALE_FACTOR")
    private Integer scaleFactor;

    @Column(name = "COGN_LEVEL_CODE")
    private String cognitiveLevelCode;

    @Column(name = "TASK_CODE")
    private String taskCode;

    @Column(name = "CLAIM_CODE")
    private String claimCode;

    @Column(name = "CONTEXT_CODE")
    private String contextCode;

    @Column(name = "CONCEPTS_CODE")
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
