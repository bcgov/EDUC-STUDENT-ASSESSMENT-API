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
@Table(name = "STAGED_ASSESSMENT_STUDENT_CHOICE")
public class StagedAssessmentStudentChoiceEntity {

    @Id
    @UuidGenerator
    @Column(name = "ASSESSMENT_STUDENT_CHOICE_ID", unique = true, updatable = false, columnDefinition = "BINARY(16)")
    private UUID assessmentStudentChoiceID;

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @ManyToOne(optional = false, targetEntity = StagedAssessmentStudentComponentEntity.class)
    @JoinColumn(name = "ASSESSMENT_STUDENT_COMPONENT_ID", referencedColumnName = "ASSESSMENT_STUDENT_COMPONENT_ID", updatable = false)
    StagedAssessmentStudentComponentEntity stagedAssessmentStudentComponentEntity;

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @ManyToOne(optional = false, targetEntity = AssessmentChoiceEntity.class)
    @JoinColumn(name = "ASSESSMENT_CHOICE_ID", referencedColumnName = "ASSESSMENT_CHOICE_ID", updatable = false)
    AssessmentChoiceEntity assessmentChoiceEntity;

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
    @OneToMany(mappedBy = "stagedAssessmentStudentChoiceEntity", fetch = FetchType.LAZY, cascade = CascadeType.ALL, targetEntity = StagedAssessmentStudentChoiceQuestionSetEntity.class)
    Set<StagedAssessmentStudentChoiceQuestionSetEntity> stagedAssessmentStudentChoiceQuestionSetEntities;

    public Set<StagedAssessmentStudentChoiceQuestionSetEntity> getStagedAssessmentStudentChoiceQuestionSetEntities() {
        if (this.stagedAssessmentStudentChoiceQuestionSetEntities == null) {
            this.stagedAssessmentStudentChoiceQuestionSetEntities = new HashSet<>();
        }
        return this.stagedAssessmentStudentChoiceQuestionSetEntities;
    }
}