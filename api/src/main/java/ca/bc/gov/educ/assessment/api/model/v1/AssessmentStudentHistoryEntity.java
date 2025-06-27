package ca.bc.gov.educ.assessment.api.model.v1;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.PastOrPresent;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UuidGenerator;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Builder
@Table(name = "ASSESSMENT_STUDENT_HISTORY")
@JsonIgnoreProperties(ignoreUnknown = true)
public class AssessmentStudentHistoryEntity {

    @Id
    @GeneratedValue(generator = "UUID")
    @UuidGenerator
    @Column(name = "ASSESSMENT_STUDENT_HISTORY_ID", unique = true, updatable = false, columnDefinition = "BINARY(16)")
    private UUID assessmentStudentHistoryID;

    @Basic
    @Column(name = "ASSESSMENT_ID", columnDefinition = "BINARY(16)")
    private UUID assessmentID;

    @Basic
    @Column(name = "ASSESSMENT_STUDENT_ID", columnDefinition = "BINARY(16)")
    private UUID assessmentStudentID;

    @Column(name = "ASSESSMENT_FORM_ID")
    private UUID assessmentFormID;

    @Column(name = "SCHOOL_OF_RECORD_AT_WRITE_SCHOOL_ID", columnDefinition = "BINARY(16)")
    private UUID schoolAtWriteSchoolID;

    @Column(name = "ASSESSMENT_CENTER_SCHOOL_ID", columnDefinition = "BINARY(16)")
    private UUID assessmentCenterSchoolID;

    @Column(name = "SCHOOL_OF_RECORD_SCHOOL_ID", nullable = false, columnDefinition = "BINARY(16)")
    private UUID schoolOfRecordSchoolID;

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

    @Column(name = "LOCAL_ASSESSMENT_ID", length = 20)
    private String localAssessmentID;

    @Column(name = "PROFICIENCY_SCORE", length = 1)
    private Integer proficiencyScore;

    @Column(name = "PROVINCIAL_SPECIAL_CASE_CODE", length = 1)
    private String provincialSpecialCaseCode;

    @Column(name = "NUMBER_OF_ATTEMPTS", length = 1)
    private Integer numberOfAttempts;

    @Column(name = "ADAPTED_ASSESSMENT_CODE", length = 10)
    private String adaptedAssessmentCode;

    @Column(name = "IRT_SCORE", length = 7)
    private String irtScore;

    @Column(name = "MARKING_SESSION", length = 6)
    private String markingSession;

    @Column(name = "RAW_SCORE")
    private BigDecimal rawScore;

    @Column(name = "MC_TOTAL")
    private BigDecimal mcTotal;

    @Column(name = "OE_TOTAL")
    private BigDecimal oeTotal;

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
