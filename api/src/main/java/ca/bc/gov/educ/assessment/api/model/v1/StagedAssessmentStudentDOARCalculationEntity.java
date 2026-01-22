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
@Table(name = "STAGED_ASSESSMENT_STUDENT_DOAR_CALCULATION")
public class StagedAssessmentStudentDOARCalculationEntity {

    @Id
    @UuidGenerator
    @Column(name = "ASSESSMENT_STUDENT_DOAR_CALC_ID", unique = true, updatable = false, columnDefinition = "BINARY(16)")
    private UUID assessmentStudentDOARCalculationID;

    @Column(name = "ASSESSMENT_STUDENT_ID")
    private UUID assessmentStudentID;

    @Column(name = "ASSESSMENT_ID")
    private UUID assessmentID;
// lte10, 12
    @Column(name = "TASK_COMPREHEND")
    private BigDecimal taskComprehend;

    @Column(name = "TASK_COMMUNICATE")
    private BigDecimal taskCommunicate;

    @Column(name = "COMPREHEND_PART_A")
    private BigDecimal comprehendPartA;

    @Column(name = "COMPREHEND_PART_B")
    private BigDecimal comprehendPartB;

    @Column(name = "COMMUNICATE_GRAPHIC_ORG")
    private BigDecimal communicateGraphicOrg;

    @Column(name = "COMMUNICATE_UNDERSTANDING")
    private BigDecimal communicateUnderstanding;

    @Column(name = "COMMUNICATE_PERSONAL_CONN")
    private BigDecimal communicatePersonalConn;

    @Column(name = "DOK1")
    private BigDecimal dok1;

    @Column(name = "DOK2")
    private BigDecimal dok2;

    @Column(name = "DOK3")
    private BigDecimal dok3;

    //numeracy
    @Column(name = "TASK_PLAN")
    private BigDecimal taskPlan;

    @Column(name = "TASK_ESTIMATE")
    private BigDecimal taskEstimate;

    @Column(name = "TASK_FAIR")
    private BigDecimal taskFair;

    @Column(name = "TASK_MODEL")
    private BigDecimal taskModel;

    @Column(name = "NUMERACY_INTERPRET")
    private BigDecimal numeracyInterpret;

    @Column(name = "NUMERACY_APPLY")
    private BigDecimal numeracyApply;

    @Column(name = "NUMERACY_SOLVE")
    private BigDecimal numeracySolve;

    @Column(name = "NUMERACY_ANALYZE")
    private BigDecimal numeracyAnalyze;

    //LTP12
    @Column(name = "TASK_ORAL")
    private BigDecimal taskOral;

    @Column(name = "COMMUNICATE_ORAL_PART_1")
    private BigDecimal communicateOralPart1;

    @Column(name = "COMMUNICATE_ORAL_PART_2")
    private BigDecimal communicateOralPart2;

    @Column(name = "COMMUNICATE_ORAL_PART_3")
    private BigDecimal communicateOralPart3;

    //LTP10
    @Column(name = "COMPREHEND_PART_A_SHORT")
    private BigDecimal comprehendPartAShort;

    //LTF12
    @Column(name = "COMPREHEND_PART_A_TASK")
    private BigDecimal comprehendPartATask;

    @Column(name = "COMPREHEND_PART_B_INFO")
    private BigDecimal comprehendPartBInfo;

    @Column(name = "COMPREHEND_PART_B_EXP")
    private BigDecimal comprehendPartBExp;

    @Column(name = "DISSERTATION_BACKGROUND")
    private BigDecimal dissertationBackground;

    @Column(name = "DISSERTATION_FORM")
    private BigDecimal dissertationForm;

    @Column(name = "COMMUNICATE_ORAL_PART_1_BACKGROUND")
    private BigDecimal communicateOralPart1Background;

    @Column(name = "COMMUNICATE_ORAL_PART_1_FORM")
    private BigDecimal communicateOralPart1Form;

    @Column(name = "COMMUNICATE_ORAL_PART_1_EXPR")
    private BigDecimal communicateOralPart1Expression;

    @Column(name = "COMMUNICATE_ORAL_PART_2_BACKGROUND")
    private BigDecimal communicateOralPart2Background;

    @Column(name = "COMMUNICATE_ORAL_PART_2_FORM")
    private BigDecimal communicateOralPart2Form;

    @Column(name = "COMMUNICATE_ORAL_PART_2_EXPR")
    private BigDecimal communicateOralPart2Expression;

    @Column(name = "SELECTED_RESPONSE_CHOICE_PATH")
    private String selectedResponseChoicePath;

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