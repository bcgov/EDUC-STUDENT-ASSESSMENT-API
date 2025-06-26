package ca.bc.gov.educ.assessment.api.struct.external.grad.v1;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = false)
@JsonIgnoreProperties(ignoreUnknown = true)
public class GradStudentRecord {

    private String studentID;
    private String exception;
    private String program;
    private String programCompletionDate;
    private String schoolOfRecordId;
    private String schoolAtGradId;
    private String studentStatusCode;
    private String graduated;

}
