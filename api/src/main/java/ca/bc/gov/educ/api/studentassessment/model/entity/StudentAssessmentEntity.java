package ca.bc.gov.educ.api.studentassessment.model.entity;

import javax.persistence.Column;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.Table;

import lombok.Data;

@Data
@Entity
@Table(name = "STUD_GRAD_ASSMT")
public class StudentAssessmentEntity {
	@EmbeddedId
    private StudentAssessmentId assessmentKey;
    
    @Column(name = "GRAD_REQT_MET", nullable = true)
    private String gradReqMet;    
    
    @Column(name = "SPECIAL_CASE", nullable = true)
    private String specialCase;

    @Column(name = "EXCEEDED_WRITES_FLAG", nullable = true)
    private String exceededWriteFlag;
    
    @Column(name = "ASSMT_PROFICIENCY_SCORE", nullable = true)
    private Double proficiencyScore;
  
}
