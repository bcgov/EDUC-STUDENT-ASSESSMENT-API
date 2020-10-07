package ca.bc.gov.educ.api.studentassessment.model.entity;

import lombok.Data;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Parameter;

import javax.persistence.*;
import java.util.Date;
import java.util.UUID;

@Data
@Entity
@Table(name = "STUD_GRAD_ASSMT")
public class StudentAssessmentEntity {
    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(
            name = "UUID",
            strategy = "org.hibernate.id.UUIDGenerator",
            parameters = {
                    @Parameter(
                            name = "uuid_gen_strategy_class",
                            value = "org.hibernate.id.uuid.CustomVersionOneStrategy"
                    )
            }
    )
    @Column(name = "stud_grad_assessment_id", unique = true, updatable = false, columnDefinition = "BINARY(16)")
    private UUID studentGradAssessmentId;

    @Column(name = "STUD_NO", nullable = true)
    private String pen;

    @Column(name = "ASSMT_SESSION", nullable = true)
    private Date sessionDate;

    @Column(name = "ASSMT_CODE", nullable = true)
    private String assessmentCode;
    
    @Column(name = "GRAD_REQT_MET", nullable = true)
    private String gradReqMet;    
    
    @Column(name = "SPECIAL_CASE", nullable = true)
    private String specialCase;

    @Column(name = "EXCEEDED_WRITES_FLAG", nullable = true)
    private String exceededWriteFlag;
    
    @Column(name = "ASSMT_PROFICIENCY_SCORE", nullable = true)
    private Double proficiencyScore;

    @Column(name = "created_by", nullable = false)
    private String createdBy;

    @Column(name = "created_timestamp", nullable = false)
    private Date createdTimestamp;

    @Column(name = "updated_by", nullable = false)
    private String updatedBy;

    @Column(name = "updated_timestamp", nullable = false)
    private Date updatedTimestamp;    
}
