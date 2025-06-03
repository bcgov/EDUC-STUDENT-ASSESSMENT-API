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
@Table(name = "ASSESSMENT_STUDENT_ANSWER")
public class AssessmentStudentAnswerEntity {

    @Id
    @UuidGenerator
    @Column(name = "ASSESSMENT_STUDENT_ANSWER_ID", unique = true, updatable = false, columnDefinition = "BINARY(16)")
    private UUID assessmentStudentAnswerID;

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @ManyToOne(optional = false, targetEntity = AssessmentQuestionResponseOptionEntity.class)
    @JoinColumn(name = "ASSESSMENT_QUESTION_RESPONSE_OPTION_ID", referencedColumnName = "ASSESSMENT_QUESTION_RESPONSE_OPTION_ID", updatable = false)
    AssessmentQuestionResponseOptionEntity assessmentQuestionResponseOptionEntity;

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @ManyToOne(optional = false, targetEntity = AssessmentStudentEntity.class)
    @JoinColumn(name = "ASSESSMENT_STUDENT_ID", referencedColumnName = "ASSESSMENT_STUDENT_ID", updatable = false)
    AssessmentStudentEntity assessmentStudentEntity;

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
