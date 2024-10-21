package ca.bc.gov.educ.eas.api.model.v1;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * The type Saga event.
 */
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "EAS_SAGA_EVENT_STATES")
@DynamicUpdate
public class SagaEventStatesEntity {

  /**
   * The Saga event id.
   */
  @Id
  @GeneratedValue(generator = "UUID")
  @UuidGenerator
  @Column(name = "SAGA_EVENT_ID", unique = true, updatable = false, columnDefinition = "BINARY(16)")
  UUID sagaEventId;


  /**
   * The Saga.
   */
  @ToString.Exclude
  @EqualsAndHashCode.Exclude
  @ManyToOne
  @JoinColumn(name = "SAGA_ID", updatable = false, columnDefinition = "BINARY(16)")
  EasSagaEntity saga;

  /**
   * The Saga event state.
   */
  @NotNull(message = "saga_event_state cannot be null")
  @Column(name = "SAGA_EVENT_STATE")
  String sagaEventState;

  /**
   * The Saga event outcome.
   */
  @NotNull(message = "saga_event_outcome cannot be null")
  @Column(name = "SAGA_EVENT_OUTCOME")
  String sagaEventOutcome;

  /**
   * The Saga step number.
   */
  @NotNull(message = "saga_step_number cannot be null")
  @Column(name = "SAGA_STEP_NUMBER")
  Integer sagaStepNumber;

  /**
   * The Saga event response.
   */
  @Column(name = "SAGA_EVENT_RESPONSE",  length = 10485760)
  String sagaEventResponse;

  /**
   * The Create user.
   */
  @NotNull(message = "create user cannot be null")
  @Column(name = "CREATE_USER", updatable = false)
  @Size(max = 100)
  String createUser;

  /**
   * The Update user.
   */
  @NotNull(message = "update user cannot be null")
  @Column(name = "UPDATE_USER")
  @Size(max = 100)
  String updateUser;

  /**
   * The Create date.
   */
  @PastOrPresent
  @Column(name = "CREATE_DATE", updatable = false)
  LocalDateTime createDate;

  /**
   * The Update date.
   */
  @PastOrPresent
  @Column(name = "UPDATE_DATE")
  LocalDateTime updateDate;

}
