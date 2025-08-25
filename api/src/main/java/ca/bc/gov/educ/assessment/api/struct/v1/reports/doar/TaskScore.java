package ca.bc.gov.educ.assessment.api.struct.v1.reports.doar;

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
public class TaskScore {
    //lte
    private String level;
    private String taskComprehend;
    private String taskCommunicate;
    //nme
    private String taskPlan;
    private String taskEstimate;
    private String taskFair;
    private String taskModel;
    //ltp
    private String taskOral;
}