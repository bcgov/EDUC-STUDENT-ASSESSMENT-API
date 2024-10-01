package ca.bc.gov.educ.eas.api.model.v1;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.PastOrPresent;
import lombok.*;
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
@Table(name = "ASSESSMENT_STUDENT")
@JsonIgnoreProperties(ignoreUnknown = true)
public class AssessmentStudentEntity {

  @Id
  @GeneratedValue(generator = "UUID")
  @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator", parameters = {
          @Parameter(name = "uuid_gen_strategy_class", value = "org.hibernate.id.uuid.CustomVersionOneStrategy")})
  @Column(name = "ASSESSMENT_STUDENT_ID", unique = true, updatable = false, columnDefinition = "BINARY(16)")
  private UUID assessmentStudentID;

  @ToString.Exclude
  @EqualsAndHashCode.Exclude
  @ManyToOne(optional = false, targetEntity = SessionEntity.class)
  @JoinColumn(name = "SESSION_ID", referencedColumnName = "SESSION_ID", updatable = false)
  SessionEntity sessionEntity;

  @Column(name = "ASSESSMENT_TYPE_CODE", nullable = false, length = 10)
  private String assessmentTypeCode;

  @Column(name = "SCHOOL_ID", nullable = false, columnDefinition = "BINARY(16)")
  private UUID schoolID;

  @Column(name = "STUDENT_ID", nullable = false, columnDefinition = "BINARY(16)")
  private UUID studentID;

  @Column(name = "PEN", nullable = false, length = 9)
  String pen;

  @Column(name = "LOCAL_ID", length = 12)
  String localID;

  @Column(name = "IS_ELECTRONIC_EXAM", length = 1)
  Boolean isElectronicExam;

  @Column(name = "FINAL_PERCENTAGE", length = 3)
  String finalPercentage;

  @Column(name = "PROVINCIAL_SPECIAL_CASE_CODE", length = 1)
  String provincialSpecialCaseCode;

  @Column(name = "COURSE_STATUS_CODE", length = 1)
  String courseStatusCode;

  @Column(name = "CREATE_USER", updatable = false , length = 32)
  private String createUser;

  @PastOrPresent
  @Column(name = "CREATE_DATE", updatable = false)
  private LocalDateTime createDate;

  @Column(name = "UPDATE_USER", length = 32)
  private String updateUser;

  @PastOrPresent
  @Column(name = "UPDATE_DATE")
  private LocalDateTime updateDate;
}
