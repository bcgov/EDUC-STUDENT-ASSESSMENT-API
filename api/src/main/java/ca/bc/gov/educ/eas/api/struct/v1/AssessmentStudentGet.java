package ca.bc.gov.educ.eas.api.struct.v1;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.io.Serializable;

@Data
@SuperBuilder
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class AssessmentStudentGet implements Serializable {
    private static final long serialVersionUID = 1L;

    @NotBlank(message = "assessmentID cannot be null")
    private String assessmentID;

    @NotBlank(message = "studentID cannot be null")
    private String studentID;


}
