package ca.bc.gov.educ.assessment.api.model.v1;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
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

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@DynamicUpdate
@Entity
@Table(name = "ADAPTED_ASSESSMENT_CODE")
@JsonIgnoreProperties(ignoreUnknown = true)
public class AdaptedAssessmentIndicatorCodeEntity {

    @Id
    @Column(name = "ADAPTED_ASSESSMENT_CODE", unique = true, length = 3)
    private String adaptedAssessmentCode;

    @Column(name = "LABEL", length = 30)
    private String label;

    @Column(name = "DESCRIPTION")
    private String description;

    @Column(name = "LEGACY_CODE")
    private String legacyCode;

    @Column(name = "DISPLAY_ORDER")
    private Integer displayOrder;

    @Column(name = "EFFECTIVE_DATE")
    private LocalDateTime effectiveDate;

    @Column(name = "EXPIRY_DATE")
    private LocalDateTime expiryDate;

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