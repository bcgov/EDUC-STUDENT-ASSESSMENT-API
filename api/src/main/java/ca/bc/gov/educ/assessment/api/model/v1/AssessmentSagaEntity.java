package ca.bc.gov.educ.assessment.api.model.v1;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * The type Saga.
 */
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
@Entity
@Table(name = "ASSESSMENT_SAGA")
@DynamicUpdate
public class AssessmentSagaEntity {

  @Id
  @GeneratedValue(generator = "UUID")
  @UuidGenerator
  @Column(name = "SAGA_ID", unique = true, updatable = false, columnDefinition = "BINARY(16)")
  UUID sagaId;

  @Column(name = "STAGED_STUDENT_RESULT_ID", nullable = false, columnDefinition = "BINARY(16)")
  private UUID stagedStudentResultID;

  @Column(name = "ASSESSMENT_STUDENT_ID", nullable = false, columnDefinition = "BINARY(16)")
  private UUID assessmentStudentID;

  @Column(name = "ASSESSMENT_SESSION_ID")
  private UUID assessmentSessionID;

  @Column(name = "ASSESSMENT_ID", nullable = false, columnDefinition = "BINARY(16)")
  private UUID assessmentID;

  @Column(name = "PEN")
  private String pen;

  @NotNull(message = "saga name cannot be null")
  @Column(name = "SAGA_NAME")
  String sagaName;

  @NotNull(message = "saga state cannot be null")
  @Column(name = "SAGA_STATE")
  String sagaState;

  @NotNull(message = "payload cannot be null")
  @Column(name = "PAYLOAD",  length = 10485760)
  private String payload;

  @NotNull(message = "status cannot be null")
  @Column(name = "STATUS")
  String status;

  @NotNull(message = "create user cannot be null")
  @Column(name = "CREATE_USER", updatable = false)
  @Size(max = 100)
  String createUser;

  @NotNull(message = "update user cannot be null")
  @Column(name = "UPDATE_USER")
  @Size(max = 100)
  String updateUser;

  @PastOrPresent
  @Column(name = "CREATE_DATE", updatable = false)
  LocalDateTime createDate;

  @PastOrPresent
  @Column(name = "UPDATE_DATE")
  LocalDateTime updateDate;

  @Column(name = "RETRY_COUNT")
  private Integer retryCount;

}
