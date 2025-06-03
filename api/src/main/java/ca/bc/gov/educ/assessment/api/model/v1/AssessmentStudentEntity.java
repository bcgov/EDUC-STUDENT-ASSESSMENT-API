package ca.bc.gov.educ.assessment.api.model.v1;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.PastOrPresent;
import lombok.*;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
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
  @UuidGenerator
  @Column(name = "ASSESSMENT_STUDENT_ID", unique = true, updatable = false, columnDefinition = "BINARY(16)")
  private UUID assessmentStudentID;

  @ToString.Exclude
  @EqualsAndHashCode.Exclude
  @ManyToOne(optional = false, targetEntity = AssessmentEntity.class)
  @JoinColumn(name = "ASSESSMENT_ID", referencedColumnName = "ASSESSMENT_ID")
  AssessmentEntity assessmentEntity;

  @ToString.Exclude
  @EqualsAndHashCode.Exclude
  @ManyToOne(targetEntity = AssessmentFormEntity.class)
  @JoinColumn(name = "ASSESSMENT_FORM_ID", referencedColumnName = "ASSESSMENT_FORM_ID")
  AssessmentFormEntity assessmentFormEntity;

  @Column(name = "DISTRICT_ID", nullable = false, columnDefinition = "BINARY(16)")
  private UUID districtID;

  @Column(name = "SCHOOL_ID", nullable = false, columnDefinition = "BINARY(16)")
  private UUID schoolID;

  @Column(name = "ASSESSMENT_CENTER_ID", columnDefinition = "BINARY(16)")
  private UUID assessmentCenterID;

  @Column(name = "STUDENT_ID", nullable = false, columnDefinition = "BINARY(16)")
  private UUID studentID;

  @Column(name = "GIVEN_NAME", length = 25)
  private String givenName;

  @Column(name = "SURNAME", nullable = false, length = 25)
  private String surname;

  @Column(name = "PEN", nullable = false, length = 9)
  private String pen;

  @Column(name = "LOCAL_ID", length = 12)
  private String localID;

  @Column(name = "LOCAL_COURSE_ID", length = 20)
  private String localCourseID;

  @Column(name = "IS_ELECTRONIC_EXAM", length = 1)
  private Boolean isElectronicExam;

  @Column(name = "PROFICIENCY_SCORE", length = 1)
  private Integer proficiencyScore;

  @Column(name = "PROVINCIAL_SPECIAL_CASE_CODE", length = 1)
  private String provincialSpecialCaseCode;

  @Column(name = "COURSE_STATUS_CODE", length = 1)
  private String courseStatusCode;

  @Column(name = "ASSESSMENT_STUDENT_STATUS_CODE", nullable = false, length = 20)
  private String assessmentStudentStatusCode;

  @Column(name = "NUMBER_OF_ATTEMPTS", length = 1)
  private Integer numberOfAttempts;

  @Column(name = "ADAPTED_ASSESSMENT_INDICATOR", length = 1)
  private String adaptedAssessmentIndicator;

  @Column(name = "IRT_SCORE", length = 7)
  private String irtScore;

  @Column(name = "SR_CHOICE_PATH", length = 1)
  private String srChoicePath;

  @Column(name = "CREATE_USER", updatable = false , length = 100)
  private String createUser;

  @PastOrPresent
  @Column(name = "CREATE_DATE", updatable = false)
  private LocalDateTime createDate;

  @Column(name = "UPDATE_USER", length = 100)
  private String updateUser;

  @PastOrPresent
  @Column(name = "UPDATE_DATE")
  private LocalDateTime updateDate;

}
