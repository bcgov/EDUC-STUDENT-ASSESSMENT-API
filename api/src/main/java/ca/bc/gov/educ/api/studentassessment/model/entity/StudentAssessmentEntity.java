package ca.bc.gov.educ.api.studentassessment.model.entity;

import javax.persistence.Column;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.Table;

import lombok.Data;

@Data
@Entity
@Table(name = "TRAX_STUDENT_ASSESSMENTS")
public class StudentAssessmentEntity {
	@EmbeddedId
    private StudentAssessmentId assessmentKey;   
    
    @Column(name = "SPECIAL_CASE", nullable = true)
    private String specialCase;

    @Column(name = "EXCEEDED_WRITES_FLAG", nullable = true)
    private String exceededWriteFlag;
    
    @Column(name = "ASSM_PROFICIENCY_SCORE", nullable = true)
    private Double proficiencyScore;
    
    @Column(name = "MINCODE_ASSMT", nullable = true)
    private String mincodeAssessment;
    
    
  
}
