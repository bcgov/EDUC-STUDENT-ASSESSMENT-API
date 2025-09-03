package ca.bc.gov.educ.assessment.api.struct.v1.reports.isr;

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
public class ISRReportNode {
    private String schoolDetail;
    private String reportGeneratedDate;
    private String studentName;
    private String studentPEN;
    private List<ISRAssessment> assessmentDetails;
    private List<ISRAssessmentSummary> assessments;
}