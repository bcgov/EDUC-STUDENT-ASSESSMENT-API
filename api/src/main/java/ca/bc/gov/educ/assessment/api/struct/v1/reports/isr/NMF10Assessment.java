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
    private Integer multiChoicePlanningScore;
    private Integer multiChoicePlanningOutOf;
    private Integer multiChoiceEstimationsScore;
    private Integer multiChoiceEstimationsOutOf;
    private Integer multiChoiceGroupingScore;
    private Integer multiChoiceGroupingOutOf;
    private Integer multiChoiceModelScore;
    private Integer multiChoiceModelOutOf;
    private Integer writtenGroupingScore;
    private Integer writtenGroupingOutOf;
    private Integer writtenPlanningScore;
    private Integer writtenPlanningOutOf;
}