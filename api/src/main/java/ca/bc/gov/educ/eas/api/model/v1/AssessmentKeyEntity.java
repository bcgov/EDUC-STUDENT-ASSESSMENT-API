package ca.bc.gov.educ.eas.api.model.v1;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.PastOrPresent;
import lombok.*;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@DynamicUpdate
//@Entity
@Builder
//@Table(name = "ASSESSMENT_KEY")
@JsonIgnoreProperties(ignoreUnknown = true)
public class AssessmentKeyEntity {

    @Id
    @GeneratedValue(generator = "UUID")
    @UuidGenerator
    @Column(name = "ASSESSMENT_KEY_ID", unique = true, updatable = false, columnDefinition = "BINARY(16)")
    private UUID assessmentKeyID;

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @ManyToOne(optional = false, targetEntity = AssessmentKeyFileEntity.class)
    @JoinColumn(name = "ASSESSMENT_KEY_FILE_ID", referencedColumnName = "ASSESSMENT_KEY_FILE_ID")
    AssessmentKeyFileEntity assessmentKeyFileEntity;

    @Column(name = "ASSESSMENT_ID", nullable = false, columnDefinition = "BINARY(16)")
    private UUID assessmentID;

    @Column(name = "FORM_CODE", nullable = false, length = 1)
    private String formCode;

    @Column(name = "QUESTION_NUMBER", length = 2)
    private Integer questionNumber;

    @Column(name = "ITEM_TYPE", nullable = false, length = 12)
    private String itemType;

    @Column(name = "MC_ANSWER", nullable = false, length = 150)
    private String multipleChoiceAnswer;

    @Column(name = "MARK_VALUE", length = 2)
    private Integer markValue;

    @Column(name = "COGNITIVE_LEVEL", length = 4)
    private String cognitiveLevel;

    @Column(name = "TASK_CODE", length = 2)
    private String taskCode;

    @Column(name = "CLAIM_CODE", length = 3)
    private String claimCode;

    @Column(name = "CONTEXT_CODE", length = 1)
    private String contextCode;

    @Column(name = "CONCEPTS_CODE", length = 3)
    private String conceptsCode;

    @Column(name = "TOPIC_TYPE", length = 1)
    private String topicType;

    @Column(name = "SCALE_FACTOR", nullable = false, length = 8)
    private String scaleFactor;

    @Column(name = "QUESTION_ORIGIN", length = 160)
    private String questionOrigin;

    @Column(name = "ITEM", length = 4)
    private String item;

    @Column(name = "IRT_COLUMN", length = 3)
    private Integer irtColumn;

    @Column(name = "ASSESSMENT_SECTION", nullable = false, length = 8)
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

}
