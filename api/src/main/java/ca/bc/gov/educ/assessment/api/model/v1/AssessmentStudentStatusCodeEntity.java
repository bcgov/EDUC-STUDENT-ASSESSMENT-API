package ca.bc.gov.educ.assessment.api.model.v1;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.PastOrPresent;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.DynamicUpdate;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@DynamicUpdate
@Entity
@Table(name = "ASSESSMENT_STUDENT_STATUS_CODE")
public class AssessmentStudentStatusCodeEntity {

    @Id
    @Column(name = "ASSESSMENT_STUDENT_STATUS_CODE", unique = true, length = 20)
    private String assessmentStudentStatusCode;

    @Column(name = "LABEL", length = 30)
    private String label;

    @Column(name = "DESCRIPTION")
    private String description;

    @Column(name = "DISPLAY_ORDER")
    private Integer displayOrder;

    @Column(name = "EFFECTIVE_DATE")
    private String effectiveDate;

    @Column(name = "EXPIRY_DATE")
    private String expiryDate;

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
