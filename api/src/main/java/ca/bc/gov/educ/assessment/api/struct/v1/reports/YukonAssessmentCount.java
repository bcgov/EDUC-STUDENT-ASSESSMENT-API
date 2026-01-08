package ca.bc.gov.educ.assessment.api.struct.v1.reports;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class YukonAssessmentCount {
    private UUID schoolID;
    private Long lte10Count;
    private Long lte12Count;
    private Long ltp10Count;
    private Long nme10Count;
    private Long nmf10Count;
    private Long ltp12Count;
    private Long ltf12Count;
    private Long total;
}
