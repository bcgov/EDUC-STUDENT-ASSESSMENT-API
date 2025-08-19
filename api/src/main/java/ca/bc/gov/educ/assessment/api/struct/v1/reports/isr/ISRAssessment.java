package ca.bc.gov.educ.assessment.api.struct.v1.reports.isr;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@ToString(callSuper = true)
public class ISRAssessment {
    private String assessmentCode;
    private String session;
    private String score;
}