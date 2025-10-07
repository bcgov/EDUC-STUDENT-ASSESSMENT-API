package ca.bc.gov.educ.assessment.api.model.v1;

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
@Entity
@Builder
@Table(name = "STAGED_ASSESSMENT_STUDENT_CHOICE_QUESTION_SET")
public class StagedAssessmentStudentChoiceQuestionSetEntity {

    @Id
    @UuidGenerator
    @Column(name = "ASSESSMENT_STUDENT_CHOICE_QUESTION_SET_ID", unique = true, updatable = false, columnDefinition = "BINARY(16)")
    private UUID assessmentStudentChoiceQuestionSetID;

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @ManyToOne(optional = false, targetEntity = StagedAssessmentStudentChoiceEntity.class)
    @JoinColumn(name = "ASSESSMENT_STUDENT_CHOICE_ID", referencedColumnName = "ASSESSMENT_STUDENT_CHOICE_ID", updatable = false)
    StagedAssessmentStudentChoiceEntity stagedAssessmentStudentChoiceEntity;

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @ManyToOne(optional = false, targetEntity = AssessmentQuestionEntity.class)
    @JoinColumn(name = "ASSESSMENT_QUESTION_ID", referencedColumnName = "ASSESSMENT_QUESTION_ID", updatable = false)
    AssessmentQuestionEntity assessmentQuestionEntity;

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