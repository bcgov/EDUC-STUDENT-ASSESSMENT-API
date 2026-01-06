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
public class NME10Assessment extends ISRAssessment {
    private String onlinePlanAndDesignScore;
    private String onlinePlanAndDesignOutOf;
    private String onlineReasonedEstimatesScore;
    private String onlineReasonedEstimatesOutOf;
    private String onlineFairShareScore;
    private String onlineFairShareOutOf;
    private String onlineModelScore;
    private String onlineModelOutOf;
    private String writtenFairScore;
    private String writtenFairOutOf;
    private String writtenReasonedEstimatesScore;
    private String writtenReasonedEstimatesOutOf;
    private String writtenPlanScore;
    private String writtenPlanOutOf;
    private String writtenModelScore;
    private String writtenModelOutOf;
    private String totalMultiOverall;
    private String outOfMultiOverall;
    private String totalWrittenOverall;
    private String outOfWrittenOverall;
}