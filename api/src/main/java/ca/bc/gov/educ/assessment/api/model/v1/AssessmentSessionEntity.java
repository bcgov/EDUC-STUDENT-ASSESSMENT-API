package ca.bc.gov.educ.assessment.api.model.v1;

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
@Table(name = "ASSESSMENT_SESSION")
public class AssessmentSessionEntity {

  @Id
  @GeneratedValue(generator = "UUID")
  @UuidGenerator
  @Column(name = "SESSION_ID", unique = true, updatable = false, columnDefinition = "BINARY(16)")
  private UUID sessionID;

  @Column(name = "SCHOOL_YEAR", nullable = false, length = 10)
  private String schoolYear;

  @Column(name = "COURSE_YEAR", nullable = false, length = 4)
  private String courseYear;

  @Column(name = "COURSE_MONTH", nullable = false, length = 2)
  private String courseMonth;

  @Column(name = "ACTIVE_FROM_DATE", nullable = false)
  private LocalDateTime activeFromDate;

  @Column(name = "ACTIVE_UNTIL_DATE", nullable = false)
  private LocalDateTime activeUntilDate;

  @Column(name = "APPROVAL_STUDENT_CERT_USER_ID", length = 25)
  private String approvalStudentCertUserID;

  @PastOrPresent
  @Column(name = "APPROVAL_STUDENT_CERT_SIGN_DATE")
  private LocalDateTime approvalStudentCertSignDate;

  @Column(name = "APPROVAL_ASSESSMENT_DESIGN_USER_ID", length = 25)
  private String approvalAssessmentDesignUserID;

  @PastOrPresent
  @Column(name = "APPROVAL_ASSESSMENT_DESIGN_SIGN_DATE")
  private LocalDateTime approvalAssessmentDesignSignDate;

  @Column(name = "APPROVAL_ASSESSMENT_ANALYSIS_USER_ID", length = 25)
  private String approvalAssessmentAnalysisUserID;

  @PastOrPresent
  @Column(name = "APPROVAL_ASSESSMENT_ANALYSIS_SIGN_DATE")
  private LocalDateTime approvalAssessmentAnalysisSignDate;

  @Column(name = "ASSESSMENT_REGISTRATIONS_EXPORT_USER_ID", length = 25)
  private String assessmentRegistrationsExportUserID;

  @PastOrPresent
  @Column(name = "ASSESSMENT_REGISTRATIONS_EXPORT_DATE")
  private LocalDateTime assessmentRegistrationsExportDate;

  @Column(name = "SESSION_WRITING_ATTEMPTS_EXPORT_USER_ID", length = 25)
  private String sessionWritingAttemptsExportUserID;

  @PastOrPresent
  @Column(name = "SESSION_WRITING_ATTEMPTS_EXPORT_DATE")
  private LocalDateTime sessionWritingAttemptsExportDate;

  @Column(name = "PEN_MERGES_EXPORT_USER_ID", length = 25)
  private String penMergesExportUserID;

  @PastOrPresent
  @Column(name = "PEN_MERGES_EXPORT_DATE")
  private LocalDateTime penMergesExportDate;

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
  @OneToMany(mappedBy = "assessmentSessionEntity", fetch = FetchType.EAGER, cascade = CascadeType.ALL, targetEntity = AssessmentEntity.class)
  Set<AssessmentEntity> assessments;
}
