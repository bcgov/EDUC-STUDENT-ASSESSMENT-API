package ca.bc.gov.educ.assessment.api.struct.v1;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.io.Serializable;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class AssessmentStudentValidationIssue extends BaseStudentValidationIssue implements Serializable {
    private static final long serialVersionUID = 1L;

    private String assessmentStudentID;

    private String validationLabel;

    private String validationMessage;
}
