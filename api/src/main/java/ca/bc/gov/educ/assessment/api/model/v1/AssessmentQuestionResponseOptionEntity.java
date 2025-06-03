package ca.bc.gov.educ.assessment.api.model.v1;

import jakarta.persistence.*;
import jakarta.validation.constraints.PastOrPresent;
import lombok.*;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.UuidGenerator;

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
@Table(name = "ASSESSMENT_QUESTION_RESPONSE_OPTION")
public class AssessmentQuestionResponseOptionEntity {

    @Id
    @UuidGenerator
    @Column(name = "ASSESSMENT_QUESTION_RESPONSE_OPTION_ID", unique = true, updatable = false, columnDefinition = "BINARY(16)")
    private UUID assessmentQuestionResponseOptionID;

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @ManyToOne(optional = false, targetEntity = AssessmentQuestionEntity.class)
    @JoinColumn(name = "ASSESSMENT_QUESTION_ID", referencedColumnName = "ASSESSMENT_QUESTION_ID", updatable = false)
    AssessmentQuestionEntity assessmentQuestionEntity;

    @Column(name = "IS_CORRECT_ANSWER", nullable = false)
    private boolean isCorrectAnswer;

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
    @OneToMany(mappedBy = "assessmentQuestionResponseOptionEntity", fetch = FetchType.LAZY, cascade = CascadeType.ALL, targetEntity = AssessmentStudentAnswerEntity.class)
    Set<AssessmentStudentAnswerEntity> assessmentStudentAnswerEntities;

    public Set<AssessmentStudentAnswerEntity> getAssessmentStudentAnswerEntities() {
        if (this.assessmentStudentAnswerEntities == null) {
            this.assessmentStudentAnswerEntities = new HashSet<>();
        }
        return this.assessmentStudentAnswerEntities;
    }

}
