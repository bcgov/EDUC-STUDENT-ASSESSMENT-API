package ca.bc.gov.educ.assessment.api.model.v1;

import jakarta.persistence.*;
import jakarta.validation.constraints.PastOrPresent;
import lombok.*;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.UuidGenerator;

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
@Table(name = "ASSESSMENT_STUDENT_COMPONENT")
public class AssessmentStudentComponentEntity {

    @Id
    @UuidGenerator
    @Column(name = "ASSESSMENT_STUDENT_COMPONENT_ID", unique = true, updatable = false, columnDefinition = "BINARY(16)")
    private UUID assessmentStudentComponentID;

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @ManyToOne(optional = false, targetEntity = AssessmentStudentEntity.class)
    @JoinColumn(name = "ASSESSMENT_STUDENT_ID", referencedColumnName = "ASSESSMENT_STUDENT_ID", updatable = false)
    AssessmentStudentEntity assessmentStudentEntity;

    @Column(name = "ASSESSMENT_COMPONENT_ID", updatable = false)
    UUID assessmentComponentID;

    @Column(name = "CHOICE_PATH")
    private String choicePath;

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
    @OneToMany(mappedBy = "assessmentStudentComponentEntity", fetch = FetchType.EAGER, cascade = CascadeType.ALL, targetEntity = AssessmentStudentAnswerEntity.class)
    Set<AssessmentStudentAnswerEntity> assessmentStudentAnswerEntities;

    public Set<AssessmentStudentAnswerEntity> getAssessmentStudentAnswerEntities() {
        if (this.assessmentStudentAnswerEntities == null) {
            this.assessmentStudentAnswerEntities = new HashSet<>();
        }
        return this.assessmentStudentAnswerEntities;
    }

}
