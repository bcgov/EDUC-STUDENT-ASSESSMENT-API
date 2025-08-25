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
public class CommunicateOralScore {
    private String level;
    private String communicateOralPart1;
    private String communicateOralPart2;
    private String communicateOralPart3;
    //ltf12
    private String communicateOralPart1Background;
    private String communicateOralPart1Form;
    private String communicateOralPart1Expression;
    private String communicateOralPart2Background;
    private String communicateOralPart2Form;
    private String communicateOralPart2Expression;
}