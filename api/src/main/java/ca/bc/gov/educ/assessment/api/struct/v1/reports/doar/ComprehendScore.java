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
public class ComprehendScore {
    private String level;
    private String comprehendPartA;
    private String comprehendPartB;
    //ltf12
    private String comprehendPartATask;
    private String comprehendPartBInfo;
    private String comprehendPartBExp;
}