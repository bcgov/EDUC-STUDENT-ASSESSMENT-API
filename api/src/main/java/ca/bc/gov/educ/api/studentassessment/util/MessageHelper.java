package ca.bc.gov.educ.api.studentassessment.util;

import org.springframework.beans.factory.annotation.Value;


public class MessageHelper {

	@Value("${validation.value.missing}")
	String missingValueString;

	@Value("${validation.value.unfound}")
	String unfoundValueString;

	public String missingValue(String missingValue) {
		return String.format(missingValueString, missingValue);
	}
	
	public String unfoundValue(String unfoundType, String value) {
		return String.format(unfoundValueString, unfoundType, value);
	}
}
