package ca.bc.gov.educ.api.studentassessment.model.dto;

import java.sql.Date;

import org.springframework.stereotype.Component;

import lombok.Data;

@Data
@Component
public class Assessment {

	private String assessmentCode;
    private String assessmentName;
    private String language;    
    private Date startDate;
    private Date endDate;
    
	@Override
	public String toString() {
		return "Assessment [assessmentCode=" + assessmentCode + ", assessmentName=" + assessmentName + ", language="
				+ language + ", startDate=" + startDate + ", endDate=" + endDate + "]";
	}
    
			
}
