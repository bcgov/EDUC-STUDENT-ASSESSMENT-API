package ca.bc.gov.educ.assessment.api.model.v1;

import jakarta.persistence.*;
import jakarta.validation.constraints.PastOrPresent;
import lombok.*;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.UuidGenerator;
import org.springframework.scheduling.config.Task;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@DynamicUpdate
@Entity
@Builder
@Table(name = "ASSESSMENT_QUESTION")
public class AssessmentQuestionEntity {

    @Id
    @UuidGenerator
    @Column(name = "ASSESSMENT_QUESTION_ID", unique = true, updatable = false, columnDefinition = "BINARY(16)")
    private UUID assessmentQuestionID;

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @ManyToOne(optional = false, targetEntity = AssessmentFormEntity.class)
    @JoinColumn(name = "ASSESSMENT_FORM_ID", referencedColumnName = "ASSESSMENT_FORM_ID", updatable = false)
    AssessmentFormEntity assessmentFormEntity;

    @Column(name="QUES_NUMBER", nullable = false)
    private Integer questionNumber;

    @Column(name = "ITEM_TYPE", nullable = false)
    private String itemType;

    @Column(name = "MARK_VALUE", nullable = false)
    private Integer markValue;

    @ManyToOne(targetEntity = CognitiveLevelCodeEntity.class)
    @JoinColumn(name = "COGN_LEVEL_CODE", referencedColumnName = "COGN_LEVEL_CODE")
    private CognitiveLevelCodeEntity cognitiveLevelCode;

    @ManyToOne(targetEntity = TaskCodeEntity.class)
    @JoinColumn(name = "TASK_CODE", referencedColumnName = "TASK_CODE")
    private TaskCodeEntity taskCode;

    @ManyToOne(targetEntity = ClaimCodeEntity.class)
    @JoinColumn(name = "CLAIM_CODE", referencedColumnName = "CLAIM_CODE")
    private ClaimCodeEntity claimCode;

    @ManyToOne(targetEntity = ContextCodeEntity.class)
    @JoinColumn(name = "CONTEXT_CODE", referencedColumnName = "CONTEXT_CODE")
    private ContextCodeEntity contextCode;

    @ManyToOne(targetEntity = ConceptCodeEntity.class)
    @JoinColumn(name = "CONCEPT_CODE", referencedColumnName = "CONCEPT_CODE")
    private ConceptCodeEntity conceptCode;

    @Column(name = "SCALE_FACTOR")
    private BigDecimal scaleFactor;

    @Column(name = "ASSMT_SECTION")
    private String assessmentSection;

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

    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    @OneToMany(mappedBy = "assessmentQuestionEntity", fetch = FetchType.EAGER, cascade = CascadeType.ALL, targetEntity = AssessmentQuestionResponseOptionEntity.class)
    Set<AssessmentQuestionResponseOptionEntity> assessmentQuestionResponseOptionEntities;

    public Set<AssessmentQuestionResponseOptionEntity> getAssessmentQuestionResponseOptionEntities() {
        if (this.assessmentQuestionResponseOptionEntities == null) {
            this.assessmentQuestionResponseOptionEntities = new HashSet<>();
        }
        return this.assessmentQuestionResponseOptionEntities;
    }
}