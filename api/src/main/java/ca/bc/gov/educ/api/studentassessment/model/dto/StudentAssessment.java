package ca.bc.gov.educ.api.studentassessment.model.dto;

import org.springframework.stereotype.Component;

import lombok.Data;

@Data
@Component
public class StudentAssessment {

	private String pen;    
    private String assessmentCode;    
    private String assessmentName;  
    private String sessionDate;
    private String gradReqMet;
    private String specialCase;
    private String exceededWriteFlag;    
    private Double proficiencyScore;
    private Assessment assessmentDetails;
    
    public String getPen() {
    	return pen != null ? pen.trim():null;
    }
    
	@Override
	public String toString() {
		return "StudentAssessment [pen=" + pen + ", assessmentCode=" + assessmentCode + ", sessionDate=" + sessionDate
				+ ", gradReqMet=" + gradReqMet + ", specialCase=" + specialCase + ", exceededWriteFlag="
				+ exceededWriteFlag + ", proficiencyScore=" + proficiencyScore + "]";
	}	 
}