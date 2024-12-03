package ca.bc.gov.educ.eas.api.struct.v1;

import ca.bc.gov.educ.eas.api.struct.v1.BaseStudentValidationIssue;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotNull;
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

    private String assessmentStudentValidationIssueID;

    @NotNull(message = "assessmentStudentID cannot be null")
    private String assessmentStudentID;
}
