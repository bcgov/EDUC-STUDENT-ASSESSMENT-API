package ca.bc.gov.educ.eas.api.model.v1;

import jakarta.persistence.*;
import jakarta.validation.constraints.PastOrPresent;
import lombok.*;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@DynamicUpdate
@Entity
@Builder
@Table(name = "ASSESSMENT_SESSION_CRITERIA")
public class AssessmentSessionCriteriaEntity {

  @Id
  @GeneratedValue(generator = "UUID")
  @UuidGenerator
  @Column(name = "ASSESSMENT_SESSION_CRITERIA_ID", unique = true, updatable = false, columnDefinition = "BINARY(16)")
  private UUID assessmentSessionCriteriaId;

  @Column(name = "SESSION_START")
  private LocalDateTime sessionStart;

  @Column(name = "SESSION_END")
  private LocalDateTime sessionEnd;

  @PastOrPresent
  @Column(name = "EFFECTIVE_DATE", updatable = false)
  private LocalDateTime effectiveDate;

  @PastOrPresent
  @Column(name = "EXPIRY_DATE")
  private LocalDateTime expiryDate;

  @ToString.Exclude
  @EqualsAndHashCode.Exclude
  @OneToMany(mappedBy = "assessmentSessionCriteriaEntity", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
  private Set<AssessmentSessionTypeCodeCriteriaEntity> assessmentSessionTypeCodeCriteriaEntities;

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