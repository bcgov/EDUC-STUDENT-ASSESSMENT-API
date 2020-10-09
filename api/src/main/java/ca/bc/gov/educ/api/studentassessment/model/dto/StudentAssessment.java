package ca.bc.gov.educ.api.studentassessment.model.dto;

import org.springframework.stereotype.Component;

import lombok.Data;

@Data
@Component
public class StudentAssessment {

	private StudentAssessmentId assessmentKey;   
    private String gradReqMet;
    private String specialCase;
    private String exceededWriteFlag;    
    private Double proficiencyScore;
    
    
	@Override
	public String toString() {
		return "StudentAssessment [assessmentKey=" + assessmentKey + ", gradReqMet=" + gradReqMet + ", specialCase="
				+ specialCase + ", exceededWriteFlag=" + exceededWriteFlag + ", proficiencyScore=" + proficiencyScore
				+ "]";
	}
    
	 
}