package ca.bc.gov.educ.assessment.api.model.v1;

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
@Table(name = "ASSESSMENT_COMPONENT")
public class AssessmentComponentEntity {

    @Id
    @UuidGenerator
    @Column(name = "ASSESSMENT_COMPONENT_ID", unique = true, updatable = false, columnDefinition = "BINARY(16)")
    private UUID assessmentComponentID;

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @ManyToOne(optional = false, targetEntity = AssessmentFormEntity.class)
    @JoinColumn(name = "ASSESSMENT_FORM_ID", referencedColumnName = "ASSESSMENT_FORM_ID", updatable = false)
    AssessmentFormEntity assessmentFormEntity;

    @Column(name="COMPONENT_TYPE_CODE", nullable = false)
    private Integer componentTypeCode;

    @Column(name = "COMPONENT_SUB_TYPE_CODE", nullable = false)
    private String componentSubTypeCode;

    @Column(name = "QUESTION_COUNT")
    private String questionCount;

    @Column(name = "NUM_OMITS")
    private String numOmits;

    @Column(name = "OE_ITEM_CNT")
    private String oeItemCount;

    @Column(name = "OE_MARK_COUNT")
    private String oeMarkCount;

    @Column(name = "PRINT_MATERIALS_FLAG")
    private String printMaterialFlag;

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