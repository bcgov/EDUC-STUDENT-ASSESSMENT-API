package ca.bc.gov.educ.assessment.api.struct.v1;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.io.Serializable;

@Data
@SuperBuilder
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class AssessmentStudentDetailResponse implements Serializable {
    private static final long serialVersionUID = 1L;

    private boolean hasPriorRegistration;

    private String numberOfAttempts;

    private boolean alreadyWrittenAssessment;

}
