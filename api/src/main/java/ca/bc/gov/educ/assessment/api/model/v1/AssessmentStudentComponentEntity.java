package ca.bc.gov.educ.assessment.api.model.v1;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.PastOrPresent;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
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
@Table(name = "ASSESSMENT_STUDENT_COMPONENT")
public class AssessmentStudentComponentEntity {

    @Id
    @UuidGenerator
    @Column(name = "ASSESSMENT_STUDENT_COMPONENT_ID", unique = true, updatable = false, columnDefinition = "BINARY(16)")
    private UUID assessmentStudentComponentID;

    @Column(name = "ASSESSMENT_STUDENT_ID", updatable = false)
    UUID assessmentStudentID;

    @Column(name="COMPONENT_TYPE_CODE", nullable = false)
    private Integer componentTypeCode;

    @Column(name = "COMPONENT_SUB_TYPE_CODE")
    private String componentSubTypeCode;

    @Column(name = "COMPONENT_TOTAL")
    private BigDecimal componentTotal;

    @Column(name = "COMPONENT_SOURCE")
    private String componentSource;

    @Column(name = "CHOICE_PATH")
    private String ChoicePath;

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

}
