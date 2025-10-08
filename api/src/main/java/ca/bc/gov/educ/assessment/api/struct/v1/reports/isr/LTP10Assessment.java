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
    private String communicateOralScore;
    private String communicateOralOutOf;
    private String partASelectedResponseScore;
    private String partASelectedResponseOutOf;
    private String partAWrittenShortScore;
    private String partAWrittenShortOutOf;
    private String partAWrittenGraphicScore;
    private String partAWrittenGraphicOutOf;
    private String partAWrittenLongScore;
    private String partAWrittenLongOutOf;
    private String partBSelectedResponseScore;
    private String partBSelectedResponseOutOf;
    private String partBWrittenShortScore;
    private String partBWrittenShortOutOf;
    private String partOralPart1Score;
    private String partOralPart1OutOf;
    private String partOralPart2Score;
    private String partOralPart2OutOf;
    private String partOralPart3Score;
    private String partOralPart3OutOf;
}