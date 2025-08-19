package ca.bc.gov.educ.assessment.api.struct.v1.reports.isr;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@ToString(callSuper = true)
public class LTP10Assessment extends ISRAssessment {
    private String comprehendScore;
    private String comprehendOutOf;
    private String communicateScore;
    private String communicateOutOf;
    private String partASelectedResponseScore;
    private String partASelectedResponseOutOf;
    private String partAWrittenResponseGraphicScore;
    private String partAWrittenResponseGraphicOutOf;
    private String partAWrittenResponseUnderstandingScore;
    private String partAWrittenResponseUnderstandingOutOf;
    private String partBSelectedResponseScore;
    private String partBSelectedResponseOutOf;
    private String partBWrittenResponseUnderstandingScore;
    private String partBWrittenResponseUnderstandingOutOf;
}