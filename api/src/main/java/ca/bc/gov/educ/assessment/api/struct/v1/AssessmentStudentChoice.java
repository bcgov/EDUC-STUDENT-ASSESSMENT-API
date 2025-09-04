package ca.bc.gov.educ.assessment.api.struct.v1;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.io.Serial;
import java.io.Serializable;

@EqualsAndHashCode(callSuper = true)
@Data
@SuperBuilder
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class AssessmentStudentChoice extends BaseRequest implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private String assessmentStudentChoiceID;
    private String assessmentStudentComponentID;
    private String assessmentChoiceID;
}
