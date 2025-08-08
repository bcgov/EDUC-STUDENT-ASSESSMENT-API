package ca.bc.gov.educ.assessment.api.model.v1;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.PastOrPresent;
import lombok.*;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.UuidGenerator;

import java.math.BigDecimal;
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
public class AssessmentStudentLightEntity {

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

  @Column(name = "ASSESSMENT_FORM_ID")
  private UUID assessmentFormID;

  @Column(name = "SCHOOL_OF_RECORD_AT_WRITE_SCHOOL_ID", columnDefinition = "BINARY(16)")
  private UUID schoolAtWriteSchoolID;

  @Column(name = "SURNAME", nullable = false, length = 25)
  private String surname;

  @Column(name = "PEN", nullable = false, length = 9)
  private String pen;

  @Column(name = "GRADE_AT_REGISTRATION", length = 2)
  private String gradeAtRegistration;

  @Column(name = "PROFICIENCY_SCORE", length = 1)
  private Integer proficiencyScore;

  @Column(name = "PROVINCIAL_SPECIAL_CASE_CODE", length = 1)
  private String provincialSpecialCaseCode;
  
  @Column(name = "RAW_SCORE")
  private BigDecimal rawScore;

  @Column(name = "MC_TOTAL")
  private BigDecimal mcTotal;

  @Column(name = "OE_TOTAL")
  private BigDecimal oeTotal;

  @Column(name = "ADAPTED_ASSESSMENT_CODE", length = 10)
  private String adaptedAssessmentCode;

  @Column(name = "IRT_SCORE", length = 7)
  private String irtScore;

  @Column(name = "STUDENT_STATUS", nullable = false, length = 10)
  private String studentStatus;

  @PastOrPresent
  @Column(name = "DOWNLOAD_DATE", updatable = false)
  private LocalDateTime downloadDate;
  
  @Column(name = "ASSESSMENT_CENTER_SCHOOL_ID", columnDefinition = "BINARY(16)")
  private UUID assessmentCenterSchoolID;

}
