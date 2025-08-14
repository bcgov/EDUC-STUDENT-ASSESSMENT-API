package ca.bc.gov.educ.assessment.api.model.v1;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
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
@Table(name = "STAGED_STUDENT_RESULT_UPLOAD")
@JsonIgnoreProperties(ignoreUnknown = true)
public class StagedStudentResultEntity {

  @Id
  @GeneratedValue(generator = "UUID")
  @UuidGenerator
  @Column(name = "STAGED_STUDENT_RESULT_ID", unique = true, updatable = false, columnDefinition = "BINARY(16)")
  private UUID stagedStudentResultID;

  @ToString.Exclude
  @EqualsAndHashCode.Exclude
  @ManyToOne(optional = false, targetEntity = AssessmentEntity.class)
  @JoinColumn(name = "ASSESSMENT_ID", referencedColumnName = "ASSESSMENT_ID")
  AssessmentEntity assessmentEntity;

  @Column(name = "PEN", nullable = false, length = 9)
  private String pen;

  @Column(name = "MINCODE", length = 8)
  private String mincode;

  @Column(name = "STAGED_STUDENT_RESULT_STATUS", length = 10)
  private String stagedStudentResultStatus;

  @Column(name = "COMPONENT_TYPE", length = 1)
  private String componentType;

  @Column(name = "ASSESSMENT_FORM_ID")
  private UUID assessmentFormID;

  @Column(name = "OE_MARKS", length = 152)
  private String oeMarks;

  @Column(name = "MC_MARKS", length = 240)
  private String mcMarks;

  @Column(name = "CHOICE_PATH", length = 1)
  private String choicePath;

  @Column(name = "PROVINCIAL_SPECIAL_CASE_CODE", length = 1)
  private String provincialSpecialCaseCode;

  @Column(name = "PROFICIENCY_SCORE")
  private Integer proficiencyScore;

  @Column(name = "ADAPTED_ASSESSMENT_CODE", length = 10)
  private String adaptedAssessmentCode;

  @Column(name = "IRT_SCORE", length = 7)
  private String irtScore;

  @Column(name = "MARKING_SESSION", length = 6)
  private String markingSession;

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
