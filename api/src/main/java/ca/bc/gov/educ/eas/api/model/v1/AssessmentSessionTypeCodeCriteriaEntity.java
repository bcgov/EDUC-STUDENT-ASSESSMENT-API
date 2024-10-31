package ca.bc.gov.educ.eas.api.model.v1;

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
@Table(name = "ASSESSMENT_SESSION_TYPE_CODE_CRITERIA")
public class AssessmentSessionTypeCodeCriteriaEntity {

    @Id
    @GeneratedValue(generator = "UUID")
    @UuidGenerator
    @Column(name = "ASSESSMENT_SESSION_TYPE_CODE_CRITERIA_ID", unique = true, updatable = false, columnDefinition = "BINARY(16)")
    private UUID assessmentSessionTypeCodeCriteriaId;


    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @ManyToOne(optional = false, targetEntity = AssessmentSessionCriteriaEntity.class)
    @JoinColumn(name = "ASSESSMENT_SESSION_CRITERIA_ID", referencedColumnName = "ASSESSMENT_SESSION_CRITERIA_ID", updatable = false)
    private AssessmentSessionCriteriaEntity assessmentSessionCriteriaEntity;

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @ManyToOne(optional = false, targetEntity = AssessmentTypeCodeEntity.class)
    @JoinColumn(name = "ASSESSMENT_TYPE_CODE", referencedColumnName = "ASSESSMENT_TYPE_CODE", updatable = false)
    private AssessmentTypeCodeEntity assessmentTypeCodeEntity;

    @PastOrPresent
    @Column(name = "EFFECTIVE_DATE", updatable = false)
    private LocalDateTime effectiveDate;

    @PastOrPresent
    @Column(name = "EXPIRY_DATE")
    private LocalDateTime expiryDate;

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