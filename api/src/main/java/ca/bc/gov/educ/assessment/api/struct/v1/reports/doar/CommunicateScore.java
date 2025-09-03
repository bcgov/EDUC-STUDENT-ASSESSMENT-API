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
public class CommunicateScore {
    private String level;
    private String communicateGraphicOrg;
    private String communicateUnderstanding;
    private String communicatePersonalConn;
    //ltp10, ltf12
    private String comprehendPartAShort;
    //ltf12
    private String dissertationBackground;
    private String dissertationForm;
}