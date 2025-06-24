package ca.bc.gov.educ.assessment.api.batch.struct;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AssessmentResultDetails {
    private String txID;
    private String componentType;
    private String assessmentCode;
    private String assessmentSession;
    private String mincode;
    private String pen;
    private String formCode;
    private String openEndedMarks;
    private String multiChoiceMarks;
    private String choicePath;
    private String specialCaseCode;
    private String proficiencyScore;
    private String irtScore;
    private String adaptedAssessmentIndicator;
    private String markingSession;
}
