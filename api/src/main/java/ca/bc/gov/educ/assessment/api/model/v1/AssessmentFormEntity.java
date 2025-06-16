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
@Table(name = "ASSESSMENT_FORM")
public class AssessmentFormEntity {

    @Id
    @UuidGenerator
    @Column(name = "ASSESSMENT_FORM_ID", unique = true, updatable = false, columnDefinition = "BINARY(16)")
    private UUID assessmentFormID;

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @ManyToOne(optional = false, targetEntity = AssessmentEntity.class)
    @JoinColumn(name = "ASSESSMENT_ID", referencedColumnName = "ASSESSMENT_ID", updatable = false)
    AssessmentEntity assessmentEntity;

    @Column(name="FORM_CODE", length = 1)
    private String formCode;

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
