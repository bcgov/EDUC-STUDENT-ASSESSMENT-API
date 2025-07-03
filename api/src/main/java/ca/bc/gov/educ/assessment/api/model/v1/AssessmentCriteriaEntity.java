package ca.bc.gov.educ.assessment.api.model.v1;

import jakarta.persistence.*;
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
@Table(name = "ASSESSMENT_CRITERIA")
public class AssessmentCriteriaEntity {

    @Id
    @GeneratedValue(generator = "UUID")
    @UuidGenerator
    @Column(name = "ASSESSMENT_CRITERIA_ID", unique = true, updatable = false, columnDefinition = "BINARY(16)")
    private UUID assessmentCriteriaId;

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @ManyToOne(optional = false, targetEntity = AssessmentSessionCriteriaEntity.class)
    @JoinColumn(name = "ASSESSMENT_SESSION_CRITERIA_ID", referencedColumnName = "ASSESSMENT_SESSION_CRITERIA_ID", updatable = false)
    private AssessmentSessionCriteriaEntity assessmentSessionCriteriaEntity;

    @Column(name = "ASSESSMENT_TYPE_CODE", nullable = false, length = 10)
    private String assessmentTypeCode;

    @Column(name = "EFFECTIVE_DATE", updatable = false)
    private LocalDateTime effectiveDate;

    @Column(name = "EXPIRY_DATE")
    private LocalDateTime expiryDate;

    @Column(name = "CREATE_USER", updatable = false, length = 100)
    private String createUser;

    @Column(name = "CREATE_DATE", updatable = false)
    private LocalDateTime createDate;

    @Column(name = "UPDATE_USER", nullable = false, length = 100)
    private String updateUser;

    @Column(name = "UPDATE_DATE", nullable = false)
    private LocalDateTime updateDate;
}
