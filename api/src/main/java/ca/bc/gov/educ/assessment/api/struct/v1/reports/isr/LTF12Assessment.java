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
public class LTF12Assessment extends ISRAssessment {
    private String comprehendScore;
    private String comprehendOutOf;
    private String communicateScore;
    private String communicateOutOf;
    private String communicateOralScore;
    private String communicateOralOutOf;
    private String partASelectedResponseScore;
    private String partASelectedResponseOutOf;
    private String partBSelectedResponseScore;
    private String partBSelectedResponseOutOf;
    private String partBWrittenResponseAnalyzeScore;
    private String partBWrittenResponseAnalyzeOutOf;
    private String partBWrittenResponseDissertationFoundationScore;
    private String partBWrittenResponseDissertationFoundationOutOf;
    private String partBWrittenResponseDissertationFormScore;
    private String partBWrittenResponseDissertationFormOutOf;
    private String partBChoicePath;
    private String partCOralResponsePart1FoundationScore;
    private String partCOralResponsePart1FoundationOutOf;
    private String partCOralResponsePart1FormScore;
    private String partCOralResponsePart1FormOutOf;
    private String partCOralResponsePart1OralScore;
    private String partCOralResponsePart1OralOutOf;
    private String partCOralResponsePart2DiscourseFoundationScore;
    private String partCOralResponsePart2DiscourseFoundationOutOf;
    private String partCOralResponsePart2DiscourseFormScore;
    private String partCOralResponsePart2DiscourseFormOutOf;
    private String partCOralResponsePart2DiscourseOralScore;
    private String partCOralResponsePart2DiscourseOralOutOf;
}