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
    private Integer onlinePlanAndDesignScore;
    private Integer onlinePlanAndDesignOutOf;
    private Integer onlineReasonedEstimatesScore;
    private Integer onlineReasonedEstimatesOutOf;
    private Integer onlineFairShareScore;
    private Integer onlineFairShareOutOf;
    private Integer onlineModelScore;
    private Integer onlineModelOutOf;
    private Integer writtenFairScore;
    private Integer writtenFairOutOf;
    private Integer writtenReasonedEstimatesScore;
    private Integer writtenReasonedEstimatesOutOf;
}