package ca.bc.gov.educ.eas.api.model.v1;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.PastOrPresent;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UuidGenerator;

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
    @Column(name = "ASSESSMENT_STUDENT_ID", columnDefinition = "BINARY(16)")
    private UUID assessmentStudentID;

    @Basic
    @Column(name = "ASSESSMENT_ID", columnDefinition = "BINARY(16)")
    private UUID assessmentID;

    @Column(name = "SCHOOL_ID", nullable = false, columnDefinition = "BINARY(16)")
    private UUID schoolID;

    @Column(name = "STUDENT_ID", nullable = false, columnDefinition = "BINARY(16)")
    private UUID studentID;

    @Column(name = "PEN", nullable = false, length = 9)
    private String pen;

    @Column(name = "LOCAL_ID", length = 12)
    private String localID;

    @Column(name = "IS_ELECTRONIC_EXAM", length = 1)
    private Boolean isElectronicExam;

    @Column(name = "FINAL_PERCENTAGE", length = 3)
    private String finalPercentage;

    @Column(name = "PROVINCIAL_SPECIAL_CASE_CODE", length = 1)
    private String provincialSpecialCaseCode;

    @Column(name = "COURSE_STATUS_CODE", length = 1)
    private String courseStatusCode;

    @Column(name = "ASSESSMENT_STUDENT_STATUS_CODE", nullable = false, length = 20)
    private String assessmentStudentStatusCode;

    @Column(name = "CREATE_USER", updatable = false, length = 100)
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
