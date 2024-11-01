package ca.bc.gov.educ.eas.api.model.v1;

import jakarta.persistence.*;
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

  @Column(name = "EFFECTIVE_DATE", updatable = false)
  private LocalDateTime effectiveDate;

  @Column(name = "EXPIRY_DATE")
  private LocalDateTime expiryDate;

  @ToString.Exclude
  @EqualsAndHashCode.Exclude
  @OneToMany(mappedBy = "assessmentSessionCriteriaEntity", fetch = FetchType.EAGER, cascade = CascadeType.ALL)
  private Set<AssessmentCriteriaEntity> assessmentCriteriaEntities;

  @Column(name = "CREATE_USER", updatable = false, length = 100)
  private String createUser;

  @Column(name = "CREATE_DATE", updatable = false)
  private LocalDateTime createDate;

  @Column(name = "UPDATE_USER", nullable = false, length = 100)
  private String updateUser;

  @Column(name = "UPDATE_DATE", nullable = false)
  private LocalDateTime updateDate;
}