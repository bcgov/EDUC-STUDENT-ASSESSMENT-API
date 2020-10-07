package ca.bc.gov.educ.api.studentassessment.model.dto;

import java.util.UUID;

import org.springframework.stereotype.Component;

import lombok.Data;

@Data
@Component
public class StudentAssessment {

	private UUID studentGradAssessmentId;
    private String pen;
    private String sessionDate;
    private String assessmentCode;    
    private String gradReqMet;
    private String specialCase;
    private String exceededWriteFlag;    
    private Double proficiencyScore;
    
	@Override
	public String toString() {
		return "StudentAssessment [studentGradAssessmentId=" + studentGradAssessmentId + ", pen=" + pen
				+ ", sessionDate=" + sessionDate + ", assessmentCode=" + assessmentCode + ", gradReqMet=" + gradReqMet
				+ ", specialCase=" + specialCase + ", exceededWriteFlag=" + exceededWriteFlag + ", proficiencyScore="
				+ proficiencyScore + "]";
	}   
}