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
public class NMF10Assessment extends ISRAssessment {
    private String multiChoicePlanningScore;
    private String multiChoicePlanningOutOf;
    private String multiChoiceEstimationsScore;
    private String multiChoiceEstimationsOutOf;
    private String multiChoiceGroupingScore;
    private String multiChoiceGroupingOutOf;
    private String multiChoiceModelScore;
    private String multiChoiceModelOutOf;
    private String writtenGroupingScore;
    private String writtenGroupingOutOf;
    private String writtenPlanningScore;
    private String writtenPlanningOutOf;
    private String totalMultiOverall;
    private String outOfMultiOverall;
    private String totalWrittenOverall;
    private String outOfWrittenOverall;
}