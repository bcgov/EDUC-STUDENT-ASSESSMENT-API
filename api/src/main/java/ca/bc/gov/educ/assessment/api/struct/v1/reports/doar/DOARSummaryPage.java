package ca.bc.gov.educ.assessment.api.struct.v1.reports.doar;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import java.util.List;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@ToString(callSuper = true)
public class DOARSummaryPage {
    private String reportId;
    private String sessionDetail;
    private String reportTitle;
    private String reportGeneratedDate;
    private String districtNumberAndName;
    private String schoolMincodeAndName;
    private String assessmentType;
    private boolean isCSF;
    List<ProficiencyLevel> proficiencySection;
    List<TaskScore> taskScore;
    List<ComprehendScore> comprehendScore;
    List<CommunicateScore> communicateScore;
    List<CognitiveLevelScore> cognitiveLevelScore;
    List<NumeracyScore> numeracyScore;
    List<CommunicateOralScore> communicateOralScore;
}