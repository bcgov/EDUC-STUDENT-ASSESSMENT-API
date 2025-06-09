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
@Table(name = "ASSESSMENT_STUDENT_ANSWER")
public class AssessmentStudentAnswerEntity {

    @Id
    @UuidGenerator
    @Column(name = "ASSESSMENT_STUDENT_ANSWER_ID", unique = true, updatable = false, columnDefinition = "BINARY(16)")
    private UUID assessmentStudentAnswerID;

    @Column(name = "ASSESSMENT_STUDENT_ID", updatable = false)
    UUID assessmentStudentID;

    @Column(name = "ASSESSMENT_QUESTION_ID", updatable = false)
    UUID assessmentQuestionID;

    @Column(name = "MC_ASSESSMENT_RESPONSE")
    private String mcAssessmentResponse;

    @Column(name = "MC_SCORE")
    private String mcScore;

    @Column(name = "NUM_OMITS")
    private String numOmits;

    @Column(name = "COMPONENT_TOTAL")
    private String componentTotal;

    @Column(name = "COMPONENT_SOURCE")
    private String componentSource;

    @Column(name = "SR_CHOICE_PATH")
    private String srChoicePath;

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
