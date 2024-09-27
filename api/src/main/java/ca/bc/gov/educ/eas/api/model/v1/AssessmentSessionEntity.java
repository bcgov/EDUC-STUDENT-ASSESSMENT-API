package ca.bc.gov.educ.eas.api.model.v1;

import jakarta.persistence.*;
import jakarta.validation.constraints.PastOrPresent;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Parameter;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@DynamicUpdate
@Entity
@Builder
@Table(name = "ASSESSMENT_SESSION")
public class AssessmentSessionEntity {

  @Id
  @GeneratedValue(generator = "UUID")
  @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator", parameters = {
          @Parameter(name = "uuid_gen_strategy_class", value = "org.hibernate.id.uuid.CustomVersionOneStrategy")})
  @Column(name = "ASSESSMENT_SESSION_ID", unique = true, updatable = false, columnDefinition = "BINARY(16)")
  private UUID assessmentSessionID;

  @Column(name = "COURSE_SESSION", nullable = false, length = 6)
  private int courseSession;

  @Column(name = "COURSE_YEAR", nullable = false, length = 4)
  private int courseYear;

  @Column(name = "COURSE_MONTH", nullable = false, length = 2)
  private int courseMonth;

  @Column(name = "STATUS_CODE", nullable = false, length = 10)
  private String statusCode;

  @Column(name = "ACTIVE_FROM_DATE", nullable = false)
  private LocalDateTime activeFromDate;

  @Column(name = "ACTIVE_UNTIL_DATE", nullable = false)
  private LocalDateTime activeUntilDate;

  @Column(name = "CREATE_USER", updatable = false, length = 32)
  private String createUser;

  @PastOrPresent
  @Column(name = "CREATE_DATE", updatable = false)
  private LocalDateTime createDate;

  @Column(name = "UPDATE_USER", nullable = false, length = 32)
  private String updateUser;

  @PastOrPresent
  @Column(name = "UPDATE_DATE", nullable = false)
  private LocalDateTime updateDate;
}
